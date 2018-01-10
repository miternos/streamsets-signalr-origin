package com.miternos.streamset.stage.util;

import microsoft.aspnet.signalr.client.*;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientResponseException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import microsoft.aspnet.signalr.client.hubs.HubProxy;

public class SignalRClient implements Runnable{

    private String deviceNetworkId;

    private String authToken ;

    private String hubUrl ;

    private String restApiUrl;

    private String odataUrl;

    private Long resourceRefreshPeriod ;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(SignalRClient.class);

    private LinkedBlockingQueue<Map> queue = new LinkedBlockingQueue<Map>();

    private List<String> resourceIdList = new ArrayList<>();

    private boolean shutdown = false;

    HubConnection connection;
    HubProxy hub;

    public SignalRClient(String deviceNetworkId, String authToken, String hubUrl, String restApiUrl, String odataUrl, Long resourceRefreshPeriod){
        this.deviceNetworkId=deviceNetworkId;
        this.authToken=authToken;
        this.hubUrl=hubUrl;
        this.restApiUrl = restApiUrl;
        this.odataUrl=odataUrl;
        this.resourceRefreshPeriod=resourceRefreshPeriod;
    }

    public void disconnect(){
        logger.info("Stopping connection to SignalR");
        shutdown = true ;
    }


    private Logger signalrLogger;

    @Override
    public void run() {


        long start = System.currentTimeMillis();

        // Create a new console signalrLogger
        signalrLogger = (message, level) -> {};


        String realTimeToken = getRealTimeToken();

        connection = new HubConnection(hubUrl, "", true, signalrLogger);

        hub = connection.createHubProxy("measurementHub");


        hub.on("NewMeasurement", p1 -> {
            logger.debug("Measurement = "+p1.toString());

            try {
                logger.debug("Put to queue "+p1.toString());
                queue.put((Map)p1);
            } catch (InterruptedException e) {
                logger.error(e.getMessage());
            }

        }, Object.class);


        // Subscribe to the connected event
        connection.connected(() -> logger.info("CONNECTED"));

        // Start the connection
        SignalRFuture<Void> awaitingConn =  connection.start();

        SignalRFuture<Void> doneConnect = awaitingConn.done(obj -> {
            hub.invoke("authenticate", deviceNetworkId, realTimeToken).done(hobj -> logger.info("Authenticated"));
            logger.info("Done Connecting!") ;

        });
        logger.info("Waiting for connection.");
        while ( connection.getState().equals(ConnectionState.Connecting) ){
            try {
                long millis = 5000;
                logger.debug("Wait connection for "+millis+" ms .");
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        try {
            doneConnect.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        //resourceIdList = readResourceIdsFromFile(resourceIdFile);

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        ResourceRegistrar registrar = new ResourceRegistrar(hub, odataUrl, authToken, deviceNetworkId);
        executorService.scheduleAtFixedRate(registrar,0,resourceRefreshPeriod,TimeUnit.SECONDS);

        logger.info("Wait for data");
        while ( !shutdown ){
            try {
                long millis = 5000;
                logger.debug("Wait data for "+millis+" ms .");
                Thread.sleep(millis);
                long seconds =  ( System.currentTimeMillis() - start ) / 1000;

            } catch (InterruptedException e) {
                e.printStackTrace();
                logger.info("Stopping");
                break;
            }
        }

        connection.disconnect();
        connection.stop();
        logger.info("Signalr connection stopped");

        try {
            logger.error("attempt to shutdown registrar executor");
            executorService.shutdown();
            executorService.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            logger.error("registrar tasks interrupted");
        } finally {
            if (!executorService.isTerminated()) {
                logger.error("cancel non-finished registrar tasks");
            }
            executorService.shutdownNow();
            logger.error("shutdown registrar finished");
        }

    }

    private String getRealTimeToken() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + authToken);
        headers.set("X-DeviceNetwork", deviceNetworkId);

        String requestJson = "";

        logger.info(requestJson);

        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Object> a = restTemplate
                    .exchange(restApiUrl + "/deviceNetwork/realtimeToken", HttpMethod.POST, entity, Object.class);

            String token = ((HashMap) ((ResponseEntity) a).getBody()).get("Token").toString();

            logger.debug("RT token is " + token);
            return token;
        } catch (RestClientResponseException e) {
            logger.error("Exception occured during real time token retrieval " + e.getMessage());
            throw e;
        }
    }

    /**
     * Read resource ids from file and return list
     */
    private List<String> readResourceIdsFromFile(String sourceFile) {
        List resourceIdList = new ArrayList();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(sourceFile));
            String line = reader.readLine();
            while (!StringUtils.isEmpty(line)) {
                resourceIdList.add(line);
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return resourceIdList;
    }

    public Map take() throws InterruptedException {
        return queue.take();
    }
}
