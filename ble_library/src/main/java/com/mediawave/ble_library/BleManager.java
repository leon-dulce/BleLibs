package com.mediawave.ble_library;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.mediawave.ble_library.model.BluetoothModel;
import com.mediawave.ble_library.model.CharacteristicModel;
import com.mediawave.ble_library.model.DescriptorModel;
import com.mediawave.ble_library.model.QueueModel;
import com.mediawave.ble_library.model.ServiceModel;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

@SuppressLint("MissingPermission")
public class BleManager {
    private static BleManager instance = new BleManager();
    private final String TAG = getClass().getSimpleName();
    private boolean debugMode = false;
    private BleManager(){};
    public static BleManager getInstance(){
        return instance;
    }
    public final static int PERMISSION_CODE = 0;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice[] bluetoothDeviceList;
    private Context context;

    /**
     * 初始化
     */
    public void initialization(@NotNull Context context){
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
        this.context = context.getApplicationContext();
    }

    /************  權 限 ************/

    /**
     * 獲取權限
     * @param activity
     */
    public void requestPermission(Activity activity){
        String[] permissionList = new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
        };
        activity.requestPermissions(permissionList,PERMISSION_CODE);
    }

    /************  搜 尋 ************/
    private OnScanListener scanListener;
    private Handler scanTimer = new Handler();
    private boolean isScanning = false;
    public static int scanTimeout = 10000;
    private final Runnable timeout = ()->{
        if(null!=scanListener)scanListener.onScanTimeout();
        stopScan();
    };

    public void scanDevice(OnScanListener listener){
        if(null==bluetoothAdapter)throw new NullPointerException("BluetoothAdapter is null ! Try to initialization.");
        this.scanListener = listener;
        scanTimer.postDelayed(timeout,scanTimeout);
        isScanning = true;
        bluetoothAdapter.getBluetoothLeScanner().startScan(scanCallback);
    }

    public void scanDevice(OnScanListener listener, List<ScanFilter>filters){
        if(null==bluetoothAdapter)throw new NullPointerException("BluetoothAdapter is null ! Try to initialization");
        this.scanListener = listener;
        ScanSettings tmpSetting = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED | ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build();
        scanTimer.postDelayed(timeout,scanTimeout);
        isScanning = true;
        bluetoothAdapter.getBluetoothLeScanner().startScan(filters,tmpSetting,scanCallback);
    }

    public void stopScan(){
        isScanning = false;
        if(null!=scanTimer)scanTimer.removeCallbacksAndMessages(null);
        bluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(result.getDevice().getName()!=null){
                if(null!=scanListener)scanListener.onScan(result.getDevice());
            }
        }
    };

    public boolean isScanning(){
        return isScanning;
    }

    public interface OnScanListener{
        void onScan(BluetoothDevice device);
        void onScanTimeout();
    }

    /************  連 線 ************/
    private BluetoothModel targetDevice;
    private final HashMap<BluetoothDevice, BluetoothModel> deviceSet = new HashMap<>();
    private final LinkedBlockingQueue<QueueModel> notifyQueue = new LinkedBlockingQueue<>();
    private OnBluetoothConnectListener connectListener;
    private boolean isEnablingCharacteristic = false;
    private static final int MTU_SIZE = 247;
    //timer
    private Handler connectTimer = new Handler();
    public static int connectTimeout = 10000;
    private final Runnable connectTimeoutRunnble = ()->{
//        if(null!= connectListener)connectListener.onConnectFail();
        if(null!=targetDevice && null!=targetDevice.getDevice()){
            disconnect(targetDevice.getDevice());
        }
    };

    public void connectBluetooth(BluetoothModel model){
        if(bluetoothAdapter==null)throw new NullPointerException("BluetoothAdapter is null ! Try to initialization");
        if(context==null)throw new NullPointerException("Context is null ! Try to initialization");
        if(bluetoothAdapter.isEnabled())bluetoothAdapter.enable();
        this.targetDevice = model;
        targetDevice.setGatt(this.targetDevice.getDevice().connectGatt(context,false,gattCallback));
        connectTimer.postDelayed(connectTimeoutRunnble,connectTimeout);
    }

    public void disconnect(){
        for(BluetoothDevice device :deviceSet.keySet()){
            if(null==deviceSet.get(device).getGatt())continue;
            deviceSet.get(device).getGatt().disconnect();
            deviceSet.get(device).getGatt().close();
            deviceSet.get(device).setGatt(null);
            if(null!= connectListener) connectListener.onDisconnected(deviceSet.get(device));
        }
        deviceSet.clear();
    }

    public void disconnect(BluetoothDevice ble){
        if(null==ble)return;
        for(BluetoothDevice device :deviceSet.keySet()){
            if(device == ble){
                if(null==deviceSet.get(device).getGatt())continue;
                deviceSet.get(device).getGatt().disconnect();
                deviceSet.get(device).getGatt().close();
                deviceSet.get(device).setGatt(null);
                if(null!= connectListener) connectListener.onDisconnected(deviceSet.get(device));
                deviceSet.remove(device);
            }
        }

    }

    public void setBluetoothConnectListener(OnBluetoothConnectListener onBluetoothConnectListener){
        this.connectListener = onBluetoothConnectListener;
    }

    public boolean writeMessage(BluetoothDevice device,String uuid,String message){
        if(deviceSet.containsKey(device)){
            BluetoothGatt gatt = deviceSet.get(device).getGatt();
            HashMap<String, ServiceModel> serviceModels = deviceSet.get(device).getServiceModels();
            if(null!=gatt){
                for(String serviceUUID : serviceModels.keySet()){ //find service
                    HashMap<String, CharacteristicModel> characteristicModelHashMap = serviceModels.get(serviceUUID).getCharacteristicModelHash();
                    if(characteristicModelHashMap.containsKey(uuid)){ //find characteristic
                        BluetoothGattCharacteristic characteristic = characteristicModelHashMap.get(uuid).getGattCharacteristic();
                        if(null!=characteristic){
                            if(null!=message){
                                characteristic.setValue(message.getBytes(StandardCharsets.UTF_8));
                                gatt.writeCharacteristic(characteristic);
                                return true;
                            }else{
                                if(debugMode)Log.e(TAG,"message is null");
                            }
                        }else{
                            if(debugMode)Log.e(TAG,"characteristic is null");
                        }
                        break;
                    }
                }
                if(debugMode)Log.e(TAG,"Can't set characteristic");
            }else{
                if(debugMode)Log.e(TAG,"Gatt is null");
            }
        }
        return false;
    }

    public boolean readMessage(BluetoothDevice device,String uuid){
        if(deviceSet.containsKey(device)){
            BluetoothGatt gatt = deviceSet.get(device).getGatt();
            HashMap<String, ServiceModel> serviceModels = deviceSet.get(device).getServiceModels();
            if(null!=gatt){
                for(String serviceUUID : serviceModels.keySet()){ //find service
                    HashMap<String, CharacteristicModel> characteristicModelHashMap = serviceModels.get(serviceUUID).getCharacteristicModelHash();
                    if(characteristicModelHashMap.containsKey(uuid)){ //find characteristic
                        BluetoothGattCharacteristic characteristic = characteristicModelHashMap.get(uuid).getGattCharacteristic();
                        if(null!=characteristic){
                            gatt.readCharacteristic(characteristic);
                            return true;
                        }else{
                            if(debugMode)Log.e(TAG,"characteristic is null");
                        }
                        break;
                    }
                }
                if(debugMode)Log.e(TAG,"Can't set characteristic");
            }else{
                if(debugMode)Log.e(TAG,"Gatt is null");
            }
        }
        return false;
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyUpdate(gatt, txPhy, rxPhy, status);
            if(debugMode)Log.d(TAG, "onPhyUpdate = "+String.format(Locale.US, "txPhy:%d, rxPhy:%d, status:%d",
                    txPhy, rxPhy, status));
        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {
            super.onPhyRead(gatt, txPhy, rxPhy, status);
            if(debugMode)Log.d(TAG, String.format(Locale.US, "txPhy:%d, rxPhy:%d, status:%d",
                    txPhy, rxPhy, status));
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
//            super.onConnectionStateChange(gatt, status, newState);
            if(debugMode)Log.d(TAG, "onConnectionStateChange newState = "+newState);
            if (newState == BluetoothGatt.STATE_CONNECTED){ // connected
                gatt.setPreferredPhy(
                        BluetoothDevice.PHY_LE_2M_MASK,
                        BluetoothDevice.PHY_LE_2M_MASK,
                        BluetoothDevice.PHY_OPTION_NO_PREFERRED);
                gatt.requestMtu(MTU_SIZE);
            }else if(newState == BluetoothProfile.STATE_DISCONNECTED){ // disconnect
                if(debugMode)Log.d(TAG,"Bluetooth disconnected");
                BluetoothModel model = deviceSet.get(gatt.getDevice());
                if(null!= connectListener) connectListener.onDisconnected(model);
                if(null!=model){
                    deviceSet.remove(model);
                }
                gatt.disconnect();
                gatt.close();
            }else{
                BluetoothModel model = deviceSet.get(gatt.getDevice());
                if(null!= connectListener) connectListener.onConnectFail(model,"Connect failed state "+newState);
                if(null!= connectListener) connectListener.onDisconnected(model);
                if(null!=model){
                    deviceSet.remove(model);
                }
                targetDevice = null;
                gatt.disconnect();
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(debugMode)Log.d(TAG, "onServicesDiscovered"+String.format(Locale.US, "%d-%s", status, parseConnectionError(status)));
            if(status == BluetoothGatt.GATT_SUCCESS){
                boolean isSet = gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
                if(debugMode)Log.d(TAG,"set connection interval priority height = "+isSet);
                if(null!=targetDevice.getServiceModels()){
                    setCharacterAction(gatt,targetDevice.getServiceModels());
                }else{
                    if(null!= connectListener) connectListener.onConnected(targetDevice);
                    deviceSet.put(targetDevice.getDevice(),targetDevice);
                    targetDevice = null;//finish connecting
                }
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if(debugMode)Log.d(TAG,"Received data = "+characteristic.getValue());
            if(null!= connectListener) connectListener.onNotifyData(deviceSet.get(gatt),characteristic.getUuid(),characteristic.getValue());
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(debugMode)Log.d(TAG,"read data = "+characteristic.getValue());
            if(null!= connectListener) connectListener.onReadData(deviceSet.get(gatt),characteristic.getUuid(),characteristic.getValue());
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(debugMode)Log.d(TAG,"write data = "+characteristic.getValue());
            if(null!= connectListener) connectListener.onWriteData(deviceSet.get(gatt),characteristic.getUuid(),characteristic.getValue());
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS){
                UUID characteristicUuid = descriptor.getCharacteristic().getUuid();
                if (debugMode)Log.d(TAG, "onDescriptorWrite = "+characteristicUuid.toString());
                isEnablingCharacteristic = false; //finish write descriptor
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            if(debugMode)Log.d(TAG, String.format(Locale.US, "mtu=%d, status=%d", mtu, status));
            if(status == BluetoothGatt.GATT_SUCCESS){
                gatt.discoverServices();
            }else{
                if(debugMode)Log.e(TAG,"onMtuChanged failure");
            }
        }
    };

    private void setCharacterAction(BluetoothGatt gatt, HashMap<String, ServiceModel> models){
        for(String serviceUUID : models.keySet()){
            ServiceModel model = models.get(serviceUUID);
            BluetoothGattService service = gatt.getService(UUID.fromString(model.getUuid()));
            if(null!=service){
                HashMap<String, CharacteristicModel> characteristics = model.getCharacteristicModelHash();
                if(characteristics.size()>0){
                    for(String uuid :characteristics.keySet()){
                        CharacteristicModel characteristicModel = characteristics.get(uuid);
                        String characteristicUUID = characteristicModel.getUuid();
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicUUID)); //Notify
                        if(null!=characteristic){
                            characteristicModel.setGattCharacteristic(characteristic);
                            setNotify(characteristicModel,characteristic);
                        }else{
                            if(debugMode)Log.e(TAG,"Can't find characteristic from "+characteristicUUID);
                            if(null!= connectListener) connectListener.onConnectFail(targetDevice,"Can't find characteristic from "+characteristicUUID);
                        }
                    }
                }else{
                    if(null!= connectListener) connectListener.onConnectFail(targetDevice,"Can't find any characteristic");
                }
            }else{
                if(debugMode)Log.e(TAG,"Can't find service "+serviceUUID);
                if(null!= connectListener) connectListener.onConnectFail(targetDevice,"Can't find service "+serviceUUID);
            }
        }
        if(!notifyQueue.isEmpty()){
            while (!notifyQueue.isEmpty()){
                try {
                    if(!isEnablingCharacteristic){
                        QueueModel notifyModel = notifyQueue.poll();
                        gatt.setCharacteristicNotification(notifyModel.getCharacteristic(),true);
                        gatt.writeDescriptor(notifyModel.getDescriptor());
                        isEnablingCharacteristic = true; // start write descriptor
                    }
                    Thread.sleep(1);
                }catch (Exception e){
                }
            }
            if(null!= connectListener) connectListener.onConnected(targetDevice);
            deviceSet.put(targetDevice.getDevice(),targetDevice);
            targetDevice = null; //finish connecting
        }else{
            if(null!= connectListener) connectListener.onConnected(targetDevice);
            deviceSet.put(targetDevice.getDevice(),targetDevice);
            targetDevice = null; //finish connecting
        }
    }

    private void setNotify(CharacteristicModel characteristicModel, BluetoothGattCharacteristic characteristic){
        ArrayList<DescriptorModel> descriptorList = characteristicModel.getDescriptorList();
        if(descriptorList.size()>0){
            for(DescriptorModel descriptorModel : descriptorList){
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(descriptorModel.getDESCRIPTOR()));
                if(null!=descriptor){
                    if(descriptorModel.getType() == DescriptorModel.NOTIFICATION){
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    }else if(descriptorModel.getType() == DescriptorModel.INDICATION){
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    }else{
                        if(debugMode)Log.e(TAG,"Descriptor type error "+descriptorModel.getType());
                        if(null!= connectListener) connectListener.onConnectFail(targetDevice,"Descriptor type error "+descriptorModel.getType());
                    }
                    notifyQueue.add(new QueueModel(characteristic,descriptor));
                }else{
                    if(debugMode)Log.e(TAG,"Can't find descriptor from "+descriptorModel.getDESCRIPTOR());
                    if(null!= connectListener) connectListener.onConnectFail(targetDevice,"Can't find descriptor from "+descriptorModel.getDESCRIPTOR());
                }
            }
        }
        else{
            if(characteristic.getDescriptors().size()!=0){
                BluetoothGattDescriptor descriptor = characteristic.getDescriptors().get(0);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                notifyQueue.add(new QueueModel(characteristic,descriptor));
            }
        }
    }

    public interface OnBluetoothConnectListener{
        void onConnected(BluetoothModel model);
        void onNotifyData(BluetoothModel model, UUID receiveUUID, byte[] data);
        void onReadData(BluetoothModel model, UUID receiveUUID, byte[] data);
        void onWriteData(BluetoothModel model, UUID receiveUUID, byte[] data);
        void onConnectFail(BluetoothModel model, String msg);
        void onDisconnected(BluetoothModel model);
    }

    /************  Other function ************/
    public void setDebugMode(boolean isDebug){this.debugMode = isDebug;}

    public boolean isDebugMode(){return this.debugMode;}

    private static String parseConnectionError(int error) {
        switch(error) {
            case 0:
                return "SUCCESS";
            case 1:
                return "GATT_CONN_L2C_FAILURE";
            case 8:
                return "GATT_CONN_TIMEOUT";
            case 19:
                return "GATT_CONN_TERMINATE_PEER_USER";
            case 22:
                return "GATT_CONN_TERMINATE_LOCAL_HOST";
            case 34:
                return "GATT_CONN_LMP_TIMEOUT";
            case 62:
                return "GATT_CONN_FAIL_ESTABLISH";
            case 133:
                return "GATT ERROR";
            case 256:
                return "GATT_CONN_CANCEL";
            default:
                return "UNKNOWN (" + error + ")";
        }
    }
}
