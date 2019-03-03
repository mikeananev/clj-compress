(ns clj-compress.core
  (:require [clojure.java.io :refer [file output-stream input-stream] :as io]
            [clojure.string :as str]
            [clojure.set])
  (:import (org.apache.commons.compress.compressors CompressorStreamFactory)
           (org.apache.commons.compress.utils IOUtils)
           (org.apache.commons.compress.archivers.tar TarArchiveOutputStream)
           (java.io File BufferedInputStream)
           (org.apache.commons.io FilenameUtils)
           (org.apache.commons.compress.archivers ArchiveStreamFactory)))

(def compressors ["lzma" "gz" "bzip2" "snappy-framed" "deflate" "lz4-framed" "xz"])

(def archive-extensions {"lzma"          ".tar.lzma"
                         "gz"            ".tar.gz"
                         "bzip2"         ".tar.bz2"
                         "snappy-framed" ".tar.sz"
                         "deflate"       ".tar.gz"
                         "lz4-framed"    ".tar.lz4"
                         "xz"            ".tar.xz"})

(defn compress-data
  "compress `src` using particular `compressor` and write compressed data into `dest`.
  `src` can be InputStream, File, URI, URL, Socket, byte array, or String.
  `dest` can be OutputStream, File, URI, URL, Socket, and String
   When `src` or `dest` is String then it is treated as filename.
  `compressor` - should be one strings from `compressors`
  return number of processed bytes from input stream (length of src).
  `dest` will receive raw compressed bytes."
  [src dest ^String compressor]
  (let [in    (input-stream src)
        f-out (output-stream dest)]
    (try
      (let [out    (.createCompressorOutputStream (CompressorStreamFactory.) compressor f-out)
            length (IOUtils/copy in out)]
        (.flush out)
        (.close in)
        (.close out)
        length)
      (catch Exception e
        (.close in)
        (.close f-out)
        (throw e)))))


(defn decompress-data
  "decompress `src` using particular `decompressor` and write normal data into `dest`.
  `src` can be InputStream, File, URI, URL, Socket, byte array, or String with compressed data.
  `dest` can be OutputStream, File, URI, URL, Socket, and String
   When `src` or `dest` is String then it is treated as filename.
  `decompressor` - should be one strings from `compressors`
  return number of bytes written to dest (length of normal data)."
  [src dest ^String decompressor]
  (let [in    (input-stream src)
        f-out (output-stream dest)]
    (try
      (let [in     (.createCompressorInputStream (CompressorStreamFactory.) decompressor in)
            length (IOUtils/copy in f-out)]
        (.flush f-out)
        (.close in)
        (.close f-out)
        length)
      (catch Exception e
        (.close in)
        (.close f-out)
        (throw e)))))

(defn- new-archive-name
  "return new archive name based on input parameters"
  [arch-name out-folder compressor]
  (let [extension        (get archive-extensions compressor)
        out-folder       (FilenameUtils/normalizeNoEndSeparator out-folder)
        final-out-folder (if (= File/separator out-folder) "" out-folder)
        full-name        (str final-out-folder File/separator arch-name extension)]
    full-name))


(defn- relativise-path
  "create relative archive entry name"
  [base path]
  (let [f        (file base)
        uri      (.toURI f)
        relative (.relativize uri (-> path file .toURI))]
    (.getPath relative)))


(defn create-archive
  "Create archive for given bulk of files or folders `input-files-vec` (String names vector).
  An archive may be decompressed by external tools (tar, unzip, bunzip2 etc...).
  New archive file will be placed to `out-folder`.
  `new-arch-name` - only name of new archive without extension or path (e.g. \"myarc\")
  At first, input data is moved to tar archive, then tar archive is compressed by `compressor`.
  An archive extension will be added automatically and depends on `compressor` type.
  Returns created archive file name as `String`."
  [^String new-arch-name input-files-vec ^String out-folder ^String compressor]
  (let [out-fname (new-archive-name new-arch-name out-folder compressor)
        fo        (output-stream out-fname)
        cfo       (.createCompressorOutputStream (CompressorStreamFactory.) compressor fo)
        a         (TarArchiveOutputStream. cfo)]
    (doseq [input-name input-files-vec]
      (let [folder? (.isDirectory (file input-name))]
        (doseq [f (if folder? (file-seq (file input-name)) [(file input-name)])]
          (when (and (.isFile f) (not= out-fname (.getPath ^File f)))
            (let [entry-name (relativise-path (FilenameUtils/getPath input-name) (-> f .getPath))
                  entry      (.createArchiveEntry a f entry-name)]
              (.putArchiveEntry a entry)
              (when (.isFile f)
                (IOUtils/copy (input-stream f) a))
              (.closeArchiveEntry a))))))
    (.finish a)
    (.close a)
    out-fname))


(defn decompress-archive
  "decompress data from archive to `out-folder` directory.
  Warning! In `out-folder` files will be overwritten by decompressed data from `arch-name`.
  `compressor` is optional argument, ArchiveStreamFactory tries to guess compressor type based on archive extension.
  returns number of decompressed entries (files)."
  [^String arch-name ^String out-folder & [compressor]]
  (let [in         (BufferedInputStream. (input-stream (file arch-name)))
        cis        (if compressor (.createCompressorInputStream (CompressorStreamFactory.) compressor in)
                                  (.createCompressorInputStream (CompressorStreamFactory.) in))
        ais        (.createArchiveInputStream (ArchiveStreamFactory.) ArchiveStreamFactory/TAR cis)
        file-count (loop [entry (.getNextEntry ais)
                          cnt   0]
                     (if entry
                       (let [save-path (str out-folder File/separatorChar (.getName entry))
                             out-file  (File. save-path)]
                         (if (.isDirectory entry)
                           (if-not (.exists out-file)
                             (.mkdirs out-file))
                           (let [parent-dir (File. (.substring save-path 0 (.lastIndexOf save-path (int File/separatorChar))))]
                             (if-not (.exists parent-dir) (.mkdirs parent-dir))
                             (clojure.java.io/copy ais out-file :buffer-size 8192)))
                         (recur (.getNextEntry ais) (inc cnt)))
                       cnt))]
    (.close ais)
    (.close cis)
    (.close in)
    file-count))

(defn list-archive
  "return list of archived items"
  [^String arch-name & [compressor]]
  (let [in         (BufferedInputStream. (input-stream (file arch-name)))
        cis        (if compressor (.createCompressorInputStream (CompressorStreamFactory.) compressor in)
                                  (.createCompressorInputStream (CompressorStreamFactory.) in))
        ais        (.createArchiveInputStream (ArchiveStreamFactory.) ArchiveStreamFactory/TAR cis)
        file-count (loop [entry      (.getNextEntry ais)
                          cnt        0
                          item-list []]
                     (if entry
                       (recur (.getNextEntry ais) (inc cnt) (conj item-list {:item/name          (.getName entry)
                                                                              :item/size          (.getSize entry)
                                                                              :item/last-modified (.getLastModifiedDate entry)}))
                       {:item/count cnt
                        :item/list item-list}))]
    (.close ais)
    (.close cis)
    (.close in)
    file-count))

(comment
  (create-archive "abc" ["data/test-folder"] "data/" "bzip2")
  (decompress-archive "data/abc.tar.bz2" "data/out/" "bzip2")
  (list-archive "data/abc.tar.bz2"))