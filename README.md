# Bluetooth sample tools

> Build from 2022/11/30

##### Sample bluetooth connection tools
[![](https://jitpack.io/v/leon-dulce/BleLibs.svg)](https://jitpack.io/#leon-dulce/BleLibs)

## Import
### gradle
#### 1. Add it in your root build.gradle at the end of repositories:

```java
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
#### 2. Add the dependency

```java
dependencies {
    implementation 'com.github.leon-dulce:BleLibs:alpha-v1.0.4'
}
```
### maven
```java
<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
</repositories>
```
```java
<dependency>
	    <groupId>com.github.leon-dulce</groupId>
	    <artifactId>BleLibs</artifactId>
	    <version>alpha-v1.0.4</version>
</dependency>
```

## Using example

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
BleManager.getInstance().setBluetoothConnectListener(connectListener);
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
            //todo timeout
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