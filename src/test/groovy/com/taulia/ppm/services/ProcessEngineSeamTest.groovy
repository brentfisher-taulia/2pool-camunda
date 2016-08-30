package com.taulia.ppm.services

import com.taulia.ppm.config.MySqlPaymentProcessSeamConfiguration
import com.taulia.ppm.tasks.ProcessDefinitions
import com.taulia.ppm.tasks.ProcessVariables
import com.taulia.ppm.util.ProcessDiagramService
import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.ProcessEngineException
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.runtime.Job
import org.camunda.bpm.engine.runtime.ProcessInstance
import org.camunda.bpm.engine.runtime.VariableInstance
import org.camunda.bpm.engine.test.ProcessEngineRule
import org.junit.Before
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.rules.SpringClassRule
import org.springframework.test.context.junit4.rules.SpringMethodRule

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

import static com.taulia.ppm.tasks.ProcessVariables.*
import static com.taulia.ppm.tasks.ProcessDefinitions.*
import static org.junit.Assert.assertEquals

@RunWith(Parameterized)
@ContextConfiguration(classes = [
  MySqlPaymentProcessSeamConfiguration,
])
class ProcessEngineSeamTest {

  private int eprCount
  private int batchSpread
  private int eprJobFrequency
  int retries
  int retrySpread


  @ClassRule
  public static final SpringClassRule SCR = new SpringClassRule();

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

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
  private int eprThreadCt

  @Before
  public void stopProcesses() {
    runtimeService.createProcessInstanceQuery().list().each { pi ->
      runtimeService.deleteProcessInstance(pi.processInstanceId, "Starting new test run!")
    }
  }

  @Parameterized.Parameters(name = "{index}: eprCount[{0}] batchSpread[{1}] jobFrequency[{2}] retries[{3}] retrySpread[{4}] eprThCt[{5}]")
  static Object[][] data() {
    [
      [10, 1000, 100, 1, 100,10],
      [10, 1000, 100, 10, 100,10],
      [100, 1000, 1000, 2, 100,10],
      [100, 1000, 500, 3, 100,10],
      [100, 1000, 100, 40, 100,10],
      [100, 1000, 200, 5, 100,10],
      [200, 1000, 100, 6, 100,10],
      [200, 500, 100, 70, 100,10],
      [200, 200, 50, 80, 100,10],
      [2000, 500, 40, 90, 100,10],
      [1000, 500, 40, 90, 100,10],
    ]

  }

  public ProcessEngineSeamTest(int eprCount, int batchSpread, int eprJobFrequency, int retries, int retrySpread, int eprThreadCt) {

    this.eprThreadCt = eprThreadCt
    this.eprJobFrequency = eprJobFrequency
    this.batchSpread = batchSpread
    this.eprCount = eprCount
    this.retries = retries
    this.retrySpread = retrySpread
  }

  @Test
  public void testBatches() {
    testBatchesWithParameters(eprCount, batchSpread, eprJobFrequency)
  }

  private void testBatchesWithParameters(int numEprs, int millisBetweenBatches, millisBetweenEPRBGJobs) {
    ProcessInstance batchProcess = kickOffBatchProcess()

    assert batchProcess

    def eprThreads = createEPRProcessThreads(numEprs)

    def batchThread = createBatchCloseThread(millisBetweenBatches)

    def eprPool = Executors.newFixedThreadPool(eprThreadCt)

    def backgroundSendEprToBatchExecutor = Executors.newFixedThreadPool(eprThreadCt)


    def eprFutures = []
    def batchExecutor = Executors.newSingleThreadExecutor()
    def batchFuture = batchExecutor.submit(batchThread)

    eprThreads.each {
      eprFutures << eprPool.submit(it)
    }
    println "All EPR threads submitted"

    Future<?> backgroundSendEprFuture = runBackgroundSendJobs(backgroundSendEprToBatchExecutor, millisBetweenEPRBGJobs)

    eprFutures.each {
      println "Waiting on [$it]"
      it.get()
    }
    println "All EPR threads finished"

    println "EPR POOL SHUTDOWN"

    eprsFinished = true
    println "EPRs Finished ${eprsFinished}"
    println "Waiting for batch"
    batchFuture.get()
    backgroundSendEprFuture.get()

    checkEndingStateIsCorrect(numEprs)
  }

