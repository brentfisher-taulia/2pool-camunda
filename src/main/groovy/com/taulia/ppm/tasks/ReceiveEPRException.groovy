package com.taulia.ppm.tasks

import org.camunda.bpm.engine.ProcessEngineException

class ReceiveEPRException extends ProcessEngineException {
  def ReceiveEPRException(String message) {
    super(message)
  }
}
