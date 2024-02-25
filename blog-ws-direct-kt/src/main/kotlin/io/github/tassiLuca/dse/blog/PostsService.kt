package io.github.tassiLuca.dse.blog

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Date
import kotlin.NoSuchElementException

/** The service exposing a set of functionalities to interact with blog posts. */
interface PostsService {

    /** Creates a new post. */
    suspend fun create(authorId: String, title: String, body: String): Result<Post>

    /** Retrieves a post by its title. */
    suspend fun get(title: String): Result<Post>

    /** Retrieves all the posts. */
    suspend fun getAll(): Result<Sequence<Post>>

    companion object {
        /** Creates a new instance of [PostsService]. */
        fun create(contentVerifier: ContentVerifier, authorsVerifier: AuthorsVerifier): PostsService =
            PostsServiceImpl(contentVerifier, authorsVerifier)
    }
}

private class PostsServiceImpl(
    private val contentVerifier: ContentVerifier,
    private val authorsVerifier: AuthorsVerifier,
) : PostsService {
    val repository: PostsRepository = PostsRepository.create()

    override suspend fun create(authorId: String, title: String, body: String): Result<Post> = runCatching {
        coroutineScope {
            require(!repository.exists(title)) { "Post with title $title already exists" }
            val content = async { verifyContent(title, body) }
            val author = async { authorBy(authorId) }
            val post = Post(author.await(), content.await(), Date())
            repository.save(post)
        }
    }

    /* Pretending to make a call to the Authorship Service that keeps track of authorized authors. */
    private suspend fun authorBy(id: String): Author {
        "PostsService".simulates("getting author $id info...", maxDuration = 1_000)
        return authorsVerifier(id).getOrThrow() ?: error("Author $id not found")
    }

    /* Some local computation that verifies the content of the post is appropriate. */
    private suspend fun verifyContent(title: String, body: String): PostContent {
        "PostsService".simulates("verifying content of post '$title'", minDuration = 1_000)
        return contentVerifier(title, body).getOrThrow() ?: error("Content of post '$title' is not appropriate")
    }

    override suspend fun get(title: String): Result<Post> = Result.runCatching {
        repository.load(title) ?: throw NoSuchElementException("Post with title $title not found")
    }

    override suspend fun getAll(): Result<Sequence<Post>> = Result.runCatching { repository.loadAll() }
}
