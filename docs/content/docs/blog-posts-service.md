# Blog posts service example: a direct-style vs monadic comparison

## The need for a new `Future` construct

The current implementation of the `Future` monadic construct suffers the following main cons:

- Lack of **referential transparency**
- Lack of **cancellation** mechanisms (and _structured concurrency_)
- **Accidental Sequentiality**

To show these weaknesses in practice here is presented a self-contained use case design for a simple blog posts web service.

## Example: a blog posts service

> **Idea**: develop a very simple (mocked) service which allows to store and retrive from a repository blog posts, as well as perform some checks before the actual storage.

The example has been implemented using:

- the current Scala `Future` constructs;
- the mechanism abstractions offered by `gears`;

### Structure

Uses Cake Pattern:

- `PostsRepositoryComponent`
  - is the component exposing the `Repository` trait allowing to store and retrieve blog posts;
  - mocking a DB technology with an in-memory collection.
- `PostsServiceComponent`
  - is the component exposing the `Service` interface.

Both the components must be designed in an async way.

### 1st version: current Future monadic

### 2nd version: direct-style

