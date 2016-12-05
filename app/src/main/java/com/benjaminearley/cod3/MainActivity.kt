package com.benjaminearley.cod3

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.Toolbar
import android.text.format.DateUtils
import android.view.View
import com.benjaminearley.cod3.Cod3ApiModule.Cod3ApiInterface
import com.google.android.gms.location.LocationRequest
import com.patloew.rxlocation.RxLocation
import com.trello.rxlifecycle2.kotlin.bindToLifecycle
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.onClick
import javax.inject.Inject


class MainActivity : RxActivity() {

    @Inject
    lateinit var cod3ApiInterface: Cod3ApiInterface
    lateinit var rxLocation: RxLocation

    var gettingLocation = false

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme_NoActionBar)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        MyApp.cod3ApiComponent.inject(this)
        rxLocation = RxLocation(this)

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    0)
        } else {
            getLocation(rxLocation)
        }

        relocate.onClick {
            if (!gettingLocation) {
                getLocation(rxLocation)
            }
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            0 -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    getLocation(rxLocation)
                }
            }
        }
    }

    fun getLocation(rxLocation: RxLocation) {

        gettingLocation = true

        val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)

        rxLocation
                .location()
                .updates(locationRequest)
                .subscribeOn(Schedulers.computation())
                .filter<Location> { it.accuracy < 100 }
                .take(1)
                .flatMap { location -> rxLocation.geocoding().fromLocation(location).toObservable() }
                .doOnNext { location ->
                    address.text = "${location.getAddressLine(0)}, ${location.locality}, ${location.adminArea}"
                }
                .flatMap { location ->
                    Observable.zip(
                            cod3ApiInterface
                                    .getEms(location.latitude, location.longitude, 500)
                                    .subscribeOn(Schedulers.io())
                                    .toObservable()
                                    .map { response -> Sum.kindA<Response, Throwable>(response) as Sum<Response, Throwable> }
                                    .onErrorResumeNext { error: Throwable ->
                                        Observable.just(Sum.kindB<Response, Throwable>(error))
                                    },
                            cod3ApiInterface
                                    .getPolice(location.latitude, location.longitude, 500)
                                    .subscribeOn(Schedulers.io())
                                    .toObservable()
                                    .map { response -> Sum.kindA<Response, Throwable>(response) as Sum<Response, Throwable> }
                                    .onErrorResumeNext { error: Throwable ->
                                        Observable.just(Sum.kindB<Response, Throwable>(error))
                                    },
                            BiFunction { x: Sum<Response, Throwable>, y: Sum<Response, Throwable> -> Pair(x, y) })
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe {
                    progress.visibility = View.VISIBLE
                }
                .bindToLifecycle(this)
                .subscribe({ pair ->
                    val (ems, police) = pair

                    gettingLocation = false
                    progress.visibility = View.GONE

                    ems.with({
                        averageTime.text = getPrettyTime(it)
                    }, {
                        averageTime.text = getString(R.string.error)
                    })

                    police.with({
                        averagePoliceTime.text = getPrettyTime(it)
                    }, {
                        averagePoliceTime.text = getString(R.string.error)
                    })

                }, { error ->
                    gettingLocation = false
                    averageTime.text = getString(R.string.error)
                    averagePoliceTime.text = getString(R.string.error)
                    AlertDialog.Builder(this).setMessage("Unable to get location.").setPositiveButton("OK", null).show()
                })
    }

    fun getPrettyTime(response: Response): String {

        return response.average?.let {
            val foo = (it) * 60 * 1000
            DateUtils.getRelativeTimeSpanString(System.currentTimeMillis() + foo.toLong(), System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS).toString()
        } ?: {
            "Now"
        }()
    }
}
