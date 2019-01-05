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

  var timer = 0

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
      // Ne pas le lancer à check heartbeat pour ne pas relancer une election
      // Lorsque celle en cours n'est pas finie
      // father ! Message("My status is " + status)
      /* status match {
        case Passive() => self ! Initiate
        case _ =>
      } */

        timer = 0
        status = new Passive()
        self ! Initiate


    }

    case Initiate =>
    {
        //println(father)
        //father ! Message ("Init Election Process")
        if(timer <= 5000)
        {
            timer = timer + 1
            Thread.sleep(1)
            self ! Initiate
        }
        else
        {
            // 1/ Status == Passive -> Candidate
            status match
            {
                case Passive() =>
                {
                    status = new Candidate()

                    candPred = -1
                    candSucc = -1

                    // Determine sucessor index
                    //Probleme avec l'index car il faut trier la liste des nodesAlive pour qu'elle soit consistante entre tout les acteurs
                    nodesAlive = quickSort(nodesAlive)

                    //Si on est seul on passe le process d'election et on devient directement leader
                    if(nodesAlive.length == 1)
                    {
                        self ! AVSRSP(List(),id)
                    }
                    else
                    {
                        var index = nodesAlive.indexOf(id)
                        var succInd = -1
                        if(id != nodesAlive.last) succInd = nodesAlive(index + 1)
                        else succInd = (nodesAlive(0))
                        father ! Message ("Election NodesAlive " + nodesAlive)
                        //  father ! Message ("Je suis à l'index " + index + "/" + (nodesAlive.size - 1) + " J'envoie à l'index " + (index+1) + "/" + (nodesAlive.size - 1))
                        var succ = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(succInd).id + "@" + terminaux(succInd).ip + ":" + terminaux(succInd).port + "/user/Node/electionActor")
                        father ! Message ("Sending ALG to Node(" + terminaux(succInd).id+")")
                        succ ! ALG(nodesAlive, id)
                    }
                }
                case _ =>
            }
        }
    }

    case ALG (list, init) =>
    {
        father ! Message("J'ai reçu un ALG")
        println("INIT : "+init)
        status match
        {
            case Passive() =>
            {
                status = new Dummy()
                father ! Message("Je suis un dummy")

                var index = list.indexOf(id)
                var succInd = -1
                if(id != list.last) succInd = list(index + 1)
                else succInd = (list(0))
                father ! Message ("Election NodesAlive " + list)
                //  father ! Message ("Je suis à l'index " + index + "/" + (nodesAlive.size - 1) + " J'envoie à l'index " + (index+1) + "/" + (nodesAlive.size - 1))
                var succ = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(succInd).id + "@" + terminaux(succInd).ip + ":" + terminaux(succInd).port + "/user/Node/electionActor")
                father ! Message ("Sending ALG to Node(" + terminaux(succInd).id+")")
                succ ! ALG(list, init)
            }
            case Candidate() =>
            {
                father ! Message("Je suis un candidat")
                candPred = init
                if(id > init)
                {
                    if(candSucc == -1)
                    {
                        val actor = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(init).id + "@" + terminaux(init).ip + ":" + terminaux(init).port + "/user/Node/electionActor")
                        father ! Message("Status Waiting")
                        status = new Waiting()
                        actor ! AVS(list, id)
                    }
                    else
                    {
                        val actor = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(candSucc).id + "@" + terminaux(candSucc).ip + ":" + terminaux(candSucc).port + "/user/Node/electionActor")
                        actor ! AVSRSP(list, candPred)
                        status = new Dummy()
                    }
                }
                if (id == init) father ! LeaderChanged(id)
            }
            case _ =>
        }
    }

    case AVS (list, j) =>
    {
      father ! Message("J'ai reçu un AVS")
      status match
    {

        case Candidate() =>
        {
            if(candPred == -1) candSucc = j
            else
            {
                val actor = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(j).id + "@" + terminaux(j).ip + ":" + terminaux(j).port + "/user/Node/electionActor")
                actor ! AVSRSP(list, candPred)
                status = new Dummy()
            }
        }

        case Waiting() => candSucc = j
        case _ =>
    }
  }

    case AVSRSP (list, k) =>
    {
        father ! Message("J'ai reçu un AVSRSP")
        if(list.length == 0)
        {
            father ! LeaderChanged(k)
        }
        else
        {
            status match
            {
                case Waiting() =>
                {
                    if(id == k) father ! LeaderChanged(id)
                    else
                    {
                        candPred = k
                        if(candPred == -1)
                        {
                            if(k < id)
                            {
                                val actor = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(k).id + "@" + terminaux(k).ip + ":" + terminaux(k).port + "/user/Node/electionActor")
                                status = new Waiting()
                                actor ! AVS(list, id)
                            }
                        }
                        else
                        {
                            val actor = context.actorSelection("akka.tcp://LeaderSystem" + terminaux(candSucc).id + "@" + terminaux(candSucc).ip + ":" + terminaux(candSucc).port + "/user/Node/electionActor")
                            status = new Dummy()
                            actor ! AVSRSP(list, k)
                        }
                    }
                }

                case _ =>
            }
        }
    }
  }
}
