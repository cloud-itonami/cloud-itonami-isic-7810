(ns employmentops.facts
  "Per-jurisdiction employment-anti-discrimination AND foreign-worker
  authorization regulatory catalog -- the G2-style spec-basis table
  the Employment Agency Governor checks every `:jurisdiction/assess`
  proposal against ('did the advisor cite an OFFICIAL public source
  for this jurisdiction's requirements, or did it invent one?').

  This blueprint's own text (docs/business-model.md's own Trust
  Controls: 'placement outside consent is blocked; matching decisions
  are explainable') names two real, distinct regulatory concerns: the
  general employment-anti-discrimination framework a matching decision
  must not violate (independent of the candidate's own immigration
  status), and a SEPARATE foreign-worker work-authorization regime
  specifically requiring verification of a candidate's legal right to
  work before placement (independent of whether the match itself is
  non-discriminatory -- a non-discriminatory match can still involve a
  candidate whose work authorization has not been verified, and a
  fully work-authorized candidate can still be matched on
  discriminatory criteria). Each jurisdiction entry below therefore
  cites BOTH the general anti-discrimination law AND a SEPARATE
  work-authorization/right-to-work law.

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries. Like
  `practiceops`/7110's own professional-seal sub-citation, ALL FOUR
  seeded jurisdictions actually have a real work-authorization
  sub-citation here, reported honestly (a full-coverage sub-citation,
  matching `quarryops`/0810's own blast-safety and `agronomyops`/
  0162's own water-buffer full coverage rather than `hospitalityops`/
  5510's own honest single-jurisdiction gap).")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  intake/registration/matching-record evidence set (PLUS a work-
  authorization-verification record for every seeded jurisdiction);
  `:legal-basis` / `:owner-authority` / `:provenance` are the G2
  citation the governor requires before any `:jurisdiction/assess`
  proposal can commit. `:authorization-owner-authority` /
  `:authorization-legal-basis` / `:authorization-provenance` are the
  SEPARATE work-authorization citation the governor's `work-
  authorization-unverified?` check is grounded in."
  {"JPN" {:name "Japan"
          :owner-authority "厚生労働省 (Ministry of Health, Labour and Welfare, MHLW)"
          :legal-basis "職業安定法 (Employment Security Act) 及び 男女雇用機会均等法 (Equal Employment Opportunity Act)"
          :national-spec "職業紹介事業の適正な運営に関するガイドライン"
          :provenance "https://www.mhlw.go.jp/stf/seisakunitsuite/bunya/koyou_roudou/koyou/shokugyoushoukai/"
          :required-evidence ["登録記録 (registration record)"
                              "マッチング記録 (matching record)"
                              "配置記録 (placement record)"
                              "在留資格確認記録 (work-authorization-verification record)"]
          :authorization-owner-authority "出入国在留管理庁 (Immigration Services Agency) / 厚生労働省"
          :authorization-legal-basis "出入国管理及び難民認定法 (Immigration Control and Refugee Recognition Act)"
          :authorization-provenance "https://www.moj.go.jp/isa/applications/status/index.html"}
   "USA" {:name "United States"
          :owner-authority "Equal Employment Opportunity Commission (EEOC)"
          :legal-basis "Title VII of the Civil Rights Act of 1964 (42 U.S.C. §2000e)"
          :national-spec "EEOC guidance on employment-agency referral non-discrimination"
          :provenance "https://www.eeoc.gov/statutes/title-vii-civil-rights-act-1964"
          :required-evidence ["Registration record"
                              "Matching record"
                              "Placement record"
                              "Work-authorization-verification record"]
          :authorization-owner-authority "U.S. Citizenship and Immigration Services (USCIS)"
          :authorization-legal-basis "Immigration and Nationality Act §274A (Form I-9 employment eligibility verification)"
          :authorization-provenance "https://www.uscis.gov/i-9"}
   "GBR" {:name "United Kingdom"
          :owner-authority "Equality and Human Rights Commission (EHRC)"
          :legal-basis "Equality Act 2010"
          :national-spec "EHRC statutory code of practice on employment"
          :provenance "https://www.equalityhumanrights.com/guidance/employment-statutory-code-practice"
          :required-evidence ["Registration record"
                              "Matching record"
                              "Placement record"
                              "Work-authorization-verification record"]
          :authorization-owner-authority "Home Office"
          :authorization-legal-basis "Immigration, Asylum and Nationality Act 2006 (right-to-work checks)"
          :authorization-provenance "https://www.gov.uk/check-job-applicant-right-to-work"}
   "DEU" {:name "Germany"
          :owner-authority "Bundesagentur für Arbeit (Federal Employment Agency)"
          :legal-basis "Allgemeines Gleichbehandlungsgesetz (AGG, General Equal Treatment Act)"
          :national-spec "AGG Diskriminierungsverbot bei Vermittlung und Auswahl"
          :provenance "https://www.antidiskriminierungsstelle.de/DE/das-gesetz/"
          :required-evidence ["Registrierungsprotokoll (registration record)"
                              "Vermittlungsprotokoll (matching record)"
                              "Einstellungsprotokoll (placement record)"
                              "Aufenthaltstitelnachweis (work-authorization-verification record)"]
          :authorization-owner-authority "Ausländerbehörden (local foreigners' authorities) / Bundesagentur für Arbeit"
          :authorization-legal-basis "Aufenthaltsgesetz (AufenthG, Residence Act) §4a (Erwerbstätigkeit)"
          :authorization-provenance "https://www.gesetze-im-internet.de/aufenthg_2004/__4a.html"}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to match or place
  a candidate on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-7810 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `employmentops.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))

(defn authorization-spec-basis
  "The jurisdiction's work-authorization requirement map, or nil --
  nil means this jurisdiction has NO formal statutory work-
  authorization regime this catalog is aware of. In this R0 catalog
  all four seeded jurisdictions actually have one, reported honestly
  (a full-coverage sub-citation, matching `quarryops`/0810's own
  blast-safety and `agronomyops`/0162's own water-buffer full
  coverage)."
  [iso3]
  (when-let [sb (spec-basis iso3)]
    (when (:authorization-owner-authority sb)
      (select-keys sb [:authorization-owner-authority :authorization-legal-basis :authorization-provenance]))))
