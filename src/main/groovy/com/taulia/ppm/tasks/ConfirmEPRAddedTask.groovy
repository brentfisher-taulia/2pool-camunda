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

@Component(value = "confirmEPRAddedTask")
@Slf4j
class ConfirmEPRAddedTask implements JavaDelegate {


  @Autowired
  RuntimeService runtimeService


  @Override
  void execute(DelegateExecution execution) throws Exception {
    println("Entering Confirm Added")
    log.debug("Entering Confirm Added")

    String earlyPaymentRequestId = ""
    if (execution.hasVariable(EARLY_PAYMENT_REQUEST_ID.id))
      earlyPaymentRequestId = execution.getVariable(EARLY_PAYMENT_REQUEST_ID.id)
    else {
      throw new ConfirmEPRAddedException("Unable to confirm payment back to EPR process due to missing execution " +
        "variable [${EARLY_PAYMENT_REQUEST_ID} for the EPR")
    }
    println "Using EPR:${earlyPaymentRequestId}"
    log.debug "Using EPR:${earlyPaymentRequestId}"

    String batchId
    if (execution.hasVariable(PAYMENT_BATCH_ID.id))
      batchId = execution.getVariable(PAYMENT_BATCH_ID.id)
    else {
      throw new ConfirmEPRAddedException("Unable to confirm payment back to EPR process due to missing execution " +
        "variable [${PAYMENT_BATCH_ID} for the payment batch")
    }
    println "Using BATCH:${batchId}"
    log.debug "Using BATCH:${batchId}"


    Execution confirmEPRAddedTask = runtimeService.createExecutionQuery()
      .processInstanceBusinessKey((String)earlyPaymentRequestId)
      .activityId(CONFIRM_EPR_ADDED_TASK.id)
      .singleResult()

    if (confirmEPRAddedTask) {
      runtimeService.signal(confirmEPRAddedTask.id,
        [
          (PAYMENT_BATCH_ID.id): batchId
        ])
    } else {
      throw new ReceiveEPRException("Unable to find the EPR Process instance with epr key [${earlyPaymentRequestId}]")
    }

  }
}
