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

(defn listar-tarefas [request]
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

(defn remover-tarefa [request]
  (let [store (:store request)
        tarefa-id (get-in request [:path-params :id])
        tarefa-id-uuid (java.util.UUID/fromString tarefa-id)]
    (swap! store dissoc tarefa-id-uuid)
    {:status 200 :body {:mensagem "Removida com sucesso"}}))

(defn atualizar-tarefa [request]
  (let [tarefa-id (get-in request [:path-params :id])
        tarefa-id-uuid (java.util.UUID/fromString tarefa-id)
        nome (get-in request [:query-params :nome])
        status (get-in request [:query-params :status])
        tarefa (criar-tarefa-mapa tarefa-id-uuid nome status)
        store (:store request)]
    (swap! store assoc tarefa-id-uuid tarefa)
    {:status 200 :body {:mensagem "Tarefa atualizada com sucesso!"
                        :tarefa tarefa}})
  )

;definindo rotas
;coleçao recebe varios vetores
;cada vetor eh uma rota
(def routes (route/expand-routes
              #{["/hello" :get funcao-hello :route-name :hello-world]
                ["/tarefa" :post [db-interceptor criar-tarefa]  :route-name :criar-tarefa]
                ["/tarefa" :get [db-interceptor lista-tarefas] :route-name :listar-tarefas]
                ["/tarefa/:id" :delete [db-interceptor remover-tarefa] :route-name :remover-tarefa]
                ["/tarefa/:id" :patch [db-interceptor atualizar-tarefa] :route-name :atualizar-tarefa]}))


;configuracoes do projeto
(def service-map {::http/routes routes
                  ::http/port   9999
                  ::http/type   :jetty
                  ::http/join?  false})

(defonce server (atom nil))

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

(try (start-server) (catch Exception e (println "Erro ao executar start" (.getMessage e))))
(try (restart-server) (catch Exception e (println "Erro ao executar restart" (.getMessage e))))

(println "Server started/restarted")



(test-request :get "/hello?name=Matheus")
(test-request :post "/tarefa?nome=Correr&status=pendente")
(test-request :post "/tarefa?nome=Ler&status=pendente")
(test-request :post "/tarefa?nome=Estudar&status=feito")
(clojure.edn/read-string (:body (test-request :get "/tarefa")))
(test-request :delete "/tarefa/9a4fc7b5-be9f-4373-a6bf-34e4dd9aa4a3")
(test-request :patch "/tarefa/0731b434-6d82-4ff3-8180-e183d645adfc?nome=Rir&status=pendente")

(println @database/store)



