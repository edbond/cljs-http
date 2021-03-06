(ns cljs-http.core
  (:import [goog.net XhrIo]
           [goog.net EventType])
  (:require [cljs-http.util :as util]
            [cljs.core.async :as async]
            [goog.events :as gevents]))

(defn request
  "Execute the HTTP request corresponding to the given Ring request
  map and return a core.async channel."
  [{:keys [request-method headers body xhr with-credentials?]
    :or {:with-credentials? true} :as request}]
  (let [channel (async/chan)
        method (name (or request-method :get))
        timeout (or (:timeout request) 0)
        headers (util/build-headers headers)
        cb #(let [target (.-target %1)]
              (->> {:status (.getStatus target)
                    :body (.getResponseText target)
                    :headers (util/parse-headers (.getAllResponseHeaders target))}
                   (async/put! channel))
              (async/close! channel))]
    (gevents/listen xhr EventType.COMPLETE cb)
    (if with-credentials?
      (.setWithCredentials xhr true))
    (if timeout
      (.setTimeoutInterval xhr timeout))
    (.send xhr (util/build-url request) method body headers)
    channel))
