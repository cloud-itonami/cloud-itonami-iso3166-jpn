(ns statute.facts
  "General-law compliance catalog for Japan (JPN) -- extends this repo's
  existing `marketentry.facts` (public-procurement market-entry only,
  narrow scope) with a second, orthogonal catalog of statutes a company
  operating in this jurisdiction must generally track for compliance.

  Per ADR-2607141700 (cloud-itonami-compliance-fact-federation): this
  repo stays a single per-country entity (no new repo family), and every
  entry cites an OFFICIAL e-Gov (https://laws.e-gov.go.jp/) law-id --
  never fabricated. A law not in this table has NO spec-basis, full
  stop; extend `catalog`, do not invent an id/url.

  Distinct from etzhayyim/global-legislation-datoms' `:legal-source/*`
  schema, which catalogs WHERE a jurisdiction's legal sources/portals
  live (coarse, ~1 row per jurisdiction, e.g. \"jp-egov\" = the e-Gov API
  itself). This namespace is finer-grained: WHICH specific statutes
  apply, for the country-repo's own compliance-fact layer.")

(def catalog
  "iso3 -> vector of statute entries. `:statute/url` + `:statute/law-number`
  are the citation the governor requires before any compliance-fact
  proposal referencing this law can commit."
  {"JPN"
   [{:statute/id "jpn.companies-act"
     :statute/title "会社法 (Companies Act)"
     :statute/jurisdiction "JPN"
     :statute/kind :law
     :statute/law-number "平成十七年法律第八十六号"
     :statute/url "https://laws.e-gov.go.jp/law/417AC0000000086"
     :statute/url-provenance :e-gov-official
     :statute/enacted-date "2005-07-26"
     :statute/retrieved-at "2026-07-14"
     :statute/topic #{:corporate-governance :incorporation}}
    {:statute/id "jpn.appi"
     :statute/title "個人情報の保護に関する法律 (Act on the Protection of Personal Information)"
     :statute/jurisdiction "JPN"
     :statute/kind :law
     :statute/law-number "平成十五年法律第五十七号"
     :statute/url "https://laws.e-gov.go.jp/law/415AC0000000057"
     :statute/url-provenance :e-gov-official
     :statute/enacted-date "2003-05-30"
     :statute/retrieved-at "2026-07-14"
     :statute/topic #{:data-protection :privacy}}
    {:statute/id "jpn.labor-standards-act"
     :statute/title "労働基準法 (Labor Standards Act)"
     :statute/jurisdiction "JPN"
     :statute/kind :law
     :statute/law-number "昭和二十二年法律第四十九号"
     :statute/url "https://laws.e-gov.go.jp/law/322AC0000000049"
     :statute/url-provenance :e-gov-official
     :statute/enacted-date "1947-04-07"
     :statute/retrieved-at "2026-07-14"
     :statute/topic #{:labor :employment}}
    {:statute/id "jpn.aptcp-enforcement-regulation"
     :statute/title "犯罪による収益の移転防止に関する法律施行規則 (Ordinance for Enforcement of the Act on Prevention of Transfer of Criminal Proceeds)"
     :statute/jurisdiction "JPN"
     ;; Joint ministerial ordinance (共同省令, 8 ministries), not a Diet
     ;; :law -- this repo's kind vocabulary was :law-only before this
     ;; entry; :ministerial-ordinance is added rather than mislabeling it.
     :statute/kind :ministerial-ordinance
     :statute/law-number "平成二十年内閣府・総務省・法務省・財務省・厚生労働省・農林水産省・経済産業省・国土交通省令第一号"
     :statute/url "https://laws.e-gov.go.jp/law/420M60000F5A001"
     :statute/url-provenance :e-gov-official
     :statute/enacted-date "2008-02-01"
     :statute/retrieved-at "2026-07-14"
     ;; Cross-referenced 2026-07-14 via the e-Gov law-data API
     ;; (api/2/law_data/420M60000F5A001): most recent amendment Reiwa 8
     ;; Ordinance No. 3, effective 2026-04-15 -- matches
     ;; kotoba-lang/ekyc's kotoba.ekyc ADR-0001 citation of the same law
     ;; id and the same 2026-04-15 revision date, an independent
     ;; cross-check between the two sources.
     :statute/topic #{:aml :identity-verification}}]})

(defn spec-basis
  "The jurisdiction's statute vector, or nil -- nil means NO spec-basis
  for that jurisdiction yet."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report, same shape/discipline as `marketentry.facts/coverage`:
  never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-iso3166-jpn statute.facts Wave 0 (ADR-2607141700): "
                 (count (get catalog "JPN")) " JPN statutes seeded with an "
                 "official e-Gov citation. Extend `statute.facts/catalog`, "
                 "never fabricate a law-id or URL.")})))

(defn by-topic
  "Statutes for `iso3` tagged with `topic` (e.g. :labor, :data-protection)."
  [iso3 topic]
  (filterv #(contains? (:statute/topic %) topic) (spec-basis iso3)))
