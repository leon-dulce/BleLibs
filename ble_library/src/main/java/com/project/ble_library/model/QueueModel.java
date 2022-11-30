package com.project.ble_library.model;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

public class QueueModel {
    private BluetoothGattCharacteristic characteristic;
    private BluetoothGattDescriptor descriptor;

    public QueueModel(BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor) {
        this.characteristic = characteristic;
        this.descriptor = descriptor;
    }

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public BluetoothGattDescriptor getDescriptor() {
        return descriptor;
    }
}
