package io.github.tassiLuca.dse.blog

import gears.async.default.given
import gears.async.{Async, Future}
import io.github.tassiLuca.dse.blog.core.{Check, CheckFlag}
import io.github.tassiLuca.dse.boundaries.either
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.shouldBe

class BlogPostsServiceTest extends AnyFlatSpec with BeforeAndAfterEach:

  val authorId = "mrossi"
  val postTitle = "A hello world post"
  val postBody = "Hello World Scala Gears!"

  "BlogPostsService" should "create posts correctly if author and content is legit" in {
    Async.blocking:
      val app = blogPostsAppInstance()
      either(app.service.create(authorId, postTitle, postBody)).isRight shouldBe true
      val post = either(app.service.get(postTitle))
      post.isRight shouldBe true
      post.foreach: p =>
        p.map(_.author) shouldBe app.authorsVerifier(authorId).toOption
        p.map(_.body) shouldBe Some(postBody)
  }

  "Attempting to create two posts with same title" should "fail" in {
    Async.blocking:
      val app = blogPostsAppInstance()
      either:
        app.service.create(authorId, postTitle, postBody)
      .isRight shouldBe true
      either:
        app.service.create(authorId, postTitle, postBody)
      .isRight shouldBe false
  }

  "BlogPostsService" should "serve concurrently several requests" in {
    Async.blocking:
      val app = blogPostsAppInstance()
      Future:
        either:
          app.service.create(authorId, postTitle, postBody)
        .isRight shouldBe true
      Future:
        either:
          app.service.create(authorId, "2nd post", "Hello world again")
        .isRight shouldBe true
  }

  "BlogPostsService" should "fail on unauthorized author cancelling content verification check" in {
    Async.blocking:
      val app = blogPostsAppInstance()
      val result = either(app.service.create("unauthorized", postTitle, postBody))
      result.isLeft shouldBe true
      Thread.sleep(3_000) // waiting for the max duration of the content verification check
      app.completedChecks.isEmpty shouldBe true
  }

  def blogPostsAppInstance(): BlogPostsApp & CheckFlag = new BlogPostsApp with CheckFlag:
    private var _completedChecks: Set[Check] = Set.empty
    override def completedChecks: Set[Check] = _completedChecks
    override val contentVerifier: ContentVerifier = (t, b) =>
      _completedChecks += Check.ContentVerified
      Right((t, b))
    override val authorsVerifier: AuthorsVerifier = a =>
      require(a == authorId, "No author with the given id matches")
      _completedChecks += Check.AuthorVerified
      Right(Author(a, "Mario", "Rossi"))
    override val service: PostsService = PostsService(contentVerifier, authorsVerifier)
