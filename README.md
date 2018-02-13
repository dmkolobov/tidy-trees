# tidy-trees

A library for making tidy drawings of trees.

Provides a reagent component which implements a O( n * depth ) algorithm for tree drawing inspired by Reingold and Tilford's [Tidier Drawings of Trees](http://reingold.co/tidier-drawings.pdf) and Bill Mill's survey of tree drawing algorithms [Drawing Presentable Trees](http://llimllib.github.io/pymag-trees/).

## Usage

 If we want to draw some `tree` in an element with id `target-el`, we write: 
 
 ```clojure 
(ns example
  (:require [reagent.core :refer [render-component]
            [tidy-trees.reagent :refer [tidy-tree]))

  ...

  (render-component [tidy-tree tree opts] (. js/document (get-ElementById "#target-el"))
 ```
 
 where opts is a map containing the following entries: 
 
- `:branch?` - `function` - When applied with a node, returns whether the node is a branch. 
- `:children?` - `function` - When applied with a node, returns the nodes children as a seq.
- `:label-branch` - `function` - When applied with a branch node, returns a label for the branch node.
- `:label-term` - `function` - When applied with a terminal node, returns a label for the terminal node.
- `:v-gap` - `number` - The minimum amount of space between levels
- `:h-gap` - `number` - The amount of space between adjacent nodes on the same level. 
- `:edges` - `keyword` - Determines the way tree edges are drawn. Supported options are `:straight` and `:diagonal`.

