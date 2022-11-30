package com.project.ble_library.model;

import android.bluetooth.BluetoothGattCharacteristic;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class CharacteristicModel {
    private String uuid;
    private BluetoothGattCharacteristic gattCharacteristic;
    private ArrayList<DescriptorModel> descriptorList = new ArrayList<>();

    public CharacteristicModel(Builder builder) {
        this.uuid = builder.characteristic;
        this.descriptorList = builder.descriptorList;
    }

    public String getUuid() {
        return uuid;
    }

    public ArrayList<DescriptorModel> getDescriptorList() {
        return descriptorList;
    }

    public BluetoothGattCharacteristic getGattCharacteristic() {
        return gattCharacteristic;
    }

    public void setGattCharacteristic(BluetoothGattCharacteristic gattCharacteristic) {
        this.gattCharacteristic = gattCharacteristic;
    }

    public static class Builder{
        private String characteristic = "";
        private ArrayList<DescriptorModel> descriptorList = new ArrayList<>();

        public Builder setCharacteristicUUID(@NotNull String UUID){
            this.characteristic = UUID;
            return this;
        }

        public Builder addDescriptor(@NotNull DescriptorModel descriptorModel){
            this.descriptorList.add(descriptorModel);
            return this;
        }

        public CharacteristicModel build(){
            return new CharacteristicModel(this);
        }
    }
}
