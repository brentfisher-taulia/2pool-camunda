package com.taulia.ppm.services

import org.camunda.bpm.engine.ProcessEngineException

class TaskExecutionWithRetryException extends ProcessEngineException {
  private ProcessEngineException[] causes

  def TaskExecutionWithRetryException(String message, causes) {
    super(message)

    this.causes = causes
  }
  def String toString(){
    "$message : caused by " +
      causes.collect{
        it.toString()
      }
  }
}
