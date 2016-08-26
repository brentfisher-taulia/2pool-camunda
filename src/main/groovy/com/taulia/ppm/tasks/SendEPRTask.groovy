package com.taulia.ppm.tasks

import com.taulia.ppm.util.ProcessDiagramService
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity
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

  @Autowired
  ProcessDiagramService processDiagramService

  @Override
  void execute(DelegateExecution execution) throws Exception {
    log.debug("Entering SendEPRTask")

    println "Entering Send EPRTask"

    String earlyPaymentRequestId
    if (execution.hasVariable(EARLY_PAYMENT_REQUEST_ID))
      earlyPaymentRequestId = execution.getVariable(EARLY_PAYMENT_REQUEST_ID)

    Execution receiveEPRTask = runtimeService.createExecutionQuery()
      .activityId(RECEIVE_EPR_TASK)
      .singleResult()

    if(receiveEPRTask) {
      runtimeService.signal(receiveEPRTask.id,
      [
        (EARLY_PAYMENT_REQUEST_ID):earlyPaymentRequestId
      ])
      println "EPR sent to batch processing"
    } else {
      String runningExecutions = ""
      runtimeService.createExecutionQuery().list().each {
        runningExecutions += prettyPrint(it) + "\n"
        processDiagramService.writeDiagramToFile(it.processInstanceId,"pi-${it.processInstanceId}.png")
      }

      throw new ReceiveEPRException("Unable to find $RECEIVE_EPR_TASK Task from the running " +
        "executions:\n[$runningExecutions]" )
    }
  }

  String prettyPrint(Execution execution) {
    if(execution instanceof ExecutionEntity) {
      ExecutionEntity ee = execution as ExecutionEntity
      if (ee.isProcessInstanceExecution()) {
        return "ProcessInstance[$ee.processDefinitionId:$ee.processInstanceId]";
      } else {
        return (ee.isConcurrent ? "Concurrent" : "") + (ee.isScope ? "Scope" : "") + "Execution[$ee.processDefinitionId:$ee.processInstanceId->$ee.activityId->${ee.getToStringIdentity()}]"
      }
    } else {
      execution.toString()
    }

  }
}
