(ns marketentry.governor
  "Market-Entry Compliance Governor -- the independent compliance layer
  that earns the MarketEntry-LLM the right to commit. The LLM has no
  notion of jurisdictional procurement law, whether a Japan-resident
  authorized representative is actually on file, whether a claimed
  engagement fee actually equals base + months x rate, whether a
  corporate number has been verified for a filing that requires it, or
  when a draft stops being a draft and becomes a real-world portal
  submission, so this MUST be a separate system able to *reject* a
  proposal and fall back to HOLD.

  `:itonami.blueprint/governor` is `:market-entry-compliance-governor`
  (shared family keyword on blueprints; this is the first *running*
  implementation of that governor for the iso3166 family).

  This blueprint's own text (docs/business-model.md Trust Controls:
  'any actual portal registration or filing submission requires
  Market-Entry Compliance Governor clearance and always escalates to
  human sign-off'; 'a false or fabricated regulatory-requirement claim
  is a HARD hold') names exactly the checks below.

  Eight checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them. The confidence/actuation gate is
  SOFT: it asks a human to look (low confidence / actuation), and the
  human may approve -- but see `marketentry.phase`: for `:stake
  :actuation/draft-filing`/`:actuation/submit-filing` NO phase ever
  allows auto-commit either. Two independent layers agree that
  actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`marketentry.facts`), or invent
                                       one?
    2. Motivating-opportunity      -- when the proposal cites a real
       unverified                    goyoukiki-ingested tender fact as
                                       the motivating case
                                       (`marketentry.goyoukiki`), was it
                                       actually verified via
                                       `kotoba.ontology.connector/
                                       tagged-conforms?`, or claimed
                                       without proof?
    3. Evidence incomplete         -- for `:filing/draft`/
                                       `:filing/submit`, has the
                                       jurisdiction actually been
                                       assessed with a full evidence
                                       checklist on file?
    4. Japan-resident-rep missing  -- for `:filing/submit`, when the
                                       engagement declares
                                       `:requires-japan-resident-rep?
                                       true` (near-universal for JPN
                                       public tenders per this
                                       blueprint's own text),
                                       INDEPENDENTLY verify
                                       `:has-japan-resident-rep?` is
                                       true. FLAGSHIP genuinely new
                                       check for the iso3166 family
                                       (grep-verified absent as a
                                       governor check function name
                                       fleet-wide at build time).
                                       Grounded in Japan's 全省庁統一資格
                                       domestic office / agent
                                       requirements.
    5. Engagement fee mismatch     -- for `:filing/submit`,
                                       INDEPENDENTLY recompute whether
                                       the engagement's own `:claimed-
                                       fee` equals `base-fee +
                                       monthly-rate x monitoring-
                                       months` -- honest reapplication
                                       of the ground-truth-recompute
                                       discipline sibling actors use.
    6. Corporate-number unverified -- for `:filing/submit`, when the
                                       engagement declares
                                       `:requires-corporate-number?
                                       true`, INDEPENDENTLY check
                                       `:corporate-number-verified?`.
                                       CONDITIONAL on the engagement's
                                       own ground truth.
    7. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:filing/draft`/
                                       `:filing/submit` (REAL acts)
                                       -> escalate.

  Two more guards, double-draft/double-submit prevention, are enforced
  off dedicated `:drafted?`/`:submitted?` facts (never a `:status`
  value)."
  (:require [marketentry.facts :as facts]
            [marketentry.registry :as registry]
            [marketentry.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Drafting a real portal package and submitting a real portal
  registration are the two real-world actuation events this actor
  performs."
  #{:actuation/draft-filing :actuation/submit-filing})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:jurisdiction/assess` (or `:filing/draft`/`:filing/submit`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's market-entry requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:jurisdiction/assess :filing/draft :filing/submit} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:filing/draft`/`:filing/submit`, the jurisdiction's required
  registration evidence must actually be satisfied."
  [{:keys [op subject]} st]
  (when (contains? #{:filing/draft :filing/submit} op)
    (let [e (store/engagement st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction e) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(法人番号/統一資格/GEPS登録/代理人確認等)が充足していない状態での提案"}]))))

(defn- japan-resident-rep-missing-violations
  "For `:filing/submit`, when the engagement declares
  `:requires-japan-resident-rep? true`, INDEPENDENTLY verify
  `:has-japan-resident-rep?` is true -- the flagship genuinely new
  check this vertical adds. CONDITIONAL on the engagement's own
  `:requires-japan-resident-rep?` ground truth (most JPN public
  tenders require it; a pure-foreign-market engagement may not)."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when (and (true? (:requires-japan-resident-rep? e))
                 (not (true? (:has-japan-resident-rep? e))))
        [{:rule :japan-resident-rep-missing
          :detail (str subject " は日本居住代理人を要するが未確認 -- 提出提案は進められない")}]))))

(defn- engagement-fee-mismatch-violations
  "For `:filing/submit`, INDEPENDENTLY recompute whether the
  engagement's own claimed fee equals base + months x rate."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when-not (registry/engagement-fee-matches-claim? e)
        [{:rule :engagement-fee-mismatch
          :detail (str subject " の申告手数料(" (:claimed-fee e)
                      ")が独立再計算値(" (registry/compute-engagement-fee e) ")と一致しない")}]))))

