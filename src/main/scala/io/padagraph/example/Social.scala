package io.padagraph.example

import scala.collection.mutable
import io.padagraph.client._
/**
  * Created by pierre on 9/16/16.
  */
object Social {
  abstract class SocialNodes extends Node
  abstract class SocialEdges extends Edge[SocialNodes, SocialNodes]


  object NodeTypePerson extends DataType {
    override val name: String = "person"
    override val description: String = "type of nodes for individuals"

    //declaration of attributes type
    override val attributesMapping: Map[String, AttributeType] =
      Map(
        "name" -> Text,
        "age" -> Numeric
      )

  }
  /**
    * Model indiviuals
    * will correspond to a NodeType in Padagraph
    * instances will be UNodes
    */
  class Person extends SocialNodes {
    val nodeType = NodeTypePerson

    //defining getter and setters for convenience
    def name: Option[String] = Text.get(properties, "name")
    def name_=(name: String) = Text.set(properties, "name", name)

    def age: Option[Int] = Numeric.get(properties, "age")
    def age_=(age: Int) = Numeric.set(properties, "age", age)

  }


  object EdgeTypeFriendOf extends DataType {
    override val name: String = "friend"
    override val description = "source is a friend of target"
  }

  /**
    * Model friendship relation between Person Nodes
    * the class will correspond to an EdgeType in Padagraph
    * instances will be UEdges
    * @param src source Node (restricted to NodeType "person")
    * @param tgt target Node (restricted to NodeType "person")
    */
  class FriendOf(src: Person, tgt: Person) extends SocialEdges {
    override val edgeType = EdgeTypeFriendOf
    override val source: Person = src
    override val target: Person = tgt
  }

  //defining two nodes

  val n1 = new Person()
  val n2 = new Person()
  n1.name = "Paul"
  n1.age = 42
  n2.name = "Jacques"
  n2.age = 32

  // and a relation
  val r1 = new FriendOf(n1,n2)


  // creating a single instance of a subclass of Graph to define a graph
  object SocialGraph extends Graph[SocialNodes, SocialEdges] {

    override val name: String = "social8"
    override val owner: String = "pierre"
    override val description =
      """
        | blabla blabla
        | bla blablabla
      """.stripMargin

    override val tags = Array("social", "peoples", "friendship")

    override val image = ""

    override val nodes:Set[SocialNodes] = Set(n1, n2)
    override val edges:Set[SocialEdges] = Set(r1)

    rebuildTypeMappings()
  }

  val tok = "WyJwQHAuaW8iLCIkMmIkMTIkR1NlalA4RFdoRjhoTC8wdGJCY3BrT2ZBV2QwYi5keE43akhHM3FVN3lCWGxPMEllTmZYOXkiXQ.CsB5EA.YridEDeCLnvvkB0BonsapcdOjXQ"
  val syn = new Synchronizer[SocialNodes, SocialEdges, SocialGraph.type ]("http://localhost:5000", tok, SocialGraph)

}
