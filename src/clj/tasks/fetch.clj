(ns tasks.fetch
  "MECCGDB import tasks"
  (:require [web.db :refer [db] :as webdb]
            [clojure.string :as string]
            [tasks.meccgdb :refer :all]
            [tasks.altart :refer [add-art]]))

(defn fetch
  "Import data from MECCGDB.
  Can accept `--local <path>` to use the `meccg-card-json` project locally,
  otherwise pulls data from NRDB.
  Specifying `--no-card-images` will not attempt to download images for cards."
  [& args]
  (webdb/connect)
  (try
    (let [use-local (some #{"--local"} args)
          localpath (first (remove #(string/starts-with? % "--") args))
          download-fn (if use-local
                        (partial read-local-data localpath)
                        download-meccgdb-data)
          mwls (fetch-data download-fn (:mwl tables))
          sets (fetch-data download-fn (:set tables))
          card-download-fn (if use-local
                             (partial read-card-dir localpath)
                             download-meccgdb-data)
          cards (fetch-cards card-download-fn (:card tables) sets (not (some #{"--no-card-images"} args)))]
      (println (count sets) "sets imported")
      (println (count mwls) "MWL versions imported")
      (println (count cards) "cards imported")
      (add-art false)
      (update-config (:config tables)))
    (catch Exception e (do
                         (println "Import data failed:" (.getMessage e))
                         (.printStackTrace e)))
    (finally (webdb/disconnect))))
