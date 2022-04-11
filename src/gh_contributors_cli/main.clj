(ns gh-contributors-cli.main
  (:gen-class)
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.string :as str]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.tools.cli :refer [parse-opts]]))

(def user-credentials
  {:username (System/getenv "GH_FETCH_CONTRIBUTORS_USER")
   :password (System/getenv "GH_FETCH_CONTRIBUTORS_TOKEN")})

(def cli-options
  [["-r" "--repo REPO" "Repository name. E.g.: apache/spark"
    :validate [#(re-matches #"[\w,\-,\_]+\/[\w,\-,\_]+" %) "Must be a valid owner/repository name"]]
   ["-h" "--help"]])

(def output-attributes
  [:html_url :email :name :most_recent_commit_date 
   :twitter_username :login :following :updated_at 
   :bio :contributions :location :blog
   :followers :company :created_at])

(def application-gh-json "application/vnd.github.v3+json")

(def contributors-filename "contributors.json")
(def contributors+user-filename "contributors+user.json")
(def contributors+user+commit-filename "contributors+user+commit.json")

(def data-dir ".data")
(def contributors+user-dir "contributors+user")
(def user+commit-dir "user+commit")

(defn- repo->data-dir-meta
  [repo]
  (let [base-path (str data-dir "/" repo)]
    {:base-dir-path              (str data-dir "/" repo)
     :contributors+user-dir-path (str base-path "/" contributors+user-dir)
     :user+commit-dir-path       (str base-path "/" user+commit-dir)}))

  (defn- data-dir!
    [cli-parameters]
    (.mkdirs (io/as-file (:base-dir-path cli-parameters)))
    (.mkdirs (io/as-file (:contributors+user-dir-path cli-parameters)))
    (.mkdirs (io/as-file (:user+commit-dir-path cli-parameters)))
    data-dir)

(defn- contributors!
  ([repo]
   (contributors! repo 1))
  ([repo page]
   (-> (curl/get (str "https://api.github.com/repos/" repo "/contributors")
                 {:basic-auth [(:username user-credentials) (:password user-credentials)]
                  :query-params {"page" (str page)
                                 "per_page" (str 100)}
                  :headers {"Accept" application-gh-json}})
       :body
       (json/parse-string true))))

(defn- repo+filename->file
  [repo filename]
  (io/as-file (str data-dir "/" repo "/" filename)))

(defn- file->json!
  [file]
  (when (.exists file)
    (-> (slurp file)
        (json/parse-string true))))

(defn- json->file!
  [json file]
  (spit file (json/generate-string json))
  json)

(defn all-contributors!
  [repo]
  (let [cf                  (repo+filename->file repo contributors-filename)
        stored-contributors (file->json! cf)]
    (if (some? stored-contributors)
      stored-contributors
      (do
        (println (str "Fetching all " repo " contributors"))
        (let [all-contributors (loop [page 1
                                      acc []]
                                 (println (str "Fetching page " page))
                                 (let [contributors (contributors! repo page)]
                                   (if (not-empty contributors)
                                     (recur (inc page) (concat acc contributors))
                                     acc)))]
          (json->file! all-contributors cf)
          (file->json! cf))))))

(defn- user-file
  [user-file-path id]
  (io/as-file (str user-file-path "/" id)))

(defn user!
  [user-file-path contributor]
  (let [id (:id contributor)
        url (:url contributor)
        stored-contributor+user (file->json! (user-file user-file-path id))]
    (if (some? stored-contributor+user)
      stored-contributor+user
      (do
        (println (str "Fetching user " (:url contributor)))
        (try (let [user (-> (curl/get url {:basic-auth [(:username user-credentials) (:password user-credentials)]
                                           :headers {"Accept" application-gh-json}})
                            :body
                            (json/parse-string true))
                   contributor+user (conj contributor user)]
               (spit (user-file user-file-path id) (json/generate-string contributor+user))
               (file->json! (user-file user-file-path id)))
             (catch java.lang.Exception e
               (throw (ex-info (.getMessage e) contributor))))))))

(defn- user+commit-file
  [user+commit-file-path id]
  (io/as-file (str user+commit-file-path "/" id)))

(defn- id->user+commit!
  [user+commit-file-path id]
  (let [user+commit-file (user+commit-file user+commit-file-path id)]
    (when (.exists user+commit-file)
      (-> (slurp user+commit-file)
          (json/parse-string true)))))

(defn user+commit!
  [repo user+commit-dir-path contributor+user]
  (let [id                 (:id contributor+user)
        login              (:login contributor+user)
        stored-user+commit (id->user+commit! user+commit-dir-path id)]
    (if (some? stored-user+commit)
      stored-user+commit
      (do
        (println (str "Fetching latest commit of " login))
        (try (let [most-recent-commit-date (-> (curl/get (str "https://api.github.com/repos/" repo "/commits")
                                                         {:basic-auth [(:username user-credentials) (:password user-credentials)]
                                                          :headers {"Accept" application-gh-json}
                                                          :query-params {"page" "1"
                                                                         "per_page" "5"
                                                                         "author" login}})
                                               :body
                                               (json/parse-string true)
                                               (first)
                                               (get-in [:commit :author :date]))
                   user+commit (conj contributor+user {:most_recent_commit_date most-recent-commit-date})]
               (spit (user+commit-file user+commit-dir-path id) (json/generate-string user+commit))
               (id->user+commit! user+commit-dir-path id))
             (catch java.lang.Exception e
               (throw (ex-info (.getMessage e) contributor+user))))))))

(defn- entry->csv-row
  [map-entry]
  (map #(str (get-in map-entry [%])) output-attributes))

(defn ->output-headers
  [attrs]
  (vec (map name attrs)))

(defn -main [& args]
  (let [username       (:username user-credentials)
        password       (:password user-credentials)
        parsed-opts    (parse-opts args cli-options)
        cli-args       (:options parsed-opts)
        cli-parameters (merge (repo->data-dir-meta (:repo cli-args))
                              cli-args)
        repo           (:repo cli-parameters)]
    (if (and username password cli-args)
      (do
        (data-dir! cli-parameters)
        (let [contributors                  (all-contributors! repo)
              contributors+user-file        (repo+filename->file repo contributors+user-filename)
              contributors+u+c-file         (repo+filename->file repo contributors+user+commit-filename)
              contributors+user-json        (json->file! (map (partial user! (:contributors+user-dir-path cli-parameters))
                                                              contributors) contributors+user-file)
              contributors+user+commit-json (json->file!
                                             (map (partial user+commit! repo (:user+commit-dir-path cli-parameters))
                                                  contributors+user-json)
                                             contributors+u+c-file)]
          (with-open [csv-writer (io/writer (io/as-file
                                             (str data-dir "/" repo "/result.csv")))]
            (csv/write-csv csv-writer [(->output-headers output-attributes)])
            (doseq [contributor contributors+user+commit-json]
              (csv/write-csv csv-writer [(entry->csv-row contributor)])))))
      (cond
        (not-empty (:errors cli-args)) (println (str/join "\n" (:errors cli-args)))
        (empty? (:options cli-args)) (println (:summary cli-args))
        :else (println "GH_FETCH_CONTRIBUTORS_USER and GH_FETCH_CONTRIBUTORS_TOKEN env vars are not set.")))))
