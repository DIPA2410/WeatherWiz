package com.diparoy.weatherwiz

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.diparoy.weatherwiz.databinding.ActivityMainBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ui.PlayerView
import com.squareup.picasso.Picasso
import org.json.JSONException
import java.io.IOException
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var weatherAdapter: WeatherAdapter
    private lateinit var weatherModel: MutableList<WeatherModel>
    private lateinit var locationManager: LocationManager
    private val PERMISSION_CODE = 1
    private lateinit var cityName: String
    private lateinit var locationListener: LocationListener
    private var userSearchedLocation = false
    private var exoPlayer: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.statusBarColor = resources.getColor(android.R.color.black)
        window.navigationBarColor = resources.getColor(android.R.color.black)

        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.cityInputEditText.setOnClickListener {
            showSoftKeyboard()
        }

        weatherModel = arrayListOf()
        weatherAdapter = WeatherAdapter(this, weatherModel)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                cityName = getCityName(location.longitude, location.latitude)
                binding.cityName.text = cityName
                getWeatherInfo(cityName)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            override fun onProviderEnabled(provider: String) {}

            override fun onProviderDisabled(provider: String) {
                Toast.makeText(this@MainActivity, "Location provider not enabled", Toast.LENGTH_SHORT).show()
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                PERMISSION_CODE
            )
        } else {
            val locationProvider = LocationManager.NETWORK_PROVIDER
            if (locationManager.isProviderEnabled(locationProvider)) {
                val location = locationManager.getLastKnownLocation(locationProvider)
                if (location != null) {
                    cityName = getCityName(location.longitude, location.latitude)
                    binding.cityName.text = cityName
                    getWeatherInfo(cityName)
                } else {
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show()
                    getWeatherInfo("Kolkata")
                    binding.cityName.text = "Kolkata"
                }
                locationManager.requestLocationUpdates(locationProvider, 0, 0f, locationListener)
            } else {
                Toast.makeText(this, "Location provider not enabled", Toast.LENGTH_SHORT).show()
                getWeatherInfo("Kolkata")
                binding.cityName.text = "Kolkata"
            }
        }

        val videoView: PlayerView = binding.videoView

        if (exoPlayer == null) {
            exoPlayer = SimpleExoPlayer.Builder(this).build()
        }

        videoView.player = exoPlayer

        binding.search.setOnClickListener {
            val city = binding.cityInputEditText.text
            if (city!!.isEmpty()) {
                getWeatherInfo(city.toString())
                userSearchedLocation = false
                Toast.makeText(this, "Please Enter City Name", Toast.LENGTH_SHORT).show()
            } else {
                binding.cityName.text = city
                getWeatherInfo(city.toString())
                userSearchedLocation = true
            }
        }
        binding.weatherRecyclerview.adapter = weatherAdapter
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Please give the Permission", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getWeatherInfo(cityName: String) {
        if (cityName.isNotEmpty()) {
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val apiKey = appInfo.metaData?.getString("keyValue", "") ?: ""
            val url = "http://api.weatherapi.com/v1/forecast.json?key=$apiKey&q=$cityName&days=1&aqi=yes&alerts=yes"
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET, url, null,
                { response ->
                    weatherModel.clear()
                    try {
                        val temperature = response.getJSONObject("current").getString("temp_c")
                        binding.temperature.text = resources.getString(R.string.temperature_celsius, temperature)
                        val isDay = response.getJSONObject("current").getInt("is_day")

                        val videoResource = if (isDay == 1) {
                            R.raw.day
                        }
                        else {
                            R.raw.nighttime
                        }
                        val videoPath = "android.resource://${packageName}/${videoResource}"

                        val mediaItem = MediaItem.fromUri(videoPath)
                        exoPlayer?.setMediaItem(mediaItem)

                        exoPlayer?.prepare()
                        exoPlayer?.play()
                        exoPlayer?.repeatMode = ExoPlayer.REPEAT_MODE_ONE

                        val condition = response.getJSONObject("current").getJSONObject("condition").getString("text")
                        binding.weatherCondition.text = condition
                        val icon = response.getJSONObject("current").getJSONObject("condition").getString("icon")
                        Picasso.get().load("https:".plus(icon)).into(binding.weatherIcon)

                        val windSpeed = response.getJSONObject("current").getString("wind_kph")
                        binding.windSpeed.text = resources.getString(R.string.wind_speed, windSpeed)
                        val humidity = response.getJSONObject("current").getString("humidity")
                        binding.humidity.text = resources.getString(R.string.humidity, humidity)
                        val cloudy = response.getJSONObject("current").getString("cloud")
                        binding.cloudy.text = resources.getString(R.string.cloudy, cloudy)
                        val feelsLike = response.getJSONObject("current").getString("feelslike_c")
                        binding.realFeel.text = resources.getString(R.string.real_feel, feelsLike)

                        val forecastObj = response.getJSONObject("forecast")
                        val forecast = forecastObj.getJSONArray("forecastday").getJSONObject(0)
                        val hourArray = forecast.getJSONArray("hour")
                        for(i in 0 until hourArray.length()){
                            val hourObj = hourArray.getJSONObject(i)
                            val time = hourObj.getString("time")
                            val temperature = hourObj.getString("temp_c")
                            val icon = hourObj.getJSONObject("condition").getString("icon")
                            val wind = hourObj.getString("wind_kph")
                            weatherModel.add(WeatherModel(time, temperature, icon, wind))
                        }
                        weatherAdapter.notifyDataSetChanged()

                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                }
            ) {
                Toast.makeText(this, "Address not Found", Toast.LENGTH_SHORT).show()
            }
            val requestQueue = Volley.newRequestQueue(this)
            requestQueue.add(jsonObjectRequest)
        } else {
            Toast.makeText(this, "City name is empty", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCityName(longitude: Double, latitude: Double): String {
        var cityName = "Not Found"
        val geocoder = Geocoder(baseContext, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 10)
            if (!addresses.isNullOrEmpty()) {
                for (address in addresses) {
                    val city = address.locality
                    if (city != null && city.isNotBlank()) {
                        cityName = city
                        break
                    }
                }
            } else {
                cityName = "No address found"
                Toast.makeText(this, "Address not Found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error parsing JSON response", Toast.LENGTH_SHORT).show()
        }
        return cityName
    }


    private fun showSoftKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(binding.cityInputEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationManager.removeUpdates(locationListener)
        exoPlayer?.release()
    }
}


