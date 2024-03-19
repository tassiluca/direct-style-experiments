override def create(authorId: AuthorId, title: Title, body: Body)(
  using Async
): Either[String, Post] = either:
  if context.repository.exists(title).? then leave(s"A post entitled $title already exists")
  val (post, author) = Async.group:
    val content = verifyContent(title, body).start()
    val author = authorBy(authorId).start()
    content.zip(author).awaitResult.?
  context.repository.save(Post(author.?, post.?._1, post.?._2, Date())).?

/* Pretending to call the Authorship Service that keeps track of authorized authors. */
private def authorBy(id: AuthorId): Task[Either[String, Author]] = ...

/* Some local computation that verifies the content of the post is appropriate. */
private def verifyContent(title: Title, body: Body): Task[Either[String, PostContent]] = ...