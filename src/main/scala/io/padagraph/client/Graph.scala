package io.padagraph.client

/**
  * Created by pierre on 9/16/16.
  */
abstract class Graph[N <: Node, E <: Edge[N,N]] {
  val name: String
  val nodes: Set[N]
  val edges: Set[E]
  val owner: String

}
