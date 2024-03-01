---
bookToc: false
---

# Going further and conclusions

- `Channel`s are the basic communication primitive for exchanging data between `Future`s/`Coroutines` and they are primarily used to model data sources that are *intrinsically hot*, i.e. *that exist without application's request from them*: incoming network connections, event streams, etc...


![expected result](/static/analyzer-e2e.png)
