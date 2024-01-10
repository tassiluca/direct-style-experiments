package io.github.tassiLuca.gears.posts.quo

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Random, Success}

def f1()(using ExecutionContext): Future[Boolean] = Future {
  println("Started f1")
  Thread.sleep(10_000)
  true
}

def f2()(using ExecutionContext): Future[Boolean] = Future {
  println("Started f2")
  Thread.sleep(5_000)
  true
}

@main def simple(): Unit =
  given ExecutionContext = ExecutionContext.global
  val results =
    for
      res1 <- f1()
      res2 <- f2()
    yield (res1, res2)
  Await.ready(results, Duration.Inf)

@main def usePostsApp(): Unit =
  given ExecutionContext = ExecutionContext.global
  val app = BlogPostsAppQuo
  val post =
    for
      _ <- app.postsService.create("ltassi@gmail.com", "A hello world post", "Hello World!")
      p <- app.postsService.get("A hello world post")
    yield p
  Await.ready(post, Duration.Inf)
  println(post)