package io.github.tassiLuca.posts.direct

import io.github.tassiLuca.posts.PostsModel

trait BlogPostsApp extends PostsServiceComponent with PostsModel with PostsRepositoryComponent:
  override type AuthorId = String
  override type Body = String
  override type Title = String

  val contentVerifier: ContentVerifier
  val authorsService: AuthorsService

  override val repository: PostsRepository = PostsRepository()
