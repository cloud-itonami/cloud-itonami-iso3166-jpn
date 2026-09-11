(ns marketentry.facts
  "Per-jurisdiction public-procurement market-entry regulatory catalog
  -- the G2-style spec-basis table the Market-Entry Compliance Governor
  checks every `:jurisdiction/assess` proposal against ('did the advisor
  cite an OFFICIAL public source for this jurisdiction's requirements,
  or did it invent one?').

  This blueprint's own text (docs/business-model.md) names Japan's real
  market-entry surface: GEPS / 全省庁統一資格 (unified central-government
  tender qualification), Corporate Number (法人番号) via the National Tax
  Agency, Qualified Invoice Issuer registration under Japan's consumption-
  tax invoice system (インボイス制度), and the near-universal requirement
  that most tenders demand a Japan-resident authorized representative
  plus Japanese-language documentation. Each jurisdiction entry below
  therefore cites the OFFICIAL procurement portal + business/tax
  registration authority.

  Coverage is reported HONESTLY (see `coverage`): a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  intake/portal-registration/filing evidence set; `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any `:jurisdiction/assess` proposal can commit.
  `:rep-owner-authority` / `:rep-legal-basis` / `:rep-provenance` are
  the SEPARATE Japan-resident-representative citation the governor's
  `japan-resident-rep-missing?` check is grounded in (where applicable)."
  {"JPN" {:name "Japan"
          :owner-authority "デジタル庁 / 全省庁統一資格 審査機関"
          :legal-basis "会計法・予算決算及び会計令に基づく全省庁統一資格 / 政府電子調達システム (GEPS)"
          :national-spec "全省庁統一資格の取得手続・有資格者名簿"
          :provenance "https://www.chotatujoho.go.jp/va/com/ShikakuTop.html"
          :required-evidence ["法人番号確認記録 (corporate-number record)"
                              "全省庁統一資格申請記録 (unified-qualification filing)"
                              "GEPS 事業者登録記録 (GEPS supplier registration)"
                              "日本居住代理人確認記録 (japan-resident-rep record)"]
          :rep-owner-authority "各発注機関 (tendering authority) / 全省庁統一資格 審査機関"
          :rep-legal-basis "全省庁統一資格申請における日本国内の本店又は主たる事務所・代理人要件"
          :rep-provenance "https://www.chotatujoho.go.jp/va/com/ShikakuTop.html"
          :corporate-number-owner-authority "国税庁 (National Tax Agency)"
          :corporate-number-legal-basis "法人番号 (Corporate Number) 制度"
          :corporate-number-provenance "https://www.houjin-bangou.nta.go.jp/"}
   "USA" {:name "United States"
          :owner-authority "U.S. General Services Administration (GSA) / SAM.gov"
          :legal-basis "Federal Acquisition Regulation (FAR); System for Award Management"
          :national-spec "SAM.gov entity registration + NAICS self-certification"
          :provenance "https://sam.gov/"
          :required-evidence ["EIN record"
                              "SAM.gov registration record"
                              "State business registration record"
                              "Authorized-representative record"]}
   "DEU" {:name "Germany"
          :owner-authority "Beschaffungsamt des BMI / e-Vergabe platforms"
          :legal-basis "Gesetz gegen Wettbewerbsbeschränkungen (GWB) / VgV"
          :national-spec "e-Vergabe supplier registration under EU procurement directives"
          :provenance "https://www.evergabe-online.de/"
          :required-evidence ["Handelsregister extract"
                              "e-Vergabe registration record"
                              "USt-IdNr record"
                              "Authorized-representative record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Crown Commercial Service / Find a Tender"
          :legal-basis "Public Contracts Regulations 2015 (as retained/amended post-Brexit)"
          :national-spec "Find a Tender Service (FTS) + Contracts Finder supplier registration"
          :provenance "https://www.find-tender.service.gov.uk/"
          :required-evidence ["Companies House record"
                              "Find a Tender registration record"
                              "VAT registration record"
                              "Authorized-representative record"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to assess or file
  on it."
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
      :note (str "cloud-itonami-iso3166-jpn R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog for market-entry navigation, "
                 "not a survey of all ~194 jurisdictions -- extend "
                 "`marketentry.facts/catalog`, never fabricate a "
                 "jurisdiction's requirements.")})))

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

(defn rep-spec-basis
  "The jurisdiction's Japan-resident-representative (or local authorized-
  rep) requirement map, or nil when this catalog has no such regime.
  For JPN this is real and near-universal for public tenders."
  [iso3]
  (when-let [sb (spec-basis iso3)]
    (when (:rep-owner-authority sb)
      (select-keys sb [:rep-owner-authority :rep-legal-basis :rep-provenance]))))

(defn corporate-number-spec-basis
  "The jurisdiction's corporate-number / tax-id regime, or nil."
  [iso3]
  (when-let [sb (spec-basis iso3)]
    (when (:corporate-number-owner-authority sb)
      (select-keys sb [:corporate-number-owner-authority
                       :corporate-number-legal-basis
                       :corporate-number-provenance]))))
