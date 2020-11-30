package com.app.rasp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.app.rasp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {
    private val uiScope by lazy { CoroutineScope(Dispatchers.Main) }
    private var cmd: String = ""
    var wifiModuleIp = ""
    var wifiModulePort = 0

    private val mSocket by lazy {
        Socket(inetAddress, wifiModulePort)
    }
    private val inetAddress by lazy {
        InetAddress.getByName(wifiModuleIp)
    }

    private val dos by lazy {
        DataOutputStream(mSocket.getOutputStream())
    }
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        binding.btnUp.setOnClickListener {
            val ipAddressValid = binding.ipAddress.text.toString().isIpAddressValid()
            if (ipAddressValid) {
                getIPandPort()
                cmd = "0"
                writeData()
            } else Snackbar.make(binding.root, "Invalid IP", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.btnUp).show()
        }
        binding.btnDown.setOnClickListener {
            val ipAddressValid = binding.ipAddress.text.toString().isIpAddressValid()
            if (ipAddressValid) {
                getIPandPort()
                cmd = "1"
                writeData()
            } else Snackbar.make(binding.root, "Invalid IP", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.btnDown).show()
        }
    }

    private fun String.isIpAddressValid(): Boolean {
        return this.isNotEmpty()
    }

    private fun writeData() {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    dos.writeBytes(cmd)
                } catch (e: UnknownHostException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onDestroy() {
        dos.close()
        mSocket.close()
        super.onDestroy()
    }

    private fun getIPandPort() {
        val iPandPort: String = binding.ipAddress.text.toString()
        Log.d("MYTEST", "IP String: $iPandPort")
        val temp = iPandPort.split(":")
        wifiModuleIp = temp[0]
        if (temp.size > 1)
            wifiModulePort = Integer.valueOf(temp[1])
        Log.d("MY TEST", "IP:$wifiModuleIp")
        Log.d("MY TEST", "PORT:$wifiModulePort")
    }
}