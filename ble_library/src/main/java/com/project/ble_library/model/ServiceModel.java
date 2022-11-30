package com.project.ble_library.model;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ServiceModel {
    private String uuid;
    private HashMap<String, CharacteristicModel> characteristicModelHash;

    public ServiceModel(Builder builder) {
        this.uuid = builder.uuid;
        this.characteristicModelHash = builder.characteristicModelHash;
    }

    public String getUuid() {
        return uuid;
    }

    public HashMap<String, CharacteristicModel> getCharacteristicModelHash() {
        return characteristicModelHash;
    }

    public static class Builder{
        private String uuid;
        private HashMap<String, CharacteristicModel> characteristicModelHash = new HashMap<>();

        public Builder setUUID(@NotNull String uuid){
            this.uuid = uuid;
            return this;
        }

        public Builder addCharacteristic(@NotNull CharacteristicModel model){
            this.characteristicModelHash.put(model.getUuid(),model);
            return this;
        }

        public ServiceModel build(){
            return new ServiceModel(this);
        }
    }
}
