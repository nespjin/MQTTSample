package com.numob.mqtt.sample

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


/**
 *
 * Author: <a href="mailto:jinzhaolu@numob.com">Jack Email:jinzhaolu@numob.com</a>
 * Time: Created 2020/10/14 1:59 PM
 * Project: MQTTSample
 * Description:
 **/
class MQTTService : Service() {

    private var conOpt: MqttConnectOptions? = null
    private val host = "tcp://10.0.0.154:1883"
    private val userName = "admin"
    private val passWord = "password"
    private val clientId = "androidId" //客户端标识
    private var iGetMessageCallBack: IGetMessageCallBack? = null

    override fun onCreate() {
        super.onCreate()
        init()
    }

    private fun init() {
        // 服务器地址（协议+地址+端口号）
        client = MqttAndroidClient(this, host, clientId)
        // 设置MQTT监听并且接受消息
        client!!.setCallback(mqttCallback)
        conOpt = MqttConnectOptions()
        // 清除缓存
        conOpt!!.isCleanSession = true
        // 设置超时时间，单位：秒
        conOpt!!.connectionTimeout = 10
        // 心跳包发送间隔，单位：秒
        conOpt!!.keepAliveInterval = 20
        // 用户名
        conOpt!!.userName = userName
        // 密码
        conOpt!!.password = passWord.toCharArray() //将字符串转换为字符串数组

        // last will message
        var doConnect = true
        val message = "{\"terminal_uid\":\"$clientId\"}"
        Log.e(TAG, "message是:$message")
        val topic = myTopic
        val qos = 0
        val retained = false
        if (message != "" || topic != "") {
            // 最后的遗嘱
            // MQTT本身就是为信号不稳定的网络设计的，所以难免一些客户端会无故的和Broker断开连接。
            //当客户端连接到Broker时，可以指定LWT，Broker会定期检测客户端是否有异常。
            //当客户端异常掉线时，Broker就往连接时指定的topic里推送当时指定的LWT消息。
            try {
                conOpt?.setWill(topic, message.toByteArray(), qos, retained)
            } catch (e: Exception) {
                Log.e(TAG, "Exception Occured", e)
                doConnect = false
                iMqttActionListener.onFailure(null, e)
            }
        }
        if (doConnect) {
            doClientConnection()
        }
    }

    override fun onDestroy() {
        stopSelf()
        try {
            client!!.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
        super.onDestroy()
    }

    /** 连接MQTT服务器  */
    private fun doClientConnection() {
        if (!client!!.isConnected && isConnectIsNormal()) {
            try {
                client?.connect(conOpt, null, iMqttActionListener)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    // MQTT是否连接成功
    private val iMqttActionListener: IMqttActionListener = object : IMqttActionListener {
        override fun onSuccess(arg0: IMqttToken) {
            Log.e(TAG, "连接成功 ")
            try {
                // 订阅myTopic话题
                client?.subscribe(myTopic, 1)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }

        override fun onFailure(arg0: IMqttToken, arg1: Throwable) {
            arg1.printStackTrace()
            // 连接失败，重连
        }
    }

    // MQTT监听并且接受消息
    private val mqttCallback: MqttCallback = object : MqttCallback {
        @Throws(Exception::class)
        override fun messageArrived(topic: String, message: MqttMessage) {
            val str1 = String(message.payload)
            iGetMessageCallBack?.setMessage(str1)
            val str2 = topic + ";qos:" + message.qos + ";retained:" + message.isRetained
            Log.i(TAG, "messageArrived:$str1")
            Log.i(TAG, "messageArrived2:$str2")
        }

        override fun deliveryComplete(arg0: IMqttDeliveryToken) {}
        override fun connectionLost(arg0: Throwable) {
            // 失去连接，重连
        }
    }

    /** 判断网络是否连接  */
    private fun isConnectIsNormal(): Boolean {
        val connectivityManager = this.applicationContext
            .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val info = connectivityManager.activeNetworkInfo
        return if (info != null && info.isAvailable) {
            val name = info.typeName
            Log.i(TAG, "MQTT当前网络名称：$name")
            true
        } else {
            Log.i(TAG, "MQTT 没有可用网络")
            false
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        Log.e(TAG, "onBind")
        return CustomBinder()
    }

    fun setIGetMessageCallBack(IGetMessageCallBack: IGetMessageCallBack?) {
        this.iGetMessageCallBack = IGetMessageCallBack
    }

    inner class CustomBinder : Binder() {
        val service: MQTTService
            get() = this@MQTTService
    }

    fun toCreateNotification(message: String?) {
        val pendingIntent = PendingIntent.getActivity(
            this, 1, Intent(
                this,
                MQTTService::class.java
            ), PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = NotificationCompat.Builder(this) //3、创建一个通知，属性太多，使用构造器模式
        val notification: Notification = builder
            .setTicker("测试标题")
            .setSmallIcon(android.R.mipmap.sym_def_app_icon)
            .setContentTitle("")
            .setContentText(message)
            .setContentInfo("")
            .setContentIntent(pendingIntent) //点击后才触发的意图，“挂起的”意图
            .setAutoCancel(true) //设置点击之后notification消失
            .build()
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        startForeground(0, notification)
        notificationManager!!.notify(0, notification)
    }

    companion object {
        private val TAG = MQTTService::class.java.simpleName

        private var client: MqttAndroidClient? = null

        private const val myTopic = "/hello" //要订阅的主题

        fun publish(msg: String) {
            val topic = myTopic
            val qos = 0
            val retained = false
            try {
                client?.publish(topic, msg.toByteArray(), qos, retained)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }
}