(ns middlesphere.clj-compress-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :refer [file output-stream input-stream] :as io]
            [middlesphere.clj-compress :refer :all])
  (:import (java.io ByteArrayOutputStream)))

(deftest compress-data-test

  (testing "Compressing string"
    (let [s    "ABACABACABADEABACABACABADEABACABACABADEABACABACABADE"
          sbuf (.getBytes s)]
      (doseq [c compressors]
        (let [cbuf        (ByteArrayOutputStream.)
              coutbuf     (ByteArrayOutputStream.)
              comp-size   (compress-data sbuf cbuf c)
              decomp-size (decompress-data (.toByteArray cbuf) coutbuf c)]
          (println (format "compressor: %13s, src size: %7s, compressed: %7s, decompressed: %7s."
                           c (.length s) (.size cbuf) decomp-size))
          (is (> comp-size 0))
          (is (= comp-size decomp-size))))))


  (testing "Compressing single file test"
    (let [in-file "data/test-file.txt"]
      (doseq [c compressors]
        (let [out-file        (str in-file "." c)
              decomp-out-file (str in-file "." "txt")
              comp-size       (compress-data in-file out-file c)
              decomp-size     (decompress-data out-file decomp-out-file c)]
          (println (format "compressor: %13s, src size: %7s, compressed: %7s, decompressed: %7s."
                           c (.length (io/file in-file)) (.length (io/file out-file)) decomp-size))
          (io/delete-file out-file)
          (io/delete-file decomp-out-file)
          (is (> comp-size 0))
          (is (= comp-size decomp-size)))))))

