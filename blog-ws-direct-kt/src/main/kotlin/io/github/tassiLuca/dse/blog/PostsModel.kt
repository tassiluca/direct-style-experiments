package io.github.tassiLuca.dse.blog

import java.util.Date

/** The post author's identifier. */
typealias AuthorId = String

/** The post title. */
typealias PostTitle = String

/** The post body. */
typealias PostBody = String

/** The post content, comprising a [title] and a [body]. */
data class PostContent(val title: PostTitle, val body: PostBody)

/** A post author and their info: their [id], [name] and [surname]. */
data class Author(val id: AuthorId, val name: String, val surname: String)

/** A blog post, comprising an [author], its [content] and the [lastModification] date. */
data class Post(
    val author: Author,
    val content: PostContent,
    val lastModification: Date,
)

/** The blog post content verification logic. It returns the post content if it is appropriate, null otherwise. */
typealias ContentVerifier = suspend (PostTitle, PostBody) -> Result<PostContent?>

/** The author verification logic. It returns the author info if it is authorized, null otherwise. */
typealias AuthorsVerifier = suspend (AuthorId) -> Result<Author?>
