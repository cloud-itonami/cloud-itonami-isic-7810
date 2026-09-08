;; Generates docs/index.html (the GitHub Pages demo UI) from EDN/Hiccup via
;; kotoba-lang/html + kotoba-lang/jp-go-digital-design-system -- the fleet demo-page rule
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
;;
;; UI は デジタル庁デザインシステム(DADS)を kotoba-lang/jp-go-digital-design-system
;; 経由で使う(superproject ADR-2607261600)。この actor は職業安定法・均等法・
;; 入管法等の労働法規をソフトウェアとして実装しており、日本の公的サービスの
;; 視覚言語に揃える方が利用者の信頼判断に効く。DADS は light mode 固定
;; (上流に dark palette が無い)なので、移行前の prefers-color-scheme による
;; dark 対応は意図的に落としている。
;;
;; Run (from this web/ directory, inside the monorepo checkout):
;;   ../../../../node_modules/.bin/nbb \
;;     --classpath "../src:../../../kotoba-lang/html/src:../../../kotoba-lang/jp-go-digital-design-system/src:../../../kotoba-lang/langchain/src:../../../kotoba-lang/langgraph/src" \
;;     generate.cljs
;;
;; dds.css の読み込みパスは環境変数 JP_GO_DDS_CSS で上書きできる
;; (CI / worktree など monorepo 以外のレイアウト用)。
(require '[kotoba.lang.text :as cstr]
         '[css.core :as css]
         '[jp-go-dds.core :as dds]
         '[jp-go-dds.page :as page]
         '[langgraph.graph :as g]
         '[employmentops.employmentopsllm :as llm]
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

;; the receiving side of the 6399 handoff (superproject ADR-2607131000 /
;; this repo's ADR-0002): a candidacy that arrived via the job board's
;; application referral JPN-REF-000000 -- the SAME record id the Meta Job
;; Search demo shows leaving its ledger -- ingested through the real
;; :candidacy/intake op (auto-commits when governor-clean at phase 3).
(exec! "t0" {:op :candidacy/intake :subject "candidacy-7"
             :patch {:id "candidacy-7" :candidate "Minato Sora" :job-title "Line Cook"
                     :annual-salary 2900000 :fee-rate 0.2 :claimed-fee 580000.0
                     :matching-criteria-discriminatory? false
                     :requires-work-authorization? false :work-authorization-verified? false
                     :matched? false :placed? false
                     :jurisdiction "JPN" :status :intake
                     :referral-id "JPN-REF-000000"}})

;; clean lifecycles: candidacy-1 (no work authorization required),
;; candidacy-6 (work authorization required AND verified) and the
;; referred candidacy-7 go intake -> assess -> match -> place, every
;; actuation through a human approval.
(doseq [[tid cid] [["a1" "candidacy-1"] ["a6" "candidacy-6"] ["a7" "candidacy-7"]]]
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

;; check 7 analog (rationale-suspect, SOFT — ported from talent.policy):
;; a fresh candidacy goes through clean intake+assess, then a
;; deliberately biased advisor proposes the match with CLEAN record
;; flags and 「女性で…」 in the free-text rationale — the live-model
;; failure mode. It escalates; the human REJECTS.
(exec! "s8-intake" {:op :candidacy/intake :subject "candidacy-8"
                    :patch {:id "candidacy-8" :candidate "Sora Aoi" :job-title "Warehouse Associate"
                            :annual-salary 3100000 :fee-rate 0.2 :claimed-fee 620000.0
                            :matching-criteria-discriminatory? false
                            :requires-work-authorization? false :work-authorization-verified? false
                            :matched? false :placed? false
                            :jurisdiction "JPN" :status :intake}})
(exec! "s8-assess" {:op :jurisdiction/assess :subject "candidacy-8"})

(def suspect-run
  (let [mock (llm/mock-advisor)
        suspect (reify llm/Advisor
                  (-advise [_ st req]
                    (if (= :candidacy/match (:op req))
                      {:summary "candidacy-8 マッチング提案"
                       :rationale "女性で家庭もあるため負荷の低い職種が妥当と判断。"
                       :cites ["candidacy-8"]
                       :effect :candidacy/mark-matched
                       :value {:candidacy-id "candidacy-8"}
                       :stake nil
                       :confidence 0.9}
                      (llm/-advise mock st req))))
        actor2 (op/build db {:advisor suspect})
        r (g/run* actor2 {:request {:op :candidacy/match :subject "candidacy-8"}
                          :context operator} {:thread-id "s8-match"})
        r2 (when (= :interrupted (:status r))
             (g/run* actor2 {:approval {:status :rejected :by "op-1"}}
                     {:thread-id "s8-match" :resume? true}))]
    {:escalated? (= :interrupted (:status r))
     :final (get-in (or r2 r) [:state :disposition])
     :suspect? (get-in r [:state :verdict :rationale-suspect?])}))

;; -- post-run state -----------------------------------------------------------

(def candidacies (store/all-candidacies db))
(def ledger (store/ledger db))

(defn ledger-line [{:keys [t op subject disposition basis]}]
  (cstr/join " · " [(name t) (str "op=" op) (str "subject=" subject)
                    (str "disposition=" (name disposition))
                    (str "basis=" (pr-str basis))]))

(def yen (js/Intl.NumberFormat. "ja-JP"))

(def dds-css-path
  (or (some-> js/process.env.JP_GO_DDS_CSS not-empty)
      "../../../kotoba-lang/jp-go-digital-design-system/resources/jp_go_dds/dds.css"))
(def dds-css (fs/readFileSync dds-css-path "utf8"))

;; ページ固有の微調整のみ。色は DADS token 参照で raw hex は書かない
;; (kotoba-uiux 規約)。レイアウトの土台は dds-ext-*(jp-go-dds.core/ext-css)。
(def app-rules
  [[".pd-header" {:padding-block "2.5rem 0"}]
   [".pd-header .dads-heading" {:margin "0 0 .5rem"}]
   [".pd-lead" {:color "var(--color-neutral-solid-gray-700)" :line-height 1.7
                :margin ".75rem 0 0"}]
   [".pd-pitch" {:margin-block "2rem"}]
   [".pd-pitch .dads-heading" {:margin "0 0 .75rem"}]
   [".pd-pitch p" {:margin "0 0 .75rem" :line-height 1.8}]
   [".pd-pitch .dads-table" {:margin-block "1rem"}]
   [".pd-ctarow" {:display "flex" :gap ".75rem" :flex-wrap "wrap" :margin-top "1.25rem"}]
   [".pd-fine" {:color "var(--color-neutral-solid-gray-600)" :font-size ".8125rem"
                :line-height 1.8 :margin-top "1rem"}]
   [".pd-search" {:max-width "32rem" :margin-bottom "1.5rem"}]
   [".dads-input-text__input" {:width "100%"}]
   ;; candidacy カードは search.cljs が実行時に注入する(dds-ext-card + pd-card)
   ["#board" {:display "grid"
              :grid-template-columns "repeat(auto-fill,minmax(16rem,1fr))"
              :gap "1rem" :margin-top "1rem"}]
   ["#board>*" {:min-width 0}]
   [".pd-card h3" {:margin "0 0 .35rem" :font-size "1rem"}]
   [".pd-card .meta" {:color "var(--color-neutral-solid-gray-600)"
                      :font-size ".8125rem" :line-height 1.7}]
   [".pd-card .chip" {:display "inline-block" :font-size ".75rem"
                      :margin ".35rem .35rem 0 0" :padding ".05rem .5rem"
                      :border-radius "1rem"
                      :border "1px solid var(--color-neutral-solid-gray-300)"
                      :color "var(--color-neutral-solid-gray-700)"}]
   [".pd-empty" {:color "var(--color-neutral-solid-gray-600)" :margin-top "1rem"}]
   [".pd-hold-rules>span" {:display "block" :margin-block ".15rem"}]
   ;; チップのラベルを途中で折り返さない。.dads-table は overflow-x:auto。
   [".dads-table .dads-chip-label" {:white-space "nowrap"}]
   ;; 台帳は等幅。横に長いので自身の中でだけ横スクロールさせる
   ["pre" {:font-family "var(--font-family-mono)" :font-size ".8125rem"
           :line-height 1.7 :background "var(--color-neutral-solid-gray-50)"
           :border "1px solid var(--color-neutral-solid-gray-200)"
           :border-radius 8 :padding "1rem" :overflow-x "auto" :margin-top "1rem"}]
   [".pd-guarantees" {:line-height 1.9 :padding-left "1.25rem" :margin 0}]
   [".pd-footer" {:border-top "1px solid var(--color-neutral-solid-gray-200)"
                  :margin-top "3rem" :padding-block "1.5rem 3rem"
                  :color "var(--color-neutral-solid-gray-600)"
                  :font-size ".875rem" :line-height 1.8}]
   [".pd-footer p" {:margin "0 0 .75rem"}]
   [".pd-footer .cta" {:font-size ".9375rem" :font-weight 700
                       :color "var(--color-neutral-solid-gray-900)"}]
   ["code" {:font-family "var(--font-family-mono)"
            :background "var(--color-neutral-solid-gray-50)"
            :border "1px solid var(--color-neutral-solid-gray-200)"
            :border-radius 4 :padding "1px 5px" :font-size ".9em"}]])

(def app-css (css/css {:rules app-rules}))

;; 判定バッジは DADS chip-label(filled-1)。
(defn- chip [label color] (dds/chip-label label {:color color :style "filled-1"}))

(defn candidacy->json-entry [c]
  {:id (:id c) :candidate (:candidate c) :job (:job-title c)
   :jurisdiction (:jurisdiction c)
   :referral (:referral-id c)
   :salary (str "年収 ¥" (.format yen (:annual-salary c))
                " × " (int (* 100 (:fee-rate c))) "% = 手数料 ¥"
                (.format yen (long (:claimed-fee c))))
   :matched (:match-number c)
   :placed (:placement-number c)})

(def body
  (dds/container
   [:header {:class "pd-header"}
    (dds/heading 1 [:span "Placement Desk " (chip "governed" "green")])
    [:p {:class "pd-lead"}
     "職業紹介デスク — マッチングと配置は必ず人間の承認を経て、独立ガバナーの HARD check"
     "(差別的マッチング基準・紹介手数料の独立再計算・就労資格確認)を人間の承認でも覆せない。 "
     [:a {:href "https://github.com/cloud-itonami/cloud-itonami-isic-7810"} "cloud-itonami-isic-7810"]
     " のライブデモ(合成データ)。このページの内容はすべて、生成時に実 actor(StateGraph + Governor)を実行した結果です。"
     "求人票の収集・掲載側は姉妹デモ "
     [:a {:href "/cloud-itonami-isic-6399/"} "Meta Job Search"] " が担います。"]]

   [:div {:class "pd-pitch"}
    (dds/card
     (dds/heading 2 "今お使いのATS、1席いくらですか?" {:size "24"})
     [:p "Crelate・JobAdder・Zoho Recruit・Bullhorn — 主要ATSはすべて「採用担当者1人あたり」の"
      "席数課金です。スタッフが増えるほど、費用も比例して膨らみます。このデスクは"
      [:strong " 席数無制限・定額 ¥80,000/月"] "。3〜5人の小規模紹介事業所であれば、多くの場合"
      "既存ATSの実支出(¥22,000〜90,000+/月、上位プランはさらに高額)を下回ります。"]
     (dds/table
      {:headers ["ATS" "課金方式" "実勢価格"]
       :rows [["Crelate" "1席課金(5席から)" "$119/席/月〜"]
              ["JobAdder" "1席課金" "$99〜160/席/月(参考値)"]
              ["Zoho Recruit (Staffing)" "1席課金" "$25〜75/席/月"]
              ["Bullhorn" "1席課金" "$99〜315/席/月(参考値)"]
              [[:strong "このデスク"] [:strong "席数無制限・定額"] [:strong "¥80,000/月"]]]})
     [:p "さらに、就労資格(入管法 / I-9 / right-to-work / AufenthG §4a)の未確認配置を"
      [:strong "独立ガバナーが構造的にブロック"] "— 4社とも、これを覆せないハード制御としては"
      "実装していません。設定ミスでも、承認者の見落としでも、未確認配置が素通りしません。"]
     [:div {:class "pd-ctarow"}
      (dds/button "🡒 Managed Placement Desk を購読(¥80,000/月)"
                  {:type :solid-fill :size "lg"
                   :href "https://buy.stripe.com/3cIcN474ncW48yA0VNbMQ0d"})
      (dds/button "自前運用(セルフホスト)に興味がある"
                  {:type :outline :size "lg"
                   :href "https://github.com/cloud-itonami/cloud-itonami-isic-7810/issues/new?template=operator-interest.yml"})]
     [:p {:class "pd-fine"} "価格根拠: "
      [:a {:href "https://github.com/com-junkawasaki/root/blob/main/90-docs/pricing-intelligence/pricing-intelligence-ledger.edn"}
       "4社の実競合調査(2026-07-16)"]
      " — 下の技術デモは合成データによる実 actor 実行結果、この価格比較表とは独立して生成されています。"])]

   (dds/section
    {:title "candidacy ボード"}
    [:div {:class "pd-search"}
     (dds/form-field
      {:label "検索" :for "q"}
      (dds/input-text {:id "q" :type "search" :autocomplete "off"
                       :placeholder "候補者・職種で検索…"}))]
    [:div {:id "board"}]
    [:p {:id "empty" :class "pd-empty" :hidden true} "該当する candidacy はありません。"])

   (dds/section
    {:title "Governor transparency — 拒否されたマッチング/配置"}
    [:p {:class "pd-lead"}
     "この表はハードコードではなく、ページ生成時に実際の OperationActor へマッチング/配置を"
     "試行させ、"
     [:a {:href "https://github.com/cloud-itonami/cloud-itonami-isic-7810/blob/main/src/employmentops/governor.cljc"}
      "Employment Agency Governor"]
     " が拒否した実判定です(HARD violation は人間の承認でも覆せません)。"]
    (dds/table
     {:headers ["candidacy" "HARD check" "理由"]
      :rows (for [{:keys [cid violations note]} held
                  :let [c (store/candidacy db cid)]]
              [[:span [:strong (:candidate c)] [:br]
                [:span {:class "meta"} (:job-title c) " · " (:jurisdiction c) " · " cid
                 (when note (str " · " note))]]
               (into [:span {:class "pd-hold-rules"}]
                     (for [v violations] [:span (chip (name (:rule v)) "red")]))
               (cstr/join " / " (map :detail violations))])}))

   (dds/section
    {:title "check 7 — rationale-suspect (SOFT, 実LLM対策)"}
    [:p {:class "pd-lead"}
     "記録フラグはクリーンなまま、自由記述の rationale に「女性で…」と書くマッチング提案"
     "(実 LLM の失敗様式)は、HARD hold でなく人間レビューへ回ります(自由文の語句照合は"
     "誤検知があるため、コストを『抑圧』でなく『人間の一瞥』に固定する設計)。この実行では: "
     "escalated=" (str (:escalated? suspect-run))
     " / rationale-suspect=" (str (:suspect? suspect-run))
     " / 人間が却下 → " (name (:final suspect-run)) "。"])

   (dds/section
    {:title "監査台帳 — 上の全実行が実際に書いた追記専用レコード"}
    [:p {:class "pd-lead"}
     "intake・アセスメント・マッチング・配置・拒否のすべてが不変の台帳に残ります。"
     "以下はページ生成時の実 actor 実行が書いた事実そのものです。"]
    [:pre (cstr/join "\n" (map ledger-line ledger))])

   (dds/section
    {:title "この紹介デスクが保証すること"}
    [:ul {:class "pd-guarantees"}
     [:li "マッチング基準に保護属性が使われた candidacy は載らない(" [:strong "公正性"] " — 均等法5条 / Title VII / Equality Act / AGG)"]
     [:li "紹介手数料は「年収 × 手数料率」の独立再計算と常に一致する"]
     [:li "就労資格の確認を要する候補者は、未確認のまま配置されない(入管法 / I-9 / right-to-work / AufenthG §4a)"]
     [:li "マッチングも配置も、どの phase でも自動実行されない — 常に人間の承認"]
     [:li "すべての決定が追記専用の監査台帳に残る"]])

   [:footer {:class "pd-footer"}
    [:p {:class "cta"}
     [:a {:href "https://github.com/cloud-itonami/cloud-itonami-isic-7810/issues/new?template=operator-interest.yml"}
      "🡒 地域でこの職業紹介デスクを運営したい方はこちら(operator-interest)"]]
    [:p "OSS (AGPL-3.0-or-later)。fork して地域の職業紹介デスクとして運営できます — "
     [:a {:href "https://github.com/cloud-itonami/cloud-itonami-isic-7810/blob/main/docs/business-model.md"} "business model"]
     " · "
     [:a {:href "https://github.com/cloud-itonami/cloud-itonami-isic-7810/blob/main/docs/operator-guide.md"} "operator guide"]
     " · 姉妹デモ: "
     [:a {:href "/cloud-itonami-isic-6399/"} "Meta Job Search"] " / "
     [:a {:href "/cloud-itonami-isic-6310/"} "Talent Board"]
     " · " [:a {:href "/"} "fleet catalog"]
     "。このページは " [:code "web/generate.cljs"] " (nbb) が実 actor を実行して生成し、検索は "
     [:code "search.cljs"] " (scittle = ブラウザ内 ClojureScript) が実行しています。"]]))

;; script は html.core の raw-text tag。子は素の文字列で渡す。
(def scripts
  [[:script {:type "application/json" :id "board-data"}
    (js/JSON.stringify (clj->js (mapv candidacy->json-entry candidacies)))]
   [:script {:src "https://cdn.jsdelivr.net/npm/scittle@0.6.22/dist/scittle.js"}]
   ;; search.cljs は hiccup を html.core で文字列化する(生 HTML を書かない)ので、
   ;; そのライブラリもブラウザへ同梱する。読み込み順は依存順。
   [:script {:type "application/x-scittle" :src "html_core.cljs"}]
   [:script {:type "application/x-scittle" :src "search.cljs"}]])

(fs/mkdirSync "../docs" #js {:recursive true})
(fs/writeFileSync
 "../docs/index.html"
 (str (page/->page
       {:title "Bullhorn・Crelate 代替 ATS — 席数無制限 定額 ¥80,000/月 | Placement Desk (cloud-itonami-isic-7810)"
        :description "地域職業紹介向けATS。Bullhorn・Crelate・JobAdder・Zoho Recruitはすべて1席課金 — このデスクは席数無制限の定額制で、就労資格未確認の配置は独立ガバナーが人間の承認でも覆せずHOLDする。"
        :lang "ja"
        :css dds-css
        :app-css app-css}
       body
       scripts)
      "\n"))
(fs/copyFileSync "search.cljs" "../docs/search.cljs")
;; ブラウザ側 .cljs はコピーするだけ(ビルド無し)。
(def html-root
  (or (some-> js/process.env.KOTOBA_HTML_ROOT not-empty) "../../../kotoba-lang/html"))
(fs/copyFileSync (str html-root "/src/html/core.cljc") "../docs/html_core.cljs")
(println (str "wrote docs/index.html (" (count candidacies) " candidacies, "
              (count held) " holds, ledger " (count ledger) " facts)"))
