package io.padagraph.client

import scala.collection.{mutable, immutable}
/**
  * Created by pierre on 9/16/16.
  */
trait DataType {
  val typeName: String
  val typeDescription: String = ""
  val meta: immutable.Map[String, String]
  val attributesMapping: immutable.Map[String, String] // we could define specific datatypes
  protected val properties: mutable.Map[String, String]
}
