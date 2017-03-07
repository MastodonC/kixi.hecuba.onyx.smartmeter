(ns kixi.hecuba.onyx.smartmeter.tasks.process-smartmeter
  (:require [clojure.xml :as xml]
            [schema.core :as s]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.string :as cstr]
            [taoensso.timbre :as timbre]))

;; For measurements JSON definition see: https://github.com/MastodonC/kixi.hecuba/blob/master/doc/api.md#measurements
;; Values are sent to the API as strings so no conversion needs doing.
;; Timestamp from Aprose is the same as the Hecuba API.
(defn get-data [segment]
  (let [measurements (map #(let [consumption-type (-> %
                                                      :content
                                                      second
                                                      :tag
                                                      name
                                                      cstr/lower-case
                                                      (str "Consumption"))
                                 datetime (->> %
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
                                              first)]
                             {:value primary :timestamp datetime :type consumption-type}) (get-in segment [:content]))]
    {:measurements measurements
     :kafka-payload {:entity-id "entity-id-to-do"
                     :device-id "device-id-to-do"}}))

(s/defn measurement-extraction
  ([task-name :- s/Keyword task-opts]
   {:task {:task-map (merge {:onyx/name task-name
                             :onyx/type :function
                             :onyx/fn :kixi.hecuba.onyx.smartmeter.tasks.process-smartmeter/get-data}
                            task-opts)}}))
