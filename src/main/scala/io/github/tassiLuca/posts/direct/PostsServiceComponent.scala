package io.github.tassiLuca.posts.direct

import gears.async.default.given
import gears.async.{Async, Future, Task}
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.?
import io.github.tassiLuca.posts.{PostsModel, simulates}

import java.util.Date

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

      override def create(authorId: AuthorId, title: Title, body: Body): Either[String, Post] =
        either:
          Async.blocking:
            val post: Future[Post] = Future:
              val p = Post(authorId, title, body, Date())
              val f1 = p.verifyAuthor.run
              val f2 = p.verifyContent.run
              f1.await.?
              f2.await.?
            context.repository.save(post.await)
            post.await

      extension (p: Post)
        private def verifyAuthor(using Async): Task[Either[String, Post]] = Task:
          "PostsService" simulates s"verifying author ${p.author}"
          if Math.random() > 0.3 then Right(p) else Left(s"${p.author} is not authorized to post content!")

        private def verifyContent(using Async): Task[Either[String, Post]] = Task:
          "PostsService" simulates s"verifying author ${p.author}"
          if Math.random() > 0.3 then Right(p) else Left("The post contains non-appropriate sections.")

      override def get(title: Title): Either[String, Post] = Async.blocking:
        context.repository.load(title).toEither.left.map(_.getMessage)

      override def all(): Either[String, LazyList[Post]] = Async.blocking:
        context.repository.loadAll().toEither.left.map(_.getMessage)
