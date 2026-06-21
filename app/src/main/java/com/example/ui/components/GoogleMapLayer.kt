package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.TouristSpot
import com.example.domain.BusLine
import com.example.domain.BusStation
import com.example.domain.LiveTransitVehicle
import com.example.domain.OptimizedJourney
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings

import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline

import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.MapProperties

@Composable
fun GoogleMapLayer(
    cameraPositionState: CameraPositionState,
    city: String,
    spots: List<TouristSpot>,
    customStartSpot: TouristSpot?,
    stations: List<BusStation>,
    lines: List<BusLine>,
    journey: OptimizedJourney?,
    isStationsVisible: Boolean,
    isTransitLinesVisible: Boolean,
    mapColorSchemeStyle: String,
    isSimulatingNavigation: Boolean,
    activeNavigationLegIndex: Int,
    navigationProgressFraction: Float,
    userGpsLocation: Pair<Double, Double>?,
    liveTransitVehicles: List<LiveTransitVehicle>,
    activeTrackingVehicle: LiveTransitVehicle?,
    onStationClick: (BusStation) -> Unit,
    onSpotClick: (TouristSpot) -> Unit,
    onMapClick: (Double, Double) -> Unit,
    isEnglish: Boolean,
    transitLineRoads: Map<String, List<Pair<Double, Double>>>,
    journeyLegRoads: Map<String, List<Pair<Double, Double>>>,
    getSpotCategoryEmoji: (String) -> String
) {
    val mapStyle = remember {
        MapStyleOptions(
            """
            [
              {
                "elementType": "geometry",
                "stylers": [
                  {
                    "color": "#212121"
                  }
                ]
              },
              {
                "elementType": "labels.icon",
                "stylers": [
                  {
                    "visibility": "off"
                  }
                ]
              },
              {
                "elementType": "labels.text.fill",
                "stylers": [
                  {
                    "color": "#757575"
                  }
                ]
              },
              {
                "elementType": "labels.text.stroke",
                "stylers": [
                  {
                    "color": "#212121"
                  }
                ]
              },
              {
                "featureType": "administrative",
                "elementType": "geometry",
                "stylers": [
                  {
                    "color": "#757575"
                  }
                ]
              },
              {
                "featureType": "administrative.country",
                "elementType": "labels.text.fill",
                "stylers": [
                  {
                    "color": "#9e9e9e"
                  }
                ]
              },
              {
                "featureType": "administrative.land_parcel",
                "stylers": [
                  {
                    "visibility": "off"
                  }
                ]
              },
              {
                "featureType": "administrative.locality",
                "elementType": "labels.text.fill",
                "stylers": [
                  {
                    "color": "#bdbdbd"
                  }
                ]
              },
              {
                "featureType": "poi",
                "elementType": "labels.text.fill",
                "stylers": [
                  {
                    "color": "#757575"
                  }
                ]
              },
              {
                "featureType": "poi.park",
                "elementType": "geometry",
                "stylers": [
                  {
                    "color": "#181818"
                  }
                ]
              },
              {
                "featureType": "poi.park",
                "elementType": "labels.text.fill",
                "stylers": [
                  {
                    "color": "#616161"
                  }
                ]
              },
              {
                "featureType": "poi.park",
                "elementType": "labels.text.stroke",
                "stylers": [
                  {
                    "color": "#1b1b1b"
                  }
                ]
              },
              {
                "featureType": "road",
                "elementType": "geometry.fill",
                "stylers": [
                  {
                    "color": "#2c2c2c"
                  }
                ]
              },
              {
                "featureType": "road",
                "elementType": "labels.text.fill",
                "stylers": [
                  {
                    "color": "#8a8a8a"
                  }
                ]
              },
              {
                "featureType": "road.arterial",
                "elementType": "geometry",
                "stylers": [
                  {
                    "color": "#373737"
                  }
                ]
              },
              {
                "featureType": "road.highway",
                "elementType": "geometry",
                "stylers": [
                  {
                    "color": "#3c3c3c"
                  }
                ]
              },
              {
                "featureType": "road.highway.controlled_access",
                "elementType": "geometry",
                "stylers": [
                  {
                    "color": "#4e4e4e"
                  }
                ]
              },
              {
                "featureType": "road.local",
                "elementType": "labels.text.fill",
                "stylers": [
                  {
                    "color": "#616161"
                  }
                ]
              },
              {
                "featureType": "transit",
                "elementType": "labels.text.fill",
                "stylers": [
                  {
                    "color": "#757575"
                  }
                ]
              },
              {
                "featureType": "water",
                "elementType": "geometry",
                "stylers": [
                  {
                    "color": "#000000"
                  }
                ]
              },
              {
                "featureType": "water",
                "elementType": "labels.text.fill",
                "stylers": [
                  {
                    "color": "#3d3d3d"
                  }
                ]
              }
            ]
            """.trimIndent()
        )
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        properties = MapProperties(mapStyleOptions = mapStyle),
        cameraPositionState = cameraPositionState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false
        ),
        onMapClick = { latLng -> onMapClick(latLng.latitude, latLng.longitude) }
    ) {
        if (isStationsVisible) {
            stations.forEach { station ->
                Marker(
                    state = MarkerState(position = LatLng(station.latitude, station.longitude)),
                    title = station.name,
                    onClick = { onStationClick(station); true }
                )
            }
        }
        
        spots.forEach { spot ->
            Marker(
                state = MarkerState(position = LatLng(spot.latitude, spot.longitude)),
                title = spot.name,
                snippet = getSpotCategoryEmoji(spot.name),
                onClick = { onSpotClick(spot); true }
            )
        }
        
        customStartSpot?.let { spot ->
            Marker(
                state = MarkerState(position = LatLng(spot.latitude, spot.longitude)),
                title = spot.name
            )
        }
        
        userGpsLocation?.let { gps ->
            Marker(
                state = MarkerState(position = LatLng(gps.first, gps.second)),
                title = "Your Location"
            )
        }
        
        if (isTransitLinesVisible) {
            transitLineRoads.forEach { (lineDetails, roadPoints) ->
                Polyline(
                    points = roadPoints.map { LatLng(it.first, it.second) },
                    color = Color(0xFF0ea5e9),
                    width = 8f
                )
            }
        }
        
        journeyLegRoads.forEach { (legDetails, roadPoints) ->
            Polyline(
                points = roadPoints.map { LatLng(it.first, it.second) },
                color = Color(0xFF10b981),
                width = 12f
            )
        }
        
        liveTransitVehicles.forEach { vehicle ->
            Marker(
                state = MarkerState(position = LatLng(vehicle.latitude, vehicle.longitude)),
                title = "Bus ${vehicle.lineName} ID: ${vehicle.id}",
                snippet = "Dir: ${vehicle.direction} / Delay: ${vehicle.delayMinutes}m"
            )
        }
    }
}
