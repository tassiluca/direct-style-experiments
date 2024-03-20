package io.github.tassiLuca.dse.blog.core

import java.util.Date
import scala.util.Try

/** The model of a simple blog posts service. */
trait PostsModel:

  /** The post author's identifier. */
  type AuthorId

  /** The posts title. */
  type Title

  /** The posts body. */
  type Body

  /** The content of the post. */
  type PostContent = (Title, Body)

  /** A post author and their info. */
  case class Author(authorId: AuthorId, name: String, surname: String)

  /** A blog post, comprising an author, title, body and the last modification. */
  case class Post(author: Author, title: Title, body: Body, lastModification: Date)

  /** A function that verifies the content of the post, returning a [[scala.util.Success]] with
    * the content of  the post if the verification succeeds or a [[scala.util.Failure]] otherwise.
    */
  type ContentVerifier = (Title, Body) => Try[PostContent]

  /** A function that verifies the author has appropriate permissions, returning a
    * [[scala.util.Success]] with their information or a [[scala.util.Failure]] otherwise.
    */
  type AuthorsVerifier = AuthorId => Try[Author]
