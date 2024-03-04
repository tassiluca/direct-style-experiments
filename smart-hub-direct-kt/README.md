# Smart hub example - Kotlin Coroutines version

To run the example:

```
./gradlew :smart-hub-direct-kt:run
```

Three panels should pop up, one for each sensor type, and a dashboard showing the state of the system.
Entering some value in the panels and pressing the "Send" button, after 10 seconds (the configured sampling window), the system should react to the data received, updating the dashboard with the new state.

![Smart hub example](../docs/content/res/img/smart-hub.png)

Have a look to [the documentation](https://tassiluca.github.io/direct-style-experiments/docs/04-rears/) for more details.
