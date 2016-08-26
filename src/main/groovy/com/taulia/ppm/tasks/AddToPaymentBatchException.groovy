package com.taulia.ppm.tasks

import org.camunda.bpm.engine.ProcessEngineException

class AddToPaymentBatchException extends  ProcessEngineException{
  def AddToPaymentBatchException(String message) {
    super(message)
  }
}
