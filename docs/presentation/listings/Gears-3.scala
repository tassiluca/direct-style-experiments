def suspendingFunction(using Async): Int =
  Async.group: // needs the Async capability! It creates a new CompletionGroup
    // here an Async.Spawn context is available
    Future: // Creates a child CompletionGroup
      // ...