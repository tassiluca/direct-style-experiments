package io.github.tassiLuca.dse.blog

import io.github.tassiLuca.dse.blog.core
import io.github.tassiLuca.dse.blog.core.{Check, CheckFlag}
import gears.async.default.given
import gears.async.{Async, Future}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.shouldBe

class BlogPostsServiceTest extends AnyFlatSpec with BeforeAndAfterEach:

  val authorId = "ltassi"
  val postTitle = "A hello world post"
  val postBody = "Hello World Scala Gears!"

  def newBlogPostsAppInstance(): BlogPostsApp & CheckFlag = new BlogPostsApp with CheckFlag:
    private var _completedChecks: Set[Check] = Set.empty
    override def completedChecks: Set[Check] = _completedChecks
    override val contentVerifier: ContentVerifier = (t, b) =>
      _completedChecks += Check.ContentVerified
      Right((t, b))
    override val authorsVerifier: AuthorsVerifier = a =>
      require(a == authorId, "No author with the given id matches")
      _completedChecks += Check.AuthorVerified
      Author(a, "Luca", "Tassinari")
    override val service: PostsService = PostsService(contentVerifier, authorsVerifier)

  "BlogPostsService" should "create posts correctly if author and content is legit" in {
    Async.blocking:
      val app = newBlogPostsAppInstance()
      app.service.create(authorId, postTitle, postBody).isRight shouldBe true
      val post = app.service.get(postTitle)
      post.isRight shouldBe true
      post.toOption.get.author shouldBe app.authorsVerifier(authorId)
      post.toOption.get.body shouldBe postBody
  }

  "Attempting to create two posts with same title" should "fail" in {
    Async.blocking:
      val app = newBlogPostsAppInstance()
      app.service.create(authorId, postTitle, postBody).isRight shouldBe true
      app.service.create(authorId, postTitle, postBody).isRight shouldBe false
  }

  "BlogPostsService" should "serve concurrently several requests" in {
    Async.blocking:
      val app = newBlogPostsAppInstance()
      val f1 = Future:
        app.service.create(authorId, postTitle, postBody)
      val f2 = Future:
        app.service.create(authorId, "2nd post", "Hello world again")
      f1.await.isRight shouldBe true
      f2.await.isRight shouldBe true
  }

  "BlogPostsService" should "fail on unauthorized author cancelling content verification check" in {
    Async.blocking:
      val app = newBlogPostsAppInstance()
      val result = app.service.create("unauthorized", postTitle, postBody)
      result.isLeft shouldBe true
      Thread.sleep(3_000) // waiting for the max duration of the content verification check
      app.completedChecks.isEmpty shouldBe true
  }
