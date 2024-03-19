override def create(authorId: AuthorId, title: Title, body: Body)(
    using ExecutionContext
): Future[Post] = for
    exists <- context.repository.exists(title)
    if !exists
    post <- save(authorId, title, body)
  yield post

private def save(authorId: AuthorId, title: Title, body: Body)(using ExecutionContext): Future[Post] =
  val authorAsync = authorBy(authorId)
  val contentAsync = verifyContent(title, body)
  for
    content <- contentAsync
    author <- authorAsync
    post = Post(author, content._1, content._2, Date())
    _ <- context.repository.save(post)
  yield post

/* Pretending call the Authorship Service that keeps track of authorized authors. */
private def authorBy(id: AuthorId)(using ExecutionContext): Future[Author] = ???

/* Some local computation that verifies the content of the post is appropriate. */
private def verifyContent(title: Title, body: Body)(using ExecutionContext): Future[PostContent] = ???