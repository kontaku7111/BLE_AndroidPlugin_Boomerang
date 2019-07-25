///////////////////////////////////////////////////////
// (c) 2019 Darwin Boomerangs                         /
// Smart -Boomerang Project                           /
// In partnership with :                              /
//   - University of St Etienne, France               /
//   - Aoyama Gakuin University / Lopez lab, Japan    /
///////////////////////////////////////////////////////

///////////////////////////////////////////////////////
// Filename    BLE.java                                       /
// This file is intended to make a plugin made it     /
// possible for Android Unity to connect BLE device   /
//                                                    /
///////////////////////////////////////////////////////
// v1.0  Date July 23th, 2019     Author Takumi Kondo /
// Modifications from previous version...             /
// ...                                                /
///////////////////////////////////////////////////////

package fr.boomerang.takumi.ble_module;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BLE {
    String mGameObjName;
    String mCallBackName;

    String debugText;

    Handler mHandler;

    BluetoothAdapter adapter;
    private ScanCallback scanCallback;
    private BluetoothLeScanner bluetoothLeScanner;
    private BluetoothGatt mBleGatt;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;
    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();

    // Must change deviceName and these UUIDs depending on the device and the application you want to use////
    private static String deviceName = "my-ble";
    private static final String CUSTOM_SERVICE_UUID = "BF9CB85F-620C-4A67-BDD2-1A64213F74CA";
    private static final String NOTIFY_CHARACTERISTIC_UUID = "5F83E23F-BCA1-42B3-B6F2-EA82BE46A93D";
    private static final String CUSTOM_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    private static final String READ_CHARACTERISTIC_UUID = "1126BF32-321F-44A3-847B-BBC373D7B32C";
    private static final String WRITE_CHARACTERISTIC_UUID = "330C0F34-9E86-4D00-95D5-669E6C6A5ACA";
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final static String TAG = BLE.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 10000;

    boolean isBleEnabled = false;
    boolean isScanning = false;

    public BLE(String gameObjName, String callbackName) {
        this.mGameObjName = gameObjName;
        this.mCallBackName = callbackName;
        this.debugText = null;

        mHandler = new Handler();

        if (!UnityPlayer.currentActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(UnityPlayer.currentActivity, "Not support BLE", Toast.LENGTH_SHORT).show();
            UnityPlayer.currentActivity.finish();
            return;
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) UnityPlayer.currentActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        adapter = null;
        if (null != bluetoothManager) {
            adapter = bluetoothManager.getAdapter();
        }
        if (adapter == null) {
            Toast.makeText(UnityPlayer.currentActivity, "not support Bluetooth", Toast.LENGTH_SHORT).show();
            UnityPlayer.currentActivity.finish();
            return;
        }

        bluetoothLeScanner = adapter.getBluetoothLeScanner();
        onActive();
    }

    public void onActive() {
        Log.d(TAG, "onActive");
        if (!adapter.isEnabled() || adapter == null) {
            Intent enableBtIntent = new Intent(adapter.ACTION_REQUEST_ENABLE);
            UnityPlayer.currentActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        scanCallback = initCallbacks();
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothLeScanner.stopScan(scanCallback);
                }
            }, SCAN_PERIOD);
            isScanning = true;

            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setDeviceName(deviceName)
                            .build();
            ArrayList scanFilterList = new ArrayList();

            scanFilterList.add(scanFilter);
            ScanSettings.Builder scanSettings =
                    new ScanSettings.Builder();
            scanSettings.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                    .setScanMode(ScanSettings.SCAN_MODE_BALANCED);
            ScanSettings mScanSettings = scanSettings.build();

            Log.d(TAG, "call startScan");
            bluetoothLeScanner.startScan(scanFilterList, mScanSettings, scanCallback);
        } else {
            isScanning = false;
            bluetoothLeScanner.stopScan(scanCallback);
        }
    }


    private ScanCallback initCallbacks() {
        return new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                Log.d(TAG, "call onScanResult");
                super.onScanResult(callbackType, result);
                if (result != null && result.getDevice() != null) {
                    if (isAdded(result.getDevice())) {

                    } else {
//                        saveDevice(result.getDevice());
                        Log.d(TAG, "callbackType = " + callbackType);
                        BluetoothDevice bluetoothDevice = result.getDevice();
                        Log.d(TAG, "address:" + bluetoothDevice.getAddress());
                        Log.d(TAG, "name:" + bluetoothDevice.getName());
//                        if(bluetoothDevice.getName()==deviceName){
                        stopBleScan();
                        Log.d(TAG, "stop BLE scan");
                        Log.d(TAG, "call connectGatt");
                        mBleGatt = bluetoothDevice.connectGatt(UnityPlayer.currentActivity, false, mGattCallback);
//                        }
                    }
                }
            }
            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                super.onBatchScanResults(results);
            }

            @Override
            public void onScanFailed(int errorCode) {
                Log.d(TAG, "call onScanFailed");
                super.onScanFailed(errorCode);
            }
        };
    }

    //save a scanned device to device list
    public void saveDevice(BluetoothDevice device) {
        if (deviceList == null) {
            deviceList = new ArrayList<>();
        }
        deviceList.add(device);
    }

    //conform whether a scanned device exist in device list
    public boolean isAdded(BluetoothDevice device) {
        if (deviceList != null && deviceList.size() > 0) {
            return deviceList.contains(device);
        } else {
            return false;
        }
    }


    public void stopBleScan() {
        bluetoothLeScanner.stopScan(scanCallback);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Call onConnectionStateChange STATE_CONNECTED");
                UnityPlayer.UnitySendMessage(mGameObjName, mCallBackName, "Connected!!");
                mBleGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "call onConnectionStateChange STATE_DISCONNECTED");
