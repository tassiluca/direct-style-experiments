package io.github.tassiLuca.dse.blog

/** The blog post repository. */
interface PostsRepository {
    /** Saves a post. */
    suspend fun save(post: Post): Post

    /** Checks if a post with the given [postTitle] exists. */
    suspend fun exists(postTitle: String): Boolean

    /** Loads a post by its [postTitle]. */
    suspend fun load(postTitle: String): Post?

    /** Loads all the posts. */
    suspend fun loadAll(): Sequence<Post>

    companion object {
        /** Creates a new instance of [PostsRepository]. */
        fun create(): PostsRepository = PostsRepositoryImpl()
    }
}

private class PostsRepositoryImpl : PostsRepository {
    @get:Synchronized @set:Synchronized
    private var posts: Set<Post> = setOf()

    override suspend fun save(post: Post): Post {
        require(!exists(post.content.title)) { "Post with title ${post.content.title} already exists" }
        posts = posts + post
        return post
    }

    override suspend fun exists(postTitle: String): Boolean =
        posts.any { it.content.title == postTitle }

    override suspend fun load(postTitle: String): Post? =
        posts.find { it.content.title == postTitle }

    override suspend fun loadAll(): Sequence<Post> =
        posts.asSequence()
}
