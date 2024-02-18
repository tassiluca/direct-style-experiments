package io.github.tassiLuca.analyzer.commons.client

trait AppController:
  def runSession(organizationName: String): Unit
  def stopSession(): Unit
