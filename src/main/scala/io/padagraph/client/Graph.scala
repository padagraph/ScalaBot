package io.padagraph.client

import play.api.libs.json.JsObject

/**
  * Created by pierre on 9/16/16.
  */
abstract class Graph[N <: Node, E <: Edge[N,N]] {
  val name: String
  val owner: String
  val description: String
  val image: String
  val tags: Array[String]
  val nodes: Set[N]
  val edges: Set[E]

  var nodeTypes: Map[String, DataType[NodeOrEdge]] = Map.empty[String, DataType[NodeOrEdge]]
  var edgeTypes: Map[String, DataType[NodeOrEdge]] = Map.empty[String, DataType[NodeOrEdge]]

  def rebuildTypeMappings() = {
    nodeTypes = nodes.map(_.dataType).map(nt => nt.name -> nt).toMap
    edgeTypes = edges.map(_.dataType).map(et => et.name -> et).toMap
  }

  def getType(padaGraphObject: PadaGraphObject, name: String): DataType[NodeOrEdge] = padaGraphObject match {
    case PdgNodeType => nodeTypes(name)
    case PdgEdgeType => edgeTypes(name)
  }

}
