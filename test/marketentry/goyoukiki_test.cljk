(ns marketentry.goyoukiki-test
  (:require [clojure.test :refer [deftest is]]
            [kotoba.ontology.connector :as connector]
            [marketentry.goyoukiki :as goyoukiki]))

(def real-tender
  (connector/tag :jp.kkj {:id "kkj-real-1" :title "図書情報システム保守業務"
                          :status :open :min-rank :B :required-categories #{:役務}}))

(deftest opportunity-assessment-context-grounds-jpn-checklist-in-a-real-tagged-opportunity
  (let [ctx (goyoukiki/opportunity-assessment-context :jp.kkj real-tender)]
    (is (= "JPN" (:jurisdiction ctx)))
    (is (= "kkj-real-1" (:opportunity-id ctx)))
    (is (= :jp.kkj (:opportunity-source ctx)))
    (is (= :B (:required-rank ctx)))
    (is (= #{:役務} (:required-categories ctx)))
    (is (seq (:checklist ctx)) "the generic JPN evidence checklist is still attached")
    (is (some? (:spec-basis ctx)))))

(deftest opportunity-assessment-context-refuses-an-untagged-opportunity
  (is (nil? (goyoukiki/opportunity-assessment-context :jp.kkj {:id "x" :status :open}))
      "a plain fact with no :ontology/* provenance never grounds an assessment"))

(deftest opportunity-assessment-context-refuses-a-mistagged-opportunity
  (is (nil? (goyoukiki/opportunity-assessment-context :jp.geps real-tender))
      "a fact tagged by jp.kkj cannot be claimed by jp.geps"))
