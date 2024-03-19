type PipelineTransformation[T, R] = ReadableChannel[T] => ReadableChannel[R]

object Controller:

  def oneToOne[T, R](
      publisherChannel: ReadableChannel[T],
      consumer: Consumer[R, ?],
      transformation: PipelineTransformation[T, R] = identity,
  ): Task[Unit] =
    val transformedChannel = transformation(publisherChannel)
    Task:
      consumer.listeningChannel.send(transformedChannel.read())
    .schedule(RepeatUntilFailure())

  def oneToMany[T, R](
      publisherChannel: ReadableChannel[T],
      consumers: Set[Consumer[R, ?]],
      transformation: PipelineTransformation[T, R] = identity,
  ): Task[Unit] = Task:
    val multiplexer = ChannelMultiplexer[R]()
    consumers.foreach(c => multiplexer.addSubscriber(c.listeningChannel))
    multiplexer.addPublisher(transformation(publisherChannel))
    multiplexer.run() // blocking!