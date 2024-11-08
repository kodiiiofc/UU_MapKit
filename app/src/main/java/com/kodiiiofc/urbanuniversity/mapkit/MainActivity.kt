package com.kodiiiofc.urbanuniversity.mapkit

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kodiiiofc.urbanuniversity.mapkit.databinding.ActivityMainBinding
import com.yandex.mapkit.Animation
import com.yandex.mapkit.GeoObject
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.RequestPoint
import com.yandex.mapkit.RequestPointType
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.DrivingOptions
import com.yandex.mapkit.directions.driving.DrivingRoute
import com.yandex.mapkit.directions.driving.DrivingRouterType
import com.yandex.mapkit.directions.driving.DrivingSession.DrivingRouteListener
import com.yandex.mapkit.directions.driving.VehicleOptions
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.MapWindow
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.map.PolylineMapObject
import com.yandex.mapkit.map.TextStyle
import com.yandex.mapkit.map.VisibleRegionUtils
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.traffic.TrafficLayer
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider


class MainActivity : AppCompatActivity() {

    private val SEARCH_EMPTY_RESULT = "Ничего не найдено"

    private lateinit var binding: ActivityMainBinding

    private var location = Point(53.2122, 50.1438)
    private val zoomValue = 16.5f

    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject
    private var placemarkStartPointMapObject: PlacemarkMapObject? = null
    private var placemarkFinishPointMapObject: PlacemarkMapObject? = null

    private lateinit var trafficLayer: TrafficLayer
    private lateinit var searchManager: SearchManager

    private lateinit var routesCollection: MapObjectCollection
    private lateinit var routesPointsCollection: MapObjectCollection
    private lateinit var mapWindow: MapWindow
    private lateinit var map: Map

    private fun PolylineMapObject.styleMainRoute() {
        zIndex = 10f
        setStrokeColor(Color.GREEN)
        strokeWidth = 5f
        outlineColor = Color.DKGRAY
        outlineWidth = 3f
    }

    private fun PolylineMapObject.styleAlternativeRoute() {
        zIndex = 5f
        setStrokeColor(Color.GRAY)
        strokeWidth = 4f
        outlineColor = Color.DKGRAY
        outlineWidth = 2f
    }


    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mapWindow = binding.mapView.mapWindow
        map = mapWindow.map
        val lat = intent.getDoubleExtra("lat", location.latitude)
        val lon = intent.getDoubleExtra("lon", location.longitude)
        location = Point(lat, lon)
        map.moveToLocation(location, zoomValue)
        setMarkerToCurrentLocation(location)
        routesPointsCollection = map.mapObjects.addCollection()
        routesCollection = map.mapObjects.addCollection()
        trafficLayer = MapKitFactory.getInstance().createTrafficLayer(mapWindow)
        binding.routeFab.setOnClickListener {
            askForRoute()
        }

