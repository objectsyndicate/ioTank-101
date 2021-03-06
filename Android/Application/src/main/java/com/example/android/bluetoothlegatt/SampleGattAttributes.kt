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

import java.util.HashMap

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
object SampleGattAttributes {
    var attributes: HashMap<String, String> = HashMap()
    var HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb"
    var CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"
    val UUID_IOTANK101 = "b643cc1c-533f-49d0-9899-ff91dc503d28"
    val JSON_IOTANK = "3117f4bd-ee31-4253-a1ec-51b6bc8da174"

    val ARDUINO_INFO = "00002a01-0000-1000-8000-00805f9b34fb"
    val ARDUINO = "00002a04-0000-1000-8000-00805f9b34fb"
    val ARDUINO2 ="00002a00-0000-1000-8000-00805f9b34fb"


    init {
        // Sample Services.
        attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service")
        attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service")
        // Sample Characteristics.
        attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement")
        attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String")


        attributes.put("00001800-0000-1000-8000-00805f9b34fb", "Arduino 101")


        attributes.put(UUID_IOTANK101, "ioTank101")
        attributes.put(JSON_IOTANK, "ioTank101 JSON")
    }


    fun lookup(uuid: String, defaultName: String): String {
        val name = attributes.get(uuid)
        return name ?: defaultName
    }
}
