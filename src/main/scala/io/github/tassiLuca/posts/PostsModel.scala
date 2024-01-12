package io.github.tassiLuca.posts

import java.util.Date

/** The model of a simple blog posts service. */
trait PostsModel:

  type AuthorId

  /** The posts title. */
  type Title

  /** The posts body. */
  type Body

  /** A blog post, comprising of an author, title, body and the information about last modification. */
  case class Post(author: AuthorId, title: Title, body: Body, lastModification: Date)
