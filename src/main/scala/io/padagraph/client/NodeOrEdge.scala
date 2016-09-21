package io.padagraph.client

/**
  * Created by pierre on 9/21/16.
  */
abstract class NodeOrEdge extends Uuidentifiable with Properties {
  var dataType: DataType[NodeOrEdge]
  var starred: Boolean = false


}
