# Blog posts service example: a direct-style vs monadic comparison

## The need for a new `Future` construct

The current implementation of the `Future` monadic construct suffers the following main cons:

- Lack of **referential transparency**
- Lack of **cancellation** mechanisms (and _structured concurrency_)
- **Accidental Sequentiality**

## Example: a blog posts service

> **Idea**: develop a very simple (mocked) service which allows to store and retrive from a repository blog posts, as well as perform some checks before the actual storage.

The example has been implemented using:

- the current Scala `Future` constructs;
- the mechanism abstractions offered by `gears`;
- Kotlin.

The sources are available in the `posts` package of both Kotlin and Scala main folders.

### Structure

#### Scala

Uses Cake Pattern:

- `PostsRepositoryComponent`
  - is the component exposing the `Repository` trait allowing to store and retrieve blog posts;
  - mocking a DB technology with an in-memory collection
- `PostsServiceComponent`
  - is the component exposing the `Service` interface.

Both the components must be designed in an async way.

### 1st version: current Future monadic
