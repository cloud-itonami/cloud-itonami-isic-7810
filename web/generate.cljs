;; Generates docs/index.html (the GitHub Pages demo UI) from EDN/Hiccup via
;; kotoba-lang/html + kotoba-lang/css -- the fleet demo-page rule
;; (superproject ADR-2607122300): NOTHING on the page is hand-typed. This
;; script runs the FULL OperationActor StateGraph (EmploymentOps-LLM sealed
;; advisor -> Employment Agency Governor -> phase gate -> approval
;; interrupts -> commit|hold) for the same lifecycle `employmentops.sim`
;; walks -- two clean match+place candidacies, every HARD-hold kind and
;; both double-actuation guards -- then renders the post-run Store as the
;; candidacy board, the real refusal verdicts as the transparency table,
;; and the append-only ledger those runs actually wrote. In-browser search
;; is `search.cljs` run by scittle (ClojureScript in the browser, no
;; hand-written JS, no build step).
;;
;; Run (from this web/ directory, inside the monorepo checkout):
;;   ../../../../node_modules/.bin/nbb \
;;     --classpath "../src:../../../kotoba-lang/html/src:../../../kotoba-lang/css/src:../../../kotoba-lang/langchain/src:../../../kotoba-lang/langgraph/src" \
;;     generate.cljs
(require '[clojure.string :as cstr]
         '[html.core :as html]
         '[css.core :as css]
         '[langgraph.graph :as g]
         '[employmentops.store :as store]
         '[employmentops.operation :as op]
         '["fs" :as fs])

(def db (store/seed-db))
(def actor (op/build db))
(def operator {:actor-id "op-1" :actor-role :agency-operator :phase 3})

(defn- exec!
  "One supervised actor run, mirroring employmentops.sim: if the graph
  interrupts for human approval, the agency operator approves and the
  run resumes."
  [tid request]
  (let [r (g/run* actor {:request request :context operator} {:thread-id tid})]
    (if (= :interrupted (:status r))
      (g/run* actor {:approval {:status :approved :by "op-1"}}
              {:thread-id tid :resume? true})
      r)))

;; -- the build-time lifecycle (same walk as employmentops.sim) ---------------

;; clean lifecycles: candidacy-1 (no work authorization required) and
;; candidacy-6 (work authorization required AND verified) go intake ->
;; assess -> match -> place, every actuation through a human approval.
(doseq [[tid cid] [["a1" "candidacy-1"] ["a6" "candidacy-6"]]]
  (exec! (str tid "-assess") {:op :jurisdiction/assess :subject cid})
  (exec! (str tid "-match") {:op :candidacy/match :subject cid})
  (exec! (str tid "-place") {:op :candidacy/place :subject cid}))

(defn- violations-of [run] (get-in run [:state :verdict :violations]))

(def held
  (let [no-spec (exec! "h2" {:op :jurisdiction/assess :subject "candidacy-2" :no-spec? true})
        _ (exec! "h4-assess" {:op :jurisdiction/assess :subject "candidacy-4"})
        discriminatory (exec! "h4" {:op :candidacy/match :subject "candidacy-4"})
        _ (exec! "h3-assess" {:op :jurisdiction/assess :subject "candidacy-3"})
        _ (exec! "h3-match" {:op :candidacy/match :subject "candidacy-3"})
        fee (exec! "h3" {:op :candidacy/place :subject "candidacy-3"})
        _ (exec! "h5-assess" {:op :jurisdiction/assess :subject "candidacy-5"})
        _ (exec! "h5-match" {:op :candidacy/match :subject "candidacy-5"})
        authz (exec! "h5" {:op :candidacy/place :subject "candidacy-5"})
        double-match (exec! "g1" {:op :candidacy/match :subject "candidacy-1"})
        double-place (exec! "g2" {:op :candidacy/place :subject "candidacy-1"})]
    [{:cid "candidacy-2" :violations (violations-of no-spec) :note "法域アセスメント時点で拒否"}
     {:cid "candidacy-4" :violations (violations-of discriminatory)}
     {:cid "candidacy-3" :violations (violations-of fee)}
     {:cid "candidacy-5" :violations (violations-of authz)}
     {:cid "candidacy-1" :violations (violations-of double-match) :note "二重マッチングの試行"}
     {:cid "candidacy-1" :violations (violations-of double-place) :note "二重配置の試行"}]))

;; -- post-run state -----------------------------------------------------------

(def candidacies (store/all-candidacies db))
(def ledger (store/ledger db))

(defn ledger-line [{:keys [t op subject disposition basis]}]
  (cstr/join " · " [(name t) (str "op=" op) (str "subject=" subject)
                    (str "disposition=" (name disposition))
                    (str "basis=" (pr-str basis))]))

(def yen (js/Intl.NumberFormat. "ja-JP"))

(def stylesheet
  (css/style-node
   {:rules
    {":root" {:--fg "#1b1f24" :--bg "#ffffff" :--muted "#57606a"
              :--card "#f6f8fa" :--line "#d0d7de" :--accent "#0b5cad"
              :--ok-bg "#dafbe1" :--ok-fg "#116329"
              :--hold-bg "#ffebe9" :--hold-fg "#a40e26"}
     "body" {:font-family "system-ui,-apple-system,'Hiragino Sans','Noto Sans JP',sans-serif"
             :margin "0 auto" :max-width 880 :padding "28px 20px 48px"
             :color "var(--fg)" :background "var(--bg)" :line-height 1.55}
     "header p.sub" {:color "var(--muted)" :margin-top 4}
     "h1"   {:font-size 24 :margin "0"}
     "h2"   {:font-size 17 :margin-top 40 :border-top "1px solid var(--line)"
             :padding-top 24}
     ".search" {:display :flex :gap 8 :margin-top 20}
     "input#q" {:flex 1 :font-size 16 :padding "10px 14px"
                :border "1.5px solid var(--line)" :border-radius 8
                :background "var(--bg)" :color "var(--fg)"}
     "#board" {:display :grid :grid-template-columns "repeat(auto-fill,minmax(250px,1fr))"
               :gap 12 :margin-top 12}
     ".card" {:background "var(--card)" :border "1px solid var(--line)"
              :border-radius 10 :padding "14px 16px"}
     ".card h3" {:margin "0 0 2px" :font-size 16}
     ".card .meta" {:color "var(--muted)" :font-size 13.5}
     ".badge" {:display :inline-block :font-size 12 :font-weight 600
               :border-radius 20 :padding "2px 10px" :margin-left 6
               :vertical-align "1px"}
     ".badge.ok" {:background "var(--ok-bg)" :color "var(--ok-fg)"}
     ".badge.hold" {:background "var(--hold-bg)" :color "var(--hold-fg)"}
     ".chip" {:display :inline-block :font-size 12 :color "var(--muted)"
              :border "1px solid var(--line)" :border-radius 20
              :padding "1px 9px" :margin-right 6 :margin-top 4}
     "#empty" {:color "var(--muted)" :margin-top 16}
     "table" {:border-collapse :collapse :width "100%" :margin-top 12
              :font-size 13.5}
     "th" {:text-align :left :color "var(--muted)" :font-weight 600
           :border-bottom "1.5px solid var(--line)" :padding "6px 8px"}
     "td" {:border-bottom "1px solid var(--line)" :padding "7px 8px"
           :vertical-align :top}
     "pre" {:background "var(--card)" :border "1px solid var(--line)"
            :border-radius 8 :padding "10px 12px" :overflow-x :auto
            :font-size 12.5 :line-height 1.7}
     "footer" {:margin-top 48 :padding-top 16 :border-top "1px solid var(--line)"
               :color "var(--muted)" :font-size 13.5}
     "a" {:color "var(--accent)"}
     "code" {:background "var(--card)" :padding "1px 5px" :border-radius 4
             :font-size "0.9em"}}
    :media
    {"(prefers-color-scheme: dark)"
     {":root" {:--fg "#e6edf3" :--bg "#0d1117" :--muted "#8d96a0"
               :--card "#161b22" :--line "#30363d" :--accent "#58a6ff"
               :--ok-bg "#12261e" :--ok-fg "#3fb950"
               :--hold-bg "#2d1215" :--hold-fg "#f85149"}}}}))

(defn candidacy->json-entry [c]
  {:id (:id c) :candidate (:candidate c) :job (:job-title c)
   :jurisdiction (:jurisdiction c)
   :salary (str "年収 ¥" (.format yen (:annual-salary c))
                " × " (int (* 100 (:fee-rate c))) "% = 手数料 ¥"
                (.format yen (long (:claimed-fee c))))
   :matched (:match-number c)
   :placed (:placement-number c)})

(def page
  [:html {:lang "ja"}
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
    [:title "Placement Desk — governed employment agency (cloud-itonami-isic-7810)"]
    [:meta {:name "description"
            :content "地域職業紹介の governed OSS 実装デモ。差別的マッチング・手数料不一致・就労資格未確認は独立ガバナーが人間の承認でも覆せない HOLD にする。"}]
    stylesheet]
   [:body
    [:header
     [:h1 "Placement Desk " [:span.badge.ok "governed"]]
     [:p.sub "職業紹介デスク — マッチングと配置は必ず人間の承認を経て、独立ガバナーの HARD check"
      "(差別的マッチング基準・紹介手数料の独立再計算・就労資格確認)を人間の承認でも覆せない。 "
      [:a {:href "https://github.com/cloud-itonami/cloud-itonami-isic-7810"} "cloud-itonami-isic-7810"]
      " のライブデモ(合成データ)。このページの内容はすべて、生成時に実 actor(StateGraph + Governor)を実行した結果です。"
      "求人票の収集・掲載側は姉妹デモ "
      [:a {:href "/cloud-itonami-isic-6399/"} "Meta Job Search"] " が担います。"]]

    [:div.search
     [:input {:id "q" :type "search" :placeholder "候補者・職種で検索…" :autocomplete "off"}]]
    [:div {:id "board"}]
    [:p {:id "empty" :hidden true} "該当する candidacy はありません。"]

    [:h2 "Governor transparency — 拒否されたマッチング/配置"]
    [:p "この表はハードコードではなく、ページ生成時に実際の OperationActor へマッチング/配置を"
     "試行させ、"
     [:a {:href "https://github.com/cloud-itonami/cloud-itonami-isic-7810/blob/main/src/employmentops/governor.cljc"}
      "Employment Agency Governor"]
     " が拒否した実判定です(HARD violation は人間の承認でも覆せません)。"]
    [:table
     [:thead [:tr [:th "candidacy"] [:th "HARD check"] [:th "理由"]]]
     (into [:tbody]
           (for [{:keys [cid violations note]} held
                 :let [c (store/candidacy db cid)]]
             [:tr
              [:td [:strong (:candidate c)] [:br]
               [:span.meta (:job-title c) " · " (:jurisdiction c) " · " cid
                (when note (str " · " note))]]
              [:td (into [:span] (for [v violations] [:span [:span.badge.hold (name (:rule v))] " "]))]
              [:td (cstr/join " / " (map :detail violations))]]))]

    [:h2 "監査台帳 — 上の全実行が実際に書いた追記専用レコード"]
    [:p "intake・アセスメント・マッチング・配置・拒否のすべてが不変の台帳に残ります。"
     "以下はページ生成時の実 actor 実行が書いた事実そのものです。"]
    (into [:pre] [(cstr/join "\n" (map ledger-line ledger))])

    [:h2 "この紹介デスクが保証すること"]
    [:ul
     [:li "マッチング基準に保護属性が使われた candidacy は載らない(" [:strong "公正性"] " — 均等法5条 / Title VII / Equality Act / AGG)"]
     [:li "紹介手数料は「年収 × 手数料率」の独立再計算と常に一致する"]
     [:li "就労資格の確認を要する候補者は、未確認のまま配置されない(入管法 / I-9 / right-to-work / AufenthG §4a)"]
     [:li "マッチングも配置も、どの phase でも自動実行されない — 常に人間の承認"]
     [:li "すべての決定が追記専用の監査台帳に残る"]]

    [:footer
     [:p "OSS (AGPL-3.0-or-later)。fork して地域の職業紹介デスクとして運営できます — "
      [:a {:href "https://github.com/cloud-itonami/cloud-itonami-isic-7810/blob/main/docs/business-model.md"} "business model"]
      " · "
      [:a {:href "https://github.com/cloud-itonami/cloud-itonami-isic-7810/blob/main/docs/operator-guide.md"} "operator guide"]
      " · 姉妹デモ: "
      [:a {:href "/cloud-itonami-isic-6399/"} "Meta Job Search"] " / "
      [:a {:href "/cloud-itonami-isic-6310/"} "Talent Board"]
      " · " [:a {:href "/"} "fleet catalog"]
      "。このページは " [:code "web/generate.cljs"] " (nbb) が実 actor を実行して生成し、検索は "
      [:code "search.cljs"] " (scittle = ブラウザ内 ClojureScript) が実行しています。"]]

    [:script {:type "application/json" :id "board-data"}
     [:hiccup/raw (js/JSON.stringify (clj->js (mapv candidacy->json-entry candidacies)))]]
    [:script {:src "https://cdn.jsdelivr.net/npm/scittle@0.6.22/dist/scittle.js"}]
    [:script {:type "application/x-scittle" :src "search.cljs"}]]])

(fs/mkdirSync "../docs" #js {:recursive true})
(fs/writeFileSync "../docs/index.html" (str "<!doctype html>\n" (html/render page) "\n"))
(fs/copyFileSync "search.cljs" "../docs/search.cljs")
(println (str "wrote docs/index.html (" (count candidacies) " candidacies, "
              (count held) " holds, ledger " (count ledger) " facts)"))
