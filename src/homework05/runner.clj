(ns homework05.runner
  (:require [clojure.test :as t]
            [clojure.tools.namespace.repl :as ns-tools]
            [homework05]))

(defn no-errors-or-failures?
  "Returns true if there have been no errors or failures reported."
  []
  (when-let [{:keys [error fail]} @t/*report-counters*]
    (zero? (+ (or error 0)
              (or fail 0)))))

(def file-and-line
  "Alias for the function in clojure.test/file-and-line."
  @(ns-resolve (find-ns 'clojure.test) 'file-and-line))

(defn do-stop-on-first-report
  "A replacement for clojure.test/do-report that will stop once the first error
  or failure is encountered."
  [m]
  (case (:type m)
    :fail  (when (no-errors-or-failures?)
             (t/report (merge (file-and-line (Throwable.) 1) m)))
    :error (when (no-errors-or-failures?)
             (t/report (merge (file-and-line (:actual m) 0) m)))
    (t/report m)))

(def test-vars
  "The list of the tests that should be run in order."
  [#'homework05/primitives
   #'homework05/lists
   #'homework05/vectors
   #'homework05/maps
   #'homework05/sets
   #'homework05/basic-sequence-operations
   #'homework05/consuming-sequences
   #'homework05/manipulating-sequences
   #'homework05/generating-sequences
   #'homework05/creating-collections-from-sequences
   #'homework05/functions
   #'homework05/branching-control-structures
   #'homework05/map-filter-reduce])

(defn run-tests
  "Runs all of the tests in test-vars, stopping at the first error."
  []
  (with-redefs [t/do-report do-stop-on-first-report]
    (binding [t/*report-counters* (ref t/*initial-report-counters*)]
      (t/do-report {:type :begin-test-ns :ns (the-ns 'homework05)})
      (let [ok (atom true)]
        (doseq [v test-vars]
          (t/test-var v)))
      (t/do-report {:type :end-test-ns :ns (the-ns 'homework05)})
      (t/do-report (assoc @t/*report-counters* :type :summary))
      @t/*report-counters*)))

(defn -main
  "Main entry for the program."
  [& args]
  (let [hw-file (-> (ClassLoader/getSystemClassLoader)
                    (.findResource "homework05.clj")
                    (.toURI)
                    (java.io.File.))]
    (loop [old-modified 0]
      (let [new-modified (.lastModified hw-file)]
        (when (not= old-modified new-modified)
          (binding [*ns* (find-ns 'homework05.runner)]
            (ns-tools/refresh :after 'homework05.runner/run-tests)))
        (Thread/sleep 100)
        (recur new-modified)))))
