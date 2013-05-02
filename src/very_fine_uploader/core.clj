(ns very-fine-uploader.core 
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [compojure.core :refer [defroutes GET POST]]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [ring.middleware.resource :as resource]
    [ring.middleware.file-info :as file-info]
    [ring.middleware.keyword-params :as kparams]
    [hiccup.core :refer [html]])
  (:import
    [java.io FileOutputStream InputStream OutputStream]))

(defn ->map [path {:keys [params multipart-params] :as request}]
  (let [multipart-params (#'kparams/keyify-params multipart-params)
        multipart? (> (count multipart-params) 0)
        {{filename :filename} :qqfile
         :as params} (if multipart? multipart-params params)
        filename (last (string/split filename #"\\"))]
    {:body (if multipart? (:tempfile (:qqfile params)) (:body request))
     :chunk-index (Integer. (params "chunk" 0))
     :params params
     :path (str path "/" (-> params :qqfile :filename))}))

(defn handle-file [{:keys [body path chunk-index]}]
  (io/copy (try (cast InputStream body) (catch ClassCastException _ body))
           (cast OutputStream
                 (FileOutputStream. path (not= 0 chunk-index)))))

(defn handler
  "Saves uploaded file to \"path\" location, from request."
  [path request]
  (handle-file (->map path request)))

(def response-success
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"success\": true}"})

(defn response-success-merge [m]
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body (json/write-str (merge {:success true} m))})

(def bootstrap-style-block
  [:style {:type "text/css"}
   " /* Fine Uploader
   -------------------------------------------------- */
   .qq-upload-list {
   text-align: left;
   }
   /* For the bootstrapped demos */
   li.alert-success {
   background-color: #DFF0D8;
   }
   li.alert-error {
   background-color: #F2DEDE;
   }
   .alert-error .qq-upload-failed-text {
   display: inline;
   }"])

(defn insert-fineuploader
  [version id upload-url label & {:keys [span fn-call]
                                  :as opts
                                  :or {span :span12}}]
  (list
    (when-not fn-call [:div {:id id}])
    [:script
     {:type "text/javascript"
      :src (format "/plugins/fineuploader-%1$s/fineuploader-%1$s.min.js" version)}]
    [:script {:type "text/javascript"}
     (if fn-call
       (format "window.onload = function() { %s(\"%s\", \"%s\", \"%s\"); };"
               (string/replace (if (or (symbol? fn-call)
                                       (keyword? fn-call))
                                 (str (namespace fn-call) \. (name fn-call))
                                 fn-call)
                               #"-|/"
                               {"-" "_" "/" "."})
               id upload-url label)
       (format
       "function createUploader() {
       var uploader = new qq.FineUploader({
       element: document.getElementById('%s'),
       request: {
       endpoint: '%s'
       },
       text: {
       uploadButton: '<div><i class=\"icon-upload icon-white\"></i> %s</div>'
       },
       template: '<div class=\"qq-uploader %s\">' +
       '<pre class=\"qq-upload-drop-area %s\"><span>{dragZoneText}</span></pre>' +
       '<div class=\"qq-upload-button btn btn-success\" style= width: auto;\">{uploadButtonText}</div>' +
       '<span class=\"qq-drop-processing\"><span>{dropProcessingText}</span><span class=\"qq-drop-processing-spinner\"></span></span>' +
       '<ul class=\"qq-upload-list\" style=\"margin-top: 10px; text-align: center;\"></ul>' +
       '</div>',
       fileTemplate: '<li>' +
           '<div class=\"qq-progress-bar\"></div>' +
           '<span class=\"qq-upload-spinner\"></span>' +
           '<span class=\"qq-upload-finished\"></span>' +
           '<span class=\"qq-upload-file\"></span>' +
           '<span class=\"qq-upload-size\"></span>' +
           '<a class=\"qq-upload-cancel\" href=\"#\">{cancelButtonText}</a>' +
           '<a class=\"qq-upload-retry\" href=\"#\">{retryButtonText}</a>' +
           '<a class=\"qq-upload-delete\" href=\"#\">{deleteButtonText}</a>' +
           '<span class=\"qq-upload-status-text\">{statusText}</span>' +
           '</li>',
       classes: {
       success: 'alert alert-success',
       fail: 'alert alert-error'
       },
       callbacks: {
       onComplete: function (id, name, response) {
       console.log(id, name, response);
       if (response.image) $('#image-attachments').append(response.image.markup);
       }
       }
       });
       }
       window.onload = createUploader;"
       id upload-url label (name span) (name span)))]))

(defn insert-fineuploader-debug
  [version id upload-url label & {:keys [span fn-call]
                                  :as opts
                                  :or {span :span12}}]
  (list
    (when-not fn-call [:div {:id id}])
    [:script
     {:type "text/javascript"
      :src (format "/plugins/fineuploader-%1$s/fineuploader-%1$s.js" version)}]
    [:script {:type "text/javascript"}
     (if fn-call
       (format "window.onload = function() { %s(\"%s\", \"%s\", \"%s\"); };"
               (string/replace (if (or (symbol? fn-call)
                                       (keyword? fn-call))
                                 (str (namespace fn-call) \. (name fn-call))
                                 fn-call)
                               #"-|/"
                               {"-" "_" "/" "."})
               id upload-url label)
       (format
       "function createUploader() {
       var uploader = new qq.FineUploader({
       element: document.getElementById('%s'),
       request: {
       endpoint: '%s'
       },
       text: {
       uploadButton: '<div><i class=\"icon-upload icon-white\"></i> %s</div>'
       },
       template: '<div class=\"qq-uploader %s\">' +
       '<pre class=\"qq-upload-drop-area %s\"><span>{dragZoneText}</span></pre>' +
       '<div class=\"qq-upload-button btn btn-success\" style= width: auto;\">{uploadButtonText}</div>' +
       '<span class=\"qq-drop-processing\"><span>{dropProcessingText}</span><span class=\"qq-drop-processing-spinner\"></span></span>' +
       '<ul class=\"qq-upload-list\" style=\"margin-top: 10px; text-align: center;\"></ul>' +
       '</div>',
       fileTemplate: '<li>' +
           '<div class=\"qq-progress-bar\"></div>' +
           '<span class=\"qq-upload-spinner\"></span>' +
           '<span class=\"qq-upload-finished\"></span>' +
           '<span class=\"qq-upload-file\"></span>' +
           '<span class=\"qq-upload-size\"></span>' +
           '<a class=\"qq-upload-cancel\" href=\"#\">{cancelButtonText}</a>' +
           '<a class=\"qq-upload-retry\" href=\"#\">{retryButtonText}</a>' +
           '<a class=\"qq-upload-delete\" href=\"#\">{deleteButtonText}</a>' +
           '<span class=\"qq-upload-status-text\">{statusText}</span>' +
           '</li>',
       classes: {
       success: 'alert alert-success',
       fail: 'alert alert-error'
       },
       callbacks: {
       onComplete: function (id, name, response) {
       console.log(id, name, response);
       if (response.image) $('#image-attachments').append(response.image.markup);
       }
       }
       });
       }
       window.onload = createUploader;"
       id upload-url label (name span) (name span)))]))

(defn fineuploader-css [version]
  [:link
   {:rel "stylesheet"
    :href (format "/plugins/fineuploader-%1$s/fineuploader-%1$s.css" version)}])

(def test-page
  (html [:html
         [:head
          (fineuploader-css "3.3.1")
          [:link
           {:rel "stylesheet"
            :href "//netdna.bootstrapcdn.com/twitter-bootstrap/2.1.1/css/bootstrap.min.css"}]
          bootstrap-style-block]
         [:body
          [:div.container
            [:div.row
             [:div.span12
              [:h1 "test"]]]
            [:div.row(insert-fineuploader "3.3.1" "bootstrap-fine-uploader" "/upload" "Upload")]]]]))

(defroutes app-routes
  (POST "/upload" request (do (handler "/tmp" request) response-success))
  (GET "/" [] test-page)
  (route/not-found "Not Found"))

(def app
  (-> app-routes
      handler/site
      (resource/wrap-resource "META-INF/resources")
      file-info/wrap-file-info))
