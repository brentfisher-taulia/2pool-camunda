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

import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.jdbc.datasource.SimpleDriverDataSource

import javax.sql.DataSource

@Configuration
class MySqlPaymentProcessSeamConfiguration extends TestPaymentProcessSeamConfiguration{

  @Bean
  @Override
  public DataSource dataSource() {
    SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
    dataSource.setDriverClass(com.mysql.jdbc.Driver.class);
    dataSource.setUrl("jdbc:mysql://localhost:3306/taulia_payment_process_manager?useGmtMillisForDatetimes=true");
    dataSource.setUsername("root");
    dataSource.setPassword("");

    return dataSource;
  }


  @Bean
  @Override
  SpringProcessEngineConfiguration processEngineConfiguration() {

    SpringProcessEngineConfiguration springProcessEngineConfiguration = new SpringProcessEngineConfiguration()
    springProcessEngineConfiguration.setDataSource(dataSource())
    springProcessEngineConfiguration.setTransactionManager(transactionManager())
    springProcessEngineConfiguration.setDatabaseSchemaUpdate('false')
    springProcessEngineConfiguration.setJobExecutorActivate(false)
    Resource[] resources = new Resource[1]
    resources[0] = new ClassPathResource("payment-batch.bpmn")
    springProcessEngineConfiguration.setDeploymentResources(resources)
    springProcessEngineConfiguration
  }


}