//                mBleGatt.close();
//                mBleGatt=null;
                isBleEnabled = false;
                mBleGatt.connect();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.d(TAG, "service discover state = " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                BluetoothGattService service = gatt.getService(UUID.fromString(CUSTOM_SERVICE_UUID));
                if (service != null) {
                    setCharacteristicNotification();
//                    readCharacteristic();
                    isBleEnabled = true;
                }
                Log.d(TAG, "call stop scan");
                stopBleScan();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            final String strChara = characteristic.getStringValue(0);
            debugText = debugText + ", " + strChara;
            Log.d(TAG, "onCharacteristicChanged: " + strChara);
            UnityPlayer.UnitySendMessage(mGameObjName, mCallBackName, "onCharactersisticChanged\n" + debugText);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "call onCharacteristicRead");
            if (BluetoothGatt.GATT_SUCCESS != status) {
                return;
            }
            Log.d(TAG, "getUUID" + characteristic.getUuid().toString());
            if (UUID.fromString(READ_CHARACTERISTIC_UUID).equals(characteristic.getUuid())) {
                String readStr = characteristic.getStringValue(0);
                Log.d(TAG, "onCharacteristicRead: " + readStr);
                UnityPlayer.UnitySendMessage(mGameObjName, mCallBackName, "onCharacteristicRead\n" + readStr);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (BluetoothGatt.GATT_SUCCESS != status) {
                return;
            }
            if (WRITE_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                String writeStr = characteristic.getStringValue(0);
                UnityPlayer.UnitySendMessage(mGameObjName, mCallBackName, "onCharacteristicWrite\n" + writeStr);
            }
        }
    };

    private void readCharacteristic() {
        UUID uuid_service = UUID.fromString(CUSTOM_SERVICE_UUID);
        UUID uuid_characteristic = UUID.fromString(READ_CHARACTERISTIC_UUID);
        if (null == mBleGatt) {
            return;
        }
        Log.d(TAG, "call readCharacteristic");
        BluetoothGattCharacteristic BleChar = mBleGatt.getService(uuid_service).getCharacteristic(uuid_characteristic);
        mBleGatt.readCharacteristic(BleChar);
    }

    private void setCharacteristicNotification() {
        UUID uuid_service = UUID.fromString(CUSTOM_SERVICE_UUID);
        UUID uuid_characteristic = UUID.fromString(NOTIFY_CHARACTERISTIC_UUID);
        if (null == mBleGatt) {
            return;
        }
        Log.d(TAG, "call setCharacteristicNotification");
        BluetoothGattCharacteristic blechar = mBleGatt.getService(uuid_service).getCharacteristic(uuid_characteristic);
        mBleGatt.setCharacteristicNotification(blechar, true);
        BluetoothGattDescriptor descriptor = blechar.getDescriptor(UUID.fromString(CUSTOM_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBleGatt.writeDescriptor(descriptor);
    }

    private void writeCharacteristic(String message) {
        UUID uuid_service = UUID.fromString(CUSTOM_SERVICE_UUID);
        UUID uuid_characteristic = UUID.fromString(WRITE_CHARACTERISTIC_UUID);
        if (null == mBleGatt) {
            return;
        }
        Log.d(TAG, "call writeCharacteristic");
        BluetoothGattCharacteristic blechar = mBleGatt.getService(uuid_service).getCharacteristic(uuid_characteristic);
        blechar.setValue(message);
        Log.d(TAG, "send message: " + message);
        mBleGatt.writeCharacteristic(blechar);
    }
}