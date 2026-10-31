package com.cooper.wheellog

import android.content.*
import android.os.IBinder
import com.cooper.wheellog.BluetoothService
import com.cooper.wheellog.WheelData
import com.welie.blessed.ConnectionState

/** Own the binding for the dashboard session, not for a themed Activity instance. */
object DashboardBluetoothBinding {
    private var connection: ServiceConnection? = null
    fun ensure(context: Context, address: String) {
        if (connection != null) return
        val app = context.applicationContext
        val next = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                val service = (binder as BluetoothService.LocalBinder).getService()
                WheelData.getInstance().bluetoothService = service
                if (service.connectionState == ConnectionState.DISCONNECTED && address.isNotEmpty()) {
                    service.wheelAddress = address; service.toggleConnectToWheel()
                }
            }
            override fun onServiceDisconnected(name: ComponentName) {
                WheelData.getInstance().bluetoothService = null
                WheelData.getInstance().isConnected = false
            }
            override fun onBindingDied(name: ComponentName) {
                onServiceDisconnected(name)
                release(app)
            }
        }
        connection = next
        if (!app.bindService(Intent(app, BluetoothService::class.java), next, Context.BIND_AUTO_CREATE)) connection = null
    }
    fun release(context: Context) {
        val previous = connection ?: return
        connection = null
        runCatching { context.applicationContext.unbindService(previous) }
        WheelData.getInstance().bluetoothService = null
    }
}
