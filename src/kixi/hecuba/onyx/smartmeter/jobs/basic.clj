(ns kixi.hecuba.onyx.smartmeter.jobs.basic
  (:require [onyx.job :refer [add-task register-job]]
            [onyx.tasks.core-async :as core-async-task]
            [kixi.hecuba.onyx.smartmeter.tasks.entity-appender :as entity-appender]
            [kixi.hecuba.onyx.smartmeter.tasks.measurement-extraction :as measurement-extraction]
            [onyx.plugin.kafka]
            [onyx.plugin.s3-output]
            [onyx.plugin.s3-utils :as s3-utils]
            [onyx.tasks.kafka :as kafka-task]
            [onyx.tasks.s3 :as s3]
            [kixi.event2s3.shared]
            [taoensso.timbre :as timbre]
            [franzy.admin.zookeeper.client :as client]
            [franzy.admin.partitions :as fp]))

(defn basic-job
  [batch-settings]
  (let [base-job {:workflow [[:read-from-queue :apply-entity]
                             [:apply-entity :backup-raw]
                             [:apply-entity :extract-measurements]
                             [:extract-measurements :to-measurements-topic]]
                  :catalog []
                  :lifecycles []
                  :windows []
                  :triggers []
                  :flow-conditions []
                  :task-scheduler :onyx.task-scheduler/balanced}]
    (-> base-job
        (add-task (kafka-task/consumer :read-from-queue kafka-in-opts))
        (add-task (entity-appender/add-entity :apply-entity batch-settings))
        (add-task (measurement-extraction/get-measurements :extract-measurements batch-settings))
        (add-task (s3/s3-output :backup-raw s3-opts))
        (add-task (kafka-task/producer :to-measurements-topic kafka-out-opts)))))


(defn get-partition-count-for-topic
  [zk-addr topic]
  (let [zk-utils (client/make-zk-utils {:servers zk-addr} false)]
    (try
      (-> zk-utils
          (fp/partitions-for [topic])
          (get (keyword topic))
          (count))
      (catch Exception e
        1)
      (finally
        (.close zk-utils)))))

(defmethod register-job "basic-job"
  [job-name config]
  (let [{:keys [kafka-in-topic kafka-out-topic aws-region s3-bucket batch-size batch-timeout]} (:job-config config)
        zk-addr (get-in config [:env-config :zookeeper/address])
        kafka-topic-partitions-in (get-partition-count-for-topic zk-addr kafka-in-topic)
        kafka-topic-partitions-out (get-partition-count-for-topic zk-addr kafka-out-topic)
        _ (timbre/info "Detected" kafka-topic-partitions "Kafka partitions for topic" kafka-topic)
        onyx-batch-size (get-in config [:job-config :onyx-batch-size])
        kafka-in-opts {:onyx/batch-size batch-size
                       :onyx/batch-timeout batch-timeout
                       :onyx/type :input
                       :onyx/medium :kafka
                       :onyx/min-peers kafka-topic-partitions ;; should be number of partitions
                       :onyx/max-peers kafka-topic-partitions
                       :kafka/zookeeper (get-in config [:env-config :zookeeper/address])
                       :kafka/topic kafka-topic
                       :kafka/group-id "hecuba-onyx-smartmeter"
                       :kafka/receive-buffer-bytes 65536
                       :kafka/offset-reset :latest
                       :kafka/force-reset? false
                       :kafka/wrap-with-metadata? false
                       :kafka/commit-interval 500
                       :kafka/deserializer-fn :onyx.travelport.workflow.shared/deserialize-gzip-message
                       :onyx/doc "Reads messages from a Kafka topic"}
        kafka-out-opts {:onyx/name :write-messages
                        :onyx/plugin :onyx.plugin.kafka/write-messages
                        :onyx/type :output
                        :onyx/medium :kafka
                        :kafka/topic kafka-out-topic
                        :kafka/zookeeper "127.0.0.1:2181"
                        :kafka/serializer-fn :kixi.hecuba.onyx.smartmeter.shared/serialise-json
                        :kafka/request-size 307200
                        :onyx/batch-size batch-size
                        :onyx/doc "Writes messages to a Kafka topic"}
        s3-opts    {:s3/bucket (get-in config [:job-config :s3-bucket-name])
                    :s3/serializer-fn :kixi.hecuba.onyx.smartmeter.shared/gzip-serializer-fn
                    :s3/key-naming-fn :kixi.hecuba.onyx.smartmeter.shared/s3-naming-function
                    :onyx/type :output
                    :onyx/medium :s3
                    :onyx/min-peers 1
                    :onyx/max-peers 1
                    :onyx/batch-size batch-size
                    :onyx/batch-timeout batch-timeout
                    :onyx/doc "Writes segments to s3 files, one file per batch"}]
    (basic-job batch-settings kafka-in-opts kafka-out-opts s3-opts)))
