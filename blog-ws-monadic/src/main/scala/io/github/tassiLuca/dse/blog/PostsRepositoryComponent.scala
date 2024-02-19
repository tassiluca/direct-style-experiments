package io.github.tassiLuca.dse.blog

import io.github.tassiLuca.dse.blog.core.{PostsModel, simulatesBlocking}

import scala.concurrent.{ExecutionContext, Future}

/** The component exposing blog posts repositories. */
trait PostsRepositoryComponent:
  context: PostsModel =>

  /** The repository instance. */
  val repository: PostsRepository

  /** The repository in charge of storing and retrieving blog posts. */
  trait PostsRepository:
    /** Save the given [[post]]. */
    def save(post: Post)(using ExecutionContext): Future[Post]

    /** @return a [[Future]] completed with true if a post exists with the given title, false otherwise. */
    def exists(postTitle: Title)(using ExecutionContext): Future[Boolean]

    /** @return
      *   a [[Future]] completed with a full optional with the post with the given [[postTitle]] or an empty optional.
      */
    def load(postTitle: Title)(using ExecutionContext): Future[Option[Post]]

    /** Load the post with the given [[postTitle]]. */
    def loadAll()(using ExecutionContext): Future[LazyList[Post]]

  object PostsRepository:
    /** Constructs a new [[PostsRepository]]. */
    def apply(): PostsRepository = InMemoryPostsRepository()

    private class InMemoryPostsRepository extends PostsRepository:
      private var posts: Set[Post] = Set()

      override def save(post: Post)(using ExecutionContext): Future[Post] = Future:
        require(!posts.exists(_.title == post.title), "A post with same title has already been saved")
        "PostsRepository" simulatesBlocking s"saving post '${post.title}'"
        synchronized { posts = posts + post }
        post

      override def exists(postTitle: Title)(using ExecutionContext): Future[Boolean] = Future:
        posts.exists(_.title == postTitle)

      override def load(postTitle: Title)(using ExecutionContext): Future[Option[Post]] = Future:
        "PostsRepository" simulatesBlocking s"loading post '$postTitle'"
        posts.find(_.title == postTitle)

      override def loadAll()(using ExecutionContext): Future[LazyList[Post]] = Future:
        "PostsRepository" simulatesBlocking s"loading all blog posts"
        LazyList.from(posts)
