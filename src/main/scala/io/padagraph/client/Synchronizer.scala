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
    case PdgUser => "???" // TODO
  }

  private def ApiPdgPost(padaGraphObject: PadaGraphObject): String => HttpRequest = {
    val url = buildURL(padaGraphObject)
     payload: String => Http(url).header("Content-Type", "application/json").header("Authorization", identToken).postData(payload)
  }

  private def ApiPdgDelete(padaGraphObject: PadaGraphObject): String => HttpRequest = {
    val url = buildURL(padaGraphObject) +"/"
    id: String => Http(url + id).header("Content-Type", "application/json").header("Authorization", identToken).method("DELETE")
  }

  private def ApiPdgBatchPost(padaGraphObject: PadaGraphObject): String => HttpRequest = {
    val url = buildURL(padaGraphObject) + "s"
    payload: String => Http(url).header("Content-Type", "application/json").header("Authorization", identToken).postData(payload)
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

  /**
    * build the mapping of uuid to (Node|Edge)Types
    * by fetching info from the server
    *
    * @return a mapping of ObjectType -> (uuid -> DataType)
    */
  def buildTypesIDMapping(): Map[PadaGraphObject, Map[String, DataType[NodeOrEdge]]] = {
    getGraphSchema match {
      case Some(schema) =>
        val jsNodeTypes = (schema \ "nodetypes").as[Array[JsObject]]
        val jsEdgeTypes = (schema \ "edgetypes").as[Array[JsObject]]
        Map(
          PdgNodeType -> jsNodeTypes.map( o => (o \ "uuid").as[String] -> graph.getType(PdgNodeType, (o \ "name").as[String])).toMap,
          PdgEdgeType -> jsEdgeTypes.map( o => (o \ "uuid").as[String] -> graph.getType(PdgEdgeType, (o \ "name").as[String])).toMap
        )
      case None => //retry ad vitam eternam
        logger.info("failed to fetch type mapping, retrying")
        buildTypesIDMapping()
    }
  }

  /**
    * set the uuid property of every datatype object the server knows about
    *
    * @param mapping
    */
  def updateDataTypesWithUuids(mapping: Map[PadaGraphObject, Map[String, DataType[NodeOrEdge]]]) = {
    for( (uuid, edgeType) <- mapping(PdgEdgeType)) edgeType.uuid = Some(uuid)
    for( (uuid, nodeType) <- mapping(PdgNodeType)) nodeType.uuid = Some(uuid)
  }


  /**
    * variable to cache UUID mapping fetched from the server
    */
  private var typeMapping: Option[Map[PadaGraphObject, Map[String, DataType[NodeOrEdge]]]] = None

  /**
    * This function fetch Uuid of the existing types
    * and upload the new ones
    */
  def synchronizeTypesWithServer() = {
    // fetch the mapping if necessary
    val mapping = typeMapping match {
      case None =>
        val m = buildTypesIDMapping()
        typeMapping = Some(m)
        m
      case Some(m) => m
    }

    // update local data
    updateDataTypesWithUuids(mapping)

    // update server
    for(nt <- graph.nodeTypes.values if nt.uuid.isEmpty)
      createType(PdgNodeType, nt)
    for(et <- graph.edgeTypes.values if et.uuid.isEmpty)
      createType(PdgEdgeType, et)

  }


  def getTypeByUuid(padaGraphObject: PadaGraphObject, uuid: String): Option[DataType[NodeOrEdge]] = {
    val mapping = typeMapping match {
      case None =>
        val m = buildTypesIDMapping()
        typeMapping = Some(m)
        m
      case Some(m) => m
    }
    mapping(padaGraphObject).get(uuid)
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
    logger.debug(response.toString)
    response.code == 200
  }

  def createType(padaGraphObject: PadaGraphObject , t: DataType[NodeOrEdge]): Boolean = {
    val payload = stringOfDataType(t)
    val response = ApiPdgPost(padaGraphObject)(payload).asString
    val json = Json.parse(response.body)
    t.uuid = Some((json \ "uuid").as[String])
    response.code == 200
  }

  def postNode(n: N): Boolean = {
    n.dataType.uuid match {
      case Some(nodetype) =>
        val payload = Json.obj(
          "nodetype" -> nodetype,
          "properties" -> n.getPropertiesAsJson()
        ).toString()
        val response = ApiPdgPost(PdgNode)(payload).asString
        logger.info(s"post node response\n$response")
        val json = Json.parse(response.body)
        n.uuid = Some((json \ "uuid").as[String])
        response.code == 200
      case None => false
    }
  }

  def postNodes(nodes: Stream[N], batchSize: Int=100) = {

    def sendBatch(buffer: List[N]) = {
      val payload = Json.obj(
        "nodes" -> buffer.flatMap(n =>
          n.dataType.uuid.map(nodetype =>
            Json.obj("nodetype" -> nodetype,
              "properties" -> n.getPropertiesAsJson()
            )
          )
        )
      ).toString()
      val response = ApiPdgBatchPost(PdgNode)(payload).asString
      //TODO: fetch uuids and set then on nodes
      logger.debug(response.toString)
    }
    def bufferize(cursor: Stream[N], buf: List[N]): Unit ={
      cursor match {
        case n #:: tl =>
          if (buf.size == batchSize -1 ) {
            sendBatch(n::buf)
            bufferize(tl, Nil)
          }
          else
            bufferize(tl, n::buf)
        case Stream() =>
          sendBatch(buf)
      }
    }
    bufferize(nodes, Nil)
  }


  def postEdge(e: E): Boolean = {
    //This match ensures all IDs are known
    (e.dataType.uuid, e.source.uuid, e.target.uuid) match {
      case (Some(edgetype),Some(source), Some(target)) =>
        val payload = Json.obj(
          "edgetype" -> edgetype,
          "source" -> source,
          "target" -> target,
          "properties" -> e.getPropertiesAsJson()
        ).toString()
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

    logger.debug(s"findNodes at $url \n with ${n.getPropertiesAsJson().toString()}")

    n.dataType.uuid match {
      case None => Stream.empty[N]
      case Some(tuuid) =>
        val payload = JsObject(Seq(
          "start" -> JsNumber(start),
          "size" -> JsNumber(size),
          "nodetype" -> JsString(tuuid),
          "properties" -> n.getPropertiesAsJson()
        )).toString()
        logger.debug(s"find nodes payload\n$payload\n")
        val response = Http(url).header("Content-Type", "application/json").header("Authorization", identToken).postData(payload).asString
        logger.debug(s"find nodes response\n${response.toString}\n")
        if (response.code != 200) {
          logger.info(s"findNodes Request failed with code ${response.code}")
          Stream.empty[N]
        }
        else {
          val json = Json.parse(response.body)
          // todo: voir la gueule de la sortie, par en récursion si il en reste
          val data: Seq[N] = (json \ "nodes").as[Seq[JsObject]].flatMap({ js =>
            val typeUuid = (js \ "nodetype").as[String]
            logger.debug(js.toString())
            getTypeByUuid(PdgNodeType, typeUuid).map(dataType => dataType.instanceOfJson(js)   )
          }).flatMap({case n:N => Some(n) case _=> None })
          data.toStream #::: (
            if ((json \ "count").as[Int] < size)
              Stream.empty[N]
            else
              findNodes(n, start + size, size)
            )
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






  private def stringOfDataType(x: DataType[NodeOrEdge]): String = {
    JsObject(Seq(
      "name" -> JsString(x.name),
      "description" -> JsString(x.description),
      "properties" -> JsObject(x.attributesMapping.map({ case (k:String, v:AttributeType) => k  -> JsObject(Seq("type" -> JsString(v.name)))}))
    )).toString()
  }

  // complex action

  def uploadNewGraph(): Unit = {
    if(createGraph()) {
      synchronizeTypesWithServer()
      postNodes(graph.nodes.toStream)
      graph.edges.foreach(postEdge)
    }
  }
}