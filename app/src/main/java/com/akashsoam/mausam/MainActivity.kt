package com.akashsoam.mausam

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.akashsoam.mausam.models.WeatherResponse
import com.akashsoam.mausam.utils.Constants
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private val REQUEST_LOCATION_CODE = 123123
    private lateinit var _mFusedLocationClient: FusedLocationProviderClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        _mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (!isLocationEnabled()) {
            Toast.makeText(
                this@MainActivity,
                "Please enable the device's Location",
                Toast.LENGTH_SHORT
            ).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_CODE && grantResults.isNotEmpty()) {
            Toast.makeText(this@MainActivity, "Permission Granted", Toast.LENGTH_SHORT).show()
            requestLocationData()
        } else {
            Toast.makeText(this@MainActivity, "Permission NOT Granted", Toast.LENGTH_SHORT).show()

        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData() {
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).build()
        _mFusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
//                val lastLocationLatitude = _mFusedLocationClient.lastLocation
//                    .addOnSuccessListener { location: Location ->
//                        val latitude = location.latitude
//                    }
//                val lastLocationLongitude = _mFusedLocationClient.lastLocation
//                    .addOnSuccessListener { location: Location ->
//                        val longitude = location.longitude
//                    }
//                Toast.makeText(
//                    this@MainActivity,
//                    "latitude: ${locationResult.lastLocation?.latitude} \n longitude: ${locationResult.lastLocation?.longitude}",
//                    Toast.LENGTH_SHORT
//                ).show()
                getLocationWeatherDetails(
                    locationResult.lastLocation?.latitude!!,
                    locationResult.lastLocation?.longitude!!
                )
            }
        }, Looper.myLooper())
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun requestPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showRequestDialog()
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            )
        ) {
            requestPermissions()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_CODE
            )
        }
    }

    private fun showRequestDialog() {
        AlertDialog.Builder(this).setPositiveButton("GO TO SETTINGS") { _, _ ->
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", packageName, null)
                intent.data = uri
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
            }
        }.setNegativeButton("CLOSE") { dialog, _ -> dialog.cancel() }
            .setTitle("Location permission required")
            .setMessage("go to settings to turn on the device location?").setCancelable(false)
            .show()

    }

    private fun getLocationWeatherDetails(latitude: Double = 28.6139, longitude: Double = 77.2090) {
        if (Constants.isNetworkAvailable(this@MainActivity)) {
//            Toast.makeText(this@MainActivity, "connected to the internet", Toast.LENGTH_SHORT).show()
            val retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val serviceApi = retrofit.create(WeatherServiceApi::class.java)
            val call = serviceApi.getWeatherDetails(
                latitude,
                longitude,
                Constants.APP_ID,
                Constants.METRIC_UNIT
            )
            call.enqueue(object : Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    if (response.isSuccessful) {
                        val weather: WeatherResponse? = response.body()
                        for (i in weather!!.weather.indices) {
                            findViewById<TextView>(R.id.text_view_sunset).text =
                                convertTime(weather.sys.sunset.toLong())
                            findViewById<TextView>(R.id.text_view_sunrise).text =
                                convertTime(weather.sys.sunrise.toLong() )
                            findViewById<TextView>(R.id.text_view_status).text =
                                weather.weather[i].description
                            findViewById<TextView>(R.id.text_view_address).text = weather.name
                            findViewById<TextView>(R.id.text_view_address).text = weather.name
                            findViewById<TextView>(R.id.text_view_temp_max).text =
                                weather.main.temp_max.toString() + " max"
                            findViewById<TextView>(R.id.text_view_temp_min).text =
                                weather.main.temp_max.toString() + " min"
                            findViewById<TextView>(R.id.text_view_temp).text =
                                weather.main.temp.toString() + "°C"
                            findViewById<TextView>(R.id.text_view_humidity).text =
                                weather.main.humidity.toString()
                            findViewById<TextView>(R.id.text_view_pressure).text =
                                weather.main.pressure.toString()
                            findViewById<TextView>(R.id.text_view_wind).text =
                                weather.wind.speed.toString()
                        }
//                        Toast.makeText(
//                            this@MainActivity,
//                            weatherList.toString(),
//                            Toast.LENGTH_SHORT
//                        ).show()
                    } else {
                        when (response.code()) {
                            400 -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Bad connection",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            404 -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Not found",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            else -> {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Something went wrong",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    TODO("Not yet implemented")
                }

            })
        } else {
            Toast.makeText(this@MainActivity, "NOT connected to the internet", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun convertTime(time: Long): String {
        val date = java.util.Date(time * 1000L)
        val sdf = java.text.SimpleDateFormat("HH:mm:ss z", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getDefault()
        return sdf.format(date)
    }
}