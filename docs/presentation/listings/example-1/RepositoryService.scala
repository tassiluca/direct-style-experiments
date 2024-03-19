/** The repository in charge of storing and retrieving blog posts.*/
trait PostsRepository:
  def save(post: Post)(using Async, CanFail): Post
  def exists(postTitle: Title)(using Async, CanFail): Boolean
  def load(postTitle: Title)(using Async, CanFail): Option[Post]

/** The service exposing a set of functionalities to interact with blog posts.*/
trait PostsService:
  def create(authorId: AuthorId, title: Title, body: Body)(using Async, CanFail): Post
  def get(title: Title)(using Async, CanFail): Option[Post]
