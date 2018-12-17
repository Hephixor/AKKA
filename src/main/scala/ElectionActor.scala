package upmc.akka.leader

import akka.actor._
import BibPerso._

abstract class NodeStatus
case class Passive () extends NodeStatus
case class Candidate () extends NodeStatus
case class Dummy () extends NodeStatus
case class Waiting () extends NodeStatus
case class Leader () extends NodeStatus

abstract class LeaderAlgoMessage
case class Initiate () extends LeaderAlgoMessage
case class ALG (list:List[Int], nodeId:Int) extends LeaderAlgoMessage
case class AVS (list:List[Int], nodeId:Int) extends LeaderAlgoMessage
case class AVSRSP (list:List[Int], nodeId:Int) extends LeaderAlgoMessage

case class StartWithNodeList (list:List[Int])

class ElectionActor (val id:Int, val terminaux:List[Terminal]) extends Actor {

  val father = context.parent
  var nodesAlive:List[Int] = List(id)

  var candSucc:Int = -1
  var candPred:Int = -1
  var status:NodeStatus = new Passive ()

  def receive = {

    // Initialisation
    case Start => {
      self ! Initiate
    }

    case StartWithNodeList (list) => {
      if (list.isEmpty) {
        this.nodesAlive = this.nodesAlive:::List(id)
      }
      else {
        this.nodesAlive = list
      }

      // Debut de l'algorithme d'election
      self ! Initiate
    }

    case Initiate =>
    {
        println(father)
        father ! Message ("Init Election Process")

        // 1/ Status == Passive -> Candidate
        status = new Candidate()

        // Determine sucessor index
        //Probleme avec l'index car il faut trier la liste des nodesAlive pour qu'elle soit consistente entre tout les acteurs
        nodesAlive = quickSort(nodesAlive)
        var index = nodesAlive.indexOf(id)

        if(id != nodesAlive.last){
          var succInd = nodesAlive(index + 1)
          father ! Message ("Candidate list " + nodesAlive)
          father ! Message ("Je suis à l'index " + index + "/" + (nodesAlive.size - 1) + " J'envoie à l'index " + (index+1) + "/" + (nodesAlive.size - 1))


          var succ = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(succInd).id + "@" + terminaux(succInd).ip + ":" + terminaux(succInd).port + "/user/Node")
          succ ! ALG(nodesAlive, id)
        }
        else{

          father ! Message ("Je suis à l'index " + index + "/" + (nodesAlive.size - 1) + " J'envoie à l'index 0/" + (nodesAlive.size - 1))

          val succ = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(0).id + "@" + terminaux(0).ip + ":" + terminaux(0).port + "/user/Node")
          succ ! ALG(nodesAlive,id)
        }
    }

    case ALG (list, init) =>
    {
        father ! Message("J'ai reçu un ALG")
        status match
        {
            case Passive() => status = new Dummy(); father ! Message("Je suis un Dummy")
            case Candidate() => father ! Message("Je suis un candidat")
        }


    }

    case AVS (list, j) =>

    case AVSRSP (list, k) =>

  }

}
