package io.padagraph.client

import play.api.libs.json.{JsObject, JsString}

import scala.collection.mutable

/**
  * Created by pierre on 9/21/16.
  */
trait Properties {
  protected val properties: mutable.Map[String, String] =  new mutable.HashMap()
  def getPropertiesAsJson(): JsObject = JsObject(properties.map {
    case (k,v) => k -> JsString(v)
  })

  def setPropertiesFromJson(jsObject: JsObject): Unit = {
    properties ++= jsObject.as[Map[String,String]]
  }
}
