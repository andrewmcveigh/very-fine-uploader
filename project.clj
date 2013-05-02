(defproject com.andrewmcveigh/very-fine-uploader "0.1.1-SNAPSHOT"
  :description "Packaged Fine Uploader (http://fineuploader.com/) for easy use
               from Clojure."
  :url "http://github.com/andrewmcveigh/very-fine-uploader"
  :license {:name "GPLv3"
            :url "http://www.gnu.org/licenses/gpl.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.2.2"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.3"]]
  :plugins [[lein-ring "0.8.2"]]
  :ring {:handler very-fine-uploader.core/app}
  :repositories [["snapshots" {:url "https://clojars.org/repo/" :creds :gpg}]
                 ["releases" {:url "https://clojars.org/repo/" :creds :gpg}]])
