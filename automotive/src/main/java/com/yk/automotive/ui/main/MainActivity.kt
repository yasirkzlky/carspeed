package com.yk.automotive.ui.main

import android.Manifest
import android.car.Car
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.yk.automotive.R
import com.yk.automotive.data.CarManager
import com.yk.automotive.databinding.MainActivityBinding
import com.yk.automotive.ui.base.BaseActivity
import kotlinx.android.synthetic.main.main_activity.*


class MainActivity : BaseActivity<MainViewModel>() {

    companion object {
        const val THEME_KEY = "theme"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val IS_REAL_DATA_KEY = "isRealData"
        const val SHOW_CAMERA_OVERLAY_KEY = "showCameraOverlay"
    }

    private val car by lazy { Car.createCar(this) }
    private val factory by lazy { MainViewModelFactory(carManager) }
    private val carManager by lazy { CarManager(car) }
    private lateinit var binding: MainActivityBinding

    private val cameraPermissionRequestCode = 100

    private var currentTheme: String = THEME_LIGHT
    private var showCameraOverlay: Boolean = false
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        currentTheme = sharedPref.getString(THEME_KEY, THEME_LIGHT)!!

        setAppTheme(currentTheme)

        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.main_activity)
        binding.viewModel = viewModel
        binding.activity = this
        binding.lifecycleOwner = this

        viewModel.isRealData = sharedPref.getBoolean(IS_REAL_DATA_KEY, true)
        showCameraOverlay = sharedPref.getBoolean(SHOW_CAMERA_OVERLAY_KEY, false)

        requestPermission()
        requestCameraPermission()
        viewModel.loadSpeedProperties()

        initSwitch()

    }

    private fun initSwitch() {
        //region SwitchUiMode
        binding.switchUIMode.isChecked = currentTheme == THEME_LIGHT
        binding.switchUIMode.setOnClickListener {
            if (binding.switchUIMode.isChecked) {
                sharedPref.edit().putString(THEME_KEY, THEME_LIGHT).apply()
                setAppTheme(THEME_LIGHT)
                recreate()
            } else {
                sharedPref.edit().putString(THEME_KEY, THEME_DARK).apply()
                setAppTheme(THEME_DARK)
                recreate()
            }
        }
        //endregion

        //region SwitchRealData
        binding.switchRealData.isChecked = viewModel.isRealData

        if (binding.switchRealData.isChecked) {
            viewModel.loadSpeedProperties()
        } else {
            viewModel.startMockSpeed()
        }

        binding.switchRealData.setOnClickListener {
            if (!binding.switchRealData.isChecked) {
                viewModel.isRealData = false
                sharedPref.edit().putBoolean(IS_REAL_DATA_KEY, false).apply()
                viewModel.startMockSpeed()
            } else {
                viewModel.isRealData = true
                sharedPref.edit().putBoolean(IS_REAL_DATA_KEY, true).apply()
                viewModel.stopMockSpeed()
                viewModel.loadSpeedProperties()
            }
        }
        //endregion

        //region SwitchCameraOverlay
        binding.switchCameraOverlay.isChecked = showCameraOverlay
        binding.switchCameraOverlay.setOnClickListener {
            if (!binding.switchCameraOverlay.isChecked) {
                showCameraOverlay = false
                sharedPref.edit().putBoolean(SHOW_CAMERA_OVERLAY_KEY, false).apply()
                binding.layoutCamera.visibility = View.GONE
                binding.tvSpeed.visibility = View.VISIBLE
            } else {
                showCameraOverlay = true
                sharedPref.edit().putBoolean(SHOW_CAMERA_OVERLAY_KEY, true).apply()
                binding.layoutCamera.visibility = View.VISIBLE
                binding.tvSpeed.visibility = View.GONE
                requestCameraPermission()
            }
        }
        //endregion

    }

    private fun setAppTheme(currentTheme: String) {
        if (currentTheme == THEME_LIGHT) {
            setTheme(R.style.Theme_App_Light)
        } else {
            setTheme(R.style.Theme_App_Dark)
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        } else {
            // Permission already granted
            if (binding.switchCameraOverlay.isChecked)
                openCamera()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == cameraPermissionRequestCode && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            if (binding.switchCameraOverlay.isChecked)
                openCamera()
        } else {
            binding.switchCameraOverlay.isChecked = false
            showCameraOverlay = false
            sharedPref.edit().putBoolean(SHOW_CAMERA_OVERLAY_KEY, false).apply()
            binding.layoutCamera.visibility = View.GONE
            binding.tvSpeed.visibility = View.VISIBLE
            // Permission denied
        }
    }

    @Suppress("It could be necessary")
    private fun isCameraAvailable(): Boolean {
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraIds = cameraManager.cameraIdList
        return cameraIds.isNotEmpty()
    }


    private fun openCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder().build()
            val imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )

                preview.setSurfaceProvider(previewView.surfaceProvider)
            } catch (exception: Exception) {
                // Handle camera setup errors
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun requestPermission() {
        requestPermissions(
            arrayOf(
                "android.car.permission.CAR_EXTERIOR_ENVIRONMENT",
                "android.car.permission.CAR_SPEED",
                "android.car.permission.CAR_ENERGY",
                "android.car.permission.CAR_ENERGY_PORTS",
                "android.car.permission.CAR_INFO"
            ), 1
        )
    }

    override fun createViewModel(): MainViewModel =
        ViewModelProvider(this, factory).get(MainViewModel::class.java)


}