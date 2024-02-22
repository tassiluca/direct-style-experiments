package io.github.tassiLuca.dse.blog.core

import java.util.Date

/** The model of a simple blog posts service. */
trait PostsModel:

  /** The author identifier. */
  type AuthorId

  /** The posts title. */
  type Title

  /** The posts body. */
  type Body

  /** The content of the post. */
  type PostContent = (Title, Body)

  /** A blog post, comprising of an author, title, body and the information about last modification. */
  case class Post(author: Author, title: Title, body: Body, lastModification: Date)

  /** A post author and their info. */
  case class Author(authorId: AuthorId, name: String, surname: String)

  /** A function that verifies the content of the post, returning [[Right]] with the content of
    * the post if the verification succeeds or [[Left]] with the reason why failed.
    */
  type ContentVerifier = (Title, Body) => Either[String, PostContent]

  /** A function that verifies the author has appropriate permissions, returning their information. */
  type AuthorsVerifier = AuthorId => Author