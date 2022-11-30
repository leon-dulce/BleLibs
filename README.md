# Bluetooth sample tools

> Build from 2022/11/30

##### Sample bluetooth connection tools

### Using example

#### 1. initialization
```java
BleManager.getInstance().initialization(this);

```
#### 2. Request permission
```java
BleManager.getInstance().requestPermission(this);

```
#### 3. Scan & Connection  device
```java
BleManager.getInstance().setBluetoothConnectListener(connectListener); // setConnectListener
BleManager.getInstance().scanDevice(new BleManager.OnScanListener(){
    @Override
    public void onScan(BluetoothDevice bluetoothDevice) {
		if(bluetoothDevice.getName().equals("your ble name")){
			BleManager.getInstance().stopScan();
			BleManager.getInstance().connectBluetooth(
				new BluetoothModel.Builder()
					.setDevice(bluetoothDevice)
					.addServiceModel(new ServiceModel.Builder()
						.setUUID(SERVER)
						.addCharacteristic(new CharacteristicModel.Builder()
							.setCharacteristicUUID(NOTIFY)
							.addDescriptor(new DescriptorModel(DESCRIPTOR,
								DescriptorModel.NOTIFICATION))
							.build())
                        .addCharacteristic(new CharacteristicModel.Builder()
                            .setCharacteristicUUID(WRITE)
                            .build())
                        .addCharacteristic(new CharacteristicModel.Builder()
                            .setCharacteristicUUID(READ)
                            .build())
                    .build())
                .build()
			);
		}
	}

	@Override
	public void onScanTimeout() {
 
	}
});

```

#### 4. When you connected device
```java
boolean write = BleManager.getInstance().writeMessage(blueDevice,WRITE_UUID,"3345678"); //write meassage to your characteristic

boolean read = BleManager.getInstance().readMessage(blueDevice,READ_UUID);
//read callback from BluetoothConnectListener

```


#### 5. You can set debug mod to watch connect meassage on logcat
```java
BleManager.getInstance().setDebugMode(true);

```