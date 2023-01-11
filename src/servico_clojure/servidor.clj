(ns servico-clojure.servidor
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]
            [io.pedestal.test :as test]))

(def store (atom {}))

(defn lista-tarefas [request]
  {:status 200 :body @store})

(defn criar-tarefa-mapa [uuid nome status]
  {:id uuid :nome nome :status status})

(defn criar-tarefa [request]
  (let [uuid (java.util.UUID/randomUUID)
        nome (get-in request [:query-params :nome])
        status (get-in request [:query-params :status])
        tarefa (criar-tarefa-mapa uuid nome status)]
    (swap! store assoc uuid tarefa)
    {:status 200 :body {:mensagem "Tarefa registrada com sucesso!"
                        :tarefa tarefa}}))

(defn funcao-hello [request]
  {:status 200 :body (str "Hello World " (get-in request [:query-params :name] "Everybody"))})

;definindo rotas
;coleçao recebe varios vetores
;cada vetor eh uma rota
(def routes (route/expand-routes
              #{["/hello" :get funcao-hello :route-name :hello-world]
                ["/tarefa" :post criar-tarefa :route-name :criar-tarefa]
                ["/tarefa" :get lista-tarefas :route-name :lista-tarefas]}))


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
(println (test-request :post "/tarefa?nome=Ler&status=pendente"))
(println (test-request :post "/tarefa?nome=Correr&status=feito"))

(println "Listando todas as tarefas:")
(println (test-request :get "/tarefa"))

(println @store)



