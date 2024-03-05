package com.infobip.webrtc.ui.internal.delegate

import android.Manifest
import android.content.Context
import android.content.Context.TELEPHONY_SERVICE
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat

internal interface PhoneStateDelegate {

    /**
     * If the value is true it is needed to request [Manifest.permission.READ_PHONE_STATE] permission before calling any another delegate's function.
     */
    val requiresPhoneStatePermission: Boolean

    /**
     * Returns current phone state defined by integer:
     * -1 = Could not obtain call state because of missing [Manifest.permission.READ_PHONE_STATE] permission on API > 31
     * 0 = [TelephonyManager.CALL_STATE_IDLE] - there is no cellular call
     * 1 = [TelephonyManager.CALL_STATE_RINGING] - there is ringing cellular call
     * 2 = [TelephonyManager.CALL_STATE_OFFHOOK] - there is outgoing or ongoing cellular call
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE, conditional = true)
    fun getState(): Int

    /**
     * Registers phone state listener.
     * On API > 31 [Manifest.permission.READ_PHONE_STATE] permission is needed to register listener.
     *
     * @param onStateChanged listener returns current phone state defined by integer:
     * -1 = Could not obtain call state because of missing [Manifest.permission.READ_PHONE_STATE] permission on API > 31
     * 0 = [TelephonyManager.CALL_STATE_IDLE] - there is no cellular call
     * 1 = [TelephonyManager.CALL_STATE_RINGING] - there is ringing cellular call
     * 2 = [TelephonyManager.CALL_STATE_OFFHOOK] - there is outgoing or ongoing cellular call
     * @return true if listener was registered, false otherwise
     */
    @RequiresPermission(Manifest.permission.READ_PHONE_STATE, conditional = true)
    fun registerStateListener(onStateChanged: (Int) -> Unit): Boolean

    /**
     * Unregisters previously registered listener.
     */
    fun unregisterStateListener()
}

internal object PhoneStateDelegateFactory {

    private var delegate: PhoneStateDelegate? = null

    fun getPhoneStateDelegate(context: Context): PhoneStateDelegate {
        return delegate ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Api31PhoneStateDelegate(context)
        } else {
            Api30PhoneStateDelegate(context)
        }.also { delegate = it }
    }

}

private class Api31PhoneStateDelegate(
    private val context: Context
): PhoneStateDelegate {

    override val requiresPhoneStatePermission: Boolean = true
    private val telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager
    private var callback: StateCallback? = null

    @RequiresApi(Build.VERSION_CODES.S)
    override fun getState(): Int {
        return if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            telephonyManager.callStateForSubscription
        } else {
            -1
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun registerStateListener(onStateChanged: (Int) -> Unit): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            callback = StateCallback { onStateChanged(it) }
            telephonyManager.registerTelephonyCallback(context.mainExecutor, StateCallback { onStateChanged(it) })
            return true
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun unregisterStateListener() {
        callback?.let { telephonyManager.unregisterTelephonyCallback(it) }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private class StateCallback(
        private val onStateChanged: (Int) -> Unit
    ) : TelephonyCallback(), TelephonyCallback.CallStateListener {
        override fun onCallStateChanged(state: Int) {
            onStateChanged(state)
        }
    }

}

@Suppress("DEPRECATION")
private class Api30PhoneStateDelegate(
    context: Context
): PhoneStateDelegate {

    override val requiresPhoneStatePermission: Boolean = false
    private val telephonyManager = context.getSystemService(TELEPHONY_SERVICE) as TelephonyManager

    override fun getState(): Int = telephonyManager.callState

    override fun registerStateListener(onStateChanged: (Int) -> Unit): Boolean {
        telephonyManager.listen(object : PhoneStateListener() {
            @Deprecated("Deprecated in Java")
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                onStateChanged(state)
            }
        }, PhoneStateListener.LISTEN_CALL_STATE)
        return true
    }

    override fun unregisterStateListener() {
    }

}