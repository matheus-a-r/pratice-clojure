(ns servico-clojure.core
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]))

(defn funcao-hello [request]
  {:status 200 :body (str "Hello World " (get-in request [:query-params :name]))})

;definindo rotas
;coleçao recebe varios vetores
;cada vetor eh uma rota
(def routes (route/expand-routes
              #{["/hello" :get funcao-hello :route-name :hello-world]}))


;configuracoes do projeto
(def service-map {::http/routes routes
                  ::http/port   9999
                  ::http/type   :jetty
                  ::http/join?  false})

;funcao para startar servidor com base nas configuacoes definidas no service-map
(http/start (http/create-server service-map))
(println "Started serve http")