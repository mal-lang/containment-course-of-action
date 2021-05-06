package com.perfah.tcss_mal.util;

import org.jline.terminal.Terminal;

public class Sugar {
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_STRIKETHROUGH = "\u001B[2m";

    public static String formatInfinity(double val){
        if(val == Double.MAX_VALUE){
            return "Inf";
        }
        else 
            return val + "";
    }
}
