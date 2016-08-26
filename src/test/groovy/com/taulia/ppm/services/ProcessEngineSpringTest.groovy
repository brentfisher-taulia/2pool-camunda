package com.taulia.ppm.services

import com.taulia.ppm.config.TestPaymentProcessEngineConfiguration
import com.taulia.ppm.util.ProcessDiagramService
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.history.HistoricVariableInstance
import org.camunda.bpm.engine.management.JobDefinition
import org.camunda.bpm.engine.runtime.Execution
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.engine.runtime.VariableInstance
import org.camunda.bpm.engine.runtime.VariableInstanceQuery
import org.camunda.bpm.engine.task.Task
import org.camunda.bpm.engine.test.Deployment
import org.camunda.bpm.engine.test.ProcessEngineRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import static com.taulia.ppm.tasks.ProcessVariables.*
import static com.taulia.ppm.tasks.ProcessDefinitions.*

import static org.junit.Assert.assertEquals

@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = [
  TestPaymentProcessEngineConfiguration,
])
class ProcessEngineSpringTest {

  @Autowired
  HistoryService historyService

  @Autowired
  RuntimeService runtimeService

  @Autowired
  TaskService taskService

  @Autowired
  @Rule
  public ProcessEngineRule processEngineRule

  @Autowired
  ManagementService managementService

  @Autowired
  ProcessDiagramService processDiagramService

  @Test
  @Deployment(resources = ['payment-batch.bpmn'])
  public void testBatches() {
    String batchId = "batch-11"
    println "Starting ${BATCH_PROCESS} with key:[${batchId}]"
    ProcessInstance batchProcess = runtimeService.startProcessInstanceByKey(BATCH_PROCESS,
      batchId,
      [
        (PAYMENT_BATCH_ID): batchId
      ]
    )
    assert batchProcess
    Execution receiveEPRTask = runtimeService.createExecutionQuery()
      .activityId(RECEIVE_EPR_TASK)
      .singleResult()


    assert receiveEPRTask


    int numEprs = 10
    (1..numEprs).each { index ->
      String earlyPaymentRequestId = "EPR-$index"

      println "Starting EPR Process with key:[$earlyPaymentRequestId]"
      ProcessInstance eprProcess = runtimeService.startProcessInstanceByKey(EPR_PROCESS,
        earlyPaymentRequestId,
        [
          (EARLY_PAYMENT_REQUEST_ID): earlyPaymentRequestId
        ]
      )
      println "EPR Process Instance ID:[${eprProcess.id}]"


      assert eprProcess
    }

    boolean batchProcessingIsFinished = false
    while (!batchProcessingIsFinished) {
      List<JobDefinition> jobDefinitions =
        managementService.createJobQuery()
          .list()
      println "Found [${jobDefinitions?.size()}] job definition(s) to execute"

      jobDefinitions.each {
        if (!BATCH_PROCESS.equals(it.processDefinitionKey)) {
          println "Executing Job:[${it.getProcessDefinitionId()}]"

          managementService.executeJob(it.id)
        } else {
          println "Skipped job [$it]"
        }
      }


      println "Finished executing [${jobDefinitions?.size()}] job definition(s)."

      ProcessInstance pi = runtimeService.createProcessInstanceQuery()
        .processInstanceBusinessKey(batchId)
        .singleResult()

      batchProcessingIsFinished = null != pi

      println "Waiting on the batch processing to finish"
      Thread.sleep(1000)
    }

    println "Batch Process ID:[$batchProcess.id]"
    List<VariableInstance> variableInstances =
      runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(batchProcess.id)
        .list()

    println "Batch Process Instance ID:[${batchProcess.id}]"

    variableInstances.each { variableInstance ->
      println "Variable:[${variableInstance?.name}:${variableInstance?.value}]"
      println "Variable:{${variableInstance}]"
    }
    int numBatches = 1
    int numBatchIds = 1
    int numEPRsAtParentScope = 1
    assert numEPRsAtParentScope + numBatches + numBatchIds == variableInstances.size()

    HistoricVariableInstance batchProcessVariable =
      historyService.createHistoricVariableInstanceQuery()
        .processInstanceId(batchProcess.id)
        .variableName(PAYMENT_BATCH)
        .singleResult()

    assert batchProcessVariable
    assert numEprs == batchProcessVariable.value.size()

    println "Jobs left to run"
    managementService.createJobQuery()
      .list()
      .each {
      println it
      processDiagramService.writeDiagramToFile(it.processInstanceId,"job:${it.processDefinitionId}.png")
    }
  }

}