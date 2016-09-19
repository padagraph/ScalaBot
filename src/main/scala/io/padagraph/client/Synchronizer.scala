package io.padagraph.client

import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json._

import scalaj.http._

/**
  * Created by pierre on 9/18/16.
  */
class Synchronizer[N <: Node,E <: Edge[N,N],G <:Graph[N,E]](var serverURL: String, var identToken: String, var graph: G) extends LazyLogging {
 // type N = Node
 // type E = Edge[N,N]
 // type G = Graph[N,E]

  private def buildURL(padaGraphObject: PadaGraphObject): String = padaGraphObject match {
    case PdgGraph => s"$serverURL/graphs/create"
    case PdgNodeType => s"$serverURL/graphs/g/${graph.name}/nodetype"
    case PdgEdgeType => s"$serverURL/graphs/g/${graph.name}/edgetype"
    case PdgNode => s"$serverURL/graphs/g/${graph.name}/node"
    case PdgEdge => s"$serverURL/graphs/g/${graph.name}/edge"
  }

  private def ApiPdgPost(padaGraphObject: PadaGraphObject): String => HttpRequest = {
    val url = buildURL(padaGraphObject)
     payload: String => Http(url).header("Content-Type", "application/json").header("Authorization", identToken).postData(payload)
  }

  private def ApiPdgDelete(padaGraphObject: PadaGraphObject): String => HttpRequest = {
    val url = buildURL(padaGraphObject) +"/"
    id: String => Http(url + id).header("Content-Type", "application/json").header("Authorization", identToken).method("DELETE")
  }



  def getGraphSchema: Option[JsObject] = {
    val url = s"$serverURL/graphs/g/${graph.name}/schema"
    val response = Http(url).asString
    if(response.code == 200) {
      val schema = (Json.parse(response.body) \  "schema").as[JsObject]
      Some(schema)
    }
    else
      None
  }

  def buildTypesIDMapping(): Map[PadaGraphObject, Map[String,String]] = {
    getGraphSchema match {
      case Some(schema) =>
        val jsNodeTypes = (schema \ "nodetypes").as[Array[JsObject]]
        val jsEdgeTypes = (schema \ "edgetypes").as[Array[JsObject]]
        Map(
          PdgNodeType -> jsNodeTypes.map( o => (o \ "name").as[String] -> (o \ "uuid").as[String]).toMap,
          PdgEdgeType -> jsEdgeTypes.map( o => (o \ "name").as[String] -> (o \ "uuid").as[String]).toMap
        )
      case None => //retry ad vitam eternam
        //todo: log it
        buildTypesIDMapping()
    }
  }

  var typeMapping: Option[Map[PadaGraphObject, Map[String, String]]] = None
  def getTypeUuid(padaGraphObject: PadaGraphObject, name: String): Option[String] = {
    val mapping = typeMapping match {
      case None =>
        val m = buildTypesIDMapping()
        typeMapping = Some(m)
        m
      case Some(m) => m
    }
    mapping(padaGraphObject).get(name)
  }


  //Edition

  def graphExists(): Boolean = {
    getGraphSchema match {
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

    val request = ApiPdgPost(PdgGraph)(payload)
    val response = request.asString
    response.code == 200
  }

  def createNodeType(n: N): Boolean = {
    val payload = stringOfDataType(n)
    val response = ApiPdgPost(PdgNodeType)(payload).asString
    response.code == 200
  }

  def createEdgeType(e:E):  Boolean = {
    val payload = stringOfDataType(e)
    val response = ApiPdgPost(PdgEdgeType)(payload).asString
    response.code == 200
  }

  def postNode(n: N): Boolean = {
    getTypeUuid(PdgNodeType, n.typeName) match {
      case Some(nodetype) =>
        val payload = JsObject(Seq(
          "nodetype" -> JsString(nodetype),
          "properties" -> n.getPropertiesAsJson()
        )).toString()
        val response = ApiPdgPost(PdgNode)(payload).asString
        logger.info(s"post node response\n$response")
        val json = Json.parse(response.body)
        n.uuid = Some((json \ "uuid").as[String])
        response.code == 200
      case None => false
    }
  }

  def postEdge(e: E): Boolean = {
    //This match ensures all IDs are known
    (getTypeUuid(PdgEdgeType, e.typeName),e.source.uuid,e.target.uuid) match {
      case (Some(edgetype),Some(source), Some(target)) =>
        val payload = JsObject(Seq(
          "edgetype" -> JsString(edgetype),
          "source" -> JsString(source),
          "target" -> JsString(target),
          "properties" -> e.getPropertiesAsJson()
        )).toString()
        val response = ApiPdgPost(PdgEdge)(payload).asString
        val json = Json.parse(response.body)
        e.uuid = Some((json \ "uuid").as[String])
        response.code == 200
      case _=> false
    }
  }

  def deleteNode(n: N): Boolean = {
    n.uuid match {
      case None => false
      case Some(id) =>
        val response = ApiPdgDelete(PdgNode)(id).asString
        response.code == 200
    }
  }

  def deleteEdge(e: E): Boolean = {
    e.uuid match {
      case None => false
      case Some(id) =>
        val response = ApiPdgDelete(PdgEdge)(id).asString
        response.code == 200
    }
  }

  def starNodes(uuids: List[String]): Boolean = {
    val url = s"$serverURL/graphs/g/${graph.name}/nodes/star"
    val payload = JsObject(Seq(
      "nodes" -> JsArray(uuids map JsString)
    )).toString()
    val response = Http(url).header("Content-Type", "application/json").header("Authorization", identToken).postData(payload).asString
    response.code == 200
  }

  def unstarNode(uuids: List[String]): Boolean = {
    val url = s"$serverURL/graphs/g/${graph.name}/nodes/unstar"
    val payload = JsObject(Seq(
      "nodes" -> JsArray(uuids map JsString)
    )).toString()
    val response = Http(url).header("Content-Type", "application/json").header("Authorization", identToken).postData(payload).asString
    response.code == 200
  }


  // finds

  def findNodes(n: Node, start: Int=0, size: Int=100): Stream[N] = {
    val url = buildURL(PdgNode) + "s/find"

    getTypeUuid(PdgNodeType, n.typeName) match {
      case None => Stream.empty[N]
      case Some(tuuid) =>
        val payload = JsObject(Seq(
          "start" -> JsNumber(start),
          "size" -> JsNumber(size),
          "nodetype" -> JsString(tuuid),
          "properties" -> n.getPropertiesAsJson()
        )).toString()
        val response = Http(url).header("Content-Type", "application/json").header("Authorization", identToken).postData(payload).asString
        if (response.code != 200) {
          Stream.empty[N]
        }
        else {
          val json = Json.parse(response.body)
          // todo: voir la gueule de la sortie, par en récursion si il en reste
          Stream.empty[N]
        }

    }

  }

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
      "properties" -> JsObject(x.attributesMapping.map({ case (k:String, v:AttributeType) => k  -> JsObject(Seq("type" -> JsString(v.name)))}))
    )).toString()
  }

  // complex action

  def uploadNewGraph(): Unit = {
    if(createGraph()) {
      val nodetypes = graph.nodes.map(_.typeName)
      for (t <- nodetypes) graph.nodes.find(_.typeName == t).map(createNodeType)

      val edgetypes = graph.edges.map(_.typeName)
      for (t <- edgetypes) graph.edges.find(_.typeName == t).map(createEdgeType)

      graph.nodes.foreach(postNode)
      graph.edges.foreach(postEdge)
    }
  }
}