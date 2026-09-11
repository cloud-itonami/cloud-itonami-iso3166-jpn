(ns marketentry.ekyc-bridge
  "Bridge from `statute.facts`'s coarse per-statute citation for JPN's
  犯罪による収益の移転防止に関する法律施行規則 (\"jpn.aptcp-enforcement-
  regulation\", identity-verification/AML topic) into
  `kotoba-lang/ekyc`'s `kotoba.ekyc` -- the SAME statute, at
  method-by-method detail (which non-face-to-face identity-verification
  methods it actually enumerates, their required-evidence combinations,
  and their NIST SP 800-63A-4 IAL cross-reference). Mirrors
  `marketentry.goyoukiki`'s existing bridge shape exactly (`facts.cljc`
  coarse citation <-> a fine-grained companion library), applied to
  `statute.facts` instead of `marketentry.facts` and to `kotoba.ekyc`
  instead of `kotoba-lang/goyoukiki`. Pure -- no I/O, no store.

  Scope, stated plainly (see the superproject ADR for the full
  reasoning): this bridge is a REFERENCE surface, not a governor HARD
  check. `marketentry.governor`'s `japan-resident-rep-missing-violations`
  independently verifies whether a Japan-resident representative is ON
  FILE for public-procurement purposes (会計法・全省庁統一資格) -- a
  company-eligibility question. `kotoba.ekyc` answers a DIFFERENT
  question: which non-face-to-face methods 犯収法施行規則 recognizes for a
  SPECIFIED BUSINESS OPERATOR (特定事業者, e.g. a bank or funds-transfer
  service) to identity-verify ITS OWN CUSTOMER under anti-money-
  laundering law. `cloud-itonami-iso3166-jpn`'s own operators are
  generic companies seeking public-procurement market entry, not
  necessarily 特定事業者 themselves -- wiring `kotoba.ekyc/validate` into
  `japan-resident-rep-missing-violations` (or any other existing HARD
  check) would conflate these two different regulatory domains under a
  single boolean and was deliberately NOT done. This namespace exists so
  that (a) an operator who DOES need to track 犯収法 as a general
  compliance matter (statute.facts's actual stated purpose) gets the
  real method-level detail behind that one-line statute citation, and
  (b) a genuine FUTURE governor check -- e.g. verifying that a Japan-
  resident representative's OWN identity was confirmed via one of the
  real enumerated methods, should this actor's scope ever grow to cover
  that -- has real data to be built on rather than starting from
  nothing."
  (:require [kotoba.ekyc :as ekyc]
            [statute.facts :as facts]))

(def aptcp-statute-id "jpn.aptcp-enforcement-regulation")

(defn statute-entry
  "The statute.facts catalog entry this bridge grounds itself in, or nil
  if it has been removed (fails closed, never fabricates a citation)."
  []
  (first (filter #(= aptcp-statute-id (:statute/id %)) (facts/spec-basis "JPN"))))

(defn identity-verification-methods
  "The real, full kotoba.ekyc/method-catalog, cross-referenced against
  this bridge's own statute.facts citation -- nil when the statute entry
  is missing (fails closed, matching statute.facts/spec-basis's own
  'no spec-basis, full stop' discipline), never the bare catalog with an
  invented citation attached."
  []
  (when-let [st (statute-entry)]
    {:statute st
     :methods ekyc/method-catalog}))

(defn methods-for-subject
  "identity-verification-methods narrowed to :individual or :corporate
  subject methods (delegates to kotoba.ekyc/methods-for), or nil when the
  statute entry is missing."
  [subject]
  (when (statute-entry)
    (ekyc/methods-for subject)))

(defn validate-representative-identity
  "Structural pass-through to kotoba.ekyc/validate for a verification
  record claiming one of the real enumerated methods. NOT called from
  marketentry.governor today (see namespace docstring) -- exposed so a
  future check can reuse this bridge's statute-grounding instead of
  calling kotoba.ekyc/validate directly and losing the statute.facts
  citation link. Returns nil (never a fabricated pass) when this
  bridge's own statute entry is missing."
  [verification]
  (when (statute-entry)
    (ekyc/validate verification)))
