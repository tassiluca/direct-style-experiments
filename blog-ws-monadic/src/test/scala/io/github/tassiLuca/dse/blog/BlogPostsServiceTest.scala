package io.github.tassiLuca.dse.blog

import io.github.tassiLuca.dse.blog.BlogPostsApp
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.shouldBe

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext}

class BlogPostsServiceTest extends AnyFlatSpec with BeforeAndAfterEach:

  private var blogPostsApp: BlogPostsApp = null
  val authorId = "ltassi"
  val postTitle = "A hello world post"
  val postBody = "Hello World Scala Gears!"
  
  given ExecutionContext = ExecutionContext.global

  override def beforeEach(): Unit =
    blogPostsApp = new BlogPostsApp:
      override val contentVerifier: ContentVerifier = (t, b) =>
        println("Verifying content")
        Right((t, b))
      override val authorsVerifier: AuthorsVerifier = a =>
        require(a == authorId, "No author with the given id matches")
        println("Verifying author")
        Author(a, "Luca", "Tassinari")
      override val service: PostsService = PostsService(contentVerifier, authorsVerifier)

  "BlogPostsService" should "create posts correctly if author and content is legit" in {
    val creation = blogPostsApp.service.create(authorId, postTitle, postBody)
    Await.ready(creation, 15.seconds)
    creation.isCompleted shouldBe true
    creation.value.get.isSuccess shouldBe true
    val query = Await.result(blogPostsApp.service.get(postTitle), 15.seconds)
    query.author shouldBe blogPostsApp.authorsVerifier(authorId)
    query.body shouldBe postBody
  }

  "Attempting to create two posts with same title" should "fail" in {
    Await.ready(blogPostsApp.service.create(authorId, postTitle, postBody), 15.seconds)
    val creation2 = blogPostsApp.service.create(authorId, postTitle, postBody)
    Await.ready(creation2, 15.seconds)
    creation2.value.get.isFailure shouldBe true
  }

  "BlogPostsService" should "serve concurrently several requests" in {
    val creation1 = blogPostsApp.service.create(authorId, postTitle, postBody)
    val postTitle2 = "2nd post"
    val creation2 = blogPostsApp.service.create(authorId, postTitle2, "Hello world again")
    Await.result(creation1, 15.seconds).title shouldBe postTitle
    Await.result(creation2, 15.seconds).title shouldBe postTitle2
  }

  "BlogPostsService" should "fail on unauthorized author **BUT** verification check is not cancelled" in {
    val creation = blogPostsApp.service.create("unauthorized", postTitle, postBody)
    Await.ready(creation, 15.seconds)
    creation.value.get.isFailure shouldBe true
  }
