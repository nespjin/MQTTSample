package com.numob.mqtt.sample

import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.numob.mqtt.sample.MQTTService.CustomBinder


/**
 *
 * Author: <a href="mailto:jinzhaolu@numob.com">Jack Email:jinzhaolu@numob.com</a>
 * Time: Created 2020/10/14 2:00 PM
 * Project: MQTTSample
 * Description:
 **/
class MyServiceConnection : ServiceConnection {

    private var mqttService: MQTTService? = null
    private var iGetMessageCallBack: IGetMessageCallBack? = null

    override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder) {
        Log.e(TAG, "onServiceConnected: ")
        mqttService = (iBinder as CustomBinder).service
        mqttService!!.setIGetMessageCallBack(iGetMessageCallBack)
    }

    override fun onServiceDisconnected(componentName: ComponentName?) {}

    fun getMqttService(): MQTTService? {
        return mqttService
    }

    fun setIGetMessageCallBack(IGetMessageCallBack: IGetMessageCallBack?) {
        this.iGetMessageCallBack = IGetMessageCallBack
    }
}