package es.jarroyo.raspberrypiledapp

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.android.things.pio.Gpio
import com.google.android.things.pio.PeripheralManager
import com.google.firebase.firestore.*
import java.io.IOException


/**
 * Skeleton of an Android Things activity.
 *
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 *
 * <pre>{@code
 * val service = PeripheralManagerService()
 * val mLedGpio = service.openGpio("BCM6")
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
 * mLedGpio.value = true
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 *
 */
class MainActivity : Activity() {
    private val TAG = MainActivity::class.java.simpleName
    private var mLedGpio: Gpio? = null


    private var db: FirebaseFirestore? = null
    private var mCurrentStatusLed: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Starting ButtonActivity")
        val pioService = PeripheralManager.getInstance()
        try {
            Log.i(TAG, "Configuring GPIO pins")
            mLedGpio = pioService.openGpio(BoardDefaults.getGPIOForLED())
            mLedGpio!!.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW)
        } catch (e: IOException) {
            Log.e(TAG, "Error configuring GPIO pins", e)
        }
        db = FirebaseFirestore.getInstance()
        getStatusFromFirestore()

    }

    fun getStatusFromFirestore() {
        val docRef = db!!.collection("LED").document("status")
        docRef.addSnapshotListener(object : EventListener<DocumentSnapshot> {
            override fun onEvent(documentSnapshot: DocumentSnapshot?, p1: FirebaseFirestoreException?) {
                mCurrentStatusLed = documentSnapshot!!.data!!.get("isEnabled") as Boolean
                if (mCurrentStatusLed) {
                    startLed()
                } else {
                    stopLedBlinker()
                }
            }

        })
    }

    fun makeLedBlinker() {
        val ledBlinker = Runnable {
            while (true) {
                // Turn on the LED
                setLedValue(true)
                sleep(1000)
                // Turn off the LED
                setLedValue(false)
                sleep(1000)
            }
        }
        Thread(ledBlinker).start()
    }

    fun stopLedBlinker() {
        setLedValue(false)
    }

    fun startLed() {
        setLedValue(true)
    }

    private fun sleep(milliseconds: Int) {
        try {
            Thread.sleep(milliseconds.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

    }

    /**
     * Update the value of the LED output.
     */
    private fun setLedValue(value: Boolean) {
        try {
            mLedGpio!!.setValue(value)
        } catch (e: IOException) {
            Log.e(TAG, "Error updating GPIO value", e)
        }

    }

    override fun onStop() {
        super.onStop()
        if (mLedGpio != null) {
            try {
                mLedGpio!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing LED GPIO", e)
            } finally {
                mLedGpio = null
            }
            mLedGpio = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mLedGpio != null) {
            try {
                mLedGpio!!.close()
            } catch (e: IOException) {
                Log.e(TAG, "Error closing LED GPIO", e)
            } finally {
                mLedGpio = null
            }
            mLedGpio = null
        }
    }
}
