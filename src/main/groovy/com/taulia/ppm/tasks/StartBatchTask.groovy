package com.taulia.ppm.tasks

import com.taulia.ppm.util.ProcessDiagramService
import groovy.util.logging.Slf4j
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity
import org.camunda.bpm.engine.runtime.Execution
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

import static com.taulia.ppm.tasks.ProcessDefinitions.BATCH_PROCESS
import static com.taulia.ppm.tasks.ProcessDefinitions.BATCH_PROCESS
import static com.taulia.ppm.tasks.ProcessDefinitions.RECEIVE_EPR_TASK
import static com.taulia.ppm.tasks.ProcessVariables.EARLY_PAYMENT_REQUEST_ID
import static com.taulia.ppm.tasks.ProcessVariables.PAYMENT_BATCH_ID

@Component(value = "startBatchTask")
@Slf4j
class StartBatchTask implements JavaDelegate {

  @Autowired
  RuntimeService runtimeService

  @Autowired
  ProcessDiagramService processDiagramService

  @Override
  void execute(DelegateExecution execution) throws Exception {
    String batchId = "${new Date()}"
    println "Starting ${BATCH_PROCESS} with key:[${batchId}]"
    ProcessInstance batchProcess = runtimeService.startProcessInstanceByKey(BATCH_PROCESS,
      batchId,
      [
        (PAYMENT_BATCH_ID): batchId
      ]
    )

    Execution receiveEPRTask = runtimeService.createExecutionQuery()
      .processInstanceId(batchProcess.processInstanceId)
      .activityId(RECEIVE_EPR_TASK)
      .singleResult()

//    if(!receiveEPRTask){
//      throw new StartBatchTaskException("Unable to start new Payment Batch Process")
//    }
  }

}
