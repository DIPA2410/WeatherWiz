package com.diparoy.weatherwiz

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.diparoy.weatherwiz.databinding.WeatherRecyclerviewItemBinding
import com.squareup.picasso.Picasso
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class WeatherAdapter(
    private var context: Context,
    private var weatherModelArrayList: List<WeatherModel>
): RecyclerView.Adapter<WeatherAdapter.ViewHolder>(){

    inner class ViewHolder(val binding: WeatherRecyclerviewItemBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = WeatherRecyclerviewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return weatherModelArrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            with(weatherModelArrayList[position]){
                Picasso.get().load("https:".plus(icon)).into(binding.rvConditionIcon)
                binding.rvTemperature.text = context.getString(R.string.temperature_celsius, temperature)
                binding.rvWindSpeed.text = context.getString(R.string.wind_speed, windSpeed)
                val raw_time = SimpleDateFormat("yyy-mm-dd hh:mm", Locale.US)
                val structure_time = SimpleDateFormat("hh:mm aa", Locale.US)
                try{
                    val t = raw_time.parse(time)
                    binding.rvTime.text = t?.let { structure_time.format(it).toString() }
                } catch (e:ParseException){
                    e.printStackTrace()
                }
            }
        }
    }
}