# clj-compress

  A Clojure library designed to compress/decompress data. This is a thin wrapper for Apache commons-compress library.
  Supported algorithms: LZMA, GZIP, BZIP2, Snappy, Deflate, LZ4.
  
  Best compression ratio (in order of ratio): BZIP2, LZMA, Deflate, GZIP.
  
  Snappy and LZ4 faster but has lower compression ratio.
  LZ4 on a big files sometimes very very slow.

## Usage

For Leiningen add to project.clj: ```[middlesphere/clj-compress "0.1.0"]```

For Deps CLI add to deps.edn:  ```{:deps {middlesphere/clj-compress {:mvn/version "0.1.0}}```

1. Import necessary namespaces:
    ```clojure
    (require '[middlesphere.clj-compress :as c])
    (require '[clojure.java.io :refer [file output-stream input-stream] :as io])
    (import '(java.io ByteArrayOutputStream))
    ```
2. To check available compressors:
    ```clojure
    c/compressors
    ;;=> ["lzma" "gz" "bzip2" "snappy-framed" "deflate" "lz4-framed"]
    ```
3. To compress string data:

    ```clojure
    
    (let [s           "ABACABACABADEABACABACABADEABACABACABADEABACABACABADE"
          sbuf        (.getBytes s)
          compressor  "lzma"
          cbuf        (ByteArrayOutputStream.)
          coutbuf     (ByteArrayOutputStream.)
          comp-size   (compress-data sbuf cbuf compressor)
          decomp-size (decompress-data (.toByteArray cbuf) coutbuf compressor)]
      (println (format "compressor: %s, src size: %s, compressed: %s, decompressed: %s."
                       compressor (.length s) (.size cbuf) decomp-size))
      (println "s: " s "decompressed s:" (.toString coutbuf)))
      
    ;; compressor: lzma, src size: 52, compressed: 33, decompressed: 52.
    ;; s:  ABACABACABADEABACABACABADEABACABACABADEABACABACABADE decompressed s: ABACABACABADEABACABACABADEABACABACABADEABACABACABADE
    ;; => nil
    ```
4. To compress single file to get raw compressed bytes and then decompress it:

    ```clojure
    (let [in-file         "data/test-file.txt"
          compressor      "lzma"
          out-file        (str in-file "." compressor)
          decomp-out-file (str in-file "." "txt")
          comp-size       (compress-data in-file out-file compressor)
          decomp-size     (decompress-data out-file decomp-out-file compressor)]
      (println (format "compressor: %s, src size: %s, compressed: %s, decompressed: %s."
                       compressor (.length (io/file in-file)) (.length (io/file out-file)) decomp-size)))
                       
    ;; compressor: lzma, src size: 11218, compressed: 4035, decompressed: 11218.
    ;; => nil
    ```
    Don't forget to delete intermediate files.

5. To create real archive:

## Run Tests

For Leiningen: ```lein test```

For Deps CLI: ```clj -A:test```


## License

Copyright © 2019 Mike Ananev

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
