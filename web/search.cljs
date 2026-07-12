;; In-browser search over the candidacy board -- ClojureScript run by
;; scittle (no build step, no hand-written .js), same pattern as the
;; sibling demo pages. Data is the JSON the generator embedded from the
;; REAL post-run employmentops.store.
(ns placement.board)

(def candidacies
  (js->clj (js/JSON.parse (.-textContent (js/document.getElementById "board-data")))
           :keywordize-keys true))

(defn- esc [s]
  (-> (str s)
      (.replaceAll "&" "&amp;")
      (.replaceAll "<" "&lt;")
      (.replaceAll ">" "&gt;")))

(defn- card-html [c]
  (str "<div class=\"card\">"
       "<h3>" (esc (:candidate c)) "</h3>"
       "<div class=\"meta\">" (esc (:job c)) " · " (esc (:jurisdiction c)) "</div>"
       "<div class=\"meta\">" (esc (:salary c)) " <span class=\"meta\">(独立再計算一致)</span></div>"
       (when (:matched c)
         (str "<span class=\"chip\">マッチ " (esc (:matched c)) "</span>"))
       (when (:placed c)
         (str "<span class=\"chip\">配置 " (esc (:placed c)) "</span>"))
       "</div>"))

(defn- matches? [c q]
  (or (= q "")
      (.includes (.toLowerCase (str (:candidate c) " " (:job c) " " (:id c))) q)))

(defn- render! []
  (let [q (.toLowerCase (.-value (js/document.getElementById "q")))
        hits (filter #(matches? % q) candidacies)]
    (set! (.-innerHTML (js/document.getElementById "board"))
          (apply str (map card-html hits)))
    (set! (.-hidden (js/document.getElementById "empty")) (boolean (seq hits)))))

(.addEventListener (js/document.getElementById "q") "input" render!)
(render!)
