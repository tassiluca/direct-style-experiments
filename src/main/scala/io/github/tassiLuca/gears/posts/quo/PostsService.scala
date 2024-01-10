package io.github.tassiLuca.gears.posts.quo

import io.github.tassiLuca.gears.posts.PostsModel

import java.lang.Thread.sleep
import java.util.Date
import scala.concurrent
import scala.concurrent.{ExecutionContext, Future}

trait PostsRepositoryComponent:
  context: PostsModel =>

  val postsRepository: PostsRepository

  trait PostsRepository:
    def save(post: Post)(using ExecutionContext): Future[Unit]
    def loadAll()(using ExecutionContext): Future[LazyList[Post]]
    def load(postTitle: Title)(using ExecutionContext): Future[Post]

  object PostsRepository:
    def apply(): PostsRepository = PostsLocalRepository()

    private class PostsLocalRepository extends PostsRepository:
      private var posts: Set[Post] = Set()

      override def save(post: Post)(using ExecutionContext): Future[Unit] = Future {
        sleep(1_000)
        require(posts.count(_.title == post.title) == 0, "A post with same title has already been saved")
        println(s"[PostsRepository - ${Thread.currentThread()}] Saving post $post...")
        sleep(3_000)
        synchronized { posts = posts + post }
      }

      override def load(postTitle: Title)(using ExecutionContext): Future[Post] = Future {
        println(s"[PostsRepository - ${Thread.currentThread()}] Loading post $postTitle...")
        sleep(2_000)
        posts.find(_.title == postTitle).get
      }

      override def loadAll()(using ExecutionContext): Future[LazyList[Post]] = Future {
        println(s"[PostsRepository - ${Thread.currentThread()}] Loading all blog posts...")
        sleep(7_000)
        LazyList.from(posts)
      }

trait PostsServiceComponent:
  context: PostsRepositoryComponent with PostsModel =>

  val postsService: PostsService

  trait PostsService:
    def create(authorId: AuthorId, title: Title, body: Body): Future[Unit]
    def get(title: Title): Future[Post]
    def all(): Future[LazyList[Post]]
    def test(): Future[Unit]

  object PostsService:
    def apply(): PostsService = PostsServiceImpl()

    private class PostsServiceImpl extends PostsService:
      given ExecutionContext = ExecutionContext.global

      override def test(): Future[Unit] = Future {
        Thread.sleep(10_000)
        println("ok")
      }

      override def create(authorId: AuthorId, title: Title, body: Body): Future[Unit] =
        println(s"[PostsService - ${Thread.currentThread()}]")
        val authorVerification = Future { verifyAuthor(authorId) } // new thread
        val contentVerification = Future { contentFiltering(title, body) } // new thread
        for
          resultAuthor <- authorVerification
          if resultAuthor
          resultVerification <- contentVerification
          if resultVerification
          _ <- context.postsRepository.save(Post(authorId, title, body, Date()))
        yield ()

      private def verifyAuthor(author: AuthorId): Boolean =
        println(s"[PostsService - ${Thread.currentThread()}] verifying author $author...")
        sleep(6_000) // simulating a call to another service...
        println(s"[PostsService - ${Thread.currentThread()}] $author verification ended!")
        true

      private def contentFiltering(title: Title, body: Body): Boolean =
        println(s"[PostsService - ${Thread.currentThread()}] verifying post entitled $title content...")
        sleep(10_000) // simulating a content filtering algorithm...
        println(s"[PostsService - ${Thread.currentThread()}] post entitled $title verification ended!")
        true

      override def get(title: Title): Future[Post] = context.postsRepository.load(title)

      override def all(): Future[LazyList[Post]] = context.postsRepository.loadAll()

object BlogPostsAppQuo extends PostsServiceComponent with PostsModel with PostsRepositoryComponent:
  override type AuthorId = String
  override type Body = String
  override type Title = String

  override val postsRepository: PostsRepository = PostsRepository()
  override val postsService: PostsService = PostsService()
