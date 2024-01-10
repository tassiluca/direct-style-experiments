package io.github.tassiLuca.gears.posts

import gears.async.{Async, Future}
import gears.async.AsyncOperations.sleep
import gears.async.default.given

import java.util.Date

trait PostsRepositoryComponent:
  context: PostsModel =>

  val postsRepository: PostsRepository

  trait PostsRepository:
    def save(post: Post)(using Async): Boolean
    def loadAll()(using Async): LazyList[Post]
    def load(postTitle: Title)(using Async): Option[Post]

  object PostsRepository:
    def apply(): PostsRepository = PostsLocalRepository()

    private class PostsLocalRepository extends PostsRepository:
      private var posts: Set[Post] = Set()

      override def save(post: Post)(using Async): Boolean =
        sleep(1_000)
        require(posts.count(_.title == post.title) == 0, "A post with same title has already been saved")
        println(s"[PostsRepository - ${Thread.currentThread()}] Saving post $post...")
        sleep(3_000)
        posts = posts + post
        true

      override def load(postTitle: Title)(using Async): Option[Post] =
        println(s"[PostsRepository - ${Thread.currentThread()}] Loading post $postTitle...")
        sleep(2_000)
        posts.find(_.title == postTitle)

      override def loadAll()(using Async): LazyList[Post] =
        println(s"[PostsRepository - ${Thread.currentThread()}] Loading all blog posts...")
        sleep(7_000)
        LazyList.from(posts)

trait PostsServiceComponent:
  context: PostsRepositoryComponent with PostsModel =>

  val postsService: PostsService

  trait PostsService:
    def create(authorId: AuthorId, title: Title, body: Body): Unit
    def get(title: Title): Post
    def all(): LazyList[Post]

  object PostsService:
    def apply(): PostsService = PostsServiceImpl()

    private class PostsServiceImpl extends PostsService:
      override def create(authorId: AuthorId, title: Title, body: Body): Unit = Async.blocking:
        println(s"[PostsService - ${Thread.currentThread()}]")
        val authorVerification = Future(verifyAuthor(authorId)) // new thread
        val contentVerification = Future(contentFiltering(title, body)) // new thread
        if (authorVerification.await && contentVerification.await) { // synchronization point
          context.postsRepository.save(Post(authorId, title, body, Date()))
        }

      private def verifyAuthor(author: AuthorId)(using Async): Boolean =
        println(s"[PostsService - ${Thread.currentThread()}] verifying author $author...")
        sleep(6_000) // simulating a call to another service...
        println(s"[PostsService - ${Thread.currentThread()}] $author verification ended!")
        true

      private def contentFiltering(title: Title, body: Body)(using Async): Boolean =
        println(s"[PostsService - ${Thread.currentThread()}] verifying post entitled $title content...")
        sleep(10_000) // simulating a content filtering algorithm...
        println(s"[PostsService - ${Thread.currentThread()}] post entitled $title verification ended!")
        true

      override def get(title: Title): Post = Async.blocking:
        context.postsRepository.load(title).get

      override def all(): LazyList[Post] = Async.blocking:
        context.postsRepository.loadAll()

object BlogPostsApp extends PostsServiceComponent with PostsModel with PostsRepositoryComponent:
  override type AuthorId = String
  override type Body = String
  override type Title = String

  override val postsRepository: BlogPostsApp.PostsRepository = PostsRepository()
  override val postsService: BlogPostsApp.PostsService = PostsService()
