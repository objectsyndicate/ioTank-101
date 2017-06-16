/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.util.Log.println
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ExpandableListView
import android.widget.ProgressBar
import android.widget.SimpleExpandableListAdapter
import android.widget.TextView
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.obj
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import com.google.gson.stream.MalformedJsonException
import java.util.*
import kotlin.concurrent.fixedRateTimer




/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with `BluetoothLeService`, which in turn interacts with the
 * Bluetooth LE API.
 */
class DeviceControlActivity : Activity() {
    private var mConnectionState: TextView? = null
    private var mDataField: TextView? = null
    private var mDeviceName: String? = null
    private var mDeviceAddress: String? = null
    private var mGattServicesList: ExpandableListView? = null
    private var mBluetoothLeService: BluetoothLeService? = null
    private var mGattCharacteristics: ArrayList<ArrayList<BluetoothGattCharacteristic>>? = ArrayList()
    private var mConnected = false
    private var mNotifyCharacteristic: BluetoothGattCharacteristic? = null

    private val LIST_NAME = "NAME"
    private val LIST_UUID = "UUID"

    lateinit var fixedRateTimer: Timer

    // Code to manage Service lifecycle.
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            mBluetoothLeService = (service as BluetoothLeService.LocalBinder).service
            if (!mBluetoothLeService!!.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth")
                finish()
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService!!.connect(mDeviceAddress)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothLeService = null
        }
    }

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private val mGattUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothLeService.ACTION_GATT_CONNECTED == action) {
                mConnected = true
                updateConnectionState(R.string.connected)
                invalidateOptionsMenu()
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED == action) {
                mConnected = false
                updateConnectionState(R.string.disconnected)
                invalidateOptionsMenu()
                clearUI()
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED == action) {
                // Show all the supported services and characteristics on the user interface.

                displayGattServices(mBluetoothLeService!!.supportedGattServices)


            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE == action) {
                //println("data")
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA))
            }
        }
    }


    private fun clearUI() {
        //mGattServicesList!!.setAdapter(null as SimpleExpandableListAdapter?)
        try {
            fixedRateTimer.cancel()
        }catch(v: UninitializedPropertyAccessException){
            print(v)
        }
        //mDataField!!.setText(R.string.no_data)
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gatt_services_characteristics)

        val intent = intent
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME)
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS)

        // Sets up UI references.
        (findViewById(R.id.device_address) as TextView).text = mDeviceAddress

        //mGattServicesList = findViewById(R.id.gatt_services_list) as ExpandableListView
        //mGattServicesList!!.setOnChildClickListener(servicesListClickListner)


        mConnectionState = findViewById(R.id.connection_state) as TextView
        //mDataField = findViewById(R.id.data_value) as TextView

        //println(mDataField)

        actionBar!!.title = mDeviceName
        actionBar!!.setDisplayHomeAsUpEnabled(true)
        val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter())
        if (mBluetoothLeService != null) {
            val result = mBluetoothLeService!!.connect(mDeviceAddress)
            Log.d(TAG, "Connect request result=" + result)
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mGattUpdateReceiver)

        try {
            fixedRateTimer.cancel()
        }catch(v: UninitializedPropertyAccessException){
            print(v)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mServiceConnection)
        mBluetoothLeService = null
        try {
            fixedRateTimer.cancel()
        }catch(v: UninitializedPropertyAccessException){
            print(v)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.gatt_services, menu)
        if (mConnected) {
            menu.findItem(R.id.menu_connect).isVisible = false
            menu.findItem(R.id.menu_disconnect).isVisible = true
        } else {
            menu.findItem(R.id.menu_connect).isVisible = true
            menu.findItem(R.id.menu_disconnect).isVisible = false
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_connect -> {
                mBluetoothLeService!!.connect(mDeviceAddress)
                return true
            }
            R.id.menu_disconnect -> {
                mBluetoothLeService!!.disconnect()
                return true
            }
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun updateConnectionState(resourceId: Int) {
        runOnUiThread { mConnectionState!!.setText(resourceId) }
    }
/////////////////////////////////////////////////// Render data captured
    @SuppressLint("SetTextI18n")
    private fun displayData(data: String?) {
        if (data != null) {


            //mDataField!!.text = data

            //println(data)
            val parser = JsonParser()
            try {
                val o = parser.parse(data).obj
// parse JSON from BLE

                //println(o["R"].asDouble)
                //mDataField!!.text = o["L"].asString

                // ambient light value
                val av = findViewById(R.id.clear_value) as TextView
                av.text = "Ambient Light " + o["A"].asString

                val ab = findViewById(R.id.clearBar) as ProgressBar
                ab.progress = (o["A"].asString.toInt())

                // red light value
                val rv = findViewById(R.id.red_value) as TextView
                rv.text = "Red Light " + o["R"].asString

                val rb = findViewById(R.id.redBar) as ProgressBar
                rb.progress = (o["R"].asString.toInt())

                // Green light raw value
                val gv = findViewById(R.id.green_value) as TextView
                gv.text = "Green Light " + o["G"].asString

                val gb = findViewById(R.id.greenBar) as ProgressBar
                gb.progress = (o["G"].asString.toInt())

                // Blue light raw value
                val bv = findViewById(R.id.blue_value) as TextView
                bv.text = "Blue Light " + o["B"].asString

                val bb = findViewById(R.id.blueBar) as ProgressBar
                bb.progress = (o["B"].asString.toInt())

                //Lux
                val lv = findViewById(R.id.lux_view) as TextView
                lv.text = "Lux " + o["L"].asString

                val lb = findViewById(R.id.luxBar) as ProgressBar
                lb.progress = (o["L"].asString.toInt())

                //Kelvin
                val kv = findViewById(R.id.kelvin_value) as TextView
                kv.text = "Kelvin " + o["K"].asString

                val kb = findViewById(R.id.kelvinBar) as ProgressBar
                kb.progress = (o["K"].asString.toInt())

                //UV Index
                val uv = findViewById(R.id.uvi_value) as TextView
                uv.text = "UV Index " + o["U"].asString

                val ub = findViewById(R.id.uviBar) as ProgressBar
                ub.progress = (o["U"].asString.toDouble().toInt())

                //Soil moisture
                val sv = findViewById(R.id.soil_value) as TextView
                sv.text = "Soil Moisture " + o["S"].asString

                val sb = findViewById(R.id.soilBar) as ProgressBar
                sb.progress = (o["S"].asString.toInt())

                //Temp probe (waterproof)
                val tv = findViewById(R.id.t1_value) as TextView
                tv.text = "Temp " + o["T"].asString

                val tb = findViewById(R.id.t1Bar) as ProgressBar
                tb.progress = (o["T"].asString.toDouble().toInt())

                //Prox (IR)
                val pv = findViewById(R.id.prox_value) as TextView
                pv.text = "IR " + o["P"].asString

                val pb = findViewById(R.id.proxBar) as ProgressBar
                pb.progress = (o["P"].asString.toInt())

            }catch(e: MalformedJsonException){
                //println(e)

            }catch(e: JsonSyntaxException){
                //println(e)
            }catch(e: NullPointerException){
                //println(e)
            }


        }
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private fun displayGattServices(gattServices: List<BluetoothGattService>?) {
        if (gattServices == null) return
        var uuid: String? = null
        val unknownServiceString = resources.getString(R.string.unknown_service)
        val unknownCharaString = resources.getString(R.string.unknown_characteristic)
        val gattServiceData = ArrayList<HashMap<String, String>>()
        val gattCharacteristicData = ArrayList<ArrayList<HashMap<String, String>>>()
        mGattCharacteristics = ArrayList<ArrayList<BluetoothGattCharacteristic>>()

        // Loops through available GATT Services.
        for (gattService in gattServices) {
            val currentServiceData = HashMap<String, String>()
            uuid = gattService.uuid.toString()

            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString))
            currentServiceData.put(LIST_UUID, uuid)
            gattServiceData.add(currentServiceData)

            val gattCharacteristicGroupData = ArrayList<HashMap<String, String>>()
            val gattCharacteristics = gattService.characteristics
            val charas = ArrayList<BluetoothGattCharacteristic>()


            //println(uuid)
            if (uuid == SampleGattAttributes.UUID_IOTANK101){
               // println("ITS TOO")
            }

            ///////////////////////////////////////////////////////////////////////////////////////
            // Code here is taken from the onclick events in the default example, and filtered to only trigger if the uuid is an ioTank
            // this is because this app is intended to only manage ioTanks and not all BLE devices.


            // Loops through available Characteristics.
            for (gattCharacteristic in gattCharacteristics) {

                uuid = gattCharacteristic.uuid.toString()

                if (uuid == SampleGattAttributes.JSON_IOTANK){
                    //println("IT IS")

                    charas.add(gattCharacteristic)
                    val currentCharaData = HashMap<String, String>()
                    currentCharaData.put(
                            LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString))
                    currentCharaData.put(LIST_UUID, uuid)
                    gattCharacteristicGroupData.add(currentCharaData)

                    //println(charas)
                    mGattCharacteristics!!.add(charas)
                    fixedRateTimer = fixedRateTimer(name = "hello-timer",
                            initialDelay = 100, period = 100) {
                        //println("thread running")

                        val characteristic = mGattCharacteristics!![0][0]
                        val charaProp = characteristic.properties
                        if (charaProp or BluetoothGattCharacteristic.PROPERTY_READ > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService!!.setCharacteristicNotification(
                                        mNotifyCharacteristic!!, false)
                                mNotifyCharacteristic = null
                            }
                            mBluetoothLeService!!.readCharacteristic(characteristic)
                        }

                    }

                }else if (uuid == SampleGattAttributes.ARDUINO_INFO){
                    // Arduino
                }else if (uuid == SampleGattAttributes.ARDUINO){
                    // Arduino
                }else if (uuid == SampleGattAttributes.ARDUINO2){
                    // Arduino
                }else{
                   println(uuid)
                   println("Not ioTank")
                   // ignore arduino UUIDs too

                }
            }
            gattCharacteristicData.add(gattCharacteristicGroupData)
        }

    }

    companion object {
        private val TAG = DeviceControlActivity::class.java.simpleName


       var EXTRAS_DEVICE_NAME = "DEVICE_NAME"
       var EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS"

        private fun makeGattUpdateIntentFilter(): IntentFilter {
            val intentFilter = IntentFilter()
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED)
            intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
            intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE)
            return intentFilter
        }
    }


}
