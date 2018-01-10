package com.miternos.streamset.stage.util;

import microsoft.aspnet.signalr.client.hubs.HubProxy;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ResourceRegistrar implements Runnable {

    private String odataUrl;
    private HubProxy hub;
    private String authToken;
    private String deviceNetworkId;

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ResourceRegistrar.class);

    public ResourceRegistrar(HubProxy hub,String odataUrl, String authToken, String deviceNetworkId){
        this.hub = hub;
        this.odataUrl = odataUrl;
        this.authToken=authToken;
        this.deviceNetworkId=deviceNetworkId;
    }

    @Override
    public void run() {

        List<String> resourceIdList = readResourceIdsFromOdata(odataUrl);

        //ToDo implement removal and keeping the list intact
        for (String r: resourceIdList){
            hub.invoke( "addResource", r).done(hobj -> logger.debug("Added resource "+hobj.toString()));
        }

    }

    private List<String> readResourceIdsFromOdata(String oDataUrl) {

        List<String> result = new ArrayList<String>();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + authToken);
        headers.set("X-DeviceNetwork", deviceNetworkId);

        String requestJson = "";

        logger.debug(requestJson);

        HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);

        RestTemplate restTemplate = new RestTemplate();

        String fullOdataUrl = oDataUrl+"/"+deviceNetworkId+"/DimResource";

        logger.info(" Number of cached items in resourceCache = "+ResourceCache.getInstance().getResources().size());

        while(true){

            logger.info("Get resources from "+fullOdataUrl);

            ResponseEntity<Object> a = restTemplate.exchange(fullOdataUrl, HttpMethod.GET, entity, Object.class);

            Map respBodyMap =  (Map)a.getBody();

            List<Object> valueList = (List<Object>) respBodyMap.get("value");

            valueList.forEach(p->{
                if ( (Boolean)((Map)p).get("IsUnavailable") == false ){
                    String r = (String)((Map)p).get("ResourceId");
                    result.add(r);

                    // Add or replace the resource data in cache
                    ResourceCache.getInstance().getResources().put(r,(Map)p);

                }
            });

            if ( respBodyMap.containsKey("@odata.nextLink") ){
                fullOdataUrl = (String) respBodyMap.get("@odata.nextLink");
            } else {
                break;
            }
        }

        return result;
    }

}