  def incrementBackgroundJobsRunCount(int amount) {
    synchronized (_backgroundJobsRunCount) {
      _backgroundJobsRunCount += amount
    }
  }

  int getBackgroundJobsRunCount() {
    synchronized (_backgroundJobsRunCount) {
      _backgroundJobsRunCount
    }
  }

  boolean getEprJobsFinished() {
    synchronized (_eprJobsFinished) {
      _eprJobsFinished
    }
  }

  def setEprJobsFinished(boolean newStatus) {
    synchronized (_eprJobsFinished) {
      _eprJobsFinished = newStatus
    }
  }
  int _backgroundJobsRunCount = 0
  boolean _eprJobsFinished = false

  private Future<?> runBackgroundSendJobs(ExecutorService backgroundSendEprToBatchExecutor, millisBetweenEPRBGJobs) {

    def backgroundSendEprFuture = backgroundSendEprToBatchExecutor.submit(new Runnable() {

      @Override
      void run() {
        println "Running EPRs eprsFinished[${eprsFinished}]:eprJobsFinished[${eprJobsFinished}]"
        println "Start Condition:${!eprsFinished || !eprJobsFinished}"
        while (!eprsFinished || !eprJobsFinished || backgroundJobsRunCount < eprCount) {

          List<Job> sendEPRJobs =
            managementService.createJobQuery()
              .activityId(BATCH_EPR_TASK)
              .list()
          incrementBackgroundJobsRunCount(sendEPRJobs.size())
          eprJobsFinished = sendEPRJobs.size() <= 0
          println "Found [${sendEPRJobs.size()}] sendEPRJobs to run"
          runBatchEprTaskJobs(sendEPRJobs)
          Thread.sleep(millisBetweenEPRBGJobs)
          println "EPRs eprsFinished[${eprsFinished}]:eprJobsFinished[${eprJobsFinished}]"
          println "while Condition:${!eprsFinished || !eprJobsFinished}"
        }
        println "Finished Condition:${!eprsFinished || !eprJobsFinished}"

      }
    })
    backgroundSendEprFuture
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
    batchProcess
  }


  List<Runnable> createEPRProcessThreads(int numEprs) {
    (1..numEprs).collect {
      new Runnable() {
        def void run() {
          kickOffEPRProcess(it)
        }
      }
    }

  }

  boolean eprsFinished = false

  Thread createBatchCloseThread(int millisBetweenBatchClosures) {
    new Thread() {
      def void run() {
        println "batch close thread running"
        while (!eprsFinished || eprProcessesExist()) {
          println "---EPRs Finished ${eprsFinished}"

          println "Closing a batch"
          // I don't really want any synchronization between
          // epr threads and the batch close thread.
          Thread.sleep(millisBetweenBatchClosures)
          closeBatchJob()
        }
        println "All EPR processes have finished"
      }

      boolean eprProcessesExist() {
        int runningEprJobs = managementService.createJobQuery()
          .processDefinitionId(ProcessDefinitions.EPR_PROCESS)
          .count()
        println "Found [$runningEprJobs] that may still need to run"
        runningEprJobs > 0
      }
    }

  }


  private void checkEndingStateIsCorrect(int numEprs) {

    assertEquals("shouldn't be any background jobs on send epr", 0, managementService.createJobQuery()
      .activityId(ProcessDefinitions.ADD_TO_PAYMENT_BATCH_TASK)
      .count())
    assertEquals("There should always be a batch process running", 1, managementService.createJobQuery()
      .activityId(ProcessDefinitions.CLOSE_BATCH_JOB_ID)
      .count())
    assertEquals("There should be the same number of EPR processes running as started, just in the analyze state",
      numEprs, runtimeService.createProcessInstanceQuery()
      .processDefinitionKey(ProcessDefinitions.EPR_PROCESS)
      .count())



    assertEquals("There should only be 1 batch process instance that is listening", 1, runtimeService.createProcessInstanceQuery()
      .processDefinitionKey(ProcessDefinitions.BATCH_PROCESS)
      .activityIdIn(ProcessDefinitions.RECEIVE_EPR_TASK)
      .count())

    assertEquals("There should be a total of eprCount, spread between all the batches",
      numEprs, getEprsSpreadBetweenBatches())
  }

