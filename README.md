# clj-compress

  A Clojure library designed to compress/decompress data. This is a thin wrapper for Apache commons-compress library.\
  Supported algorithms: LZMA, GZIP, BZIP2, Snappy, Deflate, LZ4, XZ.
  
  Best compression ratio: BZIP2, LZMA, XZ, Deflate, GZIP.
  
  Snappy and LZ4 faster but has lower compression ratio.\
  LZ4 on a big files sometimes very very slow.

## Usage

For Leiningen add to project.clj: ```[middlesphere/clj-compress "0.1.0"]```

For Deps CLI add to deps.edn:  ```{:deps {middlesphere/clj-compress {:mvn/version "0.1.0}}```

1. Import necessary namespaces:
    ```clojure
    (require '[clj-compress.core :as c])
    (require '[clojure.java.io :refer [file output-stream input-stream] :as io])
    (import '(java.io ByteArrayOutputStream))
    ```
2. To check available compressors:
    ```clojure
    c/compressors
    ;;=> ["lzma" "gz" "bzip2" "snappy-framed" "deflate" "lz4-framed" "xz"]
    ```
3. To compress string data:

    ```clojure
    
    (let [s           "ABACABACABADEABACABACABADEABACABACABADEABACABACABADE"
          sbuf        (.getBytes s)
          compressor  "lzma"
          cbuf        (ByteArrayOutputStream.)
          coutbuf     (ByteArrayOutputStream.)
          comp-size   (c/compress-data sbuf cbuf compressor)
          decomp-size (c/decompress-data (.toByteArray cbuf) coutbuf compressor)]
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
          comp-size       (c/compress-data in-file out-file compressor)
          decomp-size     (c/decompress-data out-file decomp-out-file compressor)]
      (println (format "compressor: %s, src size: %s, compressed: %s, decompressed: %s."
                       compressor (.length (io/file in-file)) (.length (io/file out-file)) decomp-size)))
                       
    ;; compressor: lzma, src size: 11218, compressed: 4035, decompressed: 11218.
    ;; => nil
    ```
    Don't forget to delete intermediate files.

5. To create archive from group of files and folders:

    ```clojure
     (c/create-archive "abc" ["data/test-folder/test-file.txt" "data/test-folder/folder1"] "data/" "xz")
    ;;=> "data/abc.tar.xz"
    ```
    
6. To decompress data from archive to folder:

    ```clojure
    ;; guess by archive extension
    (c/decompress-archive "data/abc.tar.xz" "data/out")   
    
    ;; explicitly set compressor type
    (c/decompress-archive "data/abc.tar.xz" "data/out" "xz")

    ```
 7. To list archive items:
 
    ```clojure
    (c/list-archive "data/abc.tar.bz2")  
    ;; =>
    #:item{:count 3,
           :list [#:item{:name "test-folder/test-file.txt",
                         :size 11218,
                         :last-modified #inst"2019-02-28T21:30:39.000-00:00"}
                  #:item{:name "test-folder/folder2/big.txt",
                         :size 6488666,
                         :last-modified #inst"2018-12-28T23:02:00.000-00:00"}
                  #:item{:name "test-folder/folder1/1.txt",
                         :size 11218,
                         :last-modified #inst"2019-02-28T21:30:39.000-00:00"}]}

    ``` 

## Run Tests

For Leiningen: ```lein test```

For Deps CLI: ```clj -A:test```


## License

Copyright © 2019 Mike Ananev

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
