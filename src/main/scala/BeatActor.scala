package upmc.akka.leader

import math._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor._
import akka.io._
import akka.util.Timeout

import java.net._

sealed trait BeatMessage
case class Beat (id:Int) extends BeatMessage
case class BeatLeader (id:Int) extends BeatMessage

case class BeatTick () extends Tick

case class LeaderChanged (nodeId:Int)

class BeatActor (val id:Int) extends Actor {

  val time : Int = 1000
  val father = context.parent
  var leader : Int = 0 // On estime que le premier Leader est 0

  def receive = {

    // Initialisation
    case Start => {
      self ! BeatTick

    }

    // Objectif : prevenir tous les autres nodes qu'on est en vie
    case BeatTick => {
      if (this.id == this.leader) {
        father ! Message ("I am the leader")
        father ! BeatLeader(id)
      }
      else{
        father ! Beat(id)
      }

      Thread.sleep(1000)
      self ! BeatTick

    }


    case LeaderChanged (nodeId) => {
      leader = nodeId
    }

  }

}
