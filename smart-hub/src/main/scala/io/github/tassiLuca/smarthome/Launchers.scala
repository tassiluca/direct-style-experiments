package io.github.tassiLuca.smarthome

import gears.async.default.given
import gears.async.Async
import io.github.tassiLuca.smarthome.infrastructure.MockedHubManager

/** The launcher of a mocked version of the application, using UI simulators. */
@main def launchMockedHub(): Unit =
  Async.blocking:
    MockedHubManager().run()
