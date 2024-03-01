---
bookToc: false
---

# Going further and conclusions

## Recap

- `Channel`s are the basic communication primitive for exchanging data between `Future`s/`Coroutines` and they are primarily used to model data sources that are *intrinsically hot*, i.e. **that exist without application's request from them**: incoming network connections, event streams, etc...
- `Flow`s are control structures, containing executable code. When we call the `collect` method we invoke the code inside the flow, like executing function's code by calling it. 
