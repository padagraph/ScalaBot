package io.padagraph.client

import play.api.libs.json.{JsObject, JsString}

import scala.collection.{immutable, mutable}
/**
  * Created by pierre on 9/16/16.
  */
trait DataType {
  val typeName: String
  val typeDescription: String = ""
  val meta: immutable.Map[String, String] = Map.empty
  val attributesMapping: immutable.Map[String, AttributeType]
  protected val properties: mutable.Map[String, String] =  new mutable.HashMap()
  def getPropertiesAsJson(): JsObject = JsObject(properties.map {
    case (k,v) => k -> JsString(v)
  })
}