(defn- corporate-number-unverified-violations
  "For `:filing/submit`, when the engagement declares
  `:requires-corporate-number? true`, INDEPENDENTLY check
  `:corporate-number-verified?` -- CONDITIONAL on the engagement's own
  ground truth."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (let [e (store/engagement st subject)]
      (when (and (true? (:requires-corporate-number? e))
                 (not (true? (:corporate-number-verified? e))))
        [{:rule :corporate-number-unverified
          :detail (str subject " は法人番号確認を要するが未確認 -- 提出提案は進められない")}]))))

(defn- motivating-opportunity-unverified-violations
  "For `:jurisdiction/assess`, when the proposal claims a motivating
  opportunity (marketentryllm/assess-jurisdiction sets
  `:motivating-opportunity-claimed?` whenever the engagement declares one)
  but `kotoba.ontology.connector/tagged-conforms?` could not verify it (an
  untagged or mistagged fact), that is exactly the kind of fabricated
  regulatory-requirement claim this governor's charter forbids (docs/
  business-model.md Trust Controls: 'a false or fabricated
  regulatory-requirement claim is a HARD hold') -- HARD, applied to the
  opportunity-input side the same way spec-basis-violations already
  applies it to the jurisdiction-catalog side."
  [{:keys [op]} proposal]
  (when (= op :jurisdiction/assess)
    (let [value (:value proposal)]
      (when (and (:motivating-opportunity-claimed? value)
                 (not (:motivating-opportunity-verified? value)))
        [{:rule :motivating-opportunity-unverified
          :detail "引用された案件事実がontology provenanceで検証できない -- 実在案件として扱えない"}]))))

(defn- already-drafted-violations
  "For `:filing/draft`, refuses to draft the SAME engagement twice."
  [{:keys [op subject]} st]
  (when (= op :filing/draft)
    (when (store/engagement-already-drafted? st subject)
      [{:rule :already-drafted
        :detail (str subject " は既にドラフト済み")}])))

(defn- already-submitted-violations
  "For `:filing/submit`, refuses to submit the SAME engagement twice."
  [{:keys [op subject]} st]
  (when (= op :filing/submit)
    (when (store/engagement-already-submitted? st subject)
      [{:rule :already-submitted
        :detail (str subject " は既に提出済み")}])))

(defn check
  "Censors a MarketEntry-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (motivating-opportunity-unverified-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (japan-resident-rep-missing-violations request st)
                           (engagement-fee-mismatch-violations request st)
                           (corporate-number-unverified-violations request st)
                           (already-drafted-violations request st)
                           (already-submitted-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
