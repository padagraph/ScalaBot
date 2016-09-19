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

  private def ApiPdg(padaGraphObject: PadaGraphObject) = {
    val url = padaGraphObject match {
      case PdgGraph => s"$serverURL/graphs/create"
      case PdgNodeType => s"$serverURL/g/${graph.name}/nodetype"
      case PdgEdgeType => s"$serverURL/g/${graph.name}/edgetype"
      case PdgNode => s"$serverURL/g/${graph.name}/node"
      case PdgEdge => s"$serverURL/g/${graph.name}/edge"
    }
    Http(url).header("Content-Type", "application/json").header("Authorization", identToken)
  }


  def getGraphInfo: Option[JsObject] = {
    val url = s"$serverURL/graphs/g/${graph.name}"
    val response = Http(url).asString
    response.code
    if(response.code == 200) {
      val schema = (Json.parse(response.body) \ graph.name).as[JsObject]
      Some(schema)
    }
    else
      None
  }

  def buildTypesIDMapping(): Map[String, Map[String,String]] = {
    getGraphInfo match {
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

  def graphExists(): Boolean = {
    getGraphInfo match {
      case Some(info) => info.fields.exists( _._1 == graph.name)
      case None => false
    }

  }

  def createGraph(): Boolean = {

    val payload = JsObject(Seq(
      "name" -> JsString(graph.name),
      "description" -> JsString(graph.description),
      "image" -> JsString(graph.image),
      "tags" -> JsArray(graph.tags map JsString)
    )).toString()

    val request = ApiPdg(PdgGraph).postData(payload)
    val response = request.asString
    response.code == 200
  }

  def createNodeType(n: Node): Boolean = {

    val payload = JsObject(Seq(
      "name" -> JsString(n.typeName),
      "description" -> JsString(n.typeDescription),
      "properties" -> n.getPropertiesAsJson()
    )).toString()

    val response = ApiPdg(PdgNodeType).postData(payload).asString
    response == 200
  }

  def createEdgeType(e:E):  Boolean = false

  def postNode(n: N): Boolean = false

  def postEdge(e: E): Boolean = false

  def deleteNode(n: N): Boolean = false

  def deleteEdge(n: N): Boolean = false

  def starNode(n: N): Boolean = false

  def unstarNode(n:N): Boolean = false


  // finds

  def findNodes(n: Node, start: Int=0, limit: Option[Int]=Some(100)): Stream[N] = Stream.empty[N]

  def iterNeighbors(n: Node, start: Int=0, limit: Int=100): Stream[N] = Stream.empty[N]

  def prox(pzero: Map[N, Double],
           step: Int=3,
           weights: Option[Map[N, Double]]=None,
           filterEdgeTypes:List[String]=Nil,
           filterNodeTypes:List[String]=Nil,
           limit: Int=100) : Map[N, Double] = Map.empty[N, Double]

  def getSubgraph(nodesUuids: List[String]): (Set[N],Set[E]) = (Set.empty[N], Set.empty[E])






  private def stringOfDataType(x: DataType): String = {
    JsObject(Seq(
      "name" -> JsString(x.typeName),
      "description" -> JsString(x.typeDescription),
      "properties" -> JsObject(x.attributesMapping.map({ case (k:String, v:AttributeType) => (k  -> JsObject(Seq("type" -> JsString(v.name))))}))
    )).toString()
  }
}