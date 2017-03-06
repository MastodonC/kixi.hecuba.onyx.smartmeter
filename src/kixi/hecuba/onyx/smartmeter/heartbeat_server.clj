(ns kixi.hecuba.onyx.smartmeter.heartbeat-server
  (require [clojure.java.io :as io]
           [bidi.bidi :refer [tag]]
           [bidi.vhosts :refer [make-handler vhosts-model]]
           [com.stuartsierra.component :as component]
           [taoensso.timbre :as timbre :refer [infof]]
           [yada.yada :as yada :refer [resource]]
           [yada.consume :refer [save-to-file]]
           [yada.resources.webjar-resource :refer [new-webjar-resource]]))

(defn healthcheck [ctx]
  (assoc (:response ctx)
         :status 200
         :body "All is well"))

(defn bidi-routes []
  ["" [["/health_check" healthcheck]]])

(defn routes
  "Create the URI route structure for our application."
  [config]
  [""
   [(bidi-routes)
    ;; This is a backstop. Always produce a 404 if we ge there. This
    ;; ensures we never pass nil back to Aleph.
    [true (yada/handler (fn [ctx]
                          (infof "Health Check 404 caught: %s " ctx)))]]])


(defrecord WebServer [port listener]
  component/Lifecycle
  (start [component]
    (if listener
      component
      (let [vhosts-model
            (vhosts-model
             [{:scheme :http :host (format "localhost:%d" port)}
              (routes {:port port})])
            listener (yada/listener vhosts-model {:port port})]
        (infof "Started web-server on port %s" listener)
        (assoc component :listener listener))))
  (stop [component]
    (when-let [close (get-in component [:listener :close])]
      (close))
    (assoc component :listener nil)))

(defn new-web-server [config]
  (map->WebServer (:web-server config)))
