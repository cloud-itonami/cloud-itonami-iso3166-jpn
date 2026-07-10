(ns marketentry.goyoukiki
  "Bridge from a real, ontology-tagged procurement-tender fact -- as
  ingested by kotoba-lang/goyoukiki's jp.kkj/jp.geps connectors and tagged
  by kotoba-lang/ontology's kotoba.ontology.connector/tag -- into this
  blueprint's own jurisdiction-assessment context.

  marketentry.facts/catalog is a generic, per-country evidence checklist
  ('you will need these documents to register at all'); this namespace
  grounds it in a SPECIFIC real opportunity's own eligibility floor
  (:min-rank / :required-categories), the concrete case an operator
  actually has in front of them ('here is the tender I want to bid on, and
  here is what registering for it requires'). Pure -- no I/O, no store.

  Fails closed: an opportunity that cannot be verified as genuinely tagged
  by the named connector (kotoba.ontology.connector/tagged-conforms?)
  produces nil, never a fabricated motivating case. This mirrors the
  blueprint's own charter (marketentry.facts: 'the advisor must not
  fabricate [a jurisdiction's requirements]') applied to the opportunity
  input side as well as the jurisdiction-catalog side."
  (:require [kotoba.ontology.connector :as connector]
            [marketentry.facts :as facts]))

(defn opportunity-assessment-context
  "Given `opportunity` tagged by `connector-id` (e.g. :jp.kkj), verified via
  kotoba.ontology.connector/tagged-conforms?, returns a map combining JPN's
  spec-basis evidence checklist with this specific opportunity's own
  eligibility requirement, or nil when the opportunity isn't genuinely
  tagged by that connector (fail closed) or JPN has no spec-basis (should
  not happen -- JPN is this blueprint's own home jurisdiction -- but this
  fn never assumes marketentry.facts/catalog's shape, it asks it)."
  [connector-id opportunity]
  (when (connector/tagged-conforms? connector-id opportunity)
    (when-let [sb (facts/spec-basis "JPN")]
      {:jurisdiction "JPN"
       :opportunity-id (:id opportunity)
       :opportunity-title (:title opportunity)
       :opportunity-source (:ontology/source opportunity)
       :required-rank (:min-rank opportunity)
       :required-categories (:required-categories opportunity)
       :checklist (:required-evidence sb)
       :spec-basis (:provenance sb)
       :legal-basis (:legal-basis sb)})))
