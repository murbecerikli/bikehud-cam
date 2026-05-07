package com.example.bikehud

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.Surface
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jiangdg.usbcamera.UVCCameraHelper
import com.serenegiant.usb.CameraDialog
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.widget.CameraViewInterface

class MainActivity : AppCompatActivity(), CameraDialog.CameraDialogParent, CameraViewInterface.Callback {

    private lateinit var hud: HudOverlayView
    private lateinit var camView: CameraViewInterface

    private val odo = GpsOdometer()

    private var cameraHelper: UVCCameraHelper? = null
    private var isRequest = false
    private var isPreview = false

    private val permReq = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* no-op */ }

    private val connectListener = object : UVCCameraHelper.OnMyDevConnectListener {
        override fun onAttachDev(device: UsbDevice?) {
            if (!isRequest) {
                isRequest = true
                cameraHelper?.requestPermission(0)
            }
        }

        override fun onDettachDev(device: UsbDevice?) {
            if (isRequest) {
                isRequest = false
                cameraHelper?.closeCamera()
                toast("USB kamera çıkarıldı")
            }
        }

        override fun onConnectDev(device: UsbDevice?, isConnected: Boolean) {
            if (!isConnected) {
                toast("Bağlantı başarısız: çözünürlük/OTG kontrol")
                isPreview = false
            } else {
                toast("Kamera bağlandı")
                isPreview = true
            }
        }

        override fun onDisConnectDev(device: UsbDevice?) {
            toast("Kamera bağlantısı kesildi")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hud = findViewById(R.id.hud)
        camView = findViewById(R.id.camera_view) as CameraViewInterface

        // HUD uzun basınca kalibrasyon ekranı
        hud.setOnLongClickListener {
            startActivity(Intent(this, CalibrateActivity::class.java))
            true
        }

        // İzinler
        permReq.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA
        ))

        // Kalibrasyon değerlerini yükle
        loadCalibration()

        // GPS başlat
        startLocationUpdates()

        // USB kamera helper init
        camView.setCallback(this)
        cameraHelper = UVCCameraHelper.getInstance()
        // MJPEG çoğu webcam'de stabil
        cameraHelper?.setDefaultFrameFormat(UVCCameraHelper.FRAME_FORMAT_MJPEG)
        cameraHelper?.setDefaultPreviewSize(1280, 720)
        cameraHelper?.initUSBMonitor(this, camView, connectListener)

        // İstersen: ilk açılışta izinleri kontrol et
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasBasicPermissions()) {
            requestBasicPermissions()
        }
    }

    private fun loadCalibration() {
        val prefs = getSharedPreferences("bikehud", Context.MODE_PRIVATE)
        hud.y3m = prefs.getFloat("y3m", 0.75f)
        hud.y5m = prefs.getFloat("y5m", 0.62f)
        hud.y10m = prefs.getFloat("y10m", 0.48f)
    }

    private fun hasBasicPermissions(): Boolean {
        val cam = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return cam && fine
    }

    private fun requestBasicPermissions() {
        // runtime permission handled by ActivityResultContracts above
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 400L, 0.3f) { loc ->
            val (speed, meters) = odo.update(loc)
            hud.speedKmh = speed
            hud.odoKm = (meters / 1000.0).toFloat()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    // CameraViewInterface.Callback
    override fun onSurfaceCreated(view: CameraViewInterface?, surface: Surface?) {
        if (!isPreview && cameraHelper?.isCameraOpened == true) {
            cameraHelper?.startPreview(camView)
            isPreview = true
        }
    }

    override fun onSurfaceChanged(view: CameraViewInterface?, surface: Surface?, width: Int, height: Int) {}

    override fun onSurfaceDestroy(view: CameraViewInterface?, surface: Surface?) {
        if (isPreview && cameraHelper?.isCameraOpened == true) {
            cameraHelper?.stopPreview()
            isPreview = false
        }
    }

    // CameraDialogParent
    override fun getUSBMonitor(): USBMonitor? = cameraHelper?.usbMonitor

    override fun onDialogResult(canceled: Boolean) {
        if (canceled) toast("İşlem iptal edildi")
    }

    override fun onStart() {
        super.onStart()
        cameraHelper?.registerUSB()
    }

    override fun onStop() {
        super.onStop()
        cameraHelper?.unregisterUSB()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraHelper?.release()
    }
}
