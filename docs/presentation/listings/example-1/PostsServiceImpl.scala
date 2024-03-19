override def create(authorId: AuthorId, title: Title, body: Body)(using Async, CanFail): Post =
  if repository.exists(title) then leave(s"Post entitled $title already exists")
  val (post, author) = Async.group: // new completion group
    val content = Future(verifyContent(title, body))
    val author = Future(authorBy(authorId))
    content.zip(author).awaitResult.?
  repository.save(Post(author, post._1, post._2, Date()))

/* Call the Authorship Service keeping track of authorized authors. */
private def authorBy(id: AuthorId)(using Async, CanFail): Author

/* Some computation that verifies the content of the post is appropriate. */
private def verifyContent(title: Title, body: Body)(using Async, CanFail): PostContent