        binding.trafficFab.setOnClickListener {
            switchShowTraffic()
        }
    }

    private fun askForRoute() {

        binding.textInputLayoutFrom.animate().translationY(0f)
        binding.textInputLayoutTo.animate().translationY(0f)
        binding.fromEt.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.fromEt, InputMethodManager.SHOW_IMPLICIT)

        binding.fromEt.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                findPlace(v)
                binding.toEt.requestFocus()
                true
            } else false
        }
        binding.toEt.setOnKeyListener { v, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                findPlace(v)
                true
            } else false
        }
    }

    private fun createRoute() {

        if (placemarkStartPointMapObject != null && placemarkFinishPointMapObject != null) {
            val drivingRouter =
                DirectionsFactory.getInstance().createDrivingRouter(DrivingRouterType.COMBINED)
            val drivingOptions = DrivingOptions().apply {
                routesCount = 3
            }
            val vehicleOptions = VehicleOptions()
            var fromPoint: Point
            placemarkStartPointMapObject!!.geometry.apply {
                fromPoint = Point(latitude, longitude)
            }
            var toPoint: Point
            placemarkFinishPointMapObject!!.geometry.apply {
                toPoint = Point(latitude, longitude)
            }
            val points = buildList {
                add(RequestPoint(fromPoint, RequestPointType.WAYPOINT, null, null))
                add(RequestPoint(toPoint, RequestPointType.WAYPOINT, null, null))
            }

            val drivingRouteListener = object : DrivingRouteListener {
                override fun onDrivingRoutes(drivingRoutes: MutableList<DrivingRoute>) {

                    drivingRoutes.forEachIndexed { index, route ->
                        routesCollection.addPolyline(route.geometry).apply {
                            Log.d("aaa", "onDrivingRoutes: ${geometry.points}")
                            if (index == 0) styleMainRoute() else styleAlternativeRoute()
                        }
                        Log.d("AAA", "результат построения маршрута")
                    }
                }

                override fun onDrivingRoutesError(error: Error) {
                    Log.d("AAA", "ошибка построения маршрута")
                }

            }

            val drivingSession = drivingRouter.requestRoutes(
                points,
                drivingOptions,
                vehicleOptions,
                drivingRouteListener
            )
            Log.d("aaa", "createRoute: $drivingSession")
        }
    }

    private fun findPlace(view: View) {
        view as EditText
        var searchResultGeoObject: GeoObject?

        val searchListener = object : Session.SearchListener {

            override fun onSearchResponse(response: Response) {
                try {
                    searchResultGeoObject = response.collection.children.first().obj
                } catch (e: NoSuchElementException) {
                    view.setText(SEARCH_EMPTY_RESULT)
                    return
                }

                val point = searchResultGeoObject!!.geometry.first().point!!

                when (view.id) {
                    R.id.from_et -> {
                        setMarkerToStartLocation(point)
                        Log.d("AAA", "onSearchResponse: ${point.latitude}, ${point.longitude}")
                    }
                    R.id.to_et -> {
                        setMarkerToFinishLocation(point)
                        Log.d("AAA", "onSearchResponse: ${point.latitude}, ${point.longitude}")
                    }
                }

                Log.d("aaa", "onSearchRespone: $searchResultGeoObject")
                binding.mapView.mapWindow.map.move(
                    CameraPosition(
                        searchResultGeoObject!!.geometry.first().point!!,
                        zoomValue,
                        0f,
                        0f
                    ),
                    Animation(Animation.Type.SMOOTH, 3f),
                    null
                )

                view.setText(
                    searchResultGeoObject!!.name
                )
            }

            override fun onSearchError(error: Error) {
                view.setText("Ошибка во время поиска")
            }
        }
        searchManager.submit(
            view.text.toString(),
            VisibleRegionUtils.toPolygon(binding.mapView.mapWindow.map.visibleRegion),
            SearchOptions(),
            searchListener
        )
    }

    private fun switchShowTraffic() {
        trafficLayer.isTrafficVisible = !trafficLayer.isTrafficVisible
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

    private fun Map.moveToLocation(location: Point, zoom: Float) {
        this.move(
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
        searchManager = SearchFactory.getInstance()
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

    private fun setMarkerToStartLocation(location: Point) {
        val marker = createBitmapFromVector(R.drawable.marker_blue)
        placemarkStartPointMapObject = routesPointsCollection.addPlacemark().apply {
            geometry = location
            val textStyle = TextStyle(
                16f,
                Color.BLACK,
                1f,
                R.color.white,
                TextStyle.Placement.RIGHT,
                30f,
                false,
                true
            )
            setText("Start point", textStyle)
            setIcon(ImageProvider.fromBitmap(marker))
        }
        Log.d("aaa", "setMarkerToStartLocation: $placemarkStartPointMapObject")
        createRoute()
    }
    private fun setMarkerToFinishLocation(location: Point) {
        val marker = createBitmapFromVector(R.drawable.marker_red)
        placemarkFinishPointMapObject = routesPointsCollection.addPlacemark().apply {
            geometry = location
            val textStyle = TextStyle(
                16f,
                Color.BLACK,
                1f,
                R.color.white,
                TextStyle.Placement.RIGHT,
                30f,
                false,
                true
            )
            setText("Finish point", textStyle)
            setIcon(ImageProvider.fromBitmap(marker))
        }
        createRoute()
    }

    private fun setMarkerToCurrentLocation(location: Point) {
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
            setText("Current location", textStyle)
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