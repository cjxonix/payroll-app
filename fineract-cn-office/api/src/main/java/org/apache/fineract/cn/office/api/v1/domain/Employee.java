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
package org.apache.fineract.cn.office.api.v1.domain;

import java.util.List;
import javax.validation.Valid;
import org.apache.fineract.cn.lang.validation.constraints.ValidIdentifier;

@SuppressWarnings("unused")
public class Employee {

  @ValidIdentifier
  private String identifier;
  private String givenName;
  private String middleName;
  private String surname;
  private String assignedOffice;
  @Valid
  private List<ContactDetail> contactDetails;

  public Employee() {
    super();
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getMiddleName() {
    return middleName;
  }

  public void setMiddleName(String middleName) {
    this.middleName = middleName;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getAssignedOffice() {
    return assignedOffice;
  }

  public void setAssignedOffice(String assignedOffice) {
    this.assignedOffice = assignedOffice;
  }

  public List<ContactDetail> getContactDetails() {
    return contactDetails;
  }

  public void setContactDetails(List<ContactDetail> contactDetails) {
    this.contactDetails = contactDetails;
  }
}
