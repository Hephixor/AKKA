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

  var nodesAlive:List[Int] = List()
  var datesForChecking:List[Date] = List()
  var lastDate:Date = null

  var leader : Int = -1
  var lastSize : Int = -1


  def receive = {

    // Initialisation
    case Start => {
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
      //father ! Message ("IsAliveLeader " + nodeId)
      if(!nodesAlive.contains(nodeId)){
        nodesAlive = nodeId::nodesAlive
      }
      leader = nodeId
    }

    // A chaque fois qu'on recoit un CheckerTick : on verifie qui est mort ou pas
    // Objectif : lancer l'election si le leader est mort
    case CheckerTick =>
    {
      if(leader != -1){
        //father ! Message ("leader not -1")
        //father ! Message ("Checker lastSize " + lastSize)
        //father ! Message ("Checker current list " + nodesAlive)
        //father ! Message ("Checker current leader " + leader)
        if(!nodesAlive.contains(leader)){
          electionActor ! StartWithNodeList(nodesAlive)
          electionActor ! ALG(nodesAlive,id)
        }
      }

      if(lastSize!=nodesAlive.size){
        father ! Message ("List alive nodes " + nodesAlive+"")
      }

      lastSize = nodesAlive.size
      nodesAlive = id:: List()
      Thread.sleep(2000)
      self ! CheckerTick
    }

  }

}
