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



    int numEprs = 100
    double ratio = 0.20  // eprs to batch
    kickOffEPRProcesses(numEprs,ratio)




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

  private void checkVariablesPopulated(String batchProcessInstanceId, int numEprs) {

    println "Batch Process ID:[$batchProcessInstanceId]"
    List<VariableInstance> variableInstances =
      runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(batchProcessInstanceId)
        .list()


    variableInstances.each { variableInstance ->
      println "Variable:[${variableInstance?.name}:${variableInstance?.value}]"
      println "Variable:{${variableInstance}]"
    }

    VariableInstance batchProcessVariable =
      runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(batchProcessInstanceId)
        .variableName(PAYMENT_BATCH)
        .singleResult()

    assert batchProcessVariable
    assert numEprs*2 >= batchProcessVariable.value.size()
    assert 0 <= batchProcessVariable.value.size()

  }

  private void runBatchEprTaskJobs() {
    List<Job> sendEPRJobs =
      managementService.createJobQuery()
        .activityId("batchEprTask")
        .list()
    println "Found [${sendEPRJobs?.size()}] batch EPR task jobs to execute"

    Job closeBatchJob = managementService.createJobQuery()
      .activityId("closeBatchTimerBoundarEvent")
    .singleResult()

    assert closeBatchJob

    println "Kicking off another batch"
    printActivityInstanceTree(closeBatchJob.processInstanceId)
    int indexToExecuteCloseBatchJob = Math.random()* sendEPRJobs.size()
    sendEPRJobs.eachWithIndex { it,index->
      if(index== indexToExecuteCloseBatchJob){
        managementService.executeJob(closeBatchJob.id)
      }
      printActivityInstanceTree(it.processInstanceId)

      println "Executing Job:[${it.getProcessDefinitionId()}]"

      managementService.executeJob(it.id)
    }
    checkVariablesPopulated(closeBatchJob.processInstanceId, sendEPRJobs.size())



  }

  public void printActivityInstanceTree(String pid) {
    println("---------------- Activity Instance Tree ----------------");
    println(runtimeService.getActivityInstance(pid));
    println("----------------------------------------------------");
  }
  private List<Integer> kickOffEPRProcesses(int numEprs,double ratio) {

    int numberOfEprsPerBatchMax = numEprs*ratio
    println "Number of Eprs per batch max:[$numberOfEprsPerBatchMax]"
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
        runBatchEprTaskJobs()
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