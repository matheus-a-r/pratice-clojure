(ns servico-clojure.servidor
  (:require [io.pedestal.http.route :as route]
            [io.pedestal.http :as http]
            [io.pedestal.test :as test]
            [servico-clojure.database :as database]))

(defn assoc-store [context]
  (update context :request assoc :store database/store))

;coloca o store dentro da request para o criar-tarefa nao acessar o store diretamente
(def db-interceptor
  {:name :db-interceptor
   :enter assoc-store})

(defn lista-tarefas [request]
  {:status 200 :body @(:store request)})

(defn criar-tarefa-mapa [uuid nome status]
  {:id uuid :nome nome :status status})

(defn criar-tarefa [request]
  (let [uuid (java.util.UUID/randomUUID)
        nome (get-in request [:query-params :nome])
        status (get-in request [:query-params :status])
        tarefa (criar-tarefa-mapa uuid nome status)
        store (:store request)]
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
                ["/tarefa" :post [db-interceptor criar-tarefa]  :route-name :criar-tarefa]
                ["/tarefa" :get [db-interceptor lista-tarefas] :route-name :lista-tarefas]}))


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

(defn stop-server []
  (http/stop @server))

(defn restart-server []
  (stop-server)
  (start-server))

(start-server)
;(restart-server)
(println "Server started/restarted")



(test-request :get "/hello?name=Matheus")
(test-request :post "/tarefa?nome=Correr&status=pendente")
(test-request :post "/tarefa?nome=Ler&status=pendente")
(clojure.edn/read-string (:body (test-request :get "/tarefa")))
;(println @database/store)



