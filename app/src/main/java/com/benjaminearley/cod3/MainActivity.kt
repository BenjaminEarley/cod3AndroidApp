package com.benjaminearley.cod3

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.support.v7.widget.Toolbar
import com.benjaminearley.cod3.Cod3ApiModule.Cod3ApiInterface
import com.google.android.gms.location.LocationRequest
import com.patloew.rxlocation.RxLocation
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject
import android.support.v4.content.ContextCompat
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AlertDialog
import android.text.format.DateUtils
import android.util.Log
import android.view.View
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.onClick


class MainActivity : RxActivity() {

    @Inject
    lateinit var cod3ApiInterface: Cod3ApiInterface

    var gettingLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        MyApp.cod3ApiComponent.inject(this)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        0)
        } else {
            getLocation(this)
        }

        relocate.onClick {
            if (!gettingLocation) {
                getLocation(this)
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            0 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    getLocation(this)
                }
            }
        }
    }

    fun getLocation(context: Context) {

        gettingLocation = true

        val rxLocation = RxLocation(context)

        val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)

        rxLocation
                .location()
                .updates(locationRequest)
                .subscribeOn(Schedulers.computation())
                .takeWhile<Location> { it.accuracy < 100 }
                .take(1)
                .flatMap { location -> rxLocation.geocoding().fromLocation(location).toObservable() }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    progress.visibility = View.VISIBLE
                }
                .bindToLifecycle(this)
                .subscribe { location ->
                    address.text = location.getAddressLine(0) + ", " + location.locality + ", " + location.adminArea
                    cod3ApiInterface
                            .getEms(location.latitude, location.longitude, 500)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ response ->
                                gettingLocation = false
                                progress.visibility = View.GONE
                                averageTime.text = getPrettyTime(response)

                            }, { error ->
                                gettingLocation = false
                                progress.visibility = View.GONE
                                averageTime.text = "Error"
                                AlertDialog.Builder(this).setMessage("Error").setPositiveButton("OK", null).show()
                            })

                    cod3ApiInterface
                            .getPolice(location.latitude, location.longitude, 500)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe({ response ->
                                gettingLocation = false
                                progress.visibility = View.GONE
                                averagePoliceTime.text = getPrettyTime(response)

                            }, { error ->
                                gettingLocation = false
                                progress.visibility = View.GONE
                                averagePoliceTime.text = "Error"
                            })
                }
    }

    fun getPrettyTime(response: Response): String {

        Log.d("FOOBAR", response.toString())

        return response.average?.let {
            Log.d("FOOBAR", "not null")
            val foo = (it) * 60 * 1000
             DateUtils.getRelativeTimeSpanString(System.currentTimeMillis()+ foo.toLong(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString()
        } ?: {
             "Now"
        }()
    }
}
