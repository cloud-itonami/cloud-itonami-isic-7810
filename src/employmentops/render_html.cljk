(ns employmentops.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300):
  this repo previously shipped a 2,220-byte HAND-WRITTEN
  `docs/samples/operator-console.html` with NO generator behind it --
  and it was not even this repo's page (its `<title>` read
  `cloud-itonami · robotics`, while this repo is an employment
  placement agency, ISIC 78). That stub is replaced wholesale by this
  namespace: every id, number, rule name and status below is produced
  by actually executing the REAL actor stack at build time
  (`employmentops.operation` -> `employmentops.governor` ->
  `employmentops.store`), never hand-typed.

  The scenario uses this repo's OWN seeded candidacy ids
  (`candidacy-1`..`candidacy-6` in `employmentops.store/demo-data`,
  confirmed present BEFORE this file was written by running the real
  store), and reaches every disposition the actor can produce:

    - a clean phase-3 AUTO-COMMIT (`:candidacy/intake`, the only op in
      any phase's `:auto` set -- see `employmentops.phase`),
    - human-APPROVED escalations (`:jurisdiction/assess`, and
      `:candidacy/match`/`:candidacy/place`, which are permanently
      high-stakes and NEVER auto-commit at any phase -- the governor's
      `high-stakes` set and the phase gate agree on this
      independently),
    - and all SIX distinct HARD holds this governor can raise
      (`:no-spec-basis`, `:placement-fee-mismatch`,
      `:matching-basis-discriminatory`, `:work-authorization-
      unverified`, `:already-matched`, `:already-placed`). A HARD hold
      never reaches a human -- no approver can override it.

  `-main` THROWS if the run produced zero `:governor-hold` facts, so
  the HARD-hold requirement is a build-time invariant rather than a
  comment: a regression that silently stopped censoring proposals
  fails the build instead of quietly emitting a page that claims
  compliance.

  Rendering is deterministic: no timestamps, no randomness, no
  wall-clock in the page content, and money is printed through
  `money` (BigDecimal, fixed scale) rather than a locale-sensitive
  `format`, so two consecutive runs are byte-identical.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [kotoba.lang.text :as str]
            [employmentops.facts :as facts]
            [employmentops.store :as store]
            [employmentops.registry :as registry]
            [employmentops.operation :as op]
            [langgraph.graph :as g]))

(def ^:private operator
  {:actor-id "op-1" :actor-role :agency-operator :phase 3})

(defn- exec! [actor tid request]
  (g/run* actor {:request request :context operator} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}}
          {:thread-id tid :resume? true}))

