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
package org.apache.fineract.cn.office.internal.command.handler;

import java.util.Date;
import java.util.Optional;
import org.apache.fineract.cn.api.util.UserContextHolder;
import org.apache.fineract.cn.command.annotation.Aggregate;
import org.apache.fineract.cn.command.annotation.CommandHandler;
import org.apache.fineract.cn.command.annotation.EventEmitter;
import org.apache.fineract.cn.lang.ServiceException;
import org.apache.fineract.cn.office.ServiceConstants;
import org.apache.fineract.cn.office.api.v1.EventConstants;
import org.apache.fineract.cn.office.api.v1.domain.ExternalReference;
import org.apache.fineract.cn.office.api.v1.domain.Office;
import org.apache.fineract.cn.office.internal.command.AddBranchCommand;
import org.apache.fineract.cn.office.internal.command.AddExternalReferenceCommand;
import org.apache.fineract.cn.office.internal.command.CreateOfficeCommand;
import org.apache.fineract.cn.office.internal.command.DeleteAddressOfOfficeCommand;
import org.apache.fineract.cn.office.internal.command.DeleteOfficeCommand;
import org.apache.fineract.cn.office.internal.command.SetAddressForOfficeCommand;
import org.apache.fineract.cn.office.internal.command.UpdateOfficeCommand;
import org.apache.fineract.cn.office.internal.mapper.AddressMapper;
import org.apache.fineract.cn.office.internal.mapper.OfficeMapper;
import org.apache.fineract.cn.office.internal.repository.AddressEntity;
import org.apache.fineract.cn.office.internal.repository.AddressRepository;
import org.apache.fineract.cn.office.internal.repository.ExternalReferenceEntity;
import org.apache.fineract.cn.office.internal.repository.ExternalReferenceRepository;
import org.apache.fineract.cn.office.internal.repository.OfficeEntity;
import org.apache.fineract.cn.office.internal.repository.OfficeRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.annotation.Transactional;

@SuppressWarnings({
    "unused"
})
@Aggregate
public class OfficeAggregate {

  private final Logger logger;
  private final OfficeRepository officeRepository;
  private final AddressRepository addressRepository;
  private final ExternalReferenceRepository externalReferenceRepository;

  @Autowired
  public OfficeAggregate(@Qualifier(ServiceConstants.SERVICE_LOGGER_NAME) final Logger logger,
                         final OfficeRepository officeRepository,
                         final AddressRepository addressRepository,
                         final ExternalReferenceRepository externalReferenceRepository) {
    super();
    this.logger = logger;
    this.officeRepository = officeRepository;
    this.addressRepository = addressRepository;
    this.externalReferenceRepository = externalReferenceRepository;
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_POST_OFFICE)
  public String createOffice(final CreateOfficeCommand createOfficeCommand) throws ServiceException {
    this.createOffice(createOfficeCommand.office(), null);
    return createOfficeCommand.office().getIdentifier();
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_PUT_OFFICE)
  public String updateOffice(final UpdateOfficeCommand updateOfficeCommand) throws ServiceException {
    final Office office = updateOfficeCommand.office();

    final Optional<OfficeEntity> optionalOfficeEntity = this.officeRepository.findByIdentifier(office.getIdentifier());

    if (optionalOfficeEntity.isPresent()) {
      final OfficeEntity officeEntity = optionalOfficeEntity.get();
      if (office.getName() != null) {
        officeEntity.setName(office.getName());
      }

      if (office.getDescription() != null) {
        officeEntity.setDescription(office.getDescription());
      }

      officeEntity.setCreatedBy(UserContextHolder.checkedGetUser());
      officeEntity.setCreatedOn(Utils.utcNow());

      this.officeRepository.save(officeEntity);

      if (office.getAddress() != null) {
        this.setAddress(new SetAddressForOfficeCommand(office.getIdentifier(), office.getAddress()));
      }
      return office.getIdentifier();
    } else {
      throw ServiceException.notFound("Office {0} not found.", office.getIdentifier());
    }
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_POST_OFFICE)
  public String addBranch(final AddBranchCommand addBranchCommand) {
    final Office parentOffice = new Office();
    parentOffice.setIdentifier(addBranchCommand.parentIdentifier());

    final Office branch = addBranchCommand.branch();

    this.createOffice(branch, parentOffice);

    return branch.getIdentifier();
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_DELETE_OFFICE)
  public String deleteOffice(final DeleteOfficeCommand deleteOfficeCommand) {
    final Optional<OfficeEntity> optionalOfficeEntity = this.officeRepository.findByIdentifier(deleteOfficeCommand.identifier());

    if (optionalOfficeEntity.isPresent()) {
      final OfficeEntity officeEntityToDelete = optionalOfficeEntity.get();
      final Optional<AddressEntity> optionalAddressEntity = this.addressRepository.findByOffice(officeEntityToDelete);
      optionalAddressEntity.ifPresent(this.addressRepository::delete);

      this.officeRepository.delete(officeEntityToDelete);

      this.externalReferenceRepository.deleteByOfficeIdentifier(deleteOfficeCommand.identifier());
    }

    return deleteOfficeCommand.identifier();
  }

