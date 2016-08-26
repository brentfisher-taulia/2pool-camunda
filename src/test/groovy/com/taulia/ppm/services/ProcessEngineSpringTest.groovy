package com.taulia.ppm.services

import com.taulia.ppm.config.MySqlPaymentProcessEngineConfiguration
import com.taulia.ppm.util.ProcessDiagramService
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.history.HistoricVariableInstance
import org.camunda.bpm.engine.runtime.Execution
import org.camunda.bpm.engine.runtime.Job
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.engine.runtime.VariableInstance
import org.camunda.bpm.engine.test.ProcessEngineRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

import static com.taulia.ppm.tasks.ProcessVariables.*
import static com.taulia.ppm.tasks.ProcessDefinitions.*

@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = [
  MySqlPaymentProcessEngineConfiguration,
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

  @Before
  public void stopProcesses() {
    runtimeService.createProcessInstanceQuery().list().each { pi ->
      runtimeService.deleteProcessInstance(pi.processInstanceId, "Starting new test run!")
    }
  }

  @Test
//  @Deployment(resources = ['payment-batch.bpmn'])
  public void testBatches() {
    ProcessInstance batchProcess = kickOffBatchProcess()


    int numEprs = 10
    double ratio = 0.50  // eprs to batch
    kickOffEPRProcesses(numEprs,ratio)


    checkVariablesPopulated(batchProcess, numEprs)


    checkBackgroundJobsLeftToRun()
  }

  private void checkBackgroundJobsLeftToRun() {
    println "Jobs left to run"
    managementService.createJobQuery()
      .list()
      .each {
      println it
      processDiagramService.writeDiagramToFile(it.processInstanceId, "job:${it.processDefinitionId}.png")
    }
  }

  private void checkVariablesPopulated(ProcessInstance batchProcess, int numEprs) {
    println "Batch Process ID:[$batchProcess.id]"
    List<VariableInstance> variableInstances =
      runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(batchProcess.id)
        .list()


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

  }

  private void runBackgroundJobs() {
    List<Job> jobDefinitions =
      managementService.createJobQuery()
        .list()
    println "Found [${jobDefinitions?.size()}] job definition(s) to execute"

    List<Job> sendEPRJobs = jobDefinitions.findAll { !BATCH_PROCESS.equals(it.processDefinitionKey)}
    Job closeBatchJob = jobDefinitions.find { BATCH_PROCESS.equals(it.processDefinitionKey)} as Job
    assert closeBatchJob

    sendEPRJobs.eachWithIndex { jd,index ->
      println "Executing Job:[${jd.getProcessDefinitionId()}]"

      managementService.executeJob(jd.id)
    }
    println "Kicking off another branch"
    managementService.executeJob(closeBatchJob.id)
  }


  private List<Integer> kickOffEPRProcesses(int numEprs,double ratio) {
    int numberOfEprsPerBatchMax = numEprs*ratio
    int countOfEprsPerBatch = 0
    (1..numEprs).each { index ->
      String earlyPaymentRequestId = "EPR-$index"

      println "Starting EPR Process with key:[$earlyPaymentRequestId]"
      ProcessInstance eprProcess = runtimeService.startProcessInstanceByKey(EPR_PROCESS,
        earlyPaymentRequestId,
        [
          (EARLY_PAYMENT_REQUEST_ID): earlyPaymentRequestId
        ]
      )
      countOfEprsPerBatch ++
      println "EPR Process Instance ID:[${eprProcess.id}]"
      if(countOfEprsPerBatch>=numberOfEprsPerBatchMax)
      {
        countOfEprsPerBatch = 0
        runBackgroundJobs()
      }

      assert eprProcess

    }
  }

  private ProcessInstance kickOffBatchProcess() {
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
      .processInstanceId(batchProcess.processInstanceId)
      .activityId(RECEIVE_EPR_TASK)
      .singleResult()


    assert receiveEPRTask
    batchProcess
  }

}