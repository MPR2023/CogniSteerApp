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
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.widget.Toast
import android.bluetooth.BluetoothDevice



var currentProtocol by mutableStateOf("No protocol yet")
@Suppress("DEPRECATION") // Suppressing deprecation for the entire class
class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bluetoothAdapter: BluetoothAdapter

    var currentProtocol by mutableStateOf("No protocol yet")

    private val requestEnableBluetooth = 1

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            try {
                // Check for BLE permissions
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                    Log.d("BLE_Scan", "Scan result received: ${result.device.name}, ${result.device.address}")

                    val deviceName = result.device.name
                    val deviceAddress = result.device.address

                    if (deviceName == "MyBLEDevice" || deviceAddress == "94:B5:55:C0:6B:7A") {
                        Log.d("BLE_Scan", "CogniSteerBeacon detected! Device Name: $deviceName, Device Address: $deviceAddress")

                        // Check for location permissions
                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                if (location != null) {
                                    val latitude = location.latitude
                                    val longitude = location.longitude

                                    // OkHttp code to call the API
                                    val client = OkHttpClient()
                                    val json = "{\"location\": {\"latitude\": $latitude, \"longitude\": $longitude}}"
                                    val requestBody = json.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                                    val request = Request.Builder()
                                        .url("http://192.168.68.129:8000/fetch_protocol_based_on_location/")
                                        .post(requestBody)
                                        .build()

                                    client.newCall(request).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            // Handle the error
                                        }

                                        override fun onResponse(call: Call, response: Response) {
                                            if (response.isSuccessful) {
                                                val responseBody = response.body?.string()
                                                val protocol = responseBody?.let { JSONObject(it).optString("protocol", "default_value") }
                                                currentProtocol = protocol ?: "No protocol received"
                                            }
                                        }
                                    })
                                }
                            }
                        } else {
                            Log.d("BLE_Scan", "Location permission not granted")
                        }
                    }
                } else {
                    Log.d("BLE_Scan", "Bluetooth permissions not granted")
                }
            } catch (e: Exception) {
                Log.e("BLE_Scan", "Exception occurred: ${e.message}")
            }catch (e: SecurityException) {
                // Handle the exception, perhaps show a dialog to the user
                Log.e("MainActivity", "Bluetooth permission is not granted.")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            fetchLocation()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1001)
        }
        // Initialize WiFiManager
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            // Fetch WiFi Information
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)
                == PackageManager.PERMISSION_GRANTED) {
                val wifiInfo = wifiManager.connectionInfo // Deprecated but still usable
                Log.d("WiFiInfo", "SSID: ${wifiInfo.ssid}, BSSID: ${wifiInfo.bssid}")
                val wifiList = wifiManager.scanResults
                for (scanResult in wifiList) {
                    Log.d("WiFi_Scan", "SSID: ${scanResult.SSID}, BSSID: ${scanResult.BSSID}")
                }
            }

        // Initialize BluetoothAdapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.adapter

            // Request Bluetooth permissions
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                    1001
                )
                Log.d("MainActivity", "Requested Bluetooth permissions.")  // Add this line
            } else {
                // Handle permissions for older Android versions here, if needed
            }

            val pairedDevices: Set<BluetoothDevice> = bluetoothAdapter.bondedDevices
            for (device in pairedDevices) {
                val deviceName = device.name
                val deviceAddress = device.address
                Log.d("Bluetooth_Paired", "Name: $deviceName, Address: $deviceAddress")
            }

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
        }
        // Check and Request Bluetooth permissions for scanning
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                requestEnableBluetooth // Use the variable here
            )
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
                    DisplayProtocol()
                }
            }
        }
    } // <-- Make sure to close the onCreate() method here

    @Suppress("DEPRECATION") // Suppressing deprecation for this method
    @Deprecated("SUPPRESS")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1001 -> {  // This should match the request code you used for Bluetooth permissions
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    try {
                        // Permission granted, proceed with Bluetooth operations
                        val bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
                        bluetoothLeScanner?.startScan(leScanCallback)
                        Log.d("Permissions", "Bluetooth permission granted. Starting scan.")
                    } catch (e: SecurityException) {
                        // Handle the SecurityException
                        Log.e("Permissions", "SecurityException while starting Bluetooth scan", e)
                    }
                } else {
                    // Permission denied, disable functionality that depends on this permission
                    Toast.makeText(
                        this,
                        "Bluetooth permissions are required for this feature. Disabling Bluetooth functionality.",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.d("Permissions", "Bluetooth permission denied. Disabling Bluetooth functionality.")
                }
            }
            else -> {
                // Ignore all other requests
                Log.d("Permissions", "Received unhandled requestCode: $requestCode")
            }
        }
    }

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
        text = "  $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CogniSteerTheme {
        Greeting(" ")
    }
}

@Composable
fun DisplayProtocol() {
    Text(text = currentProtocol)
}