  @SuppressWarnings("WeakerAccess")
  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_PUT_ADDRESS)
  public String setAddress(final SetAddressForOfficeCommand setAddressForOfficeCommand) {
    final Optional<OfficeEntity> optionalOfficeEntity = this.officeRepository.findByIdentifier(setAddressForOfficeCommand.identifier());

    if (optionalOfficeEntity.isPresent()) {
      final OfficeEntity officeEntity = optionalOfficeEntity.get();
      final Optional<AddressEntity> optionalAddressEntity = this.addressRepository.findByOffice(officeEntity);
      if (optionalAddressEntity.isPresent()) {
        this.addressRepository.delete(optionalAddressEntity.get());
      }

      final AddressEntity addressEntity = AddressMapper.map(setAddressForOfficeCommand.address());
      addressEntity.setOffice(officeEntity);
      this.addressRepository.save(addressEntity);

      officeEntity.setLastModifiedBy(UserContextHolder.checkedGetUser());
      officeEntity.setLastModifiedOn(Utils.utcNow());
      this.officeRepository.save(officeEntity);

      return setAddressForOfficeCommand.identifier();
    } else {
      throw ServiceException.notFound("Office {0} not found.", setAddressForOfficeCommand.identifier());
    }
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_DELETE_ADDRESS)
  public String deleteAddress(final DeleteAddressOfOfficeCommand deleteAddressOfOfficeCommand) {
    final Optional<OfficeEntity> optionalOfficeEntity = this.officeRepository.findByIdentifier(deleteAddressOfOfficeCommand.identifier());
    if (optionalOfficeEntity.isPresent()) {
      final OfficeEntity officeEntity = optionalOfficeEntity.get();
      if (officeEntity != null) {
        final Optional<AddressEntity> optionalAddressEntity = this.addressRepository.findByOffice(officeEntity);
        if (optionalAddressEntity.isPresent()) {
          this.addressRepository.delete(optionalAddressEntity.get());

          officeEntity.setLastModifiedBy(UserContextHolder.checkedGetUser());
          officeEntity.setLastModifiedOn(Utils.utcNow());
          this.officeRepository.save(officeEntity);
          return deleteAddressOfOfficeCommand.identifier();
        }
      } else {
        this.logger.info("Office {} not found.", deleteAddressOfOfficeCommand.identifier());
      }
    }
    return null;
  }

  @Transactional
  @CommandHandler
  @EventEmitter(selectorName = EventConstants.OPERATION_HEADER, selectorValue = EventConstants.OPERATION_PUT_REFERENCE)
  public String addExternalReference(final AddExternalReferenceCommand addExternalReferenceCommand) {

    final String officeIdentifier = addExternalReferenceCommand.officeIdentifier();
    final ExternalReference externalReference = addExternalReferenceCommand.externalReference();

    final Optional<ExternalReferenceEntity> optionalExternalReference =
        this.externalReferenceRepository.findByOfficeIdentifierAndType(officeIdentifier, externalReference.getType());

    final ExternalReferenceEntity externalReferenceEntity;
    if (optionalExternalReference.isPresent()) {
      externalReferenceEntity = optionalExternalReference.get();
    } else {
      externalReferenceEntity = new ExternalReferenceEntity();
      externalReferenceEntity.setOfficeIdentifier(officeIdentifier);
      externalReferenceEntity.setType(externalReference.getType());
    }
    externalReferenceEntity.setState(externalReference.getState());

    this.externalReferenceRepository.save(externalReferenceEntity);

    return officeIdentifier;
  }

  private void createOffice(final Office office, final Office parentOffice) {
    if (this.officeRepository.existsByIdentifier(office.getIdentifier())) {
      this.logger.info("Office {} already exists.", office.getIdentifier());
      throw ServiceException.conflict("Office {0} already exists.", office.getIdentifier());
    }

    final String modificationUser = UserContextHolder.checkedGetUser();
    final Date modificationDate = Utils.utcNow();

    final OfficeEntity officeEntity = OfficeMapper.map(office);
    if (parentOffice != null) {
      final Optional<OfficeEntity> optionalParentOfficeEntity = this.officeRepository.findByIdentifier(parentOffice.getIdentifier());
      if (optionalParentOfficeEntity.isPresent()) {
        final OfficeEntity parentOfficeEntity = optionalParentOfficeEntity.get();
        officeEntity.setParentOfficeId(parentOfficeEntity.getId());
        parentOfficeEntity.setLastModifiedBy(modificationUser);
        parentOfficeEntity.setLastModifiedOn(modificationDate);
        this.officeRepository.save(parentOfficeEntity);
      }
    }

    officeEntity.setCreatedBy(modificationUser);
    officeEntity.setCreatedOn(modificationDate);

    final OfficeEntity savedOfficeEntity = this.officeRepository.save(officeEntity);

    if (office.getAddress() != null) {
      final AddressEntity addressEntity = AddressMapper.map(office.getAddress());
      addressEntity.setOffice(savedOfficeEntity);
      this.addressRepository.save(addressEntity);
    }
  }
}
