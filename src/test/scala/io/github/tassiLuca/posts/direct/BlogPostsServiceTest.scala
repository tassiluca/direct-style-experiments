package io.github.tassiLuca.posts.direct

import gears.async.default.given
import gears.async.{Async, Future}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.shouldBe

class BlogPostsServiceTest extends AnyFlatSpec with BeforeAndAfterEach:

  private var blogPostsApp: BlogPostsApp = null
  val authorId = "ltassi"
  val postTitle = "A hello world post"
  val postBody = "Hello World Scala Gears!"

  override def beforeEach(): Unit =
    blogPostsApp = new BlogPostsApp:
      override val contentVerifier: ContentVerifier = (t, b) => Right((t, b))
      override val authorsVerifier: AuthorsVerifier = a =>
        require(a == authorId, "No author with the given id matches")
        Author(a, "Luca", "Tassinari")
      override val service: PostsService = PostsService(contentVerifier, authorsVerifier)

  "BlogPostsService" should "create posts correctly if author and content is legit" in {
    Async.blocking:
      blogPostsApp.service.create(authorId, postTitle, postBody).isRight shouldBe true
      val post = blogPostsApp.service.get(postTitle)
      post.isRight shouldBe true
      post.toOption.get.author shouldBe blogPostsApp.authorsVerifier(authorId)
      post.toOption.get.body shouldBe postBody
  }

  "Attempting to create two posts with same title" should "fail" in {
    Async.blocking:
      blogPostsApp.service.create(authorId, postTitle, postBody).isRight shouldBe true
      blogPostsApp.service.create(authorId, postTitle, postBody).isRight shouldBe false
  }

  "BlogPostsService" should "serve concurrently several requests" in {
    Async.blocking:
      val f1 = Future:
        blogPostsApp.service.create(authorId, postTitle, postBody)
      val f2 = Future:
        blogPostsApp.service.create(authorId, "2nd post", "Hello world again")
      f1.await.isRight shouldBe true
      f2.await.isRight shouldBe true
  }

  "BlogPostsService" should "fail on unauthorized author and cancel the content verification check" in {
    Async.blocking:
      val result = blogPostsApp.service.create("unauthorized", postTitle, postBody)
      result.isLeft shouldBe true
      // the cancelling can be observed looking at the logs :(
  }
