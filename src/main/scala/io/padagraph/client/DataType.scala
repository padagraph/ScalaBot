package io.padagraph.client

import scala.collection.{mutable, immutable}
/**
  * Created by pierre on 9/16/16.
  */
trait DataType {
  val typeName: String
  val typeDescription: String = ""
  val meta: immutable.Map[String, String] = Map.empty
  val attributesMapping: immutable.Map[String, AttributeType]
  protected val properties: mutable.Map[String, String] =  new mutable.HashMap()
}
