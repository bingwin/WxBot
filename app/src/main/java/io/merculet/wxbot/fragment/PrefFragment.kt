package io.merculet.wxbot.fragment

import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.*
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.support.v4.content.ContextCompat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.chenenyu.router.annotation.Route
import io.merculet.core.config.Config
import io.merculet.core.config.Config.ACTION_UPDATE_PREF
import io.merculet.core.config.Config.FOLDER_SHARED_PREFS
import io.merculet.core.config.Config.SETTINGS_INTERFACE_HIDE_ICON
import io.merculet.core.ext.routerWithAnim
import io.merculet.wxbot.R
import io.merculet.core.utils.Utils
import io.merculet.wxbot.util.ext.putExtra
import java.io.File

@Route(value = [Config.ROUTER_FRAGMENT_SETTINGS])
class PrefFragment : PreferenceFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Move old shared preferences to device protected storage if it exists
            val oldPrefDir = "${activity?.applicationInfo?.dataDir}/$FOLDER_SHARED_PREFS"
            val newPrefDir = "${activity?.applicationInfo?.deviceProtectedDataDir}/$FOLDER_SHARED_PREFS"
            try {
                File(oldPrefDir).renameTo(File(newPrefDir))
            } catch (t: Throwable) {
                // Ignore this one
            }
            preferenceManager.setStorageDeviceProtected()
        }

        if (arguments != null) {
            val preferencesResId = arguments!!.getInt(Config.FRAGMENT_SETTINGS_ARG_PREF_RES)
            val preferencesName = arguments!!.getString(Config.FRAGMENT_SETTINGS_ARG_PREF_NAME)
            preferenceManager.sharedPreferencesName = preferencesName
            addPreferencesFromResource(preferencesResId)
        }

        val preference = findPreference("settings_wx_info") as Preference
        preference.setOnPreferenceClickListener {
            routerWithAnim(Config.ROUTER_ACTIVITY_WX_INFO).go(this)
            true
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return super.onCreateView(inflater, container, savedInstanceState)?.apply {
            setBackgroundColor(ContextCompat.getColor(activity!!, R.color.card_background))
        }
    }

    override fun onStart() {
        super.onStart()
        preferenceManager.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        preferenceManager.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        super.onStop()
    }

    override fun onSharedPreferenceChanged(preferences: SharedPreferences, key: String) {
        when (key) {
            SETTINGS_INTERFACE_HIDE_ICON -> {
                // Hide/Show the icon as required.
                try {
                    val hide = preferences.getBoolean(SETTINGS_INTERFACE_HIDE_ICON, false)
                    val newState = if (hide) COMPONENT_ENABLED_STATE_DISABLED else COMPONENT_ENABLED_STATE_ENABLED
                    val className = "${Utils.getPackageName(activity)}.MainActivityAlias"
                    val componentName = ComponentName(Utils.getPackageName(activity), className)
                    activity!!.packageManager.setComponentEnabledSetting(componentName, newState, DONT_KILL_APP)
                } catch (t: Throwable) {
                    Log.e(TAG, "Cannot hide icon: $t")
                    Toast.makeText(activity, t.localizedMessage, Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                val value = preferences.all[key] ?: return
                activity?.sendBroadcast(Intent(ACTION_UPDATE_PREF).apply {
                    putExtra(Config.PROVIDER_PREF_KEY, key)
                    putExtra(Config.PROVIDER_PREF_VALUE, value)
                })
            }
        }
    }

    companion object {
        private const val TAG = "PrefFragment"

        /*private const val ARG_PREF_RES = "preferencesResId"
        private const val ARG_PREF_NAME = "preferencesFileName"

        fun newInstance(preferencesResId: Int, preferencesName: String): PrefFragment {
            val fragment = PrefFragment()
            fragment.arguments = Bundle().apply {
                putInt(ARG_PREF_RES, preferencesResId)
                putString(ARG_PREF_NAME, preferencesName)
            }
            return fragment
        }*/
    }
}