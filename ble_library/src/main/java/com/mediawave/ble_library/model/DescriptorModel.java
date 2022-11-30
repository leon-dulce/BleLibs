package com.mediawave.ble_library.model;

import org.jetbrains.annotations.NotNull;

public class DescriptorModel {
    private String DESCRIPTOR;
    public static final int NOTIFICATION = 0x01;
    public static final int INDICATION = 0x02;
    private int type;

    public DescriptorModel(@NotNull String DESCRIPTOR, int type) {
        this.DESCRIPTOR = DESCRIPTOR;
        this.type = type;
    }

    public String getDESCRIPTOR() {
        return DESCRIPTOR;
    }

    public int getType() {
        return type;
    }

}
