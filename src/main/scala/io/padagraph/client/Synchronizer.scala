package io.padagraph.client

import play.api.libs.json._
import scalaj.http._

/**
  * Created by pierre on 9/18/16.
  */
class Synchronizer[N <: Node,E <: Edge[N,N],G <:Graph[N,E]](serverURL: String, identToken: String, graph: G) {
 // type N = Node
 // type E = Edge[N,N]
 // type G = Graph[N,E]

  def getGraph(): Option[JsObject] = {
    val url = s"$serverURL/graphs/g/${graph.name}"
    val response = Http(url).asString
    if(response.headers.getOrElse("Status",List(""))(0).contains("200 OK")) {
      val schema = (Json.parse(response.body) \ graph.name).as[JsObject]
      Some(schema)
    }
    else
      None
  }

  def buildTypesIDMapping(): Map[String, Map[String,String]] = {
    getGraph() match {
      case Some(schema) =>
        val jsNodeTypes = (schema \ "nodetypes").as[Array[JsObject]]
        val jsEdgeTypes = (schema \ "edgetypes").as[Array[JsObject]]
        Map(
          "nodetypes" -> jsNodeTypes.map( o => (o \ "name").as[String] -> (o \ "uuid").as[String]).toMap,
          "edgetypes" -> jsEdgeTypes.map( o => (o \ "name").as[String] -> (o \ "uuid").as[String]).toMap
        )
      case None => //retry ad vitam eternam
        //todo: log it
        buildTypesIDMapping()
    }
  }

  //Edition

  def hasGraph(graphname: String): Boolean

  def createGraph(): Boolean

  def createNodeType(n: Node): Boolean

  def createEdgeType(e:E):  Boolean

  def postNode(n: N): Boolean

  def postEdge(e: E): Boolean

  def deleteNode(n: N): Boolean

  def deleteEdge(n: N): Boolean

  def starNode(n: N): Boolean

  def unstarNode(n:N): Boolean


  // finds

  def findNodes(n: Node, start: Int=0, limit: Option[Int]=Some(100)): Stream[N]

  def iterNeighbors(n: Node, start: Int=0, limit: Int=100): Stream[N]

  def prox(pzero: Map[Node, Double],
           step: Int=3,
           weights: Option[Map[Node, Double]]=None,
           filterEdgeTypes:List[String]=Nil,
           filterNodeTypes:List[String]=Nil,
           limit: Int=100) : Map[Node, Double]

  def getSubgraph(nodesUuids: List[String]): (Set[N],Set[E])






  private def stringOfDataType(x: DataType): String = {
    JsObject(Seq(
      "name" -> JsString(x.typeName),
      "description" -> JsString(x.typeDescription),
      "properties" -> JsObject(x.attributesMapping.map({ case (k:String, v:AttributeType) => (k  -> JsObject(Seq("type" -> JsString(v.name))))}))
    )).toString()
  }
}