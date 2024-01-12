package io.github.tassiLuca.posts.quo

import io.github.tassiLuca.posts.PostsModel
import io.github.tassiLuca.posts.simulatesBlocking

import java.lang.Thread.sleep
import scala.concurrent.{ExecutionContext, Future}

/** The component exposing blog posts repositories. */
trait PostsRepositoryComponent:
  context: PostsModel =>

  /** The repository instance. */
  val repository: PostsRepository

  /** The repository in charge of storing and retrieving blog posts. */
  trait PostsRepository:
    /** Save the given [[post]]. */
    def save(post: Post)(using ExecutionContext): Future[Unit]

    /** Load the post with the given [[postTitle]]. */
    def loadAll()(using ExecutionContext): Future[LazyList[Post]]

    /** Load all the saved post. */
    def load(postTitle: Title)(using ExecutionContext): Future[Post]

  object PostsRepository:
    /** Constructs a new [[PostsRepository]]. */
    def apply(): PostsRepository = PostsLocalRepository()

    private class PostsLocalRepository extends PostsRepository:
      private var posts: Set[Post] = Set()

      override def save(post: Post)(using ExecutionContext): Future[Unit] = Future:
        require(posts.count(_.title == post.title) == 0, "A post with same title has already been saved")
        "PostsRepository" simulatesBlocking s"saving post ${post.title}"
        synchronized { posts = posts + post }

      override def load(postTitle: Title)(using ExecutionContext): Future[Post] = Future:
        "PostsRepository" simulatesBlocking s"loading post $postTitle"
        posts.find(_.title == postTitle).get

      override def loadAll()(using ExecutionContext): Future[LazyList[Post]] = Future:
        "PostsRepository" simulatesBlocking s"loading all blog posts"
        LazyList.from(posts)
