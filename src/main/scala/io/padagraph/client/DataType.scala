package io.padagraph.client

import play.api.libs.json.{JsObject, JsString}

import scala.collection.{immutable, mutable}
/**
  * Created by pierre on 9/16/16.
  */
abstract class DataType[+T <: NodeOrEdge](factory: (Option[Node], Option[Node]) => T) extends Uuidentifiable {
  val name: String
  val description: String = ""
  val meta: immutable.Map[String, String] = Map.empty
  val attributesMapping: immutable.Map[String, AttributeType] = immutable.Map.empty[String, AttributeType]

  def instanceOfJson(jsObject: JsObject): T = {
    //todo: deal with source and target for edges
    val o = factory(None, None)
    o.setPropertiesFromJson((jsObject \ "properties").as[JsObject])
    o
  }

}
