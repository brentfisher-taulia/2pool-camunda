package com.taulia.ppm.tasks

import org.camunda.bpm.engine.ProcessEngineException

class ConfirmEPRAddedException extends ProcessEngineException{
  def ConfirmEPRAddedException(String message) {
    super(message)
  }
}
