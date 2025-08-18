package net.forestany.mediacollection.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import net.forestany.mediacollection.R
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import net.forestany.mediacollection.MainActivity
import net.forestany.mediacollection.main.GlobalInstance
import net.forestany.mediacollection.main.LanguageRecord
import net.forestany.mediacollection.main.MediaCollectionRecord
import net.forestany.mediacollection.main.Util.errorSnackbar
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.Preference
import net.forestany.mediacollection.main.Util.notifySnackbar

class SettingsActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "SettingsActivity"
    }

    private val glob: GlobalInstance = GlobalInstance.get()
    private lateinit var openFileLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        // default settings
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment(this, findViewById(android.R.id.content), null))
                .commit()
        }

        // settings toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_settings)
        toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_hamburger_menu)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(false) /* standard back/home button */
        supportActionBar?.setDisplayShowTitleEnabled(false)
        //supportActionBar?.title = getString(R.string.title_settings_activity)

        // deactivate standard back button
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                    setResult(MainActivity.RETURN_CODE_RELOAD)
                    finish()
                }
            }
        )

        setupFilePicker()

        Log.v(TAG, "onCreate $TAG")
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.settings_menu, menu)

        // allow showing icons on dropdown toolbar menu
        try {
            if (menu is androidx.appcompat.view.menu.MenuBuilder) {
                val menuBuilder: androidx.appcompat.view.menu.MenuBuilder = menu as androidx.appcompat.view.menu.MenuBuilder
                menuBuilder.setOptionalIconsVisible(true)
            }
            // does not run with release build, so the solution above is enough - @SuppressLint("RestrictedApi") needed
            //val method = menu?.javaClass?.getDeclaredMethod("setOptionalIconsVisible", Boolean::class.javaPrimitiveType)
            //method?.isAccessible = true
            //method?.invoke(menu, true)
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onCreateOptionsMenu method.", view = findViewById(android.R.id.content))
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mI_overwrite_truststore -> {
                overwriteTruststore()

                return true
            }
            R.id.mI_delete_truststore -> {
                deleteTruststore()

                return true
            }
            R.id.mI_reset -> {
                resetData()

                return true
            }
            R.id.mI_factory_reset -> {
                factoryReset()

                return true
            }
        }
        
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment(private val context: Context, private val view: View, private val anchorView: View?) : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
        private val sharedPreferencesHistory = mutableMapOf<String, Any?>()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            // store all shared preferences key values in a map as history
            PreferenceManager.getDefaultSharedPreferences(context).all.forEach {
                sharedPreferencesHistory[it.key] = it.value
                //Log.v(TAG, "${it.key} -> ${it.value.toString()}")
            }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
        }

        override fun onResume() {
            super.onResume()
            preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            super.onPause()
            preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            return when (preference.key) {
                "search_tmdb_api_key" -> {
                    PasswordDialogFragment.newInstance(
                        prefKey = "search_tmdb_api_key",
                        title = getString(R.string.settings_search_tmdb_api_key)
                    ).show(parentFragmentManager, "password_dialog_1")
                    true
                }
                "sync_common_passphrase" -> {
                    PasswordDialogFragment.newInstance(
                        prefKey = "sync_common_passphrase",
                        title = getString(R.string.settings_sync_common_passphrase)
                    ).show(parentFragmentManager, "password_dialog_2")
                    true
                }
                "sync_auth_passphrase" -> {
                    PasswordDialogFragment.newInstance(
                        prefKey = "sync_auth_passphrase",
                        title = getString(R.string.settings_sync_auth_passphrase)
                    ).show(parentFragmentManager, "password_dialog_3")
                    true
                }
                "sync_truststore_password" -> {
                    PasswordDialogFragment.newInstance(
                        prefKey = "sync_truststore_password",
                        title = getString(R.string.settings_sync_truststore_password)
                    ).show(parentFragmentManager, "password_dialog_4")
                    true
                }
                "toolbar_offset" -> {
                    SliderDialogFragment.newInstance(
                        prefKey = "toolbar_offset",
                        title = getString(R.string.settings_general_toolbar_offset),
                        min = 0,
                        max = 100,
                        defaultValue = 50
                    ).show(parentFragmentManager, "slider_dialog_1")
                    true
                }
                "toolbar_wait_autosearch" -> {
                    SliderDialogFragment.newInstance(
                        prefKey = "toolbar_wait_autosearch",
                        title = getString(R.string.settings_general_toolbar_wait_autosearch),
                        min = 0,
                        max = 5000,
                        defaultValue = 2500
                    ).show(parentFragmentManager, "slider_dialog_2")
                    true
                }
                else -> super.onPreferenceTreeClick(preference)
            }
        }

        override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
            if (sharedPreferences == null || key == null) return

            var wrongValue = false
            var exceptionMessage = "Wrong value for settings."
            val value: Any? = sharedPreferences.all[key]

            // do nothing if current value is equal to history value
            if (this.sharedPreferencesHistory[key].toString().contentEquals(value.toString())) return

            //Log.v(TAG, "old $key -> ${this.sharedPreferencesHistory[key].toString()} < - - - > ${value.toString()}")

            // check values
            if (key.contentEquals("general_locale")) {
                // setting is 'de' or 'en'
                if ((value.toString().contentEquals("de")) || (value.toString().contentEquals("en"))) {
                    // restart app with new language setting after 1 second
                    notifySnackbar(message = getString(R.string.settings_language_changed, 5), view = view, anchorView = anchorView)

                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        requireActivity().finish()
                        Runtime.getRuntime().exit(0)
                    }, 5000)
                }
            } else if (key.contentEquals("backup_folder")) {
                // not empty, starts with 'Documents/'
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!value.toString().startsWith("Documents/")) {
                    exceptionMessage = getString(R.string.settings_general_backup_folder_false_start)
                    wrongValue = true
                }
            } else if (key.contentEquals("toolbar_offset")) {
                // not empty, isInt, between 0 and 100
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) < 0) {
                    exceptionMessage = getString(R.string.settings_value_to_low, 0)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) > 100) {
                    exceptionMessage = getString(R.string.settings_value_to_high, 100)
                    wrongValue = true
                }
            } else if (key.contentEquals("toolbar_wait_autosearch")) {
                // not empty, isInt, between 0 and 5000
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) < 0) {
                    exceptionMessage = getString(R.string.settings_value_to_low, 0)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) > 5000) {
                    exceptionMessage = getString(R.string.settings_value_to_high, 5000)
                    wrongValue = true
                }
            } else if (key.contentEquals("poster_desired_width")) {
                // not empty, isInt
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                }
            } else if (key.contentEquals("poster_overview_fixed_height")) {
                // not empty, isInt, between 150 and 3000
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) < 150) {
                    exceptionMessage = getString(R.string.settings_value_to_low, 150)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) > 3000) {
                    exceptionMessage = getString(R.string.settings_value_to_high, 3000)
                    wrongValue = true
                }
            } else if (key.contentEquals("poster_tmdb_url")) {
                // not empty, starts with https://, has 2 placeholders in value
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!value.toString().startsWith("https://")) {
                    exceptionMessage = getString(R.string.settings_value_not_https)
                    wrongValue = true
                } else if (!value.toString().contains("%1")) {
                    exceptionMessage = getString(R.string.settings_value_missing_placeholder_one)
                    wrongValue = true
                } else if (!value.toString().contains("%2")) {
                    exceptionMessage = getString(R.string.settings_value_missing_placeholder_two)
                    wrongValue = true
                }
            } else if (key.contentEquals("poster_movieposterdb_url")) {
                // not empty, starts with https://, has one placeholder in value
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!value.toString().startsWith("https://")) {
                    exceptionMessage = getString(R.string.settings_value_not_https)
                    wrongValue = true
                } else if (!value.toString().contains("%1")) {
                    exceptionMessage = getString(R.string.settings_value_missing_placeholder_one)
                    wrongValue = true
                }
            } else if (
                (key.contentEquals("search_tmdb_url_movies")) ||
                (key.contentEquals("search_tmdb_url_movie_details")) ||
                (key.contentEquals("search_tmdb_url_series")) ||
                (key.contentEquals("search_tmdb_url_series_details"))
            ) {
                // not empty, starts with https://, has 3 placeholders in value
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!value.toString().startsWith("https://")) {
                    exceptionMessage = getString(R.string.settings_value_not_https)
                    wrongValue = true
                } else if (!value.toString().contains("%1")) {
                    exceptionMessage = getString(R.string.settings_value_missing_placeholder_one)
                    wrongValue = true
                } else if (!value.toString().contains("%2")) {
                    exceptionMessage = getString(R.string.settings_value_missing_placeholder_two)
                    wrongValue = true
                } else if (!value.toString().contains("%3")) {
                    exceptionMessage = getString(R.string.settings_value_missing_placeholder_three)
                    wrongValue = true
                }
            } else if (key.contentEquals("search_tmdb_api_key")) {
                // not empty, at least 32 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 32) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 32)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_automatic")) {
                // not empty, isInt
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_server_ip")) {
                // not empty, at least 8 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 8) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 8)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_server_port")) {
                // not empty, isInt, between 1 and 65535
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) < 1) {
                    exceptionMessage = getString(R.string.settings_value_to_low, 1)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) > 65535) {
                    exceptionMessage = getString(R.string.settings_value_to_high, 65535)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_receive_buffer_size")) {
                // not empty, isInt, between 1500 and 32768
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) < 1500) {
                    exceptionMessage = getString(R.string.settings_value_to_low, 1500)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) > 32768) {
                    exceptionMessage = getString(R.string.settings_value_to_high, 32768)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_common_passphrase")) {
                // not empty, at least 36 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 36) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 36)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_auth_user")) {
                // not empty, at least 8 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 8) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 8)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_auth_passphrase")) {
                // not empty, at least 36 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 32) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 32)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_truststore_filename")) {
                // not empty, at least 8 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 8) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 8)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_truststore_password")) {
                // not empty, at least 6 characters long
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (value.toString().length < 6) {
                    exceptionMessage = getString(R.string.settings_value_length_to_low, 6)
                    wrongValue = true
                }
            } else if (key.contentEquals("sync_receive_max_unknown_amount_in_mib")) {
                // not empty, isInt, between 1 and 16
                if (value.toString().isEmpty()) {
                    exceptionMessage = getString(R.string.settings_value_empty)
                    wrongValue = true
                } else if (!net.forestany.forestj.lib.Helper.isInteger(value.toString())) {
                    exceptionMessage = getString(R.string.settings_value_is_not_integer)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) < 1) {
                    exceptionMessage = getString(R.string.settings_value_to_low, 1)
                    wrongValue = true
                } else if (Integer.parseInt(value.toString()) > 16) {
                    exceptionMessage = getString(R.string.settings_value_to_high, 16)
                    wrongValue = true
                }
            }

            // entered value is wrong
            if (wrongValue) {
                // show error snackbar
                errorSnackbar(message = exceptionMessage, view = view, anchorView = anchorView)

                // manually update setting UI component
                when (val preference = findPreference<Preference>(key)) {
                    is EditTextPreference -> {
                        preference.text = sharedPreferencesHistory[key].toString()
                    }
                    is ListPreference -> {
                        preference.value = sharedPreferencesHistory[key].toString()
                    }
                    is Preference -> {
                        // nothing to do for password fields
                    }
                }
            } else {
                // update value is shared preferences history
                sharedPreferencesHistory[key] = value
            }
        }
    }

    private fun overwriteTruststore() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
        }

        openFileLauncher.launch(intent)
    }

    private fun setupFilePicker() {
        openFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // get choosen file
                val clipData = result.data?.clipData
                val uriList = mutableListOf<android.net.Uri>()

                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        uriList.add(clipData.getItemAt(i).uri)
                    }
                } else {
                    result.data?.data?.let { uriList.add(it) }
                }

                // check if .p12 or .bks truststore is chosen
                val truststoreFile = uriList.find { getFileNameFromUri(this, it).endsWith(".p12", true) }
                val truststoreFileBks = uriList.find { getFileNameFromUri(this, it).endsWith(".bks", true) }

                if (truststoreFile != null) {
                    // overwrite .p12 truststore
                    val internalFile = java.io.File(filesDir, GlobalInstance.get().syncTruststoreFilename + ".p12")
                    contentResolver.openInputStream(truststoreFile)?.use { input ->
                        internalFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    notifySnackbar(message = getString(R.string.settings_truststore_p12_overwritten), view = findViewById(android.R.id.content))
                } else if (truststoreFileBks != null) {
                    // overwrite .bks truststore
                    val internalFile = java.io.File(filesDir, GlobalInstance.get().syncTruststoreFilename + ".bks")
                    contentResolver.openInputStream(truststoreFileBks)?.use { input ->
                        internalFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    notifySnackbar(message = getString(R.string.settings_truststore_bks_overwritten), view = findViewById(android.R.id.content))
                } else {
                    errorSnackbar(message = getString(R.string.settings_truststore_not_found), view = findViewById(android.R.id.content))
                }
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: android.net.Uri): String {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && index != -1) {
                return it.getString(index)
            }
        }
        return "unknown"
    }

    private fun deleteTruststore() {
        androidx.appcompat.app.AlertDialog.Builder(this@SettingsActivity, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.settings_delete_truststore))
            .setMessage(getString(R.string.settings_delete_truststore_question))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                var somethingDeleted = false

                // check if .p12 truststore exists at app files directory
                if (net.forestany.forestj.lib.io.File.exists(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".p12")) {
                    // delete .p12 truststore at app files directory
                    net.forestany.forestj.lib.io.File.deleteFile(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".p12")

                    somethingDeleted = true

                    notifySnackbar(message = getString(R.string.settings_delete_truststore_p12_deleted), view = findViewById(android.R.id.content))
                }

                // check if .bks truststore exists at app files directory
                if (net.forestany.forestj.lib.io.File.exists(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".bks")) {
                    // delete .bks truststore at app files directory
                    net.forestany.forestj.lib.io.File.deleteFile(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".bks")

                    somethingDeleted = true

                    notifySnackbar(message = getString(R.string.settings_delete_truststore_bks_deleted), view = findViewById(android.R.id.content))
                }

                if (!somethingDeleted) {
                    errorSnackbar(message = getString(R.string.settings_delete_truststore_none_deleted), view = findViewById(android.R.id.content))
                }
            }
            .setNegativeButton(getString(R.string.text_no), null)
            .show()
    }

    private fun resetData() {
        androidx.appcompat.app.AlertDialog.Builder(this@SettingsActivity, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.settings_reset_data))
            .setMessage(getString(R.string.settings_reset_data_question))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                showProgress()

                Thread {
                    try {
                        // check if sqlite database is available
                        if (!glob.fJGlob.Base.testConnection()) {
                            throw Exception(getString(R.string.settings_reset_data_no_db))
                        }

                        // create mediacollection record and language record instance
                        val o_mediaCollectionRecordInstance = MediaCollectionRecord()
                        val o_languageRecordInstance = LanguageRecord()

                        // truncate mediacollection table
                        if (o_mediaCollectionRecordInstance.truncateTable() < 0) {
                            throw Exception(getString(R.string.settings_reset_data_not_truncate_mediacollection))
                        }

                        // truncate language table
                        if (o_languageRecordInstance.truncateTable() < 0) {
                            throw Exception(getString(R.string.settings_reset_data_not_truncate_languages))
                        }

                        // insert standard languages
                        o_languageRecordInstance.ColumnLanguage = "German"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "English"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "Japanese"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "French"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "Spanish"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "Korean"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "Italian"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "Hindi"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "Russian"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "Swedish"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        o_languageRecordInstance.ColumnLanguage = "Thai"
                        if (o_languageRecordInstance.insertRecord() <= 0) { Log.e(TAG, "Result of insert record is lower equal '0'") }

                        runOnUiThread {
                            notifySnackbar(message = getString(R.string.settings_reset_data_finished), view = findViewById(R.id.main))
                            setResult(MainActivity.RETURN_CODE_RELOAD_AFTER_RESET)
                            finish()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            errorSnackbar(message = "Error: ${e.message ?: "Exception in resetData method."}", view = findViewById(android.R.id.content))
                        }
                    } finally {
                        runOnUiThread {
                            hideProgress()
                        }
                    }
                }.start()
            }
            .setNegativeButton(getString(R.string.text_no), null)
            .show()
    }

    private fun factoryReset() {
        androidx.appcompat.app.AlertDialog.Builder(this@SettingsActivity, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.settings_factory_reset))
            .setMessage(getString(R.string.settings_factory_reset_question))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                setResult(MainActivity.RETURN_CODE_FACTORY_RESET)
                finish()
            }
            .setNegativeButton(getString(R.string.text_no), null)
            .show()
    }

    private fun showProgress() {
        findViewById<View>(R.id.loading_overlay_settings).visibility = View.VISIBLE
    }

    private fun hideProgress() {
        findViewById<View>(R.id.loading_overlay_settings).visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()

        Log.v(TAG, "onStart $TAG")
    }

    override fun onResume() {
        super.onResume()

        Log.v(TAG, "onResume $TAG")
    }

    override fun onPause() {
        super.onPause()

        Log.v(TAG, "onPause $TAG")
    }

    override fun onStop() {
        super.onStop()

        Log.v(TAG, "onStop $TAG")
    }

    override fun onRestart() {
        super.onRestart()

        Log.v(TAG, "onRestart $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.v(TAG, "onDestroy $TAG")
    }
}