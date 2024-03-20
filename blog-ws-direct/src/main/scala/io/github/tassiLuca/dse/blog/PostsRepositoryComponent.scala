package io.github.tassiLuca.dse.blog

import gears.async.Async
import io.github.tassiLuca.dse.blog.core.{PostsModel, simulates}
import io.github.tassiLuca.dse.boundaries.either.fail
import io.github.tassiLuca.dse.boundaries.{CanFail, either}

/** The component exposing blog posts repositories. */
trait PostsRepositoryComponent:
  context: PostsModel =>

  /** The repository instance. */
  val repository: PostsRepository

  /** The repository in charge of storing and retrieving blog posts. */
  trait PostsRepository:
    /** Save the given [[post]]. */
    def save(post: Post)(using Async, CanFail): Post

    /** Return true if a post exists with the given title, false otherwise. */
    def exists(postTitle: Title)(using Async, CanFail): Boolean

    /** Load the post with the given [[postTitle]]. */
    def load(postTitle: Title)(using Async, CanFail): Option[Post]

    /** Load all the saved post. */
    def loadAll()(using Async, CanFail): LazyList[Post]

  object PostsRepository:
    /** Constructs a new [[PostsRepository]]. */
    def apply(): PostsRepository = PostsLocalRepository()

    private class PostsLocalRepository extends PostsRepository:
      private var posts: Set[Post] = Set()

      override def save(post: Post)(using Async, CanFail): Post =
        if exists(post.title) then fail("A post with same title has already been saved")
        "PostsRepository".simulates(s"saving post '${post.title}'")
        synchronized { posts = posts + post }
        post

      override def exists(postTitle: Title)(using Async, CanFail): Boolean =
        posts.exists(_.title == postTitle)

      override def load(postTitle: Title)(using Async, CanFail): Option[Post] =
        "PostsRepository".simulates(s"loading post '$postTitle'")
        posts.find(_.title == postTitle)

      override def loadAll()(using Async, CanFail): LazyList[Post] =
        "PostsRepository".simulates(s"loading all blog posts")
        LazyList.from(posts)
