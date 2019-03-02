(ns middlesphere.clj-compress
  (:require [clojure.java.io :refer [file output-stream input-stream] :as io])
  (:import (org.apache.commons.compress.compressors CompressorStreamFactory)
           (org.apache.commons.compress.utils IOUtils)
           (org.apache.commons.compress.archivers.tar TarArchiveOutputStream)
           (java.io File)
           (org.apache.commons.io FilenameUtils)))

(def compressors ["lzma" "gz" "bzip2" "snappy-framed" "deflate" "lz4-framed"])

(def archive-extensions {"lzma"          ".tar.lzma"
                         "gz"            ".tar.gz"
                         "bzip2"         ".tar.bz2"
                         "snappy-framed" ".tar.snappy"
                         "deflate"       ".tar.gz"
                         "lz4-framed"    ".tar.lz4"})

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
  [input-name out-folder compressor]
  (let [extension (get archive-extensions compressor)
        fname     (FilenameUtils/getBaseName (FilenameUtils/normalizeNoEndSeparator input-name))]
    (str (FilenameUtils/normalizeNoEndSeparator out-folder) "/" fname extension)))


(defn- relativise-path
  "create relative archive entry name"
  [base path]
  (let [f        (file base)
        uri      (.toURI f)
        relative (.relativize uri (-> path file .toURI))]
    (.getPath relative)))


(defn create-archive
  "Create archive for given `input-name` (file or folder). An archive may be decompressed by external
  tools (tar, unzip, bunzip2 etc...). New archive file will be placed to `out-folder`.
  At first, input data is moved to tar archive, then tar archive is compressed by `compressor`.
  An archive extension will be created during compression and depends on `compressor` type.
  Returns created archive file name as `String`."
  [^String input-name ^String out-folder ^String compressor]
  (let [folder?   (.isDirectory (file input-name))
        out-fname (new-archive-name input-name out-folder compressor)
        fo        (output-stream out-fname)
        cfo       (.createCompressorOutputStream (CompressorStreamFactory.) compressor fo)
        a         (TarArchiveOutputStream. cfo)]
    (doseq [f (if folder? (file-seq (file input-name)) [(file input-name)])]
      (when (and (.isFile f) (not= out-fname (.getPath ^File f)))
        (let [entry-name (relativise-path (FilenameUtils/getPath input-name) (-> f .getPath))
              entry      (.createArchiveEntry a f entry-name)]
          (.putArchiveEntry a entry)
          (when (.isFile f)
            (IOUtils/copy (input-stream f) a))
          (.closeArchiveEntry a))))
    (.finish a)
    (.close a)
    out-fname))

