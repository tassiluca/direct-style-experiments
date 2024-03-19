/** A [[state]]ful consumer of [[SensorEvent]] detecting possible malfunctioning and
  * keeping track of last known active sensing units.
  */
trait SensorHealthChecker extends Consumer[Seq[E], Seq[E]] with State[Seq[E], Seq[E]]

object SensorHealthChecker:
  def apply(): SensorHealthChecker = SensorHealthCheckerImpl()

  private class SensorHealthCheckerImpl 
      extends SensorHealthChecker
      with State[Seq[E], Seq[E]](Seq()):

    override protected def react(e: Try[Seq[E]])(using Async.Spawn): Seq[E] = e match
      case Success(current) =>
        val noMoreActive = state.map(_.name).toSet -- current.map(_.name).toSet
        if noMoreActive.nonEmpty then sendAlert(s"[$currentTime] ${noMoreActive.mkString(", ")} no more active!")
        current
      case Failure(es) => sendAlert(es.getMessage); Seq()