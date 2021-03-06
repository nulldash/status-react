(ns status-im.utils.views
  (:require [clojure.walk :as w]))

(defn atom? [sub]
  (or (vector? sub)
      (and (seq sub)
           (#{'atom `reagent.core/atom} (first sub)))))

(defn walk-sub [sub form->sym]
  (if (coll? sub)
    (w/postwalk (fn [f]
                  (or (form->sym f) f)) sub)
    (or (form->sym sub) sub)))

(defn prepare-subs [subs]
  (let [pairs     (map (fn [[form sub]]
                         {:form form
                          :sub  sub
                          :sym  (if (atom? sub)
                                  (gensym (str (if (map? form) "keys" form)))
                                  form)})
                       (partition 2 subs))
        form->sym (->> pairs
                       (map (fn [{:keys [form sym]}]
                              [form sym]))
                       (into {}))]
    [(mapcat (fn [{:keys [form sym sub]}]
               (if (vector? sub)
                 [sym `(re-frame.core/subscribe ~(walk-sub sub form->sym))]
                 [form (walk-sub sub form->sym)]))
             pairs)
     (apply concat (keep (fn [{:keys [sym form sub]}]
                           (when (atom? sub)
                             [form `(deref ~sym)]))
                         pairs))]))

(defmacro defview
  [n params & rest]
  (let [[subs component-map body] (case (count rest)
                                    1 [nil {} (first rest)]
                                    2 [(first rest) {} (second rest)]
                                    3 rest)
        [subs-bindings vars-bindings] (prepare-subs subs)]
    `(defn ~n ~params
       (let [~@subs-bindings]
         (reagent.core/create-class
           (merge ~(->> component-map
                        (map (fn [[k f]]
                               (let [args (gensym "args")]
                                 [k `(fn [& ~args]
                                       (let [~@vars-bindings]
                                         (apply ~f ~args)))])))
                        (into {}))
                  {:reagent-render
                   (fn ~params
                     (let [~@vars-bindings]
                       ~body))}))))))

