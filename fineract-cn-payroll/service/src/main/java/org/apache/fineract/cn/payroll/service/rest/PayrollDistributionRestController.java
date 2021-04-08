/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.cn.payroll.service.rest;

import org.apache.fineract.cn.payroll.api.v1.PermittableGroupIds;
import org.apache.fineract.cn.payroll.api.v1.domain.PayrollCollectionHistory;
import org.apache.fineract.cn.payroll.api.v1.domain.PayrollCollectionSheet;
import org.apache.fineract.cn.payroll.api.v1.domain.PayrollConfiguration;
import org.apache.fineract.cn.payroll.api.v1.domain.PayrollPaymentPage;
import org.apache.fineract.cn.payroll.service.ServiceConstants;
import org.apache.fineract.cn.payroll.service.internal.command.DistributePayrollCommand;
import org.apache.fineract.cn.payroll.service.internal.service.PayrollConfigurationService;
import org.apache.fineract.cn.payroll.service.internal.service.PayrollDistributionService;
import org.apache.fineract.cn.payroll.service.internal.service.adaptor.AccountingAdaptor;
import org.apache.fineract.cn.payroll.service.rest.util.PageableBuilder;
import java.util.List;
import javax.validation.Valid;
import org.apache.fineract.cn.accounting.api.v1.domain.Account;
import org.apache.fineract.cn.anubis.annotation.AcceptedTokenType;
import org.apache.fineract.cn.anubis.annotation.Permittable;
import org.apache.fineract.cn.anubis.annotation.Permittables;
import org.apache.fineract.cn.command.gateway.CommandGateway;
import org.apache.fineract.cn.lang.ServiceException;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/distribution")
public class PayrollDistributionRestController {

  private final Logger logger;
  private final CommandGateway commandGateway;
  private final PayrollDistributionService payrollDistributionService;
  private final PayrollConfigurationService payrollConfigurationService;
  private final AccountingAdaptor accountingAdaptor;

  @Autowired
  public PayrollDistributionRestController(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                           final CommandGateway commandGateway,
                                           final PayrollDistributionService payrollDistributionService,
                                           final PayrollConfigurationService payrollConfigurationService,
                                           final AccountingAdaptor accountingAdaptor) {
    super();
    this.logger = logger;
    this.commandGateway = commandGateway;
    this.payrollDistributionService = payrollDistributionService;
    this.payrollConfigurationService = payrollConfigurationService;
    this.accountingAdaptor = accountingAdaptor;
  }

  @Permittables({
      @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.DISTRIBUTION)
  })
  @RequestMapping(
      method = RequestMethod.POST,
      consumes = {
          MediaType.APPLICATION_JSON_VALUE
      },
      produces = {
          MediaType.APPLICATION_JSON_VALUE
      }
  )
  @ResponseBody
  public ResponseEntity<Void> distribute(@RequestBody @Valid final PayrollCollectionSheet payrollCollectionSheet) {

    this.verifyAccount(payrollCollectionSheet.getSourceAccountNumber());
    payrollCollectionSheet.getPayrollPayments()
        .forEach(payrollPayment -> {
          final PayrollConfiguration payrollConfiguration =
              this.payrollConfigurationService.findPayrollConfiguration(payrollPayment.getCustomerIdentifier())
                  .orElseThrow(() -> ServiceException.conflict("Payroll configuration for certain customers not available."));

          this.verifyAccount(payrollConfiguration.getMainAccountNumber());
          payrollConfiguration.getPayrollAllocations()
              .forEach(payrollAllocation -> this.verifyAccount(payrollAllocation.getAccountNumber()));
        });

    this.commandGateway.process(new DistributePayrollCommand(payrollCollectionSheet));

    return ResponseEntity.accepted().build();
  }

  @Permittables({
      @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.DISTRIBUTION)
  })
  @RequestMapping(
      method = RequestMethod.GET,
      consumes = {
          MediaType.ALL_VALUE
      },
      produces = {
          MediaType.APPLICATION_JSON_VALUE
      }
  )
  @ResponseBody
  public ResponseEntity<List<PayrollCollectionHistory>> fetchDistributionHistory() {
    return ResponseEntity.ok(this.payrollDistributionService.fetchHistory());
  }

  @Permittables({
      @Permittable(value = AcceptedTokenType.TENANT, groupId = PermittableGroupIds.DISTRIBUTION)
  })
  @RequestMapping(
      value = "/{identifier}/payments",
      method = RequestMethod.GET,
      consumes = {
          MediaType.ALL_VALUE
      },
      produces = {
          MediaType.APPLICATION_JSON_VALUE
      }
  )
  @ResponseBody
  ResponseEntity<PayrollPaymentPage> fetchPayments(
      @PathVariable("identifier") final String identifier,
      @RequestParam(value = "pageIndex", required = false) final Integer pageIndex,
      @RequestParam(value = "size", required = false) final Integer size,
      @RequestParam(value = "sortColumn", required = false) final String sortColumn,
      @RequestParam(value = "sortDirection", required = false) final String sortDirection) {
    this.payrollDistributionService.findDistribution(identifier)
        .orElseThrow(() -> ServiceException.notFound("Payroll distribution {0} not found."));

    return ResponseEntity.ok(this.payrollDistributionService
        .fetchPayments(identifier, PageableBuilder.create(pageIndex, size, sortColumn, sortDirection)));
  }

  private void verifyAccount(final String accountIdentifier) {
    final Account account = this.accountingAdaptor.findAccount(accountIdentifier).orElseThrow(
        () -> ServiceException.conflict("Account {0} not found.")
    );

    if (!account.getState().equals(Account.State.OPEN.name())) {
      throw ServiceException.conflict("Account {0} must be open.", accountIdentifier);
    }
  }
}
