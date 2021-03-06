(ns rigui.core-test
  (:require [cljs.core :refer [ExceptionInfo]]
            [cljs.test :refer-macros [deftest is testing] :as t]
            [rigui.core :refer [start later! every! cancel! stop]]))

(deftest test-scheduler
  (let [task-count 10000
        task-time 2500
        task-counter (atom task-count)
        tws (start 1 8 (fn [_] (swap! task-counter dec)))
        verifier-tws (start 1 12 (fn [_]
                                   (is (= (count (stop tws)) 0))
                                   (is (= @task-counter 0))))]
    (time
     (dotimes [_ task-count]
       (later! tws nil (rand-int task-time))))
    (later! verifier-tws nil task-time)))

(deftest test-cancel
  (let [tw (start 1 8 (fn [_] (is false)))
        task (later! tw nil 2000)]
    (cancel! tw task)
    (is (= 0 (count (stop tw))))
    (is true)))

(deftest test-schedule-interval
  (testing "normal interval"
    (let [counter (atom 0)
          tw (start 1 8 (fn [f] (f)))
          t (every! tw #(swap! counter inc) 0 80)]
      (later! tw (fn [] (cancel! tw t) (is (= @counter 13))) 1000)))
  (testing "interval less than tick"
    (let [tw (start 1 8 (fn [_] (is false)))]
      (try (every! tw :a 0 1)
           (is false)
           (catch ExceptionInfo e
             (is (= :rigui.impl/invalid-interval (:reason (ex-data e))))
             (stop tw))))))
