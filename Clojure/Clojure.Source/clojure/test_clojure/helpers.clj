﻿;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

; Author: Stuart Halloway

(ns clojure.test-clojure.helpers
  (:use clojure.test))

(defn temp-ns
  "Create and return a temporary ns, using clojure.core + uses"
  [& uses]
  (binding [*ns* *ns*]
    (in-ns (gensym))
    (apply clojure.core/use 'clojure.core uses)
    *ns*))

(defmacro eval-in-temp-ns [& forms]
  `(binding [*ns* *ns*]
     (in-ns (gensym))
     (clojure.core/use 'clojure.core)
     (eval
      '(do ~@forms))))

(defn causes
  [^Exception throwable]                                                      ;;; Throwable
  (loop [causes []
         t throwable]
    (if t (recur (conj causes t) (.InnerException t)) causes)))               ;;; .getCause

;; this is how I wish clojure.test/thrown? worked...
;; Does body throw expected exception, anywhere in the .getCause chain?
(defmethod assert-expr 'fails-with-cause?
  [msg [_ exception-class msg-re & body :as form]]
  `(try
   ~@body
   (report {:type :fail, :message ~msg, :expected '~form, :actual nil})
   (catch Exception t#                                                          ;;; Throwable
     (if (some (fn [cause#]
                 (and
                  (= ~exception-class (class cause#))
                  (re-find ~msg-re (.Message cause#))))                      ;;; .getMessage
               (causes t#))
       (report {:type :pass, :message ~msg,
                :expected '~form, :actual t#})
       (report {:type :fail, :message ~msg,
                :expected '~form, :actual t#})))))


(defn get-field
  "Access to private or protected field.  field-name is a symbol or
  keyword."
  ([klass field-name]
     (get-field klass field-name nil))
  ([klass field-name inst]
     (-> klass (.GetField (name field-name) System.Reflection.BindingFlags/NonPublic )                ;;; .getDeclaredField
         ;;;(doto (.setAccessible true))
         (.GetValue inst))))                                       ;;; .get
         
(defn set-var-roots
  [maplike]
  (doseq [[var val] maplike]
    (alter-var-root var (fn [_] val))))

(defn with-var-roots*
  "Temporarily set var roots, run block, then put original roots back."
  [root-map f & args]
  (let [originals (doall (map (fn [[var _]] [var @var]) root-map))]
    (set-var-roots root-map)
    (try
     (apply f args)
     (finally
      (set-var-roots originals)))))

(defmacro with-var-roots
  [root-map & body]
  `(with-var-roots* ~root-map (fn [] ~@body)))

(defn exception
  "Use this function to ensure that execution of a program doesn't
  reach certain point."
  []
  (throw (new Exception "Exception which should never occur")))
        