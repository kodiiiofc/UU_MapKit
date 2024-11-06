package com.kodiiiofc.urbanuniversity.mapkit

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kodiiiofc.urbanuniversity.mapkit.databinding.ActivityMainBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var location = Point(53.2122, 50.1438)
    private val zoomValue = 16.5f

    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject

    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val lat = intent.getDoubleExtra("lat", location.latitude)
        val lon = intent.getDoubleExtra("lon", location.longitude)
        location = Point(lat, lon)
        binding.mapView.moveToLocation(location, zoomValue)
        setMarkerToLocation(location)
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
        val searchOptions = SearchOptions()
        val searchManager = SearchFactory.getInstance()
            .createSearchManager(SearchManagerType.ONLINE)
        searchManager.submit(
            location,
            zoom.toInt(),
            searchOptions,
            object : Session.SearchListener {
                override fun onSearchResponse(p0: Response) {
                    placemarkMapObject.setText(
                        p0.collection.children.first().obj?.name ?: "Нет данных"
                    )
                }

                override fun onSearchError(p0: Error) {
                    Log.d("aaa", "onSearchError: search ERROR")
                }

            })
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