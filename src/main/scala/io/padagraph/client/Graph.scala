package io.padagraph.client

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

  var nodeTypes: Map[String, DataType] = Map.empty[String, DataType]
  var edgeTypes: Map[String, DataType] = Map.empty[String, DataType]

  def rebuildTypeMappings() = {
    nodeTypes = nodes.map(_.nodeType).map(nt => nt.name -> nt).toMap
    edgeTypes = edges.map(_.edgeType).map(et => et.name -> et).toMap
  }

  def getType(padaGraphObject: PadaGraphObject, name: String): DataType = padaGraphObject match {
    case PdgNodeType => nodeTypes(name)
    case PdgEdgeType => edgeTypes(name)
  }
}
