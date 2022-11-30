package com.mediawave.ble_library.model;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class BluetoothModel {
    private final BluetoothDevice device;
    private BluetoothGatt gatt;
    private HashMap<String, ServiceModel> serviceModels;

    public BluetoothModel(Builder builder) {
        this.device = builder.device;
        this.serviceModels = builder.serviceModels;

    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public HashMap<String, ServiceModel> getServiceModels() {
        return serviceModels;
    }

    public BluetoothGatt getGatt() {
        return gatt;
    }

    public void setGatt(BluetoothGatt gatt) {
        this.gatt = gatt;
    }

    public static class Builder{
        private BluetoothDevice device;
        private HashMap<String, ServiceModel> serviceModels = new HashMap<>();

        public Builder setDevice(@NotNull BluetoothDevice device){
            this.device = device;
            return this;
        }

        public Builder addServiceModel(@NotNull ServiceModel model){
            this.serviceModels.put(model.getUuid(),model);
            return this;
        }

        public BluetoothModel build(){
            return new BluetoothModel(this);
        }
    }
}
