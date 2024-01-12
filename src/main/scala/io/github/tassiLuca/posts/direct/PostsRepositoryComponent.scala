package io.github.tassiLuca.posts.direct

import gears.async.Async
import io.github.tassiLuca.posts.{PostsModel, simulates}

import scala.util.Try

/** The component exposing blog posts repositories. */
trait PostsRepositoryComponent:
  context: PostsModel =>

  /** The repository instance. */
  val repository: PostsRepository

  /** The repository in charge of storing and retrieving blog posts. */
  trait PostsRepository:
    /** Save the given [[post]]. */
    def save(post: Post)(using Async): Try[Unit]

    /** Load the post with the given [[postTitle]]. */
    def load(postTitle: Title)(using Async): Try[Post]

    /** Load all the saved post. */
    def loadAll()(using Async): Try[LazyList[Post]]

  object PostsRepository:
    /** Constructs a new [[PostsRepository]]. */
    def apply(): PostsRepository = PostsLocalRepository()

    private class PostsLocalRepository extends PostsRepository:
      private var posts: Set[Post] = Set()

      override def save(post: Post)(using Async): Try[Unit] = Try:
        require(posts.count(_.title == post.title) == 0, "A post with same title has already been saved")
        "PostsRepository" simulates s"saving post ${post.title}"
        synchronized { posts = posts + post }

      override def load(postTitle: Title)(using Async): Try[Post] = Try:
        "PostsRepository" simulates s"loading post $postTitle"
        posts.find(_.title == postTitle).get

      override def loadAll()(using Async): Try[LazyList[Post]] = Try:
        "PostsRepository" simulates s"loading all blog posts"
        LazyList.from(posts)
