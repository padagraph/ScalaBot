package io.padagraph.example

import scala.collection.mutable
import io.padagraph.client._
/**
  * Created by pierre on 9/16/16.
  */
object Social {
  abstract class SocialNodes extends Node
  abstract class SocialEdges extends Edge[SocialNodes, SocialNodes]

  class Person extends SocialNodes {
    override val typeName: String = "person"
    override val attributesMapping: Map[String, String] =
      Map(
        "name" -> "Text",
        "age" -> "Numeric"
      )

    override val meta: Map[String, String] = Map.empty
    override protected val properties: mutable.Map[String, String] = new mutable.HashMap()

    def name: String = {properties.getOrElse("name", "")}
    def name_=(name: String) = properties += "name" -> name

    def age: Int = {properties.getOrElse("age","0").toInt}
    def age_=(age: Int) = properties += "age" -> age.toString

  }

  class FriendOf(src: Person, tgt: Person) extends SocialEdges {
    override val typeName: String = "friend"
    override val source: Person = src
    override val target: Person = tgt
    override val attributesMapping: Map[String, String] = Map.empty
    override val meta: Map[String, String] = Map.empty

    override protected val properties: mutable.Map[String, String] = new mutable.HashMap()
  }

  class SocialGraph(override val nodes:Set[SocialNodes],
                    override val edges:Set[SocialEdges]) extends Graph[SocialNodes, SocialEdges] {

    override val name: String = "social"
    override val owner: String = "pierre"
  }

  val n1 = new Person()
  val n2 = new Person()
  n1.name = "Paul"
  n1.age = 42
  n2.name = "Jacques"
  n2.age = 32

  val r1 = new FriendOf(n1,n2)

   val g = new SocialGraph(Set(n1 ,n2), Set[SocialEdges](r1))

}
