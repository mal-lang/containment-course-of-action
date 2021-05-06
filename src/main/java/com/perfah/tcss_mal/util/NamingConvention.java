package com.perfah.tcss_mal.util;

public class NamingConvention {
    public static String getDefenseName(long assetId, String defenseName){
        return assetId + "." + defenseName;
    }

}
