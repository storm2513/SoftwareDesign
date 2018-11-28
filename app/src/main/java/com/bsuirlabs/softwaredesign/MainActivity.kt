package com.bsuirlabs.softwaredesign

import android.Manifest
import android.Manifest.permission.READ_PHONE_STATE
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.checkSelfPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.telephony.TelephonyManager
import kotlinx.android.synthetic.main.activity_main.*


private const val PermissionsRequestReadPhoneState = 0
private const val IMEI = "IMEI"

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = resources.getInteger(R.integer.screen_orientation)
        setContentView(R.layout.activity_main)
        val imeiNumber = savedInstanceState?.getString(IMEI)
        if (imeiNumber == null) {
            if (checkSelfPermission(this@MainActivity,
                            READ_PHONE_STATE) != PERMISSION_GRANTED) {
                showPermissionExplanationDialog()
            } else {
                setImei()
            }
        } else {
            imei.text = imeiNumber
        }
        setProjectVersion()
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        if (checkSelfPermission(this@MainActivity,
                        READ_PHONE_STATE) == PERMISSION_GRANTED) {
            savedInstanceState.putString(IMEI, imei.text.toString())
        }
        super.onSaveInstanceState(savedInstanceState)
    }

    private fun setProjectVersion(){
        projectVersion.text = BuildConfig.VERSION_NAME
    }

    private fun requestReadPhoneStatePermission(){
        ActivityCompat.requestPermissions(this@MainActivity,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                PermissionsRequestReadPhoneState)
    }

    private fun showPermissionExplanationDialog(){
        val builder = AlertDialog.Builder(this)
        builder.setMessage(resources.getString(R.string.phone_calls_permission_explanation))
                .setCancelable(false)
                .setPositiveButton(resources.getString(R.string.ok)) { _, _ ->
                    requestReadPhoneStatePermission()
                }
        val alert = builder.create()
        alert.show()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PermissionsRequestReadPhoneState -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    setImei()
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity,
                                    Manifest.permission.READ_PHONE_STATE)) {
                        showSnackbar()
                    }
                }
                return
            }
        }
    }

    private fun showSnackbar(){
        val snackbar = Snackbar.make(imei,
                resources.getString(R.string.dont_have_imei_permission), Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(resources.getString(R.string.grant_permission)) {
            snackbar.dismiss()
            requestReadPhoneStatePermission()
        }
        snackbar.show()
    }

    @SuppressLint("HardwareIds")
    private fun setImei() {
        if (checkSelfPermission(this@MainActivity,
                        READ_PHONE_STATE) == PERMISSION_GRANTED) {
            val tel = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            imei.text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            tel.imei
                        } else {
                            @Suppress("DEPRECATION")
                            tel.deviceId
                        }
        }
    }
}
