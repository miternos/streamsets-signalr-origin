package com.miternos.streamset.stage.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ResourceCache {

    private static ResourceCache instance;

    private static Map<String,Map<String,Object>> resources;


    private ResourceCache(){
        resources = new ConcurrentHashMap<String,Map<String,Object>>();
    }

    public static synchronized ResourceCache getInstance(){
        if ( instance == null )
            instance = new ResourceCache();

        return instance;
    }

    public Map<String,Map<String,Object>> getResources(){
        return resources;
    }
}
