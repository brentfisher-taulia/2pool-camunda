package com.taulia.ppm.tasks

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.runtime.VariableInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.taulia.ppm.tasks.ProcessVariables.*
import static com.taulia.ppm.tasks.ProcessDefinitions.*

@Component(value = "addToPaymentBatchTask")
@Slf4j
@CompileStatic
class AddToPaymentBatchTask implements JavaDelegate {

  @Autowired
  RuntimeService runtimeService

  @Override
  void execute(DelegateExecution execution) throws Exception {
    log.debug("Entering addToPaymentBatchTask")

    println "Hello world : addToPaymentBatchTask"

    String earlyPaymentRequestId
    if (execution.hasVariable(EARLY_PAYMENT_REQUEST_ID))
      earlyPaymentRequestId = execution.getVariable(EARLY_PAYMENT_REQUEST_ID)
    else {
      throw new AddToPaymentBatchException("Unable to find the EPR to add to the batch in this execution")
    }

    println "Adding EPR:[$earlyPaymentRequestId] to batch"
    VariableInstance variableInstance = runtimeService.createVariableInstanceQuery()
      .variableName(PAYMENT_BATCH)
      .singleResult()

    List<String> paymentBatch
    if(variableInstance) {
      paymentBatch = variableInstance.value as List<String>
    } else {
      paymentBatch = new ArrayList<String>()
    }
    paymentBatch.add(earlyPaymentRequestId)
    runtimeService.setVariable(execution.id,PAYMENT_BATCH,paymentBatch)
  }
}
