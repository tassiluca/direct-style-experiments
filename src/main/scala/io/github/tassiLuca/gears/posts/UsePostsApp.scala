package io.github.tassiLuca.gears.posts

import gears.async.{Async, Future}
import gears.async.default.given

object UsePostsApp extends App:
  val app = BlogPostsApp
  Async.blocking:
    val f = Future:
      println("POST 1")
      app.postsService.create("ltassi@gmail.com", "A hello world post", "Hello World!")
      // println(app.postsService.get("A hello world post"))
//    val f2 = Future:
//      println("POST 2")
//      app.postsService.create("ltassi@gmail.com", "An another post", "Hello World 2!")
//      println(app.postsService.get("An another post"))
//    f1.await
//    f2.await
    println("End of the world...")
    f.await
    println("Ok")
