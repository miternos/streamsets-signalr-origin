/**
 * Copyright 2015 StreamSets Inc.
 * <p>
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.miternos.streamset.stage.origin.signalr;

import com.miternos.streamset.stage.lib.Errors;
import com.miternos.streamset.stage.util.ResourceCache;
import com.miternos.streamset.stage.util.SignalRClient;
import com.streamsets.pipeline.api.BatchMaker;
import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Record;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.base.BaseSource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This source is an example and does not actually read from anywhere.
 * It does however, generate generate a simple record with one field.
 */
public abstract class SignalrSource extends BaseSource {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SignalrSource.class);

    private SignalRClient client;

    private static ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override protected List<ConfigIssue> init() {
        // Validate configuration values and open any required resources.
        List<ConfigIssue> issues = super.init();

        if (StringUtils.isEmpty(getDeviceNetworkId())) {
            issues.add(getContext().createConfigIssue(Groups.SIGNALR.name(), "deviceNetworkId", Errors.ERROR_00,
                                                      "DeviceNetworkId missing"));
        }
        if (StringUtils.isEmpty(getAuthToken())) {
            issues.add(getContext().createConfigIssue(Groups.SIGNALR.name(), "authToken", Errors.ERROR_00,
                                                      "AuthToken missing"));
        }
        if (StringUtils.isEmpty(getHubUrl())) {
            issues.add(
                    getContext().createConfigIssue(Groups.SIGNALR.name(), "hubUrl", Errors.ERROR_00, "HubUrl missing"));
        }
        if (StringUtils.isEmpty(getRestApiUrl())) {
            issues.add(getContext().createConfigIssue(Groups.SIGNALR.name(), "restApiUrl", Errors.ERROR_00,
                                                      "REST Api Url missing"));
        }
        if (StringUtils.isEmpty(getOdataUrl())) {
            issues.add(getContext().createConfigIssue(Groups.SIGNALR.name(), "odataUrl", Errors.ERROR_00,
                                                      "Odata Url missing"));
        }
        if (StringUtils.isEmpty(getResourceRefreshPeriod())) {
            issues.add(getContext().createConfigIssue(Groups.SIGNALR.name(), "resourceRefreshPeriod", Errors.ERROR_00,
                                                      "Refresh Period missing "));
        }

        if ( issues.size() == 0 ){

            try {
                long refreshPeriod = Long.valueOf(getResourceRefreshPeriod());

                if ( issues.size() == 0 ){


                    SignalRClient signalRClient = new SignalRClient(getDeviceNetworkId(), getAuthToken(), getHubUrl(),
                                                                    getRestApiUrl(), getOdataUrl(), refreshPeriod);
                    logger.info("Signalr client thread created");
                    client = signalRClient;

                    if ( executorService.isShutdown() )
                        executorService = Executors.newSingleThreadExecutor();

                    executorService.submit(signalRClient);
                    logger.info("Signalr client thread submitted");

                }

            } catch (NumberFormatException e){
                issues.add(getContext().createConfigIssue(Groups.SIGNALR.name(), "resourceRefreshPeriod", Errors.ERROR_00,
                                                          "Refresh Period not valid "));
            }

        }
        // If issues is not empty, the UI will inform the user of each configuration issue in the list.
        return issues;
    }

    /** {@inheritDoc} */
    @Override public void destroy() {
        // Clean up any open resources.

        if ( client != null ){
            client.disconnect();
            logger.info("Client disconnected");
        }

        try {
            logger.error("attempt to shutdown signalr executor");
            executorService.shutdown();
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("signalr tasks interrupted");
        } finally {
            if (!executorService.isTerminated()) {
                logger.error("cancel non-finished signalr tasks");
            }
            executorService.shutdownNow();
            logger.error("shutdown signalr finished");
        }

        super.destroy();
    }

    /** {@inheritDoc} */
    @Override public String produce(String lastSourceOffset, int maxBatchSize, BatchMaker batchMaker) throws
                                                                                                      StageException {

        // Offsets can vary depending on the data source. Here we use an integer as an example only.
        long nextSourceOffset = 0;
        if (lastSourceOffset != null) {
            nextSourceOffset = Long.parseLong(lastSourceOffset);
        }

        int numRecords = 0;

        if (client != null) {
            try {
                while (numRecords < maxBatchSize) {
                    Map item = client.take();                                 // Blocking
                    logger.debug("Add to batchMaker NumRecords:"+numRecords+" maxBatchSize:"+maxBatchSize+" Item: "+item);

                    Record r = getContext().createRecord(String.valueOf(numRecords));

                    Map<String, Field> fieldMap = new HashMap<>();

                    for ( Object e: item.keySet() ){
                        String key = (String)e;
                        Object val = item.get(key);
                        fieldMap.put(key,Field.create(String.valueOf(val)));
                    }

                    // Enrich data from cache -odata-
                    Map<String,Object> resourceInfo =  ResourceCache.getInstance().getResources().get(item.get("r"));
                    for ( String k: resourceInfo.keySet() ){
                        String v = String.valueOf(resourceInfo.get(k));
                        fieldMap.put(k,Field.create(v));
                    }

                    r.set(Field.create(fieldMap));
                    batchMaker.addRecord(r);
                    ++nextSourceOffset;
                    ++numRecords;
                }
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

        }

        logger.debug("Return from produce wit offset: "+nextSourceOffset);
        return String.valueOf(nextSourceOffset);
    }

    public abstract String getDeviceNetworkId();

    public abstract String getAuthToken();

    public abstract String getHubUrl();

    public abstract String getRestApiUrl();

    public abstract String getOdataUrl();

    public abstract String getResourceRefreshPeriod();
}
