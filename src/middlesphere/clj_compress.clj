(ns middlesphere.clj-compress
  (:require [clojure.java.io :refer [file output-stream input-stream] :as io])
  (:import (org.apache.commons.compress.compressors CompressorStreamFactory)
           (org.apache.commons.compress.utils IOUtils)))

(def compressors ["lzma" "gz" "bzip2" "snappy-framed" "deflate" "lz4-framed"])

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
      (let [in    (.createCompressorInputStream (CompressorStreamFactory.) decompressor in)
            length (IOUtils/copy in f-out)]
        (.flush f-out)
        (.close in)
        (.close f-out)
        length)
      (catch Exception e
        (.close in)
        (.close f-out)
        (throw e)))))
