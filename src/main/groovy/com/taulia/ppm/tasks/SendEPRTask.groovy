package com.taulia.ppm.tasks

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.runtime.Execution
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import static com.taulia.ppm.tasks.ProcessVariables.*
import static com.taulia.ppm.tasks.ProcessDefinitions.*

@Component(value = "sendEPRTask")
@Slf4j
class SendEPRTask implements JavaDelegate {

  @Autowired
  RuntimeService runtimeService

  @Override
  void execute(DelegateExecution execution) throws Exception {
    log.debug("Entering SendEPRTask")

    println "Hello world : Send EPRTask"

    String earlyPaymentRequestId
    if (execution.hasVariable(EARLY_PAYMENT_REQUEST_ID.id))
      earlyPaymentRequestId = execution.getVariable(EARLY_PAYMENT_REQUEST_ID.id)

    Execution receiveEPRTask = runtimeService.createExecutionQuery()
      .activityId(RECEIVE_EPR_TASK.id)
      .singleResult()

    if(receiveEPRTask) {
      runtimeService.signal(receiveEPRTask.id,
      [
        (EARLY_PAYMENT_REQUEST_ID.id):earlyPaymentRequestId
      ])
      println "EPR sent to batch processing"
    } else {
      throw new ReceiveEPRException("Unable to find the Payment Batch Process")
    }
  }
}
