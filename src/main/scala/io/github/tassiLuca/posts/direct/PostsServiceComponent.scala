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

      override def create(authorId: AuthorId, title: Title, body: Body): Either[String, Post] = either:
        Async.blocking:
          val author = authorBy(authorId).run
          val content = verifyContent(title, body).run
          val post = Post(author.await.?, content.await.?._1, content.await.?._2, Date())
          context.repository.save(post).?
          post

      private def authorBy(id: AuthorId)(using Async): Task[Either[String, Author]] = Task:
        either:
          "PostsService" simulates s"getting author $id info..."
          Author(id, "Luca", "Tassinari")

      private def verifyContent(title: Title, body: Body)(using Async): Task[Either[String, PostContent]] = Task:
        either:
          "PostsService" simulates s"verifying content of the post '$title'"
          (title, body)

      override def get(title: Title): Either[String, Post] = either:
        Async.blocking { context.repository.load(title).? }

      override def all(): Either[String, LazyList[Post]] = either:
        Async.blocking { context.repository.loadAll().? }
