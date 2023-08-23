package com.example.gps_ip

import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import android.Manifest
import android.icu.text.SimpleDateFormat
import android.util.Log
import android.widget.TextView
import com.google.android.gms.location.LocationRequest
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var ipAddressEditText: EditText
    private lateinit var portEditText: EditText
    private lateinit var coordinatesTextView: TextView

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ipAddressEditText = findViewById(R.id.ipAddressEditText)
        portEditText = findViewById(R.id.portEditText)
        coordinatesTextView = findViewById(R.id.coordinatesTextView)

        val btnSendTCP: Button = findViewById(R.id.sendTCPButton)
        btnSendTCP.setOnClickListener {
            Log.d("MainActivity", "sendTCPButton onClick")
            requestLocationAndSendCoordinatesAsync(true)
        }

        val btnSendUDP: Button = findViewById(R.id.sendUDPButton)
        btnSendUDP.setOnClickListener {
            Log.d("MainActivity", "sendUDPButton onClick")
            requestLocationAndSendCoordinatesAsync(false)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun formatTime(timeInMillis: Long): String {
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(timeInMillis)
    }


    private fun requestLocationAndSendCoordinatesAsync(isTCP: Boolean) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("MainActivity", "Location permission granted")
            fusedLocationClient.getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                null
            ).addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val time = location.time
                    val formattedTime = formatTime(time)
                    Log.d("MainActivity", "Latitude: $latitude, Longitude: $longitude, Time: $formattedTime")
                    val coordinates = "Latitud: $latitude, Longitud: $longitude, Tiempo: $formattedTime"
                    coordinatesTextView.text = coordinates // Actualiza el coordinatesTextView

                    val ipAddress = ipAddressEditText.text.toString()
                    val port = portEditText.text.toString().toInt()
                    try {
                        Thread {
                            if (isTCP) {
                                sendCoordinatesTCP(ipAddress, port, coordinates)
                            } else {
                                sendCoordinatesUDP(ipAddress, port, coordinates)
                            }
                        }.start()
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error al enviar los datos", e)
                        showToast("Error al enviar los datos: ${e.message}")
                    }
                } else {
                    showToast("No se pudo obtener la ubicación.")
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun sendCoordinatesTCP(ipAddress: String, port: Int, coordinates: String) {
        try {
            Thread {
                try {
                    val socket = Socket(ipAddress, port)
                    val outputStream = socket.getOutputStream()
                    outputStream.write(coordinates.toByteArray())
                    outputStream.close()
                    socket.close()
                    showToastOnMainThread("Coordenadas enviadas por TCP.")
                } catch (e: Exception) {
                    e.printStackTrace()
                    showToastOnMainThread("Error al enviar las coordenadas por TCP: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            e.printStackTrace()
            showToastOnMainThread("Error al iniciar el hilo de envío por TCP: ${e.message}")
        }
    }

    private fun sendCoordinatesUDP(ipAddress: String, port: Int, coordinates: String) {
        try {
            val serverAddr = InetAddress.getByName(ipAddress)
            val data = coordinates.toByteArray()
            val udpSocket = DatagramSocket()
            val packet = DatagramPacket(data, data.size, serverAddr, port)
            udpSocket.send(packet)
            udpSocket.close()
            showToastOnMainThread("Coordenadas enviadas por UDP.")
        } catch (e: Exception) {
            e.printStackTrace()
            showToastOnMainThread("Error al enviar las coordenadas por UDP: ${e.message}")
        }
    }

    private fun showToastOnMainThread(message: String) {
        runOnUiThread {
            showToast(message)
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestLocationAndSendCoordinatesAsync(true)
                } else {
                    showToast("Permiso de ubicación denegado. No se pudo obtener la ubicación.")
                }
            }
        }
    }
}