(defn- approvals-in
  "The `:approval-granted` facts a resumed run produced. The commit
  node appends only its own `:committed` fact to the ledger, so the
  approver is NOT in `store/ledger` -- it exists only in the graph's
  `:audit` channel. Collecting it here is what lets the approvals
  section below name a real approver instead of leaving a blank."
  [run-result]
  (filter #(= :approval-granted (:t %)) (get-in run-result [:state :audit])))

(defn run-demo!
  "Runs a fresh seeded store through the scenario described in the ns
  docstring. Returns `{:db <store> :approvals [<approval-granted facts>]}`
  -- every field read by `render` is real governor/store output.

  candidacy-1 (JPN, clean, no work authorization required) clears a
  full lifecycle: intake auto-commits, then assessment, match and
  placement each escalate to a human who approves. candidacy-6 (JPN,
  work authorization REQUIRED and verified) clears the same lifecycle,
  demonstrating the conditional work-authorization check PASSING --
  the check is conditional on the candidacy's own
  `:requires-work-authorization?` ground truth, so a repo that only
  ever showed it failing would not have shown that it discriminates.

  Then the six HARD holds, each isolated on its own candidacy so the
  hold that fires is the one under test (the governor concatenates all
  violations, so an unassessed candidacy would additionally raise
  `:evidence-incomplete` and mask the check being demonstrated --
  hence the assessment that precedes each):
  candidacy-2 assessed against a deliberately unregistered
  jurisdiction (ATL) -> `:no-spec-basis`; candidacy-3 placed with a
  claimed fee that fails independent recompute ->
  `:placement-fee-mismatch`; candidacy-4 matched on criteria its own
  record flags as discriminatory -> `:matching-basis-discriminatory`;
  candidacy-5 placed while requiring but lacking work-authorization
  verification -> `:work-authorization-unverified`; and candidacy-1
  matched and placed a SECOND time -> `:already-matched` /
  `:already-placed`."
  []
  (let [db (store/seed-db)
        actor (op/build db)
        approvals (volatile! [])
        approve-and-record! (fn [tid]
                              (let [r (approve! actor tid)]
                                (vswap! approvals into (approvals-in r))
                                r))]
    ;; -- candidacy-1: full clean lifecycle -----------------------------
    (exec! actor "c1-intake" {:op :candidacy/intake :subject "candidacy-1"
                              :patch {:id "candidacy-1" :candidate "Kita Taro"}})

    (exec! actor "c1-assess" {:op :jurisdiction/assess :subject "candidacy-1"})
    (approve-and-record! "c1-assess")

    (exec! actor "c1-match" {:op :candidacy/match :subject "candidacy-1"})
    (approve-and-record! "c1-match")

    (exec! actor "c1-place" {:op :candidacy/place :subject "candidacy-1"})
    (approve-and-record! "c1-place")

    ;; -- candidacy-6: work authorization required AND verified ---------
    (exec! actor "c6-intake" {:op :candidacy/intake :subject "candidacy-6"
                              :patch {:id "candidacy-6" :candidate "Chuo Yuki"}})

    (exec! actor "c6-assess" {:op :jurisdiction/assess :subject "candidacy-6"})
    (approve-and-record! "c6-assess")

    (exec! actor "c6-match" {:op :candidacy/match :subject "candidacy-6"})
    (approve-and-record! "c6-match")

    (exec! actor "c6-place" {:op :candidacy/place :subject "candidacy-6"})
    (approve-and-record! "c6-place")

    ;; -- HARD hold 1: no official spec-basis for the jurisdiction ------
    (exec! actor "c2-assess" {:op :jurisdiction/assess :subject "candidacy-2" :no-spec? true})

    ;; -- HARD hold 2: claimed placement fee fails independent recompute -
    (exec! actor "c3-assess" {:op :jurisdiction/assess :subject "candidacy-3"})
    (approve-and-record! "c3-assess")
    (exec! actor "c3-match" {:op :candidacy/match :subject "candidacy-3"})
    (approve-and-record! "c3-match")
    (exec! actor "c3-place" {:op :candidacy/place :subject "candidacy-3"})

    ;; -- HARD hold 3: matching criteria flagged discriminatory ---------
    (exec! actor "c4-assess" {:op :jurisdiction/assess :subject "candidacy-4"})
    (approve-and-record! "c4-assess")
    (exec! actor "c4-match" {:op :candidacy/match :subject "candidacy-4"})

    ;; -- HARD hold 4: work authorization required but unverified -------
    (exec! actor "c5-assess" {:op :jurisdiction/assess :subject "candidacy-5"})
    (approve-and-record! "c5-assess")
    (exec! actor "c5-match" {:op :candidacy/match :subject "candidacy-5"})
    (approve-and-record! "c5-match")
    (exec! actor "c5-place" {:op :candidacy/place :subject "candidacy-5"})

    ;; -- HARD holds 5 & 6: double actuation ----------------------------
    (exec! actor "c1-match-again" {:op :candidacy/match :subject "candidacy-1"})
    (exec! actor "c1-place-again" {:op :candidacy/place :subject "candidacy-1"})

    {:db db :approvals @approvals}))

;; ----------------------------- rendering -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- money
  "Fixed-scale, locale-independent money rendering. `format \"%.2f\"`
  is locale-sensitive (a comma decimal separator in some locales would
  make the page non-reproducible across machines), so this goes
  through BigDecimal instead."
  [x]
  (if (number? x)
    (.toPlainString (.setScale (bigdec (double x)) 2 java.math.RoundingMode/HALF_UP))
    "—"))

(defn- op-code
  "The op rendered as the FULLY-QUALIFIED keyword it actually is.
  `name` would drop the namespace, collapsing `:jurisdiction/assess`
  to `assess` and `:candidacy/match` to `match` -- but the namespace is
  the part that says which lifecycle the op belongs to, and the action
  gate table states these ops in qualified form, so the run tables must
  match it."
  [op]
  (if (keyword? op) (str op) "—"))

(defn- last-fact-for [ledger candidacy-id]
  (last (filter #(= (:subject %) candidacy-id) ledger)))

(defn- status-cell [ledger candidacy-id]
  (let [f (last-fact-for ledger candidacy-id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (= :governor-hold (:t f))
      (str "<span class=\"critical\">HARD hold &middot; "
           (esc (name (or (-> f :violations first :rule) :unknown))) "</span>")
      (= :committed (:t f)) "<span class=\"ok\">committed</span>"
      :else "<span class=\"muted\">in progress</span>")))

(defn- lifecycle-cell [{:keys [matched? placed?]}]
  (cond
    placed? "<span class=\"ok\">matched &amp; placed</span>"
    matched? "<span class=\"warn\">matched, not yet placed</span>"
    :else "<span class=\"muted\">intake</span>"))

(defn- candidacy-row [ledger {:keys [id candidate job-title jurisdiction] :as c}]
  (format (str "        <tr><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td>"
               "<td class=\"amt\">%s</td><td>%s</td><td>%s</td></tr>")
          (esc id) (esc candidate) (esc job-title) (esc jurisdiction)
          (esc (money (:annual-salary c)))
          (lifecycle-cell c)
          (status-cell ledger id)))

(defn- fee-row
  "Claimed vs INDEPENDENTLY RECOMPUTED placement fee. Both numbers come
  from the candidacy's own record and `employmentops.registry`; the
  verdict column is `registry/placement-fee-matches-claim?` itself --
  the exact predicate the governor's `:placement-fee-mismatch` HARD
  check calls."
  [{:keys [id claimed-fee] :as c}]
  (let [recomputed (registry/compute-placement-fee c)
        ok? (registry/placement-fee-matches-claim? c)]
    (format (str "        <tr><td><code>%s</code></td><td class=\"amt\">%s</td>"
                 "<td class=\"amt\">%s</td><td class=\"amt\">%s</td>"
                 "<td class=\"amt\">%s</td><td>%s</td></tr>")
            (esc id)
            (esc (money (:annual-salary c)))
            (esc (str (:fee-rate c)))
            (esc (money claimed-fee))
            (esc (money recomputed))
            (if ok?
              "<span class=\"ok\">matches</span>"
              "<span class=\"critical\">MISMATCH &middot; HARD hold on :candidacy/place</span>"))))

(defn- hold-row [{:keys [op subject violations confidence]}]
  (let [{:keys [rule detail]} (first violations)]
    (format (str "        <tr><td><code>%s</code></td><td><code>%s</code></td>"
                 "<td><span class=\"critical\">%s</span></td><td>%s</td>"
                 "<td class=\"num\">%s</td></tr>")
            (esc subject) (esc (op-code op))
            (esc (name (or rule :unknown))) (esc detail)
            (esc (str confidence)))))

(defn- approver-cell
  "Approver attribution, DERIVED at render time rather than assumed.

  Measured in this repo: `store/commit-record!` reads `:payload` for
  the `:assessment/set` effect (so the approver IS retained on the
  assessment record), but for `:candidacy/mark-matched` /
  `:candidacy/mark-placed` it derives everything from `path` and reads
  neither `:value` nor `:payload` -- so the approver is NOT retained on
  the candidacy record. Rather than hard-code that split, this checks
  whether `:approved-by` is actually present on the record the op
  wrote. If the store is later fixed to persist the approver on
  actuations, this page self-corrects with no edit here; and where it
  genuinely is not retained the approver is still named, from the audit
  trail, and explicitly labelled -- a blank would read as `nobody
  approved`, which is false."
  [db {:keys [op subject by]}]
  (let [record (case op
                 :jurisdiction/assess (store/assessment-of db subject)
                 (store/candidacy db subject))
        retained? (contains? record :approved-by)]
    (if retained?
      (format "<span class=\"ok\">%s</span> <span class=\"muted\">(retained in record as <code>:approved-by</code>)</span>"
              (esc (:approved-by record)))
      (format "%s <span class=\"muted\">(audit only — not retained in record)</span>"
              (esc by)))))

(defn- approval-row [db {:keys [op subject] :as approval}]
  (format "        <tr><td><code>%s</code></td><td><code>%s</code></td><td>%s</td></tr>"
          (esc subject) (esc (op-code op))
          (approver-cell db approval)))

(defn- draft-row [r]
  (format (str "        <tr><td><code>%s</code></td><td>%s</td><td><code>%s</code></td>"
               "<td>%s</td><td>%s</td></tr>")
          (esc (get r "record_id")) (esc (get r "kind"))
          (esc (get r "candidacy_id")) (esc (get r "jurisdiction"))
          (if (get r "immutable")
            "<span class=\"ok\">immutable</span>"
            "<span class=\"warn\">mutable</span>")))

(defn- jurisdiction-row [iso3]
  (let [sb (facts/spec-basis iso3)]
    (format (str "        <tr><td><code>%s</code></td><td>%s</td><td>%s</td>"
                 "<td>%s</td><td class=\"num\">%s</td></tr>")
            (esc iso3) (esc (:name sb)) (esc (:owner-authority sb))
            (esc (:authorization-owner-authority sb))
            (esc (count (:required-evidence sb))))))

(defn- ledger-row [{:keys [t op subject disposition basis]}]
  (format (str "        <tr><td>%s</td><td><code>%s</code></td><td><code>%s</code></td>"
               "<td>%s</td><td>%s</td></tr>")
          (if (= :governor-hold t)
            (str "<span class=\"critical\">" (esc (name t)) "</span>")
            (str "<span class=\"ok\">" (esc (name t)) "</span>"))
          (esc (op-code op)) (esc subject)
          (esc (some-> disposition name))
          (esc (or (some->> basis (map name) (str/join ", ")) ""))))

(def ^:private action-gate-rows
  ;; Static description of this actor's own closed op contract
  ;; (README `Ops`, `employmentops.governor` / `employmentops.phase`) --
  ;; documentation of FIXED behaviour, not runtime telemetry, so it is
  ;; legitimately hand-described rather than derived from a live run.
  ;; Everything the run actually produced is in the tables above/below.
  ["        <tr><td><code>:candidacy/intake</code></td><td><span class=\"ok\">phase-3 auto-commit when governor-clean &middot; no candidate-facing risk</span></td></tr>"
   "        <tr><td><code>:jurisdiction/assess</code></td><td><span class=\"warn\">phase-3: human approval (not yet auto-eligible) &middot; official spec-basis citation required</span></td></tr>"
   "        <tr><td><code>:candidacy/match</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto at any phase &middot; matching criteria independently screened for a protected characteristic</span></td></tr>"
   "        <tr><td><code>:candidacy/place</code></td><td><span class=\"warn\">ALWAYS human approval &middot; never auto at any phase &middot; placement fee independently recomputed &amp; work authorization re-verified</span></td></tr>"])

(defn render
  "Renders the operator console from a `run-demo!` result. Pure --
  reads only the store and the collected approval facts."
  [{:keys [db approvals]}]
  (let [ledger (vec (store/ledger db))
        candidacies (store/all-candidacies db)
        holds (filter #(= :governor-hold (:t %)) ledger)]
    (str
     "<html lang=\"en\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
     "<title>cloud-itonami-isic-7810 &middot; employment-placement-agency</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Employment placement agency (ISIC 7810) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · candidate matching/placement always human-approved</span>\n"
     "</header>\n"
     "<main>\n"

     "  <section class=\"card\">\n"
     "    <h2>Candidacies</h2>\n"
     "    <p class=\"muted\">Build-time snapshot generated from <code>employmentops.store</code> by driving the real actor stack (<code>employmentops.operation</code> → <code>employmentops.governor</code> → <code>employmentops.store</code>) via <code>clojure -M:dev:render-html</code>. Every id, amount and status below is actor output, not sample text.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Candidacy</th><th>Candidate</th><th>Role</th><th>Jurisdiction</th><th>Annual salary</th><th>Lifecycle</th><th>Last op status</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map (partial candidacy-row ledger) candidacies)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Placement fee — independent recompute</h2>\n"
     "    <p class=\"muted\">The claimed fee is never trusted. <code>employmentops.registry/compute-placement-fee</code> recomputes it from the candidacy's own <code>annual-salary × fee-rate</code>, and <code>placement-fee-matches-claim?</code> — the exact predicate the governor's <code>:placement-fee-mismatch</code> HARD check calls — decides whether <code>:candidacy/place</code> may proceed.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Candidacy</th><th>Annual salary</th><th>Fee rate</th><th>Claimed fee</th><th>Recomputed</th><th>Verdict</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map fee-row candidacies)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Action gate (Employment Agency Governor)</h2>\n"
     "    <p class=\"muted\">HARD holds cannot be overridden by any approver. <code>:candidacy/match</code> and <code>:candidacy/place</code> are absent from every phase's auto set — the governor's high-stakes gate and the rollout phase gate enforce that independently, so two layers agree that a real match or placement is always a human call.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Op</th><th>Gate</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" action-gate-rows) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Governor HARD holds (this run)</h2>\n"
     "    <p class=\"muted\">Proposals the Employment Agency Governor refused. A HARD hold never reaches a human — there is no approval step to override. Rule names and details below are the governor's own output.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Candidacy</th><th>Op</th><th>Rule</th><th>Detail</th><th>Advisor confidence</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map hold-row holds)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Human approvals (this run)</h2>\n"
     "    <p class=\"muted\">Escalations a human operator resumed with <code>{:approval {:status :approved}}</code>. Whether the approver survives into the written record is <em>measured per record</em>, not assumed: <code>commit-record!</code> persists the approver for the <code>:assessment/set</code> effect (it reads <code>:payload</code>) but not for <code>:candidacy/mark-matched</code>/<code>:candidacy/mark-placed</code> (which derive from <code>path</code> and read neither <code>:value</code> nor <code>:payload</code>). Where it is not retained the approver is still named, from the audit trail, and labelled as such.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Candidacy</th><th>Op</th><th>Approver</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map (partial approval-row db) approvals)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Registry drafts — matches</h2>\n"
     "    <p class=\"muted\">Jurisdiction-scoped record drafts built by <code>employmentops.registry/register-match</code>. Every certificate this actor produces is <strong>unsigned</strong>: signing is the agency operator's act, not the actor's.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Record id</th><th>Kind</th><th>Candidacy</th><th>Jurisdiction</th><th>Immutability</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map draft-row (store/match-history db))) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Registry drafts — placements</h2>\n"
     "    <p class=\"muted\">Built by <code>employmentops.registry/register-placement</code> only after the governor cleared the placement AND a human approved it.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Record id</th><th>Kind</th><th>Candidacy</th><th>Jurisdiction</th><th>Immutability</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map draft-row (store/placement-history db))) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Jurisdiction spec-basis catalog</h2>\n"
     "    <p class=\"muted\">The official sources <code>employmentops.facts</code> is seeded with. A jurisdiction absent from this table has <strong>no</strong> spec-basis and the governor HARD-holds any proposal against it — that is exactly why the <code>ATL</code> assessment in the run above was refused. Coverage is reported honestly: this is a starting catalog, not a survey of all jurisdictions.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>ISO3</th><th>Jurisdiction</th><th>Anti-discrimination authority</th><th>Work-authorization authority</th><th>Required evidence items</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map jurisdiction-row (sort (keys facts/catalog)))) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "  <section class=\"card\">\n"
     "    <h2>Audit ledger (this run)</h2>\n"
     "    <p class=\"muted\">Append-only decision-fact log — every commit and hold this scenario produced, in order.</p>\n"
     "    <table>\n"
     "      <thead><tr><th>Fact</th><th>Op</th><th>Candidacy</th><th>Disposition</th><th>Basis</th></tr></thead>\n"
     "      <tbody>\n"
     (str/join "\n" (map ledger-row ledger)) "\n"
     "      </tbody>\n"
     "    </table>\n"
     "  </section>\n"

     "</main>\n"
     "<footer>\n"
     "  <p>Generated at build time by <code>employmentops.render-html</code> from a real actor run — no hand-written figures, no timestamps, byte-identical across reruns against the same seed. Regenerate with <code>clojure -M:dev:render-html</code>.</p>\n"
     "</footer>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [db approvals] :as result} (run-demo!)
        ledger (vec (store/ledger db))
        holds (filter #(= :governor-hold (:t %)) ledger)]
    ;; Build-time invariant, not a comment: a run that censored nothing
    ;; must not be allowed to produce a page claiming it did.
    (when (zero? (count holds))
      (throw (ex-info (str "render-html: the demo run produced 0 :governor-hold facts. "
                           "The operator console must demonstrate at least one HARD hold "
                           "that never reaches a human; refusing to emit a page that would "
                           "claim compliance the run did not exercise.")
                      {:ledger-facts (count ledger)
                       :holds 0
                       :fact-types (frequencies (map :t ledger))})))
    (spit out (render result))
    (println "wrote" out
             (str "(" (count ledger) " ledger facts, "
                  (count holds) " HARD holds ["
                  (str/join ", " (map #(name (-> % :violations first :rule)) holds))
                  "], " (count approvals) " human approvals, "
                  (count (store/match-history db)) " match drafts, "
                  (count (store/placement-history db)) " placement drafts)"))))
