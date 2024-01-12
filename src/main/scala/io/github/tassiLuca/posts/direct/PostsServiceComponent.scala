package io.github.tassiLuca.posts.direct

import gears.async.AsyncOperations.sleep
import gears.async.default.given
import gears.async.{Async, Future, Task}
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.{?, ThrowableConverter}
import io.github.tassiLuca.posts.{PostsModel, simulates, both}

import java.util.Date
import scala.util.{Failure, Success, Try}

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
    def create(authorId: AuthorId, title: Title, body: Body): Either[String, Post]

    /** Get a post from its [[title]] or a string explaining the reason of the failure. */
    def get(title: Title): Either[String, Post]

    /** Gets all the stored blog posts in a lazy manner or a string explaining the reason of the failure. */
    def all(): Either[String, LazyList[Post]]

  object PostsService:
    def apply(): PostsService = PostsServiceImpl()

    private class PostsServiceImpl extends PostsService:

      given ThrowableConverter[String] = (t: Throwable) => t.getMessage

      override def create(authorId: AuthorId, title: Title, body: Body): Either[String, Post] =
        Async.blocking:
          val post = Post(authorId, title, body, Date())
          if post.verifyAuthor.run.zip(post.verifyContent.run).await.both(r => r.isSuccess && r.get) then
            either { context.repository.save(post).? }
          else Left("Error")

      extension (p: Post)
        private def verifyAuthor(using Async): Task[Try[Boolean]] = Task:
          Try:
            sleep(10_000)
            "PostsService" simulates s"verifying author '${p.author}'"
            true

        private def verifyContent(using Async): Task[Try[Boolean]] = Task:
          Try:
            "PostsService" simulates s"verifying post '${p.title}' content"
            false

      override def get(title: Title): Either[String, Post] = either:
        Async.blocking { context.repository.load(title).? }

      override def all(): Either[String, LazyList[Post]] = either:
        Async.blocking(context.repository.loadAll().?)
