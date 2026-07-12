;; Headless verification harness for docs/search.cljs -- stubs the DOM
;; surface, feeds it the REAL JSON block from the generated
;; docs/index.html, loads search.cljs, and asserts the rendered board,
;; a search interaction, and the page's build-time actor-run content.
;; nbb script, same pattern as the sibling demo harnesses.
;;
;; Run (from this web/ directory):
;;   ../../../../node_modules/.bin/nbb verify_search.cljs
(require '["fs" :as fs])

(def html (fs/readFileSync "../docs/index.html" "utf8"))

(def json-block
  (let [m (re-find #"<script type=\"application/json\" id=\"board-data\">(\[.*?\])</script>" html)]
    (or (second m) (throw (js/Error. "board-data JSON block not found")))))

(def listeners (atom {}))
(defn- el [id init]
  (let [o (js-obj)]
    (doseq [[k v] init] (aset o k v))
    (aset o "addEventListener" (fn [ev f] (swap! listeners assoc [id ev] f)))
    o))

(def elements
  {"board-data" (el "board-data" {"textContent" json-block})
   "q"          (el "q" {"value" ""})
   "board"      (el "board" {"innerHTML" ""})
   "empty"      (el "empty" {"hidden" true})})

(aset js/globalThis "document" (js-obj "getElementById" (fn [id] (get elements id))))
(load-string (fs/readFileSync "../docs/search.cljs" "utf8"))

(defn- board-html [] (aget (get elements "board") "innerHTML"))
(defn- assert! [ok? msg]
  (if ok? (println "ok  " msg) (do (println "FAIL" msg) (js/process.exit 1))))

;; board renders all six candidacies from the post-run store
(assert! (.includes (board-html) "Kita Taro") "board shows candidacy-1")
(assert! (.includes (board-html) "Chuo Yuki") "board shows candidacy-6")
(assert! (.includes (board-html) "マッチ JPN-MTC-000000") "candidacy-1 carries its real match number")
(assert! (.includes (board-html) "配置 JPN-PLC-000000") "candidacy-1 carries its real placement number")
(assert! (.includes (board-html) "JPN-PLC-000001") "candidacy-6 placed too (work-auth verified path)")

;; search interaction
(aset (get elements "q") "value" "cook")
((get @listeners ["q" "input"]))
(assert! (.includes (board-html) "Line Cook") "query 'cook' keeps the cooks")
(assert! (not (.includes (board-html) "Kita Taro")) "query 'cook' filters others out")
(aset (get elements "q") "value" "zzz")
((get @listeners ["q" "input"]))
(assert! (= "" (board-html)) "no-hit query renders no cards")
(assert! (false? (boolean (aget (get elements "empty") "hidden"))) "no-hit reveals empty notice")

;; the receiving side of the handoff (superproject ADR-2607131000 /
;; repo ADR-0002): the referred candidacy went through the full desk
(aset (get elements "q") "value" "")
((get @listeners ["q" "input"]))
(assert! (.includes (board-html) "Minato Sora") "referred candidacy-7 on the board")
(assert! (.includes (board-html) "紹介経由 JPN-REF-000000") "candidacy-7 carries the 6399 referral record id")
(assert! (.includes (board-html) "JPN-PLC-000002") "referred candidacy placed through the same governed lifecycle")

;; the transparency table carries the REAL run verdicts (all hold kinds)
(doseq [rule ["no-spec-basis" "matching-basis-discriminatory" "placement-fee-mismatch"
              "work-authorization-unverified" "already-matched" "already-placed"]]
  (assert! (.includes html rule) (str "hold rule '" rule "' present in transparency table")))

;; the audit ledger is the real append-only record
(assert! (.includes html "監査台帳") "audit ledger section present")
(assert! (.includes html "op=:candidacy/place") "ledger has placement facts")
(assert! (.includes html "basis=[:matching-basis-discriminatory]") "ledger has the discriminatory-match hold fact")

;; check 7 analog surfaced: suspect match escalated, human rejected
(assert! (.includes html "rationale-suspect") "check-7 section present")
(assert! (.includes html "escalated=true") "suspect match escalated (not auto, not hard-held)")
(assert! (.includes html "人間が却下 → hold") "human rejection recorded on the page")
(assert! (.includes (board-html) "Sora Aoi") "candidacy-8 on the board (unmatched)")

(println "verify_search: all assertions passed")
