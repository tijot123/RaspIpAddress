package com.app.rasp

import android.content.Intent
import android.util.Log
import com.app.rasp.base.BaseActivity
import com.app.rasp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.Socket
import java.net.UnknownHostException

class MainActivity : BaseActivity<ActivityMainBinding>() {
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

    private fun String.isIpAddressValid(): Boolean {
        return this.isNotEmpty()
    }

    private fun writeData() {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                var connected = false
                try {
                    connected = mSocket.isConnected
                    dos.writeBytes(cmd)
                } catch (e: UnknownHostException) {
                    e.printStackTrace()
                } catch (e: IOException) {
                    e.printStackTrace()
                } catch (e: ConnectException) {
                    e.printStackTrace()
                }
                withContext(Dispatchers.Main) {
                    setUpConnectionStatus(connected)
                    if (connected) {
                        startActivity(Intent(this@MainActivity, SuccessPageActivity::class.java))
                        //closeConnection()
                    }

                }
            }
        }
    }

    private fun setUpConnectionStatus(connected: Boolean) {
        if (connected) {
            binding.status.text = getString(R.string.connected)
        } else binding.status.text = getString(R.string.disconnected)
    }

    override fun onDestroy() {
        closeConnection()
        super.onDestroy()
    }

    private fun closeConnection() {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    dos.close()
                    mSocket.close()
                } catch (e: ConnectException) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onResume() {
        uiScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val connected = mSocket.isConnected
                    withContext(Dispatchers.Main) {
                        setUpConnectionStatus(connected)
                    }
                } catch (e: ConnectException) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        setUpConnectionStatus(false)
                    }
                }
            }
        }
        super.onResume()
    }

    private fun getIPandPort() {
        val iPandPort: String = binding.ipAddressEdit.text.toString()
        Log.d("MYTEST", "IP String: $iPandPort")
        val temp = iPandPort.split(":")
        wifiModuleIp = temp[0]
        if (temp.size > 1)
            wifiModulePort = Integer.valueOf(temp[1])
        Log.d("MY TEST", "IP:$wifiModuleIp")
        Log.d("MY TEST", "PORT:$wifiModulePort")
    }

    override fun initViews() {
        binding.btnUp.setOnClickListener {
            val ipAddressValid = binding.ipAddressEdit.text.toString().isIpAddressValid()
            if (ipAddressValid) {
                getIPandPort()
                cmd = "0"
                writeData()
            } else Snackbar.make(binding.root, "Invalid IP", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.btnUp).show()
        }
        binding.btnDown.setOnClickListener {
            val ipAddressValid = binding.ipAddressEdit.text.toString().isIpAddressValid()
            if (ipAddressValid) {
                getIPandPort()
                cmd = "1"
                writeData()
            } else Snackbar.make(binding.root, "Invalid IP", Snackbar.LENGTH_LONG)
                .setAnchorView(binding.btnDown).show()
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_main
}