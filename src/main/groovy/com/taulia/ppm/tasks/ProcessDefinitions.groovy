package com.taulia.ppm.tasks

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

class ProcessDefinitions {

  static final String BATCH_PROCESS = 'batchProcess'
  static final String EPR_PROCESS = 'EPRProcess'
  static final String RECEIVE_EPR_TASK = 'receiveEPRTask'
  static final String CONFIRM_EPR_ADDED_TASK = 'confirmEPRAddedTask'
  static final String ADD_TO_PAYMENT_BATCH_TASK = 'addToPaymentBatchTask'
  static final String CLOSE_BATCH_JOB_ID = "closeBatchTimerBoundaryEvent"
  static final String BATCH_EPR_TASK = "batchEprTask"

}