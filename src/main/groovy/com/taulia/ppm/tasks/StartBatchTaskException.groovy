package com.taulia.ppm.tasks

import org.camunda.bpm.engine.ProcessEngineException


class StartBatchTaskException extends ProcessEngineException{
  def StartBatchTaskException(String message) {
    super(message)
  }
}
