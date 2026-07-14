(ns statute.facts-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [statute.facts :as facts]))

(deftest jpn-has-spec-basis
  (let [sb (facts/spec-basis "JPN")]
    (is (= 4 (count sb)))
    (is (every? #(str/starts-with? (:statute/url %) "https://laws.e-gov.go.jp/") sb))
    (is (every? :statute/law-number sb))))

(deftest unknown-jurisdiction-has-no-spec-basis
  (is (nil? (facts/spec-basis "ATL")))
  (is (nil? (facts/spec-basis "ZZZ"))))

(deftest coverage-is-honest
  (let [c (facts/coverage ["JPN" "USA" "ATL"])]
    (is (= 3 (:requested c)))
    (is (= 1 (:covered c)))
    (is (= ["ATL" "USA"] (:missing-jurisdictions c)))))

(deftest by-topic-filters
  (is (= ["jpn.labor-standards-act"]
         (mapv :statute/id (facts/by-topic "JPN" :labor))))
  (is (= ["jpn.aptcp-enforcement-regulation"]
         (mapv :statute/id (facts/by-topic "JPN" :identity-verification))))
  (is (empty? (facts/by-topic "JPN" :environment)))
  (is (empty? (facts/by-topic "ATL" :labor))))
