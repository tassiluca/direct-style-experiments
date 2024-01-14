package io.github.tassiLuca.posts.quo

import io.github.tassiLuca.posts.PostsModel

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

trait BlogPostsApp extends PostsServiceComponent with PostsModel with PostsRepositoryComponent:
  override type AuthorId = String
  override type Body = String
  override type Title = String

  val contentVerifier: ContentVerifier
  val authorsVerifier: AuthorsVerifier

  override val repository: PostsRepository = PostsRepository()

@main def usePostsApp(): Unit =
  given ExecutionContext = ExecutionContext.global
  val app = new BlogPostsApp:
    override val contentVerifier: ContentVerifier = (t, b) => Right((t, b))
    override val authorsVerifier: AuthorsVerifier = a =>
      require(a == "ltassi@gmail.com", "No author with the given id matches")
      Author(a, "Luca", "Tassinari")
    override val service: PostsService = PostsService(contentVerifier, authorsVerifier)
  val post =
    for
      _ <- app.service.create("ltassi@gmail.com", "A hello world post", "Hello World!")
      p <- app.service.get("A hello world post")
    yield p
  Await.ready(post, Duration.Inf)
  println(post.value)
