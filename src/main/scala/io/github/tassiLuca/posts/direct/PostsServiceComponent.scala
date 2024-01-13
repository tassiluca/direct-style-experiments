package io.github.tassiLuca.posts.direct

import gears.async.default.given
import gears.async.{Async, Task}
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.{?, ThrowableConverter}
import io.github.tassiLuca.posts.{PostsModel, simulates}

import java.util.Date
import scala.util.{Failure, Success, Try}

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
    def apply(contentVerifier: ContentVerifier, authorsService: AuthorsService): PostsService =
      PostsServiceImpl(contentVerifier, authorsService)

    private class PostsServiceImpl(
        contentVerifier: ContentVerifier,
        authorsService: AuthorsService,
    ) extends PostsService:

      given ThrowableConverter[String] = (t: Throwable) => t.getMessage // TODO put in an object of given instances

      override def create(authorId: AuthorId, title: Title, body: Body)(using Async): Either[String, Post] = either:
        val (author, content) = authorBy(authorId).run.zip(verifyContent(title, body).run).awaitResult.?
        val post = Post(author, content.?._1, content.?._2, Date())
        context.repository.save(post).?
        post

      private def authorBy(id: AuthorId): Task[Author] = Task:
        authorsService.by(id).get

      private def verifyContent(title: Title, body: Body): Task[Either[String, PostContent]] = Task:
        contentVerifier(title, body)

      override def get(title: Title)(using Async): Either[String, Post] = either:
        context.repository.load(title).?

      override def all()(using Async): Either[String, LazyList[Post]] = either:
        context.repository.loadAll().?
