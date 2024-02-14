package io.github.tassiLuca.smarthome

import gears.async.default.given
import gears.async.Async
import io.github.tassiLuca.smarthome.infrastructure.MockedHubManager

@main def launchMockedHub(): Unit = Async.blocking:
  MockedHubManager.run
