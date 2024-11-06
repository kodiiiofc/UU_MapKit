package com.kodiiiofc.urbanuniversity.mapkit

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.kodiiiofc.urbanuniversity.mapkit.databinding.ActivityMainBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var startLocation = Point(53.2122, 50.1438)
    private val zoomValue = 16.5f
    private var location: Location? = null
    private var locationManager: LocationManager? = null

    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        getCurrentLocation()
        GlobalScope.launch(Dispatchers.Main) {
            delay(5000)
            if (location != null) {
                startLocation = Point(location!!.latitude, location!!.longitude)
            }
            binding.mapView.moveToLocation(startLocation, zoomValue)
            setMarkerToLocation(startLocation)
            GeoObject.getNativeName()
            )

        }


    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        binding.mapView.onStop()
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

    private val permissionsRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        var allAreGranted = true
        for (isGranted in result.values) {
            allAreGranted = allAreGranted && isGranted
        }
        if (allAreGranted) {
            Toast.makeText(this@MainActivity, "Разрешения предоставлены", Toast.LENGTH_LONG)
        } else {
            Toast.makeText(this@MainActivity, "В разрешениях отказано", Toast.LENGTH_LONG)
        }
    }

    private fun getCurrentLocation() {
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager!!.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                500,
                200f,
            ) {
                location = it
                Log.d("aaa", "getCurrentLocation: ${it.longitude} , ${it.latitude}")
            }
        } else {
            val permissionsLocation = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            permissionsRequest.launch(permissionsLocation)
        }
    }

    private fun MapView.moveToLocation(location: Point, zoom: Float) {
        this.mapWindow.map.move(
            CameraPosition(
                location,
                zoom,
                0f,
                0f
            ),
            Animation(Animation.Type.SMOOTH, 3f),
            null
        )
    }

    private fun setMarkerToLocation(location: Point) {
        val marker = createBitmapFromVector(R.drawable.marker)
        mapObjectCollection = binding.mapView.mapWindow.map.mapObjects
        placemarkMapObject = mapObjectCollection.addPlacemark().apply {
            geometry = location
            val textStyle = TextStyle(
                16f,
                R.color.black,
                1f,
                R.color.white,
                TextStyle.Placement.RIGHT,
                30f,
                false,
                true
            )
            setText("Start location", textStyle)
            setIcon(ImageProvider.fromBitmap(marker))
        }
    }

    private fun createBitmapFromVector(vectorResource: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, vectorResource) ?: return null
        val bitmap = Bitmap.createBitmap(
            128,
            128,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}