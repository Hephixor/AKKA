package upmc.akka.leader

import java.util
import java.util.Date

import akka.actor._
import BibPerso._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Tick
case class CheckerTick () extends Tick

class CheckerActor (val id:Int, val terminaux:List[Terminal], electionActor:ActorRef) extends Actor {

  var time : Int = 2000
  val father = context.parent

  var nodesAlive:List[Int] = List()
  var datesForChecking:List[Date] = List()
  var lastDate:Date = null

  var leader : Int = -2
  var lastSize : Int = -1


  def receive = {

    // Initialisation
    case Start => {
      // temps de pause avant le premier checkerTick
      // il peut être utile de le modifier si un node entrant n'a pas le temps
      // de recevoir de BeatTick des autres nodes en se connectant
      // avant de lancer une élection
      Thread.sleep(time)
      self ! CheckerTick
      nodesAlive = id::nodesAlive
    }

    // A chaque fois qu'on recoit un Beat : on met a jour la liste des nodes
    case IsAlive (nodeId) =>{
      if(!nodesAlive.contains(nodeId)){
        nodesAlive = nodeId::nodesAlive
      }
    }


    case IsAliveLeader (nodeId) => {
      //father ! Message ("Received IsAliveLeader " + nodeId)
      if(!nodesAlive.contains(nodeId)){
        nodesAlive = nodeId::nodesAlive
      }
      leader = nodeId
    }

    // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
    // Objectif : lancer l'election si le leader est mort
    case CheckerTick =>
    {
      // println("checkerTick || nodesAlive = " + nodesAlive + " || Leader = " + leader)
      if(nodesAlive.size >= 2)
      {
        if(!nodesAlive.contains(leader) && leader != -1)
        {
          father ! LeaderChanged(-1)
          leader = -1
          electionActor ! StartWithNodeList(nodesAlive)
        }
      }

      else if(lastSize != -1 && leader != id && leader != -1)
      {
        leader = -1
        electionActor ! StartWithNodeList(nodesAlive)
      }

      if(lastSize!=nodesAlive.size) nodesAlive = quickSort(nodesAlive)

      lastSize = nodesAlive.size
      nodesAlive = id:: List()
      Thread.sleep(time)
      self ! CheckerTick

    }

  }

}
