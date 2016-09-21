package io.padagraph.client

/**
  * Created by pierre on 9/16/16.
  */
abstract class Node extends Uuidentifiable with Properties {
  val nodeType: DataType

}
