/** An asynchronous cold data stream that emits values, inspired to Kotlin Flows. */
trait Flow[+T]:

  /** Start the flowing of data which can be collected reacting through the given [[collector]] function. */
  def collect(collector: Try[T] => Unit)(using Async, AsyncOperations): Unit

/** An interface modeling an entity capable of [[emit]]ting [[Flow]]able values. */
trait FlowCollector[-T]:

  /** Emits a value to the flow. */
  def emit(value: T)(using Async): Unit