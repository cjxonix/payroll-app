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
package org.apache.fineract.cn.provisioner;

import com.opentable.db.postgres.embedded.EmbeddedPostgres;
import org.junit.rules.ExternalResource;

import java.io.IOException;

/**
 * @author Myrle Krantz
 */
public class ProvisionerPostgreSQLInitializer extends ExternalResource {
  private static EmbeddedPostgres EMBEDDED_POSTGRESQL_DB;
  @Override
  protected void before() throws IOException {
    try {
      EMBEDDED_POSTGRESQL_DB = EmbeddedPostgres.builder().setPort(5432).start();
    }
    catch (IOException ioex) {
      System.out.println(ioex);
    }
  }

  @Override
  protected void after() {
    try {
      EMBEDDED_POSTGRESQL_DB.close();
    } catch (IOException io) {
      System.out.println(io);
    }
  }
}
