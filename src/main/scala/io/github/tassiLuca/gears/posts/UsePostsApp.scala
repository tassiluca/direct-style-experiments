package io.github.tassiLuca.gears.posts

import gears.async.{Async, Future}
import gears.async.default.given

/* TODO WARNING: USING AN OBJECT WHICH EXTENDS `App` CAUSES STARVATION!
 * THE WORKER THREAD GETS STUCK WAITING FOR THE RELEASE OF THE MONITOR LOCK
 *    waiting on the Class initialization monitor for io.github.tassiLuca.gears.posts.UsePostsApp$
 * TO BE INVESTIGATED! */
@main def usePostsApp(): Unit =
  val app = BlogPostsApp
  Async.blocking:
    val f1 = Future:
      println(s"POST 1 carried by ${Thread.currentThread()}")
      app.postsService.create("ltassi@gmail.com", "A hello world post", "Hello World!")
      println(app.postsService.get("A hello world post"))
    val f2 = Future:
      println(s"POST 2 carried by ${Thread.currentThread()}")
      app.postsService.create("ltassi@gmail.com", "An another post", "Hello World 2!")
      println(app.postsService.get("An another post"))
    f1.await
    f2.await
