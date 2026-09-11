(ns employmentops.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean candidacy
  through intake -> jurisdiction assessment -> candidate matching
  (escalate/approve/commit) -> candidate placement (escalate/approve/
  commit), then a SEPARATE clean work-authorization-required candidacy
  through the same lifecycle (demonstrating the conditional work-
  authorization check passing cleanly), then shows HARD-hold
  scenarios: a jurisdiction with no spec-basis, a placement-fee
  mismatch (verified first), a discriminatory matching basis, and an
  unverified work authorization on a work-authorization-required
  candidacy, a double match, and a double placement.

  Like `retailops`/4711's, `freightops`/4920's, `quarryops`/0810's,
  `agronomyops`/0162's, `hospitalityops`/5510's and `practiceops`/
  7110's own new checks, this actor's new checks (`matching-basis-
  discriminatory?`, `work-authorization-unverified?`) are evaluated
  directly at `:candidacy/match`/`:candidacy/place` time rather than
  via a separate screening op -- a real matching/placement decision
  validates non-discriminatory criteria and work authorization at the
  point of the act itself. Each check is still exercised directly and
  independently below, one candidacy per HARD-hold scenario, following
  the SAME 'exercise the failure mode directly, never only via a
  happy-path actuation' discipline `parksafety`'s ADR-2607071922
  Decision 5 and every sibling since establish."
  (:require [langgraph.graph :as g]
            [employmentops.store :as store]
            [employmentops.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :agency-operator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== candidacy/intake candidacy-1 (JPN, clean, no work authorization needed) ==")
    (println (exec-op actor "t1" {:op :candidacy/intake :subject "candidacy-1"
                                  :patch {:id "candidacy-1" :candidate "Kita Taro"}} operator))

    (println "== jurisdiction/assess candidacy-1 (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :jurisdiction/assess :subject "candidacy-1"} operator))
    (println (approve! actor "t2"))

    (println "== candidacy/match candidacy-1 (always escalates -- actuation/match-candidate) ==")
    (let [r (exec-op actor "t3" {:op :candidacy/match :subject "candidacy-1"} operator)]
      (println r)
      (println "-- human agency operator approves --")
      (println (approve! actor "t3")))

    (println "== candidacy/place candidacy-1 (always escalates -- actuation/place-candidate) ==")
    (let [r (exec-op actor "t4" {:op :candidacy/place :subject "candidacy-1"} operator)]
      (println r)
      (println "-- human agency operator approves --")
      (println (approve! actor "t4")))

    (println "== candidacy/intake candidacy-6 (JPN, clean, work authorization required and verified) ==")
    (println (exec-op actor "t5" {:op :candidacy/intake :subject "candidacy-6"
                                  :patch {:id "candidacy-6" :candidate "Chuo Yuki"}} operator))

    (println "== jurisdiction/assess candidacy-6 (escalates -- human approves) ==")
    (println (exec-op actor "t6" {:op :jurisdiction/assess :subject "candidacy-6"} operator))
    (println (approve! actor "t6"))

    (println "== candidacy/match candidacy-6 (always escalates) ==")
    (println (exec-op actor "t6b" {:op :candidacy/match :subject "candidacy-6"} operator))
    (println (approve! actor "t6b"))

    (println "== candidacy/place candidacy-6 (work authorization required, verified -- escalates -- human approves) ==")
    (println (exec-op actor "t7" {:op :candidacy/place :subject "candidacy-6"} operator))
    (println (approve! actor "t7"))

    (println "== jurisdiction/assess candidacy-2 (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t8" {:op :jurisdiction/assess :subject "candidacy-2" :no-spec? true} operator))

    (println "== jurisdiction/assess candidacy-3 (escalates -- human approves; sets up the fee-mismatch test) ==")
    (println (exec-op actor "t9" {:op :jurisdiction/assess :subject "candidacy-3"} operator))
    (println (approve! actor "t9"))

    (println "== candidacy/match candidacy-3 (always escalates) ==")
    (println (exec-op actor "t9b" {:op :candidacy/match :subject "candidacy-3"} operator))
    (println (approve! actor "t9b"))

    (println "== candidacy/place candidacy-3 (claimed 800000.0 vs recompute 700000.0 -> HARD hold) ==")
    (println (exec-op actor "t10" {:op :candidacy/place :subject "candidacy-3"} operator))

    (println "== jurisdiction/assess candidacy-4 (escalates -- human approves; sets up the discriminatory-match test) ==")
    (println (exec-op actor "t11" {:op :jurisdiction/assess :subject "candidacy-4"} operator))
    (println (approve! actor "t11"))

    (println "== candidacy/match candidacy-4 (discriminatory matching basis -> HARD hold) ==")
    (println (exec-op actor "t12" {:op :candidacy/match :subject "candidacy-4"} operator))

    (println "== jurisdiction/assess candidacy-5 (escalates -- human approves; sets up the work-authorization test) ==")
    (println (exec-op actor "t13" {:op :jurisdiction/assess :subject "candidacy-5"} operator))
    (println (approve! actor "t13"))

    (println "== candidacy/match candidacy-5 (always escalates) ==")
    (println (exec-op actor "t13b" {:op :candidacy/match :subject "candidacy-5"} operator))
    (println (approve! actor "t13b"))

    (println "== candidacy/place candidacy-5 (work authorization required, unverified -> HARD hold) ==")
    (println (exec-op actor "t14" {:op :candidacy/place :subject "candidacy-5"} operator))

    (println "== candidacy/match candidacy-1 AGAIN (double-match -> HARD hold) ==")
    (println (exec-op actor "t15" {:op :candidacy/match :subject "candidacy-1"} operator))

    (println "== candidacy/place candidacy-1 AGAIN (double-placement -> HARD hold) ==")
    (println (exec-op actor "t16" {:op :candidacy/place :subject "candidacy-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft match records ==")
    (doseq [r (store/match-history db)] (println r))

    (println "== draft placement records ==")
    (doseq [r (store/placement-history db)] (println r))))
