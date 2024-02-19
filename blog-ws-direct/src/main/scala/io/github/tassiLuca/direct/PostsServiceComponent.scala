package io.github.tassiLuca.direct

import gears.async.default.given
import gears.async.{Async, Future, Task}
import io.github.tassiLuca.boundaries.either.?
import io.github.tassiLuca.boundaries.EitherConversions.given
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.dse.blog.core.{PostsModel, simulates}
import java.time.LocalDate
import java.util.Date

/** The blog posts service component. */
trait PostsServiceComponent:
  context: PostsRepositoryComponent with PostsModel =>

  /** The blog post service instance. */
  val service: PostsService

  /** The service exposing a set of functionalities to interact with blog posts. */
  trait PostsService:
    /** Creates a new blog post with the given [[title]] and [[body]], authored by [[authorId]], or a string explaining
      * the reason of the failure.
      */
    def create(authorId: AuthorId, title: Title, body: Body)(using Async): Either[String, Post]

    /** Get a post from its [[title]] or a string explaining the reason of the failure. */
    def get(title: Title)(using Async): Either[String, Post]

    /** Gets all the stored blog posts in a lazy manner or a string explaining the reason of the failure. */
    def all()(using Async): Either[String, LazyList[Post]]

  object PostsService:
    def apply(contentVerifier: ContentVerifier, authorsService: AuthorsVerifier): PostsService =
      PostsServiceImpl(contentVerifier, authorsService)

    private class PostsServiceImpl(
        contentVerifier: ContentVerifier,
        authorsVerifier: AuthorsVerifier,
    ) extends PostsService:

      override def create(authorId: AuthorId, title: Title, body: Body)(using Async): Either[String, Post] =
        if context.repository.exists(title)
        then Left(s"A post entitled $title already exists")
        else either:
          val f = Future:
            val content = verifyContent(title, body).run
            val author = authorBy(authorId).run
            content.zip(author).await
          val (post, author) = f.awaitResult.?
          context.repository.save(Post(author, post.?._1, post.?._2, Date()))

      /* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
      private def authorBy(id: AuthorId): Task[Author] = Task:
        "PostsService".simulates(s"getting author $id info...", maxDuration = 1_000)
        authorsVerifier(id)

      /* Some local computation that verifies the content of the post is appropriate. */
      private def verifyContent(title: Title, body: Body): Task[Either[String, PostContent]] = Task:
        "PostsService".simulates(s"verifying content of post '$title'", minDuration = 1_000)
        contentVerifier(title, body)

      override def get(title: Title)(using Async): Either[String, Post] =
        context.repository.load(title).toRight(s"Post $title not found")

      override def all()(using Async): Either[String, LazyList[Post]] = either:
        context.repository.loadAll()
