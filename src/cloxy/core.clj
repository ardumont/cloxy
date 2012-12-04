(ns cloxy.core
  (:use     [clojure.pprint :only [pprint print-table]]
            [clojure.string :only [split join]]
            [clojure.repl   :only [doc]]
            [table.core     :only [table]])
  (:require [clojure
             [string            :as str]
             [set               :as set]
             [walk              :as w]
             [xml               :as xml]]
            [clojure.java
             [shell             :as sh]
             [io                :as io]]
            [clj-http.client    :as c]
            [ring.adapter.jetty :as rj]))

;; utilities ==================================================================

(defn prns
  "Print a preview of a datastructure"
  ([                         s] (prns 2 s))
  ([depth                    s] (prns depth depth s))
  ([print-level print-length s] (binding [*print-level*  print-level
                                          *print-length* print-length]
                                  (pprint s))))

;; http cli ===================================================================

(def omdb-url "http://www.omdbapi.com/")

(defn omdb-q "Template for all omdb queries"
  [& [opts]]
  (c/request (merge {:debug  false
                     :method :get
                     :url    omdb-url}
                    opts)))

(defn- get-fake-server
  [] (:body (c/get "http://localhost:9090")))

(defn- get-proxy
  [] (:body (c/get "http://localhost:3009")))

(comment
  "- omdb example:"
  (omdb-q {:query-params {"t" "True Grit", "y" "1969"}})

  "curl equivalent:"
  (sh/sh "curl" "-s" (str omdb-url "?t=True%20Grit&y=1969"))

  "- fake server example"
  (sh/sh "curl" "-s" "http://localhost:9090?t=True%20Grit&y=1969")

  (c/get "http://localhost:9090"))

;; http server ================================================================

(defn- show
  [x]
  (println)
  (println "show")
  (println (str "type="(type x)))
  (pprint x)
  x)

(defn wrap-debug "A middleware that debugs the request."
  [handler]
  (fn [request]
    (println "-------")
    (pprint  request)
    (handler request)))

(defn- for-fake?
  [request]
  (re-find #"^/fake-server" (:uri request)))

(def routing
  {#"^/bobby"       "www.google.com"
   #"^/fake-server" "localhost:9090"})



(let [uri (:uri request)] (map (fn [x] [(re-find x uri) x])
                               (keys routing)))



(defn- redirect "Take a request and redirect it, if it maches one of the routing table entry"
  [request routing])

(defn wrap-proxy "A middleware that will relay the request to another server, depending on its routing table"
  [handler routing]
  (fn [request]
    (println "++++++++++++++++++++++++++++++++++++>>> proxy was here ;)")
    (if (for-fake? request)
      (c/get "http://localhost:9090")
      (handler request))))

(defn- response "Takes a body as a string, return the response body (string)"
  [body-str] (-> body-str
                 read-string
                 eval
                 str))

(defn- response "Takes a body as a string, return the response body (string)"
  [body-str] (str "hello world !, date=" (java.util.Date.)))

(defn handler [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (-> request
                :body
                slurp
                response)})

(def app
  (-> handler
      #_wrap-debug
      (wrap-proxy nil)))

;; Stop jetty-server, if it exists
(declare stop)
(if (resolve 'jetty-server) (stop))

(def jetty-server
  (rj/run-jetty app {:port 3009
                     :join? false}))

(defn start   [] (.start jetty-server))
(defn stop    [] (.stop  jetty-server))
(defn restart [] (stop) (start))

(comment
  (start)
  (stop)
  (restart))

(comment "Usage:"
         "In a shell run:"

         (sh/sh "curl" "-s" "http://localhost:3009"))

