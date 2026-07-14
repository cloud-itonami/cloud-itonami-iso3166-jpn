(ns marketentry.ekyc-bridge-test
  (:require [clojure.test :refer [deftest is]]
            [kotoba.ekyc :as ekyc]
            [marketentry.ekyc-bridge :as bridge]))

(deftest statute-entry-resolves-the-real-citation
  (let [st (bridge/statute-entry)]
    (is (some? st))
    (is (= "jpn.aptcp-enforcement-regulation" (:statute/id st)))
    (is (= "https://laws.e-gov.go.jp/law/420M60000F5A001" (:statute/url st)))
    (is (contains? (:statute/topic st) :identity-verification))))

(deftest identity-verification-methods-grounds-the-full-catalog-in-the-statute
  (let [{:keys [statute methods]} (bridge/identity-verification-methods)]
    (is (= "jpn.aptcp-enforcement-regulation" (:statute/id statute)))
    (is (= (count ekyc/method-catalog) (count methods)))
    (is (= ekyc/method-catalog methods))))

(deftest methods-for-subject-delegates-to-kotoba-ekyc
  (is (= (ekyc/methods-for :individual) (bridge/methods-for-subject :individual)))
  (is (= (ekyc/methods-for :corporate) (bridge/methods-for-subject :corporate)))
  (is (= 8 (count (bridge/methods-for-subject :individual))))
  (is (= 2 (count (bridge/methods-for-subject :corporate)))))

(deftest validate-representative-identity-mirrors-kotoba-ekyc-validate
  (let [claim (ekyc/subject-claim "Yamada Taro" "1-1-1 Chiyoda, Tokyo" "1990-01-01")
        v (ekyc/verification "V-bridge-1" :he claim
            [(ekyc/evidence :ic-chip-read "chip-ref" :fields-confirmed [:name :address :dob :photo])
             (ekyc/evidence :facial-image "face-ref" :captured-live? true)])]
    (is (= (ekyc/validate v) (bridge/validate-representative-identity v)))
    (is (= :pass (:ekyc.result/disposition (bridge/validate-representative-identity v))))))
