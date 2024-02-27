package io.github.tassiLuca.dse.blog

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

class PostsServiceTest : FunSpec() {

    enum class Check { CONTENT_VERIFIED, AUTHOR_VERIFIED }

    init {
        test("BlogPostsService should create posts correctly if author and content is legit") {
            val postsService = newPostsServiceInstance()
            runBlocking {
                val result = postsService.create(AUTHOR_ID, TITLE, BODY)
                result.isSuccess shouldBe true
                val post = postsService.get(TITLE)
                post.isSuccess shouldBe true
                with(post.getOrThrow()) {
                    content.title shouldBe TITLE
                    content.body shouldBe BODY
                    author.id shouldBe AUTHOR_ID
                }
            }
        }

        test("Attempting to create two posts with same title should fail") {
            val postsService = newPostsServiceInstance()
            runBlocking {
                postsService.create(AUTHOR_ID, TITLE, BODY).isSuccess shouldBe true
                postsService.create(AUTHOR_ID, TITLE, BODY).isFailure shouldBe true
            }
        }

        test("Failing on unauthorized author cancel content verification check") {
            var completedChecks = emptySet<Check>()
            val postsService = PostsService.create(
                contentVerifier = { _, _ ->
                    completedChecks = completedChecks + Check.CONTENT_VERIFIED
                    Result.success(PostContent(TITLE, BODY))
                },
                authorsVerifier = { _ ->
                    completedChecks = completedChecks + Check.AUTHOR_VERIFIED
                    error("Verification aborted")
                },
            )
            runBlocking {
                postsService.create(AUTHOR_ID, TITLE, BODY).isFailure shouldBe true
                delay(3_000) // waiting for the max duration of the content verification check
                completedChecks shouldBe setOf(Check.AUTHOR_VERIFIED)
            }
        }
    }

    private fun newPostsServiceInstance() = PostsService.create(
        contentVerifier = { _, _ -> Result.success(PostContent(TITLE, BODY)) },
        authorsVerifier = { id -> Result.success(Author(id, AUTHOR_NAME, AUTHOR_SURNAME)) },
    )

    companion object {
        private const val AUTHOR_ID = "mrossi"
        private const val AUTHOR_NAME = "Mario"
        private const val AUTHOR_SURNAME = "Rossi"
        private const val TITLE = "A hello world post"
        private const val BODY = "Hello World Kotlin Coroutines!"
    }
}
