package io.github.tassiLuca.posts.direct

import gears.async.default.given
import gears.async.{Async, Future}
import io.github.tassiLuca.posts.simulates
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.shouldBe

import scala.util.Try

class BlogPostsServiceTest extends AnyFlatSpec with BeforeAndAfterEach:

  private var blogPostsApp: BlogPostsApp = null
  val authorId = "ltassi"
  val postTitle = "A hello world post"
  val postBody = "Hello World Scala Gears!"

  override def beforeEach(): Unit =
    blogPostsApp = new BlogPostsApp:
      override val contentVerifier: ContentVerifier = (t, b) =>
        "PostsService" simulates s"verifying content of post '$t'"
        Right((t, b))
      override val authorsService: AuthorsService = new AuthorsService:
        private val authors = Set(
          Author("ltassi", "Luca", "Tassinari"),
          Author("mrossi", "Mario", "Rossi"),
        )
        override def by(id: AuthorId)(using Async): Try[Author] =
          "PostsService" simulates s"getting author $id info..."
          Try(authors.find(_.authorId == id).get)
      override val service: PostsService = PostsService(contentVerifier, authorsService)

  "BlogPostsService" should "create posts correctly if author and content is legit" in {
    Async.blocking:
      blogPostsApp.service.create(authorId, postTitle, postBody).isRight shouldBe true
      val post = blogPostsApp.service.get(postTitle)
      post.isRight shouldBe true
      post.toOption.get.author shouldBe blogPostsApp.authorsService.by(authorId).get
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
      blogPostsApp.service.create("unauthorized", postTitle, postBody).isLeft shouldBe true
      // TODO: check println?
  }
