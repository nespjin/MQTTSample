package com.numob.mqtt.sample

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), IGetMessageCallBack {

    private val myServiceConnection: MyServiceConnection by lazy { MyServiceConnection() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        myServiceConnection.setIGetMessageCallBack(this)
        val intent = Intent(this, MQTTService::class.java)
        bindService(intent, myServiceConnection, Context.BIND_AUTO_CREATE)


        btnTest.setOnClickListener {
            MQTTService.publish("测试一下")
        }
    }

    override fun setMessage(message: String) {
        tvDisplay.text = message
        val mqttService = myServiceConnection.getMqttService()
        mqttService?.toCreateNotification(message)
    }

    override fun onDestroy() {
        unbindService(myServiceConnection)
        super.onDestroy()
    }
}