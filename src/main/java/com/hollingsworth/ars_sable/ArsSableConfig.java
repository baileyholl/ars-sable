package com.hollingsworth.ars_sable;


import net.neoforged.neoforge.common.ModConfigSpec;

public class ArsSableConfig {

    public static ModConfigSpec SERVER_CONFIG;


    static {
        ModConfigSpec.Builder SERVER_BUILDER = new ModConfigSpec.Builder();
        SERVER_CONFIG = SERVER_BUILDER.build();
    }
    
}