  int getEprsSpreadBetweenBatches() {
    def processInstances = runtimeService.createProcessInstanceQuery()
      .processDefinitionKey(ProcessDefinitions.BATCH_PROCESS)
      .list()

    def eprCount = 0;

    String[] batchPIs = processInstances.collect { it.processInstanceId }.toArray()



    def paymentBatchVariables = runtimeService.createVariableInstanceQuery()
      .processInstanceIdIn(batchPIs)
      .variableName(ProcessVariables.PAYMENT_BATCH)
      .list()

    paymentBatchVariables.each { var ->
      List<String> eprList = var.value as List<String>
      eprCount += eprList.size()
    }
    eprCount

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

    Thread.sleep(1000)
    VariableInstance batchProcessVariable =
      runtimeService.createVariableInstanceQuery()
        .processInstanceIdIn(batchProcessInstanceId)
        .variableName(PAYMENT_BATCH)
        .singleResult()

    assert batchProcessVariable
    assert numEprs * 2 >= batchProcessVariable.value.size()
    assert 0 <= batchProcessVariable.value.size()

  }

  private void closeBatchJob() {
    Job closeBatchJob = managementService.createJobQuery()
      .activityId(ProcessDefinitions.CLOSE_BATCH_JOB_ID)
      .singleResult()

    executeWithRetry({ managementService.executeJob(closeBatchJob.id) }, "Close Batch")

  }

  def executeWithRetry(Closure closure, String task) {
    int retryCount = 0
    def retryExceptions = []
    boolean taskSucceeded = false
    while (retryCount < retries && !taskSucceeded) {
      try {
        println "Starting executing task [$task]. Try [${retryCount + 1}] "
        closure.call()
        taskSucceeded = true
        println "Finished executing task [$task] after [${retryCount + 1}] tries"
      } catch (ProcessEngineException e) {
        println "Unable to execute task [$task].  Will retry ${retries - retryCount} more times in ${retrySpread}ms"
        retryExceptions << e
        if (++retryCount >= retries) {
          throw new TaskExecutionWithRetryException("Failed execute task [$task]  after [$retryCount] retries", retryExceptions)
        }
        Thread.sleep(retrySpread)
      }
    }
  }

  private void runBatchEprTaskJobs(List<Job> sendEPRJobs) {

    println "Found [${sendEPRJobs?.size()}] batch EPR task jobs to execute"

    sendEPRJobs.eachWithIndex { sendEprJob, index ->
      printActivityInstanceTree(sendEprJob.processInstanceId)

      println "Executing Job:[${sendEprJob.getProcessDefinitionId()}]"

      executeWithRetry({ managementService.executeJob(sendEprJob.id) }, "Send EPR Job pid[${sendEprJob?.processInstanceId}]")
    }
  }

  public void printActivityInstanceTree(String pid) {
    println("---------------- Activity Instance Tree ----------------");
    println(runtimeService.getActivityInstance(pid));
    println("----------------------------------------------------");
  }

  private List<Integer> kickOffEPRProcess(int index) {

    String earlyPaymentRequestId = "EPR-$index"

    println "Starting EPR Process with key:[$earlyPaymentRequestId]"
    ProcessInstance eprProcess = runtimeService.startProcessInstanceByKey(EPR_PROCESS,
      earlyPaymentRequestId,
      [
        (EARLY_PAYMENT_REQUEST_ID): earlyPaymentRequestId
      ]
    )

    assert eprProcess

  }


}