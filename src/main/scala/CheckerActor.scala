package upmc.akka.leader

import java.util
import java.util.Date

import akka.actor._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Tick
case class CheckerTick () extends Tick

class CheckerActor (val id:Int, val terminaux:List[Terminal], electionActor:ActorRef) extends Actor {

  var time : Int = 200
  val father = context.parent

  var lastNodesAlive:LIst[Int] = List()
  var datesForChecking:List[Date] = List()
  var lastDate:Date = null

  var leader : Int = -1

  def receive = {

    // Initialisation
    case Start => {
      self ! CheckerTick
      nodesAlive = id::nodesAlive
    }

    // A chaque fois qu'on recoit un Beat : on met a jour la liste des nodes
    case IsAlive (nodeId) =>{
      father ! Message ("List Alive")
      if(!nodesAlive.contains(nodeId)){
        nodesAlive = nodeId::nodesAlive
      }
      father ! Message (nodesAlive+"")
    }


    case IsAliveLeader (nodeId) =>

    // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
    // Objectif : lancer l'election si le leader est mort
    case CheckerTick =>
    {
        if(!nodesAlive.contains(leader)) father!Message("PANIQUE")
        nodesAlive = List()
        Thread.sleep(5000)
        self ! CheckerTick
    }


  }


}
