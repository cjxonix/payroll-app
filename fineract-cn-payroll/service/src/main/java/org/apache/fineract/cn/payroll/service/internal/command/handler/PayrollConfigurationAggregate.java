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
package org.apache.fineract.cn.payroll.service.internal.command.handler;

import org.apache.fineract.cn.payroll.api.v1.EventConstants;
import org.apache.fineract.cn.payroll.api.v1.domain.PayrollConfiguration;
import org.apache.fineract.cn.payroll.service.ServiceConstants;
import org.apache.fineract.cn.payroll.service.internal.command.PutPayrollConfigurationCommand;
import org.apache.fineract.cn.payroll.service.internal.mapper.PayrollAllocationMapper;
import org.apache.fineract.cn.payroll.service.internal.repository.PayrollAllocationEntity;
import org.apache.fineract.cn.payroll.service.internal.repository.PayrollAllocationRepository;
import org.apache.fineract.cn.payroll.service.internal.repository.PayrollConfigurationEntity;
import org.apache.fineract.cn.payroll.service.internal.repository.PayrollConfigurationRepository;
import org.apache.fineract.cn.payroll.service.internal.service.PayrollConfigurationService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

@Aggregate
public class PayrollConfigurationAggregate {

  private Logger logger;
  private PayrollConfigurationService payrollConfigurationService;
  private PayrollConfigurationRepository payrollConfigurationRepository;
  private PayrollAllocationRepository payrollAllocationRepository;

  @Autowired
  public PayrollConfigurationAggregate(@Qualifier(ServiceConstants.LOGGER_NAME) final Logger logger,
                                       final PayrollConfigurationService payrollConfigurationService,
                                       final PayrollConfigurationRepository payrollConfigurationRepository,
                                       final PayrollAllocationRepository payrollAllocationRepository) {
    super();
    this.logger = logger;
    this.payrollConfigurationService = payrollConfigurationService;
    this.payrollConfigurationRepository = payrollConfigurationRepository;
    this.payrollAllocationRepository = payrollAllocationRepository;
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.SELECTOR_NAME, selectorValue = EventConstants.PUT_CONFIGURATION)
  public String process(final PutPayrollConfigurationCommand putPayrollConfigurationCommand) {
    final String customerIdentifier = putPayrollConfigurationCommand.customerIdentifier();
    final PayrollConfiguration payrollConfiguration = putPayrollConfigurationCommand.payrollConfiguration();

    final PayrollConfigurationEntity payrollConfigurationEntity;

    final Optional<PayrollConfigurationEntity> optionalPayrollConfiguration =
        this.payrollConfigurationRepository.findByCustomerIdentifier(customerIdentifier);
    if (optionalPayrollConfiguration.isPresent()) {
      payrollConfigurationEntity = optionalPayrollConfiguration.get();
      this.payrollAllocationRepository.deleteByPayrollConfiguration(payrollConfigurationEntity);
      this.payrollAllocationRepository.flush();

      payrollConfigurationEntity.setLastModifiedBy(UserContextHolder.checkedGetUser());
      payrollConfigurationEntity.setLastModifiedOn(LocalDateTime.now(Clock.systemUTC()));
    } else {
      payrollConfigurationEntity = new PayrollConfigurationEntity();
      payrollConfigurationEntity.setCustomerIdentifier(customerIdentifier);
      payrollConfigurationEntity.setCreatedBy(UserContextHolder.checkedGetUser());
      payrollConfigurationEntity.setCreatedOn(LocalDateTime.now(Clock.systemUTC()));
    }

    payrollConfigurationEntity.setMainAccountNumber(payrollConfiguration.getMainAccountNumber());
    final PayrollConfigurationEntity savedPayrollConfigurationEntity =
        this.payrollConfigurationRepository.save(payrollConfigurationEntity);

    if (payrollConfiguration.getPayrollAllocations() != null) {
      payrollConfiguration.getPayrollAllocations()
          .forEach(payrollAllocation -> {
            final PayrollAllocationEntity payrollAllocationEntity = PayrollAllocationMapper.map(payrollAllocation);
            payrollAllocationEntity.setPayrollConfiguration(savedPayrollConfigurationEntity);
            this.payrollAllocationRepository.save(payrollAllocationEntity);
          });
    }
    return customerIdentifier;
  }
}
