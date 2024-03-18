package io.github.tassiLuca.dse.blog

import gears.async.{Async, Future}
import io.github.tassiLuca.dse.blog.core.{PostsModel, simulates}
import io.github.tassiLuca.dse.boundaries.EitherConversions.given
import io.github.tassiLuca.dse.boundaries.either.{?, leave}
import io.github.tassiLuca.dse.boundaries.CanFail

import java.util.Date
import scala.util.{Failure, Try}

/** The blog posts service component. */
trait PostsServiceComponent:
  context: PostsRepositoryComponent & PostsModel =>

  /** The blog post service instance. */
  val service: PostsService

  /** The service exposing a set of functionalities to interact with blog posts. */
  trait PostsService:
    /** Creates a new blog post with the given [[title]] and [[body]], authored by [[authorId]],
      * or a string explaining the reason of the failure.
      */
    def create(authorId: AuthorId, title: Title, body: Body)(using Async, CanFail): Post

    /** Get a post from its [[title]] or a string explaining the reason of the failure. */
    def get(title: Title)(using Async, CanFail): Option[Post]

    /** Gets all the stored blog posts in a lazy manner or a string explaining the reason of the failure. */
    def all()(using Async, CanFail): LazyList[Post]

  object PostsService:
    def apply(contentVerifier: ContentVerifier, authorsService: AuthorsVerifier): PostsService =
      PostsServiceImpl(contentVerifier, authorsService)

    private class PostsServiceImpl(
        contentVerifier: ContentVerifier,
        authorsVerifier: AuthorsVerifier,
    ) extends PostsService:

      override def create(authorId: AuthorId, title: Title, body: Body)(using Async, CanFail): Post =
        if context.repository.exists(title) then leave(s"A post entitled $title already exists")
        val (post, author) = Async.group:
          val content = Future(verifyContent(title, body))
          val author = Future(authorBy(authorId))
          content.zip(author).awaitResult.?
        context.repository.save(Post(author, post._1, post._2, Date()))

      /* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
      private def authorBy(id: AuthorId)(using Async, CanFail): Author =
        "PostsService".simulates(s"getting author $id info...", maxDuration = 1_000)
        authorsVerifier(id).?

      /* Some local computation that verifies the content of the post is appropriate. */
      private def verifyContent(title: Title, body: Body)(using Async, CanFail): PostContent =
        "PostsService".simulates(s"verifying content of post '$title'", minDuration = 1_000)
        contentVerifier(title, body).?

      override def get(title: Title)(using Async, CanFail): Option[Post] =
        context.repository.load(title)

      override def all()(using Async, CanFail): LazyList[Post] =
        context.repository.loadAll()
