(ns kixi.hecuba.onyx.smartmeter.tasks.process-smartmeter
  (:require [clojure.xml :as xml]
            [schema.core :as s]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [taoensso.timbre :as timbre]))
;; sample file in resources/test-files/ folder.
(defn get-data [segment]
  (mapv #(let [datetime (->> %
                             :content
                             first
                             :content
                             first)
               primary (->> %
                            :content
                            second
                            :content
                            first
                            :content
                            first
                            Double/valueOf)
               secondary (->> %
                              :content
                              second
                              :content
                              second
                              :content
                              first
                              Double/valueOf)]
           ;; WIP: create the measurements to push to hecuba measurements queue here....
           {:primary primary :secondary secondary :timestamp datetime}) (get-in segment [:content])))

(s/defn measurement-extraction
  ([task-name :- s/Keyword task-opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/type :function
                             :onyx/fn :kixi.hecuba.onyx.smartmeter.tasks.process-smartmeter/get-data}
                            task-opts)}}))


(comment

  ;; all the required fields in a map
  (mapv #(let [datetime (->> %
                             :content
                             first
                             :content
                             first)
               primary (->> %
                            :content
                            second
                            :content
                            first
                            :content
                            first
                            Double/valueOf)
               secondary (->> %
                              :content
                              second
                              :content
                              second
                              :content
                              first
                              Double/valueOf)]
           {:primary primary :secondary secondary :timestamp datetime}) (get-in meas [:content]))

  ;; primary electricity values
  (mapv (fn [row] (->> row
                       :content
                       second
                       :content
                       first)) (get-in meas [:content]))
  ;; secondary
  (mapv (fn [row] (->> row
                       :content
                       second
                       :content
                       second)) (get-in meas [:content]))
  ;; get timestamp of reading
  (mapv (fn [row] (->> row
                       :content
                       first
                       )) (get-in meas [:content])))
