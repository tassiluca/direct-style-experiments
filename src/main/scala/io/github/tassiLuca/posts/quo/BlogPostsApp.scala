package io.github.tassiLuca.posts.quo

import io.github.tassiLuca.posts.PostsModel

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

object BlogPostsApp$ extends PostsServiceComponent with PostsModel with PostsRepositoryComponent:
  override type AuthorId = String
  override type Body = String
  override type Title = String

  override val repository: PostsRepository = PostsRepository()
  override val service: PostsService = PostsService()

@main def usePostsApp(): Unit =
  given ExecutionContext = ExecutionContext.global
  val app = BlogPostsApp$
  val post =
    for
      _ <- app.service.create("ltassi@gmail.com", "A hello world post", "Hello World!")
      p <- app.service.get("A hello world post")
    yield p
  Await.ready(post, Duration.Inf)
  println(post.value)
  