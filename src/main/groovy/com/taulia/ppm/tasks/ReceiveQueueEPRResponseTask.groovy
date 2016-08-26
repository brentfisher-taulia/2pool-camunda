package com.taulia.ppm.tasks

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.springframework.stereotype.Component
import static com.taulia.ppm.tasks.ProcessVariables.*
import static com.taulia.ppm.tasks.ProcessDefinitions.*

@Component(value = "receiveQueueEPRResponseTask")
@Slf4j
@CompileStatic
class ReceiveQueueEPRResponseTask implements JavaDelegate {



  @Override
  void execute(DelegateExecution execution) throws Exception {
    println "Hello world"

  }
}
