package com.taulia.ppm.util

import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.impl.bpmn.diagram.ProcessDiagramGenerator
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ProcessDiagramService {

  @Autowired
  RepositoryService repositoryService

  @Autowired
  RuntimeService runtimeService

  @Autowired
  HistoryService historyService


  InputStream generateDiagram(String processInstanceId) {
    def process = runtimeService.createProcessInstanceQuery()
      .processInstanceId(processInstanceId)
      .singleResult()

    if (!process) {
      process = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult()
      ProcessDiagramGenerator.generateDiagram(
        repositoryService.getProcessDefinition(process.processDefinitionId) as ProcessDefinitionEntity,
        "png",
        []
      )
    } else {
      ProcessDiagramGenerator.generateDiagram(
        repositoryService.getProcessDefinition(process.processDefinitionId) as ProcessDefinitionEntity,
        "png",
        runtimeService.getActiveActivityIds(processInstanceId)
      )

    }
  }

  void writeDiagramToFile(String processInstanceId, String filename) {
    try {
      def par = new File("build/diagrams")

      File file = new File(par, filename)
      if (!file.parentFile.exists()) {
        if (!file.parentFile.mkdirs()) {
          throw new RuntimeException("Couldn't create the directory for the file ${file.absolutePath}")
        }
      }
      file.bytes = generateDiagram(processInstanceId).bytes
    } catch (Exception e){
      println "Failed to generate diagram to file for processInstanceId [${processInstanceId}] due to ${e.getMessage()}"
      e.printStackTrace()
    }
  }

}
