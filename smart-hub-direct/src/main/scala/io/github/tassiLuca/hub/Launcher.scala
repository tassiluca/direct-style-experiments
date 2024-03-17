package io.github.tassiLuca.hub

import gears.async.default.given
import gears.async.Async
import io.github.tassiLuca.hub.adapters.MockedHubManager

/** The launcher of a mocked version of the application, using UI simulators. */
@main def launchUIMockedHub(): Unit = Async.blocking:
  MockedHubManager().run()
