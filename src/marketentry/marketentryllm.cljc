(ns marketentry.marketentryllm
  "MarketEntry-LLM client -- the *contained intelligence node* for
  the Japan public-sector market-entry compliance actor.

  It normalizes engagement intake, drafts a per-jurisdiction market-
  entry evidence checklist, drafts the filing-draft action, and drafts
  the filing-submit action. CRITICAL: it is a smart-but-untrusted
  advisor. It returns a *proposal* (with a rationale + the fields it
  cited), never a committed record or a real portal submission. Every
  output is censored downstream by `marketentry.governor` before
  anything touches the SSoT, and `:filing/draft`/`:filing/submit`
  proposals NEVER auto-commit at any phase -- see README Actuation.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [clojure.string :as str]
            [marketentry.facts :as facts]
            [marketentry.goyoukiki :as goyoukiki]
            [marketentry.store :as store]))

(defn- normalize-intake
  [_db {:keys [patch]}]
  {:summary    (str "参入案件記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :engagement/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- assess-jurisdiction
  "Per-jurisdiction market-entry evidence checklist draft. `:no-spec?`
  injects the failure mode we must defend against: proposing a
  checklist for a jurisdiction with NO official spec-basis.

  When the engagement declares a `:motivating-opportunity` (a real,
  ontology-tagged tender fact from kotoba-lang/goyoukiki -- see
  marketentry.goyoukiki), grounds the generic checklist in that SPECIFIC
  opportunity's own eligibility floor. `:motivating-opportunity-claimed?`/
  `:motivating-opportunity-verified?` are set whenever one was declared, so
  `marketentry.governor` can HARD-hold a claim that
  kotoba.ontology.connector/tagged-conforms? could not actually verify --
  never silently degrade a fabricated citation into the unremarkable
  generic case."
  [db {:keys [subject no-spec?]}]
  (let [e (store/engagement db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction e))
        sb (facts/spec-basis iso3)
        motivating-opp (:motivating-opportunity e)
        opp-ctx (when motivating-opp
                  (goyoukiki/opportunity-assessment-context
                   (:motivating-connector e) motivating-opp))
        opp-flags (when motivating-opp
                    {:motivating-opportunity-claimed? true
                     :motivating-opportunity-verified? (boolean opp-ctx)})]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "marketentry.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :assessment/set
       :value      (merge {:jurisdiction iso3 :checklist [] :spec-basis nil} opp-flags)
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案"
                        (when opp-ctx (str " -- 案件 " (:opportunity-title opp-ctx) " に基づく")))
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb)
                        (when opp-ctx
                          (str " / 対象案件: " (:opportunity-id opp-ctx)
                               " (出所: " (name (:opportunity-source opp-ctx)) ")")))
       :cites      (cond-> [(:legal-basis sb) (:provenance sb)]
                     opp-ctx (conj (:opportunity-source opp-ctx)))
       :effect     :assessment/set
       :value      (merge {:jurisdiction iso3
                           :checklist (:required-evidence sb)
                           :spec-basis (:provenance sb)
                           :legal-basis (:legal-basis sb)}
                          opp-flags
                          (when opp-ctx
                            {:motivating-opportunity-id (:opportunity-id opp-ctx)
                             :required-rank (:required-rank opp-ctx)
                             :required-categories (:required-categories opp-ctx)}))
       :stake      nil
       :confidence 0.9})))

(defn- propose-draft
  "Draft the actual FILING-DRAFT action. ALWAYS `:stake
  :actuation/draft-filing`."
  [db {:keys [subject]}]
  (let [e (store/engagement db subject)]
    {:summary    (str subject " 向け提出ドラフト提案"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "jurisdiction=" (:jurisdiction e)
                        " portal=" (:portal e))
                   "engagementが見つかりません")
     :cites      (if e [subject] [])
     :effect     :engagement/mark-drafted
     :value      {:engagement-id subject}
     :stake      :actuation/draft-filing
     :confidence (if e 0.9 0.3)}))

(defn- propose-submit
  "Draft the actual FILING-SUBMIT action. ALWAYS `:stake
  :actuation/submit-filing` -- real-world portal submission."
  [db {:keys [subject]}]
  (let [e (store/engagement db subject)]
    {:summary    (str subject " 向けポータル提出提案"
                      (when e (str " (operator=" (:operator e) ")")))
     :rationale  (if e
                   (str "has-japan-resident-rep?=" (:has-japan-resident-rep? e)
                        " corporate-number-verified?=" (:corporate-number-verified? e)
                        " claimed-fee=" (:claimed-fee e))
                   "engagementが見つかりません")
     :cites      (if e [subject] [])
     :effect     :engagement/mark-submitted
     :value      {:engagement-id subject}
     :stake      :actuation/submit-filing
     :confidence (if (and e
                          (or (not (:requires-japan-resident-rep? e))
                              (:has-japan-resident-rep? e))
                          (or (not (:requires-corporate-number? e))
                              (:corporate-number-verified? e)))
                   0.9 0.3)}))

(defprotocol Advisor
  (-advise [this db request] "Return a proposal map for `request`."))

(defrecord MockAdvisor []
  Advisor
  (-advise [_ db {:keys [op] :as request}]
    (case op
      :engagement/intake    (normalize-intake db request)
      :jurisdiction/assess (assess-jurisdiction db request)
      :filing/draft        (propose-draft db request)
      :filing/submit       (propose-submit db request)
      {:summary "unknown op" :rationale "unsupported" :cites []
       :effect :noop :value {} :stake nil :confidence 0.0})))

(defn mock-advisor [] (->MockAdvisor))

(defn trace [request proposal]
  {:t :advisor-proposal
   :op (:op request)
   :subject (:subject request)
   :summary (:summary proposal)
   :confidence (:confidence proposal)
   :stake (:stake proposal)})
