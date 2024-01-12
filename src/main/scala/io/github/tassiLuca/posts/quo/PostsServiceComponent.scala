package io.github.tassiLuca.posts.quo

import io.github.tassiLuca.posts.{PostsModel, simulatesBlocking}

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
    /** Creates a new blog post with the given [[title]] and [[body]], authored by [[authorId]], or a string explaining
      * the reason of the failure.
      */
    def create(authorId: AuthorId, title: Title, body: Body): Future[Unit]

    /** Get a post from its [[title]] or a string explaining the reason of the failure. */
    def get(title: Title): Future[Post]

    /** Gets all the stored blog posts in a lazy manner or a string explaining the reason of the failure. */
    def all(): Future[LazyList[Post]]

  object PostsService:
    def apply(): PostsService = PostsServiceImpl()

    private class PostsServiceImpl extends PostsService:
      given ExecutionContext = ExecutionContext.global

      override def create(authorId: AuthorId, title: Title, body: Body): Future[Unit] =
        val post = Post(authorId, title, body, Date())
        val authorVerification = Future { post.verifyAuthor }
        val contentVerification = Future { post.verifyContent }
        for
          resultAuthor <- authorVerification
          if resultAuthor
          resultVerification <- contentVerification
          if resultVerification
          _ <- context.repository.save(post)
        yield ()

      extension (p: Post)
        private def verifyAuthor: Boolean =
          "PostsService" simulatesBlocking s"verifying author '${p.author}''"
          if Math.random() > 0.3 then true else false

        private def verifyContent: Boolean =
          "PostsService" simulatesBlocking s"verifying post '${p.title}' content"
          if Math.random() > 0.3 then true else false

      override def get(title: Title): Future[Post] = context.repository.load(title)

      override def all(): Future[LazyList[Post]] = context.repository.loadAll()
