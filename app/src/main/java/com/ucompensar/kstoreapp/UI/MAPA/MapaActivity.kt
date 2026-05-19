package com.ucompensar.kstoreapp.UI.MAPA

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ucompensar.kstoreapp.R

class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Coordenadas opcionales pasadas por Intent (desde DetalleServicioActivity)
    private var latExtra    = 0.0
    private var lngExtra    = 0.0
    private var tituloExtra = "Profesional"

    private val permisosUbicacion = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permisos ->
        val concedido = permisos[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permisos[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (concedido) centrarEnUbicacionUsuario()
        else Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Recibir coordenadas opcionales desde otro Activity
        latExtra    = intent.getDoubleExtra("latitud",  0.0)
        lngExtra    = intent.getDoubleExtra("longitud", 0.0)
        tituloExtra = intent.getStringExtra("titulo") ?: "Profesional"

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        if (latExtra != 0.0 && lngExtra != 0.0) {
            // ✅ Mostrar ubicación del profesional pasada por Intent
            val destino = LatLng(latExtra, lngExtra)
            mMap.addMarker(MarkerOptions().position(destino).title(tituloExtra))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destino, 15f))
        } else {
            // ✅ Mostrar ubicación actual del usuario con GPS real
            verificarYMostrarUbicacion()
        }
    }

    private fun verificarYMostrarUbicacion() {
        val fineOk   = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

        if (fineOk == PackageManager.PERMISSION_GRANTED ||
            coarseOk == PackageManager.PERMISSION_GRANTED) {
            centrarEnUbicacionUsuario()
        } else {
            permisosUbicacion.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun centrarEnUbicacionUsuario() {
        val fineOk = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        if (fineOk != PackageManager.PERMISSION_GRANTED) return

        try {
            mMap.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val miUbicacion = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(miUbicacion).title("Mi ubicación"))
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(miUbicacion, 14f))
                } else {
                    // Fallback: Bogotá
                    val bogota = LatLng(4.7110, -74.0721)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bogota, 12f))
                    Toast.makeText(this, "No se pudo obtener la ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error de permisos de ubicación", Toast.LENGTH_SHORT).show()
        }
    }
}