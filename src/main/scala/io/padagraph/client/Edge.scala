package io.padagraph.client

/**
  * Created by pierre on 9/16/16.
  */
abstract class Edge[S <: Node,T <: Node]  extends Uuidentifiable  with Properties {
  val source: S
  val target: T
  val edgeType: DataType

}
