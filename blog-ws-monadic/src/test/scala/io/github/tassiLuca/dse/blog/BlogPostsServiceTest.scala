package io.github.tassiLuca.dse.blog

import io.github.tassiLuca.dse.blog.core.{Check, CheckFlag}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.shouldBe

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext}

class BlogPostsServiceTest extends AnyFlatSpec with BeforeAndAfterEach:

  val timeout: FiniteDuration = 15.seconds
  val authorId = "mrossi"
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
      Right(Author(a, "Mario", "Rossi"))
    override val service: PostsService = PostsService(contentVerifier, authorsVerifier)

  given ExecutionContext = ExecutionContext.global

  "BlogPostsService" should "create posts correctly if author and content is legit" in {
    val app = newBlogPostsAppInstance()
    val creation = app.service.create(authorId, postTitle, postBody)
    Await.ready(creation, timeout)
    creation.isCompleted shouldBe true
    creation.value.get.isSuccess shouldBe true
    val query = Await.result(app.service.get(postTitle), timeout)
    query.isDefined shouldBe true
    query.get.author shouldBe app.authorsVerifier(authorId).toOption.get
    query.get.body shouldBe postBody
  }

  "Attempting to create two posts with same title" should "fail" in {
    val app = newBlogPostsAppInstance()
    Await.ready(app.service.create(authorId, postTitle, postBody), timeout)
    val creation2 = app.service.create(authorId, postTitle, postBody)
    Await.ready(creation2, timeout)
    creation2.value.get.isFailure shouldBe true
  }

  "BlogPostsService" should "serve concurrently several requests" in {
    val app = newBlogPostsAppInstance()
    val creation1 = app.service.create(authorId, postTitle, postBody)
    val postTitle2 = "2nd post"
    val creation2 = app.service.create(authorId, postTitle2, "Hello world again")
    Await.result(creation1, timeout).title shouldBe postTitle
    Await.result(creation2, timeout).title shouldBe postTitle2
  }

  "BlogPostsService" should "fail on unauthorized author **BUT** verification check is not cancelled" in {
    val app = newBlogPostsAppInstance()
    val creation = app.service.create("unauthorized", postTitle, postBody)
    Await.ready(creation, timeout)
    creation.value.get.isFailure shouldBe true
    Thread.sleep(3_000) // waiting for the max duration of the content verification check
    app.completedChecks shouldBe Set(Check.ContentVerified)
  }
