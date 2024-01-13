package io.github.tassiLuca.posts.direct

import gears.async.default.given
import gears.async.{Async, Task}
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.{?, ThrowableConverter}
import io.github.tassiLuca.posts.{PostsModel, simulates}

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

      opaque type PostContent = (Title, Body)

      given ThrowableConverter[String] = (t: Throwable) => t.getMessage

      override def create(authorId: AuthorId, title: Title, body: Body): Either[String, Post] = Async.blocking:
        either:
          val (author, content) = authorBy(authorId).run.zip(verifyContent(title, body).run).awaitResult.?
          val post = Post(author, content._1, content._2, Date())
          context.repository.save(post).?
          post

      private def authorBy(id: AuthorId): Task[Author] = Task:
        "PostsService" simulates s"getting author $id info..."
        Author(id, "Luca", "Tassinari")

      private def verifyContent(title: Title, body: Body): Task[PostContent] = Task:
        "PostsService" simulates s"verifying content of the post '$title'"
        (title, body)

      override def get(title: Title): Either[String, Post] = either:
        Async.blocking { context.repository.load(title).? }

      override def all(): Either[String, LazyList[Post]] = either:
        Async.blocking { context.repository.loadAll().? }
