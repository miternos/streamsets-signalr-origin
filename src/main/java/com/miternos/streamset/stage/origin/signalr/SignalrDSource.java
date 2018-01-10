/**
 * Copyright 2015 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.miternos.streamset.stage.origin.signalr;

import com.streamsets.pipeline.api.ConfigDef;
import com.streamsets.pipeline.api.ConfigGroups;
import com.streamsets.pipeline.api.ExecutionMode;
import com.streamsets.pipeline.api.GenerateResourceBundle;
import com.streamsets.pipeline.api.StageDef;

@StageDef(
    version = 1,
    label = "SignalR",
    description = "SignalR Origin to get records from",
    icon = "signalr.png",
    execution = ExecutionMode.STANDALONE,
    recordsByRef = true,
    onlineHelpRefUrl = ""
)
@ConfigGroups(value = Groups.class)
@GenerateResourceBundle
public class SignalrDSource extends SignalrSource {

  @ConfigDef(
      required = true,
      type = ConfigDef.Type.STRING,
      defaultValue = "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
      label = "Device Network Id",
      displayPosition = 10,
      group = "SIGNALR"
  ) public String deviceNetworkId;

  @ConfigDef(
          required = true,
          type = ConfigDef.Type.STRING,
          defaultValue = "*********",
          label = "Authentication Token",
          displayPosition = 10,
          group = "SIGNALR"
  ) public String authToken;

  @ConfigDef(
          required = true,
          type = ConfigDef.Type.STRING,
          defaultValue = "https://some.signalr.url/signalr/hubs",
          label = "HUB Url",
          displayPosition = 10,
          group = "SIGNALR"
  ) public String hubUrl;

  @ConfigDef(
          required = true,
          type = ConfigDef.Type.STRING,
          defaultValue = "https://some.rest.api/",
          label = "Rest API Url",
          displayPosition = 10,
          group = "SIGNALR"
  ) public String restApiUrl;

  @ConfigDef(
          required = true,
          type = ConfigDef.Type.STRING,
          defaultValue = "http://some.odata.url/",
          label = "Odata Url",
          displayPosition = 10,
          group = "SIGNALR"
  ) public String odataUrl;

  @ConfigDef(
          required = true,
          type = ConfigDef.Type.STRING,
          defaultValue = "60",
          label = "Refresh period in secs to update resource data from odata",
          displayPosition = 10,
          group = "SIGNALR"
  ) public String resourceRefreshPeriod;


  @Override
  public String getDeviceNetworkId() {
    return deviceNetworkId;
  }

  @Override
  public String getAuthToken() {
    return authToken;
  }

  @Override
  public String getHubUrl() {
    return hubUrl;
  }

  @Override
  public String getRestApiUrl() {
    return restApiUrl;
  }

  @Override
  public String getOdataUrl() {
    return odataUrl;
  }

  @Override
  public String getResourceRefreshPeriod() {
    return resourceRefreshPeriod;
  }
}
