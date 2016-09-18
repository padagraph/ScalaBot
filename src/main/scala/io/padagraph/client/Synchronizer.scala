package io.padagraph.client

import play.api.libs.json._

/**
  * Created by pierre on 9/18/16.
  */
class Synchronizer(serverURL: String, identToken: String) {

  def stringOfDataType(x: DataType): String = {
    JsObject(Seq(
      "name" -> JsString(x.typeName),
      "description" -> JsString(x.typeDescription),
      "properties" -> JsObject(x.attributesMapping.map({ case (k:String, v:String) => (k  -> JsObject(Seq("type" -> JsString(v))))}))
    )).toString()
  }
}