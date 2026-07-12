(ns employmentops.store
  "SSoT for the community-employment-agency actor, behind a `Store`
  protocol so the backend is a swap, not a rewrite -- the same seam
  every prior `cloud-itonami-isic-*` actor in this fleet uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/employmentops/store_contract_test.clj), which is the whole
  point: the actor, the Employment Agency Governor and the audit
  ledger never know which SSoT they run on.

  Like `practiceops`/7110's own `commission`, the primary entity here
  is a `candidacy` -- candidate-matching and candidate-placement
  actuation events apply SEQUENTIALLY to the SAME candidacy record
  (match first, place later), matching the freight/quarry/agronomy/
  hospitality/practice cluster's own sequential entity shape.
  Dedicated double-actuation-guard booleans (`:matched?`/`:placed?`,
  never a `:status` value).

  The ledger stays append-only on every backend: 'which candidacy was
  screened for a discriminatory matching basis or an unverified work
  authorization, which candidate was matched, which candidate was
  placed, on what jurisdictional basis, approved by whom' is always a
  query over an immutable log -- the audit trail a community
  employment agency or workforce program trusting an operator needs,
  and the evidence an operator needs if a match or a placement is
  later disputed."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [employmentops.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (candidacy [s id])
  (all-candidacies [s])
  (assessment-of [s candidacy-id] "committed jurisdiction assessment, or nil")
  (ledger [s])
  (match-history [s] "the append-only candidate-matching history (employmentops.registry drafts)")
  (placement-history [s] "the append-only candidate-placement history (employmentops.registry drafts)")
  (next-match-sequence [s jurisdiction] "next match-number sequence for a jurisdiction")
  (next-placement-sequence [s jurisdiction] "next placement-number sequence for a jurisdiction")
  (candidacy-already-matched? [s candidacy-id] "has this candidacy already been matched?")
  (candidacy-already-placed? [s candidacy-id] "has this candidacy already been placed?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-candidacies [s candidacies] "replace/seed the candidacy directory (map id->candidacy)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained candidacy set covering both actuation
  lifecycles (match, place) plus the governor's own new checks, so
  the actor + tests run offline."
  []
  {:candidacies
   {"candidacy-1" {:id "candidacy-1" :candidate "Kita Taro" :job-title "Warehouse Associate"
                    :annual-salary 3000000 :fee-rate 0.2 :claimed-fee 600000.0
                    :matching-criteria-discriminatory? false
                    :requires-work-authorization? false :work-authorization-verified? false
                    :matched? false :placed? false
                    :jurisdiction "JPN" :status :intake}
    "candidacy-2" {:id "candidacy-2" :candidate "Atlantis Ann" :job-title "Warehouse Associate"
                    :annual-salary 2000000 :fee-rate 0.2 :claimed-fee 400000.0
                    :matching-criteria-discriminatory? false
                    :requires-work-authorization? false :work-authorization-verified? false
                    :matched? false :placed? false
                    :jurisdiction "ATL" :status :intake}
    "candidacy-3" {:id "candidacy-3" :candidate "Minami Hana" :job-title "Machine Operator"
                    :annual-salary 3500000 :fee-rate 0.2 :claimed-fee 800000.0
                    :matching-criteria-discriminatory? false
                    :requires-work-authorization? false :work-authorization-verified? false
                    :matched? false :placed? false
                    :jurisdiction "JPN" :status :intake}
    "candidacy-4" {:id "candidacy-4" :candidate "Higashi Ichiro" :job-title "Delivery Driver"
                    :annual-salary 3200000 :fee-rate 0.2 :claimed-fee 640000.0
                    :matching-criteria-discriminatory? true
                    :requires-work-authorization? false :work-authorization-verified? false
                    :matched? false :placed? false
                    :jurisdiction "JPN" :status :intake}
    "candidacy-5" {:id "candidacy-5" :candidate "Nishi Kenji" :job-title "Line Cook"
                    :annual-salary 2800000 :fee-rate 0.2 :claimed-fee 560000.0
                    :matching-criteria-discriminatory? false
                    :requires-work-authorization? true :work-authorization-verified? false
                    :matched? false :placed? false
                    :jurisdiction "JPN" :status :intake}
    "candidacy-6" {:id "candidacy-6" :candidate "Chuo Yuki" :job-title "Line Cook"
                    :annual-salary 2900000 :fee-rate 0.2 :claimed-fee 580000.0
                    :matching-criteria-discriminatory? false
                    :requires-work-authorization? true :work-authorization-verified? true
                    :matched? false :placed? false
                    :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- match-candidate!
  "Backend-agnostic `:candidacy/mark-matched` -- looks up the
  candidacy via the protocol and drafts the match record, and returns
  {:result .. :candidacy-patch ..} for the caller to persist."
  [s candidacy-id]
  (let [c (candidacy s candidacy-id)
        seq-n (next-match-sequence s (:jurisdiction c))
        result (registry/register-match candidacy-id (:jurisdiction c) seq-n)]
    {:result result
     :candidacy-patch {:matched? true
                       :match-number (get result "match_number")}}))

(defn- place-candidate!
  "Backend-agnostic `:candidacy/mark-placed` -- looks up the
  candidacy via the protocol and drafts the placement record, and
  returns {:result .. :candidacy-patch ..} for the caller to
  persist."
  [s candidacy-id]
  (let [c (candidacy s candidacy-id)
        seq-n (next-placement-sequence s (:jurisdiction c))
        result (registry/register-placement candidacy-id (:jurisdiction c) seq-n)]
    {:result result
     :candidacy-patch {:placed? true
                       :placement-number (get result "placement_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (candidacy [_ id] (get-in @a [:candidacies id]))
  (all-candidacies [_] (sort-by :id (vals (:candidacies @a))))
  (assessment-of [_ candidacy-id] (get-in @a [:assessments candidacy-id]))
  (ledger [_] (:ledger @a))
  (match-history [_] (:match-records @a))
  (placement-history [_] (:placement-records @a))
  (next-match-sequence [_ jurisdiction] (get-in @a [:match-sequences jurisdiction] 0))
  (next-placement-sequence [_ jurisdiction] (get-in @a [:placement-sequences jurisdiction] 0))
  (candidacy-already-matched? [_ candidacy-id] (boolean (get-in @a [:candidacies candidacy-id :matched?])))
  (candidacy-already-placed? [_ candidacy-id] (boolean (get-in @a [:candidacies candidacy-id :placed?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :candidacy/upsert
      (swap! a update-in [:candidacies (:id value)] merge value)

      :assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :candidacy/mark-matched
      (let [candidacy-id (first path)
            {:keys [result candidacy-patch]} (match-candidate! s candidacy-id)
            jurisdiction (:jurisdiction (candidacy s candidacy-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:match-sequences jurisdiction] (fnil inc 0))
                       (update-in [:candidacies candidacy-id] merge candidacy-patch)
                       (update :match-records registry/append result))))
        result)

      :candidacy/mark-placed
      (let [candidacy-id (first path)
            {:keys [result candidacy-patch]} (place-candidate! s candidacy-id)
            jurisdiction (:jurisdiction (candidacy s candidacy-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:placement-sequences jurisdiction] (fnil inc 0))
                       (update-in [:candidacies candidacy-id] merge candidacy-patch)
                       (update :placement-records registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-candidacies [s candidacies] (when (seq candidacies) (swap! a assoc :candidacies candidacies)) s))

(defn seed-db
  "A MemStore seeded with the demo candidacy set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :match-sequences {} :match-records []
                           :placement-sequences {} :placement-records []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (assessment payloads, ledger facts,
  match/placement records) are stored as EDN strings so
  `langchain.db` doesn't expand them into sub-entities -- the same
  convention every sibling actor's store uses."
  {:candidacy/id                   {:db/unique :db.unique/identity}
   :assessment/candidacy-id        {:db/unique :db.unique/identity}
   :ledger/seq                     {:db/unique :db.unique/identity}
   :match-record/seq               {:db/unique :db.unique/identity}
   :placement-record/seq           {:db/unique :db.unique/identity}
   :match-sequence/jurisdiction        {:db/unique :db.unique/identity}
   :placement-sequence/jurisdiction    {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

(defn- candidacy->tx [{:keys [id candidate job-title annual-salary fee-rate claimed-fee
                              matching-criteria-discriminatory?
                              requires-work-authorization? work-authorization-verified?
                              matched? placed?
                              jurisdiction status match-number placement-number
                              referral-id]}]
  (cond-> {:candidacy/id id}
    candidate                                       (assoc :candidacy/candidate candidate)
    job-title                                           (assoc :candidacy/job-title job-title)
    annual-salary                                          (assoc :candidacy/annual-salary annual-salary)
    fee-rate                                                  (assoc :candidacy/fee-rate fee-rate)
    claimed-fee                                                  (assoc :candidacy/claimed-fee claimed-fee)
    (some? matching-criteria-discriminatory?)                       (assoc :candidacy/matching-criteria-discriminatory? matching-criteria-discriminatory?)
    (some? requires-work-authorization?)                               (assoc :candidacy/requires-work-authorization? requires-work-authorization?)
    (some? work-authorization-verified?)                                  (assoc :candidacy/work-authorization-verified? work-authorization-verified?)
    (some? matched?)                                                         (assoc :candidacy/matched? matched?)
    (some? placed?)                                                            (assoc :candidacy/placed? placed?)
    jurisdiction                                                                  (assoc :candidacy/jurisdiction jurisdiction)
    status                                                                          (assoc :candidacy/status status)
    match-number                                                                      (assoc :candidacy/match-number match-number)
    placement-number                                                                     (assoc :candidacy/placement-number placement-number)
    ;; the 6399-side application-referral record id this candidacy arrived
    ;; with (superproject ADR-2607131000: the human-carried handoff; the
    ;; end-to-end story is reconstructed by joining BOTH ledgers).
    referral-id                                                                             (assoc :candidacy/referral-id referral-id)))

(def ^:private candidacy-pull
  [:candidacy/id :candidacy/candidate :candidacy/job-title :candidacy/annual-salary :candidacy/fee-rate :candidacy/claimed-fee
   :candidacy/matching-criteria-discriminatory? :candidacy/requires-work-authorization? :candidacy/work-authorization-verified?
   :candidacy/matched? :candidacy/placed?
   :candidacy/jurisdiction :candidacy/status :candidacy/match-number :candidacy/placement-number
   :candidacy/referral-id])

(defn- pull->candidacy [m]
  (when (:candidacy/id m)
    {:id (:candidacy/id m) :candidate (:candidacy/candidate m) :job-title (:candidacy/job-title m)
     :annual-salary (:candidacy/annual-salary m) :fee-rate (:candidacy/fee-rate m) :claimed-fee (:candidacy/claimed-fee m)
     :matching-criteria-discriminatory? (boolean (:candidacy/matching-criteria-discriminatory? m))
     :requires-work-authorization? (boolean (:candidacy/requires-work-authorization? m))
     :work-authorization-verified? (boolean (:candidacy/work-authorization-verified? m))
     :matched? (boolean (:candidacy/matched? m)) :placed? (boolean (:candidacy/placed? m))
     :jurisdiction (:candidacy/jurisdiction m) :status (:candidacy/status m)
     :match-number (:candidacy/match-number m) :placement-number (:candidacy/placement-number m)
     :referral-id (:candidacy/referral-id m)}))

(defrecord DatomicStore [conn]
  Store
  (candidacy [_ id]
    (pull->candidacy (d/pull (d/db conn) candidacy-pull [:candidacy/id id])))
  (all-candidacies [_]
    (->> (d/q '[:find [?id ...] :where [?e :candidacy/id ?id]] (d/db conn))
         (map #(pull->candidacy (d/pull (d/db conn) candidacy-pull [:candidacy/id %])))
         (sort-by :id)))
  (assessment-of [_ candidacy-id]
    (dec* (d/q '[:find ?p . :in $ ?cid
                :where [?a :assessment/candidacy-id ?cid] [?a :assessment/payload ?p]]
              (d/db conn) candidacy-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (match-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :match-record/seq ?s] [?e :match-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (placement-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :placement-record/seq ?s] [?e :placement-record/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-match-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :match-sequence/jurisdiction ?j] [?e :match-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-placement-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :placement-sequence/jurisdiction ?j] [?e :placement-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (candidacy-already-matched? [s candidacy-id]
    (boolean (:matched? (candidacy s candidacy-id))))
  (candidacy-already-placed? [s candidacy-id]
    (boolean (:placed? (candidacy s candidacy-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :candidacy/upsert
      (d/transact! conn [(candidacy->tx value)])

      :assessment/set
      (d/transact! conn [{:assessment/candidacy-id (first path) :assessment/payload (enc payload)}])

      :candidacy/mark-matched
      (let [candidacy-id (first path)
            {:keys [result candidacy-patch]} (match-candidate! s candidacy-id)
            jurisdiction (:jurisdiction (candidacy s candidacy-id))
            next-n (inc (next-match-sequence s jurisdiction))]
        (d/transact! conn
                     [(candidacy->tx (assoc candidacy-patch :id candidacy-id))
                      {:match-sequence/jurisdiction jurisdiction :match-sequence/next next-n}
                      {:match-record/seq (count (match-history s)) :match-record/record (enc (get result "record"))}])
        result)

      :candidacy/mark-placed
      (let [candidacy-id (first path)
            {:keys [result candidacy-patch]} (place-candidate! s candidacy-id)
            jurisdiction (:jurisdiction (candidacy s candidacy-id))
            next-n (inc (next-placement-sequence s jurisdiction))]
        (d/transact! conn
                     [(candidacy->tx (assoc candidacy-patch :id candidacy-id))
                      {:placement-sequence/jurisdiction jurisdiction :placement-sequence/next next-n}
                      {:placement-record/seq (count (placement-history s)) :placement-record/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-candidacies [s candidacies]
    (when (seq candidacies) (d/transact! conn (mapv candidacy->tx (vals candidacies)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:candidacies ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [candidacies]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-candidacies s candidacies))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo candidacy set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
