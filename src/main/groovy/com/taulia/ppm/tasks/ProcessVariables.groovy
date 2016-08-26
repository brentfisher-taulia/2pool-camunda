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

public enum ProcessVariables {

  VALIDATION_RETRY_COUNT("VALIDATION_RETRY_COUNT"),
  VALIDATION_ERROR_TASK_ID("VALIDATION_ERROR_TASK_ID"),
  PAYMENT_PROCESSOR("PAYMENT_PROCESSOR"),
  EARLY_PAYMENT_REQUEST_ID('earlyPaymentRequestId'),
  PAYMENT_BATCH_ID('paymentBatchId'),
  PAYMENT_BATCH('paymentBatch'),
  BANK_COUNTRY('bankCountry'),
  IS_BUYER_ACK_EXEMPT('isBuyerAckExempt'),
  INVOICE_STATUS('invoiceStatus'),



  final String id

  ProcessVariables(String id) {
    this.id = id
  }

  public String toString() {
    id
  }

}