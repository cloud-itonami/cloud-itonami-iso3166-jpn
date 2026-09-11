(ns marketentry.governor-contract-test
  "The governor contract as executable tests -- this vertical's own
  Trust Controls implemented faithfully. The single invariant under test:

    MarketEntry-LLM never drafts or submits a filing the Market-Entry
    Compliance Governor would reject, `:filing/draft`/`:filing/submit`
    NEVER auto-commit at any phase, `:engagement/intake` MAY auto-commit
    when clean, and every decision (commit OR hold) leaves exactly one
    ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [kotoba.ontology.connector :as connector]
            [langgraph.graph :as g]
            [marketentry.store :as store]
            [marketentry.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :market-entry-operator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- assess!
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-assess") {:op :jurisdiction/assess :subject subject} operator)
  (approve! actor (str tid-prefix "-assess")))

(defn- draft!
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-draft") {:op :filing/draft :subject subject} operator)
  (approve! actor (str tid-prefix "-draft")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :engagement/intake :subject "eng-1"
                   :patch {:id "eng-1" :operator "Kita Fabrication KK"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Kita Fabrication KK" (:operator (store/engagement db "eng-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest jurisdiction-assess-always-needs-approval
  (testing "assess is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :jurisdiction/assess :subject "eng-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "eng-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a jurisdiction/assess proposal with no official spec-basis -> HOLD"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :jurisdiction/assess :subject "eng-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "eng-1")) "no assessment written"))))

(def ^:private real-tagged-tender
  (connector/tag :jp.kkj {:id "kkj-real-1" :title "図書情報システム保守業務"
                          :status :open :min-rank :B :required-categories #{:役務}}))

(deftest motivating-opportunity-verified-grounds-the-assessment
  (testing "a real, ontology-tagged tender fact grounds the generic JPN checklist"
    (let [[db actor] (fresh)]
      (exec-op actor "t3a" {:op :engagement/intake :subject "eng-1"
                           :patch {:id "eng-1" :motivating-opportunity real-tagged-tender
                                   :motivating-connector :jp.kkj}} operator)
      (let [res (exec-op actor "t3b" {:op :jurisdiction/assess :subject "eng-1"} operator)]
        (is (= :interrupted (:status res)) "assess still always needs approval")
        (approve! actor "t3b")
        (let [assessment (store/assessment-of db "eng-1")]
          (is (true? (:motivating-opportunity-verified? assessment)))
          (is (= "kkj-real-1" (:motivating-opportunity-id assessment)))
          (is (= :B (:required-rank assessment)))
          (is (= #{:役務} (:required-categories assessment))))))))

(deftest motivating-opportunity-unverified-is-held-and-unoverridable
  (testing "a claimed motivating opportunity that fails ontology verification -> HARD hold"
    (let [[db actor] (fresh)]
      (exec-op actor "t3c" {:op :engagement/intake :subject "eng-1"
                           :patch {:id "eng-1" :motivating-opportunity {:id "fake" :status :open}
                                   :motivating-connector :jp.kkj}} operator)
      (let [res (exec-op actor "t3d" {:op :jurisdiction/assess :subject "eng-1"} operator)]
        (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
        (is (not= :interrupted (:status res)))
        (is (some #{:motivating-opportunity-unverified} (-> (store/ledger db) last :basis)))
        (is (nil? (store/assessment-of db "eng-1")) "no assessment written")))))

(deftest draft-without-assessment-is-held
  (testing "filing/draft before any jurisdiction assessment -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :filing/draft :subject "eng-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest japan-resident-rep-missing-is-held-and-unoverridable
  (testing "missing Japan-resident authorized representative -> HARD hold (flagship check)"
    (let [[db actor] (fresh)
          _ (assess! actor "t5pre" "eng-4")
          _ (draft! actor "t5pre" "eng-4")
          res (exec-op actor "t5" {:op :filing/submit :subject "eng-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:japan-resident-rep-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest engagement-fee-mismatch-is-held
  (testing "claimed fee that doesn't equal base + months x rate -> HOLD"
    (let [[db actor] (fresh)
          _ (assess! actor "t6pre" "eng-3")
          _ (draft! actor "t6pre" "eng-3")
          res (exec-op actor "t6" {:op :filing/submit :subject "eng-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:engagement-fee-mismatch} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest corporate-number-unverified-is-held-and-unoverridable
  (testing "unverified corporate number when required -> HARD hold"
    (let [[db actor] (fresh)
          _ (assess! actor "t7pre" "eng-5")
          _ (draft! actor "t7pre" "eng-5")
          res (exec-op actor "t7" {:op :filing/submit :subject "eng-5"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:corporate-number-unverified} (-> (store/ledger db) last :basis)))
      (is (empty? (store/submit-history db))))))

(deftest submit-always-escalates-then-human-decides
  (testing "a clean fully-assessed submit still ALWAYS interrupts for human approval"
    (let [[db actor] (fresh)
          _ (assess! actor "t8pre" "eng-1")
          _ (draft! actor "t8pre" "eng-1")
          r1 (exec-op actor "t8" {:op :filing/submit :subject "eng-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, submit record drafted"
        (let [r2 (approve! actor "t8")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:submitted? (store/engagement db "eng-1"))))
          (is (= 1 (count (store/submit-history db))) "one draft submit record"))))))

(deftest draft-always-escalates-then-human-decides
  (testing "a clean fully-assessed draft still ALWAYS interrupts for human approval"
    (let [[db actor] (fresh)
          _ (assess! actor "t9pre" "eng-1")
          r1 (exec-op actor "t9" {:op :filing/draft :subject "eng-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, draft record drafted"
        (let [r2 (approve! actor "t9")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:drafted? (store/engagement db "eng-1"))))
          (is (= 1 (count (store/draft-history db))) "one draft record"))))))

(deftest engagement-double-draft-is-held
  (testing "drafting the same engagement twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t10pre" "eng-1")
          _ (draft! actor "t10pre" "eng-1")
          res (exec-op actor "t10" {:op :filing/draft :subject "eng-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-drafted} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/draft-history db))) "still only the one earlier draft"))))

(deftest engagement-double-submit-is-held
  (testing "submitting the same engagement twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (assess! actor "t11pre" "eng-1")
          _ (draft! actor "t11pre" "eng-1")
          _ (exec-op actor "t11a" {:op :filing/submit :subject "eng-1"} operator)
          _ (approve! actor "t11a")
          res (exec-op actor "t11" {:op :filing/submit :subject "eng-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-submitted} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/submit-history db))) "still only the one earlier submit"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :engagement/intake :subject "eng-1"
                          :patch {:id "eng-1" :operator "Kita Fabrication KK"}} operator)
      (exec-op actor "b" {:op :jurisdiction/assess :subject "eng-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
