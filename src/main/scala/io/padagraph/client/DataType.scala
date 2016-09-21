package io.padagraph.client

import play.api.libs.json.{JsObject, JsString}

import scala.collection.{immutable, mutable}
/**
  * Created by pierre on 9/16/16.
  */
trait DataType extends Uuidentifiable {
  val name: String
  val description: String = ""
  val meta: immutable.Map[String, String] = Map.empty
  val attributesMapping: immutable.Map[String, AttributeType] = immutable.Map.empty[String, AttributeType]

}
