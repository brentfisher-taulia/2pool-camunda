/*
 * Copyright (c) 2016. Taulia Inc. All rights reserved.
 *
 * All content of this file, its package and related information is to be treated
 * as confidential, proprietary information.
 *
 * This notice does not imply restricted, unrestricted or public access to these materials
 * which are a trade secret of Taulia Inc ('Taulia') and which may not be reproduced, used,
 * sold or transferred to any third party without Taulia's prior written consent.
 *
 * Any rights not expressly granted herein are reserved by Taulia.
 */

package com.taulia.ppm.config

import org.camunda.bpm.engine.HistoryService
import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.RepositoryService
import org.camunda.bpm.engine.RuntimeService
import org.camunda.bpm.engine.TaskService
import org.camunda.bpm.engine.spring.ProcessEngineFactoryBean
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration
import org.camunda.bpm.engine.test.ProcessEngineRule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import org.springframework.jdbc.datasource.SimpleDriverDataSource
import org.springframework.transaction.PlatformTransactionManager

import javax.sql.DataSource

@Configuration
@ComponentScan(
  basePackages = ['com.taulia.ppm']
)
class TestPaymentProcessEngineConfiguration  {

  @Bean
  public DataSource dataSource() {
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
    dataSource.setDriverClass(org.h2.Driver.class);
    dataSource.setUrl("jdbc:h2:mem:camunda-test;DB_CLOSE_DELAY=-1");
    dataSource.setUsername("sa");
    dataSource.setPassword("");
    return dataSource;
  }

  @Bean
  public PlatformTransactionManager transactionManager() {
    return new DataSourceTransactionManager(dataSource());
  }


  @Bean
  SpringProcessEngineConfiguration processEngineConfiguration() {

    SpringProcessEngineConfiguration springProcessEngineConfiguration = new SpringProcessEngineConfiguration()
    springProcessEngineConfiguration.setDataSource(dataSource())
    springProcessEngineConfiguration.setTransactionManager(transactionManager())
    springProcessEngineConfiguration.setDatabaseSchemaUpdate('false')
    springProcessEngineConfiguration.setJobExecutorActivate(false)
    springProcessEngineConfiguration.setDatabaseSchemaUpdate('create-drop')
    springProcessEngineConfiguration
  }


  @Bean
  ProcessEngineFactoryBean processEngine() {
    ProcessEngineFactoryBean processEngine = new ProcessEngineFactoryBean()
    processEngine.setProcessEngineConfiguration(processEngineConfiguration())
    processEngine
  }


  @Bean
  RepositoryService repositoryService() {
    processEngine().getObject().getRepositoryService()
  }


  @Bean
  TaskService taskService() {
    processEngine().getObject().getTaskService()
  }

  @Bean
  RuntimeService runtimeService() {
    processEngine().getObject().getRuntimeService()
  }

  @Bean
  HistoryService historyService() {
    processEngine().getObject().getHistoryService()
  }

  @Bean
  ManagementService managementService() {
    processEngine().getObject().getManagementService()
  }

  @Bean
  ProcessEngineRule processEngineRule() {
    ProcessEngineRule processEngineRule = new ProcessEngineRule()
    processEngineRule.setProcessEngine(processEngine().getObject())
    processEngineRule
  }

}
