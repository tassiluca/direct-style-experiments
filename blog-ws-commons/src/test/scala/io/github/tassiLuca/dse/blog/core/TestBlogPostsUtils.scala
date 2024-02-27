package io.github.tassiLuca.dse.blog.core

/** A simple enumeration representing the checks that can be performed on a blog post. */
enum Check:
  /** The content verification check. */
  case ContentVerified

  /** The author verification check. */
  case AuthorVerified

/** A trait representing keeping track of the checks performed. */
trait CheckFlag:
  def completedChecks: Set[Check]
