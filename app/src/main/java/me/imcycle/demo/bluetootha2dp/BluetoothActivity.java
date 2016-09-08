package me.imcycle.demo.bluetootha2dp;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.app.Activity;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
/**
 *
 * Copyright 2013 Kevin Coppock
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Main activity which handles the flow of connecting to the requested Bluetooth A2DP device
 */
public class BluetoothActivity extends Activity implements BluetoothBroadcastReceiver.Callback, BluetoothA2DPRequester.Callback {
    private static final String TAG = "BluetoothActivity";

    /**
     * This is the name of the device to connect to. You can replace this with the name of
     * your device.
     */
    private static final String HTC_MEDIA = "ATH-S700BT";

    /**
     * Local reference to the device's BluetoothAdapter
     */
    private BluetoothAdapter mAdapter;
    //private ListView listView;
    private ArrayList listBle;
    private ArrayAdapter listAdapter;
    private Set<BluetoothDevice> devicesArray;
    private BluetoothActivity bluetoothActivity;
    private String clickName;
    private User userdata;


    private Button btn_control;
    private boolean isStart = false;
    private MediaRecorder mr = null;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothActivity = this;

        ListView listView = (ListView) findViewById(R.id.listview1);

        //Store a local reference to the BluetoothAdapter
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        //listAdapter=new ArrayAdapter<String[]>(this,android.R.layout.simple_list_item_1,0);

        listBle = new ArrayList();
        
       // listView.setAdapter(getPairedDevices(mAdapter));
        
        /*
         * Add new adapter for ble device scan
         * */
        
        UsersAdapter adapterble = new UsersAdapter(this, getPairedDevicesBle(mAdapter));
        
        listView.setAdapter(adapterble);
        
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                int position, long id) {
              final User item = (User) parent.getItemAtPosition(position);
              view.animate().setDuration(2000).alpha(0)
                  .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                      //listAdapter.remove(item);
                      //listAdapter.notifyDataSetChanged();
                      view.setAlpha(1);
                      Toast.makeText(view.getContext(), "you click " + item.name, Toast.LENGTH_LONG).show();;
                      clickName = item.address;
                      //Already connected, skip the rest
                      if (mAdapter.isEnabled()) {
                          onBluetoothConnected();
                          return;
                      }

                      //Check if we're allowed to enable Bluetooth. If so, listen for a
                      //successful enabling
                      if (mAdapter.enable()) {
                          BluetoothBroadcastReceiver.register(bluetoothActivity, bluetoothActivity);
                      } else {
                          Log.e(TAG, "Unable to enable Bluetooth. Is Airplane Mode enabled?");
                      }
                      
                    }
                  });
            }

          });
        btn_control = (Button) findViewById(R.id.btn_control);
        btn_control.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isStart){
                    startRecord();
                    btn_control.setText("停止录制");
                    isStart = true;
                }else{
                    stopRecord();
                    btn_control.setText("开始录制");
                    isStart = false;
                }
            }
        });
    }

    //开始录制
    private void startRecord(){
        if(mr == null){
            File dir = new File(Environment.getExternalStorageDirectory(),"sounds");
            if(!dir.exists()){
                dir.mkdirs();
            }
            File soundFile = new File(dir,"11111111111111111111.amr");
            if(!soundFile.exists()){
                try {
                    soundFile.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            mr = new MediaRecorder();
            mr.setAudioSource(MediaRecorder.AudioSource.MIC);  //音频输入源
            mr.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);   //设置输出格式
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);   //设置编码格式
            mr.setOutputFile(soundFile.getAbsolutePath());
            try {
                mr.prepare();
                mr.start();  //开始录制
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    //停止录制，资源释放
    private void stopRecord(){
        if(mr != null){
            mr.stop();
            mr.release();
            mr = null;
        }
    }

    private ArrayAdapter<String[]> getPairedDevices(BluetoothAdapter mBleAdapter) {
        devicesArray=mBleAdapter.getBondedDevices();
        if(devicesArray.size()>0){
            for(BluetoothDevice device:devicesArray) {
            	String[] additem ={ device.getName(), device.getAddress()};
                listAdapter.add(additem);
            }
        }
        
        return listAdapter;

    }
    
    private ArrayList<User> getPairedDevicesBle(BluetoothAdapter mBleAdapter) {
        devicesArray=mBleAdapter.getBondedDevices();
        if(devicesArray.size()>0){
            for(BluetoothDevice device:devicesArray) {
            	userdata = new User(device.getName(),device.getAddress());
            	//String[] additem ={ device.getName(), device.getAddress()};
            	listBle.add(userdata);
            }
        }
        
        return listBle;

    }

    
    @Override
    public void onBluetoothError () {
        Log.e(TAG, "There was an error enabling the Bluetooth Adapter.");
    }

    @Override
    public void onBluetoothConnected () {
        new BluetoothA2DPRequester(this).request(this, mAdapter);
    }

    @Override
    public void onA2DPProxyReceived (BluetoothA2dp proxy) {
        Method connect = getConnectMethod();
        BluetoothDevice device = findBondedDeviceByAddress(mAdapter, clickName);

        //If either is null, just return. The errors have already been logged
        if (connect == null || device == null) {
            return;
        }

        try {
            connect.setAccessible(true);
            connect.invoke(proxy, device);
            Toast.makeText(BluetoothActivity.this, "lllllll", Toast.LENGTH_LONG).show();
        } catch (InvocationTargetException ex) {
            Log.e(TAG, "Unable to invoke connect(BluetoothDevice) method on proxy. " + ex.toString());
        } catch (IllegalAccessException ex) {
            Log.e(TAG, "Illegal Access! " + ex.toString());
        }
    }

    /**
     * Wrapper around some reflection code to get the hidden 'connect()' method
     * @return the connect(BluetoothDevice) method, or null if it could not be found
     */
    private Method getConnectMethod () {
        try {
            return BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
        } catch (NoSuchMethodException ex) {
            Log.e(TAG, "Unable to find connect(BluetoothDevice) method in BluetoothA2dp proxy.");
            return null;
        }
    }

    /**
     * Search the set of bonded devices in the BluetoothAdapter for one that matches
     * the given name
     * @param adapter the BluetoothAdapter whose bonded devices should be queried
     * @param address the name of the device to search for
     * @return the BluetoothDevice by the given name (if found); null if it was not found
     */
    private static BluetoothDevice findBondedDeviceByAddress (BluetoothAdapter adapter, String address) {
        for (BluetoothDevice device : getBondedDevices(adapter)) {
            if (address.matches(device.getAddress())) {
                Log.v(TAG, String.format("Found device with name %s and address %s.", device.getName(), device.getAddress()));
                return device;
            }
        }
        Log.w(TAG, String.format("Unable to find device with address %s.", address));
        return null;
    }

    /**
     * Safety wrapper around BluetoothAdapter#getBondedDevices() that is guaranteed
     * to return a non-null result
     * @param adapter the BluetoothAdapter whose bonded devices should be obtained
     * @return the set of all bonded devices to the adapter; an empty set if there was an error
     */
    private static Set<BluetoothDevice> getBondedDevices (BluetoothAdapter adapter) {
        Set<BluetoothDevice> results = adapter.getBondedDevices();
        if (results == null) {
            results = new HashSet<BluetoothDevice>();
        }
        return results;
    }
}
