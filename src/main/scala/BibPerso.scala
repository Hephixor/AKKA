package upmc.akka.leader

object BibPerso
{
    def quickSort (list : List[Int]) : List[Int] =
    {
        list match
        {
            case Nil => list
            case x :: xs => quickSort(xs.filter(_ < x)) ++ List(x) ++ quickSort(xs.filter(_ >= x))
        }
    }
}
