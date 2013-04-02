(ns very-fine-uploader.core 
  (:require
    [clojure.java.io :as io]
    [compojure.core :refer [defroutes GET POST]]
    [compojure.handler :as handler]
    [compojure.route :as route]
    [ring.middleware.resource :as resource]
    [ring.middleware.file-info :as file-info]
    [ring.middleware.keyword-params :as kparams]
    [hiccup.core :refer [html]])
  (:import
    [java.io FileOutputStream InputStream OutputStream]))

(defn handler
  "Saves uploaded file to \"save-to-path\" location, and generates the
  appropriate HTTP response to a \"Plupload\" request map."
  ([path {:keys [params multipart-params] :as request}]
   (let [multipart-params (#'kparams/keyify-params multipart-params)
         multipart? (> (count multipart-params) 0)
         params (if multipart? multipart-params params)
         path (str path "/" (-> params :qqfile :filename))
         chunk-index (Integer. (params "chunk" 0))
         body (if multipart? (:tempfile (:qqfile params)) (:body request))]
     (io/copy (try (cast InputStream body) (catch ClassCastException _ body))
              (cast OutputStream
                    (FileOutputStream. path (not= 0 chunk-index)))))))

(def response-success
  {:status 200
   :headers {"Content-Type" "application/json"}
   :body "{\"success\": true}"})

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

(defn insert-fineuploader [version id upload-url label]
  (list
    [:div {:id id}]
    [:script
     {:type "text/javascript"
      :src (format "/plugins/fineuploader-%1$s/fineuploader-%1$s.min.js" version)}]
    [:script {:type "text/javascript"}
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
       template: '<div class=\"qq-uploader span12\">' +
       '<pre class=\"qq-upload-drop-area span12\"><span>{dragZoneText}</span></pre>' +
       '<div class=\"qq-upload-button btn btn-success\" style= width: auto;\">{uploadButtonText}</div>' +
       '<span class=\"qq-drop-processing\"><span>{dropProcessingText}</span><span class=\"qq-drop-processing-spinner\"></span></span>' +
       '<ul class=\"qq-upload-list\" style=\"margin-top: 10px; text-align: center;\"></ul>' +
       '</div>',
       classes: {
       success: 'alert alert-success',
       fail: 'alert alert-error'
       }
       });
       }
       window.onload = createUploader;"
       id upload-url label)]))

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
