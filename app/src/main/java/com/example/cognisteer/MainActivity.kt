package com.example.cognisteer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.cognisteer.ui.theme.CogniSteerTheme
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.net.wifi.WifiManager
import android.content.Context
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bluetoothAdapter: BluetoothAdapter

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            try {
                val deviceName = result.device.name
                val deviceAddress = result.device.address
                Log.d("BLE_Scan", "Device Name: $deviceName, Device Address: $deviceAddress")
                // TODO: Send this information to your backend
            } catch (e: SecurityException) {
                // Handle the exception
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        }

        // Initialize WiFiManager
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Fetch WiFi Information
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
            == PackageManager.PERMISSION_GRANTED) {
            val wifiInfo = wifiManager.connectionInfo
            Log.d("WiFiInfo", "SSID: ${wifiInfo.ssid}, BSSID: ${wifiInfo.bssid}")
        }

        // Initialize BluetoothAdapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.adapter

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_ADMIN
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                bluetoothLeScanner?.startScan(leScanCallback)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_ADMIN),
                    1
                )
            }
            // Check Bluetooth support and status
            if (bluetoothAdapter == null) {
                Log.d("BluetoothInfo", "Device doesn't support Bluetooth")
            } else {
                if (!bluetoothAdapter.isEnabled) {
                    Log.d("BluetoothInfo", "Bluetooth is not enabled")
                } else {
                    Log.d("BluetoothInfo", "Bluetooth is enabled")
                }
            }
            val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
            bluetoothLeScanner?.startScan(leScanCallback)
        } else {
            Log.d("BluetoothInfo", "Could not obtain BluetoothManager")
        }

        setContent {
            CogniSteerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }  // <-- Make sure to close the onCreate() method here

    private fun fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // Do something with the location
                    Log.d(
                        "Location",
                        "Latitude: ${location.latitude}, Longitude: ${location.longitude}"
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CogniSteerTheme {
        Greeting("Android")
    }
}