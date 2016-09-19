package io.padagraph

import scala.collection.mutable

/**
  * Created by pierre on 9/19/16.
  */
package object client {

  /**
    * Upper bound type for attributes data types
    */
  abstract class AttributeType {
    type T
    val name: String
    def getterConversion(x:String): T
    def setterConversion(x:T): String

    def get(properties: mutable.Map[String,String], key: String): Option[T] = properties.get(key) map getterConversion
    def set(properties: mutable.Map[String,String], key: String, value: T): Unit = {
      properties += key -> setterConversion(value)
    }
  }

  case object Text extends  AttributeType {
    override type T = String
    override val name = "Text"
    override def getterConversion(x: String): String = identity(x)
    override def setterConversion(x: String): String = identity(x)
  }

  case object Numeric extends AttributeType {
    override type T = Int
    override val name = "Numeric"
    override def getterConversion(x:String) = x.toInt
    override def setterConversion(x:Int) = x.toString
  }


  /**
    * Upper bound class for server-side object
    */
  abstract class PadaGraphObject
  case object PdgGraph extends PadaGraphObject
  case object PdgUser extends PadaGraphObject
  case object PdgNode extends PadaGraphObject
  case object PdgEdge extends PadaGraphObject
  case object PdgNodeType extends PadaGraphObject
  case object PdgEdgeType extends PadaGraphObject


}
