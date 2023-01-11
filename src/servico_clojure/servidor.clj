(ns servico-clojure.servidor
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]
            [io.pedestal.test :as test]))

(defn funcao-hello [request]
  {:status 200 :body (str "Hello World " (get-in request [:query-params :name] "Everybody"))})

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

(def server (atom nil))

;funcao para startar servidor com base nas configuacoes definidas no service-map
(defn start-server []
  (reset! server (http/start (http/create-server service-map))))

(defn test-request [verb url]
  (test/response-for (::http/service-fn @server) verb url))

(start-server)
(println (test-request :get "/hello?name=Matheus"))



