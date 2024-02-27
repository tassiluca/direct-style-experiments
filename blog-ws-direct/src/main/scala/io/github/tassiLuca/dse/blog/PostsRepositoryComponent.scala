package io.github.tassiLuca.dse.blog

import gears.async.Async
import io.github.tassiLuca.boundaries.either
import io.github.tassiLuca.boundaries.either.{?, left}
import io.github.tassiLuca.dse.blog.core.{PostsModel, simulates}

/** The component exposing blog posts repositories. */
trait PostsRepositoryComponent:
  context: PostsModel =>

  /** The repository instance. */
  val repository: PostsRepository

  /** The repository in charge of storing and retrieving blog posts. */
  trait PostsRepository:
    /** Save the given [[post]]. */
    def save(post: Post)(using Async): Either[String, Post]

    /** Return true if a post exists with the given title, false otherwise. */
    def exists(postTitle: Title)(using Async): Either[String, Boolean]

    /** Load the post with the given [[postTitle]]. */
    def load(postTitle: Title)(using Async): Either[String, Option[Post]]

    /** Load all the saved post. */
    def loadAll()(using Async): Either[String, LazyList[Post]]

  object PostsRepository:
    /** Constructs a new [[PostsRepository]]. */
    def apply(): PostsRepository = PostsLocalRepository()

    private class PostsLocalRepository extends PostsRepository:
      private var posts: Set[Post] = Set()

      override def save(post: Post)(using Async): Either[String, Post] = either:
        if exists(post.title).? then left("A post with same title has already been saved")
        "PostsRepository".simulates(s"saving post '${post.title}'")
        synchronized { posts = posts + post }
        post

      override def exists(postTitle: Title)(using Async): Either[String, Boolean] = either:
        posts.exists(_.title == postTitle)

      override def load(postTitle: Title)(using Async): Either[String, Option[Post]] = either:
        "PostsRepository".simulates(s"loading post '$postTitle'")
        posts.find(_.title == postTitle)

      override def loadAll()(using Async): Either[String, LazyList[Post]] = either:
        "PostsRepository".simulates(s"loading all blog posts")
        LazyList.from(posts)
