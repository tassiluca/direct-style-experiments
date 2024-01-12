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
    /** Creates a new blog post with the given [[title]] and [[body]], authored by [[authorId]]. */
    def create(authorId: AuthorId, title: Title, body: Body): Future[Post]

    /** Get a post from its [[title]]. */
    def get(title: Title): Future[Post]

    /** Gets all the stored blog posts in a lazy manner. */
    def all(): Future[LazyList[Post]]

  object PostsService:
    def apply(): PostsService = PostsServiceImpl()

    private class PostsServiceImpl extends PostsService:

      opaque type PostContent = (Title, Body)
      given ExecutionContext = ExecutionContext.global

      override def create(authorId: AuthorId, title: Title, body: Body): Future[Post] =
        val author = authorBy(authorId)
        val content = verifyContent(title, body)
        for
          a <- author
          c <- content
          post = Post(a, c._1, c._2, Date())
          _ <- context.repository.save(post)
        yield post

      private def authorBy(id: AuthorId)(using ExecutionContext): Future[Author] = Future:
        "PostsService" simulatesBlocking s"getting author $id info..."
        Author(id, "Luca", "Tassinari")

      private def verifyContent(title: Title, body: Body)(using ExecutionContext): Future[PostContent] = Future:
        "PostsService" simulatesBlocking s"verifying content of the post '$title'"
        (title, body)

      override def get(title: Title): Future[Post] = context.repository.load(title)

      override def all(): Future[LazyList[Post]] = context.repository.loadAll()
