package io.github.tassiLuca.dse.blog

import io.github.tassiLuca.dse.blog.core.{PostsModel, simulatesBlocking}

import java.util.Date
import scala.concurrent
import scala.concurrent.{ExecutionContext, Future}

/** The component blog posts service. */
trait PostsServiceComponent:
  context: PostsRepositoryComponent with PostsModel =>

  /** The blog post service instance. */
  val service: PostsService

  /** The service exposing a set of functionalities to interact with blog posts. */
  trait PostsService:
    /** Creates a new blog post with the given [[title]] and [[body]], authored by [[authorId]]. */
    def create(authorId: AuthorId, title: Title, body: Body)(using ExecutionContext): Future[Post]

    /** Get a post from its [[title]]. */
    def get(title: Title)(using ExecutionContext): Future[Post]

    /** Gets all the stored blog posts in a lazy manner. */
    def all()(using ExecutionContext): Future[LazyList[Post]]

  object PostsService:
    def apply(contentVerifier: ContentVerifier, authorsVerifier: AuthorsVerifier): PostsService =
      PostsServiceImpl(contentVerifier, authorsVerifier)

    private class PostsServiceImpl(
        contentVerifier: ContentVerifier,
        authorsVerifier: AuthorsVerifier,
    ) extends PostsService:

      override def create(authorId: AuthorId, title: Title, body: Body)(using ExecutionContext): Future[Post] =
        for
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

      /* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
      private def authorBy(id: AuthorId)(using ExecutionContext): Future[Author] = Future:
        "PostsService".simulatesBlocking(s"getting author $id info...", maxDuration = 1_000)
        authorsVerifier(id)

      /* Some local computation that verifies the content of the post is appropriate. */
      private def verifyContent(title: Title, body: Body)(using ExecutionContext): Future[PostContent] = Future:
        "PostsService".simulatesBlocking(s"verifying content of the post '$title'", minDuration = 1_000)
        contentVerifier(title, body) match { case Left(e) => throw RuntimeException(e); case Right(v) => v }

      override def get(title: Title)(using ExecutionContext): Future[Post] =
        context.repository.load(title).map(_.get)

      override def all()(using ExecutionContext): Future[LazyList[Post]] =
        context.repository.loadAll()
