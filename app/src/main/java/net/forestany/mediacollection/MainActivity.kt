package net.forestany.mediacollection

// android studio: collapse all methods: ctrl + shift + * and then 1 on numpad
// android studio: expand all with ctrl + shift + numpad + several times

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.InputFilter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.os.LocaleListCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.forestany.mediacollection.item.ItemViewActivity
import net.forestany.mediacollection.main.FilterItem
import net.forestany.mediacollection.main.FilterItemAdapter
import net.forestany.mediacollection.main.GlobalInstance
import net.forestany.mediacollection.main.ItemBean
import net.forestany.mediacollection.main.JSONMediaCollection
import net.forestany.mediacollection.main.LanguageRecord
import net.forestany.mediacollection.main.MediaCollectionRecord
import net.forestany.mediacollection.main.RecyclerViewAdapter
import net.forestany.mediacollection.main.SortItem
import net.forestany.mediacollection.main.SortItemAdapter
import net.forestany.mediacollection.main.Util.errorSnackbar
import net.forestany.mediacollection.main.Util.notifySnackbar
import net.forestany.mediacollection.settings.SettingsActivity
import org.bouncycastle.jce.provider.BouncyCastleProvider
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerViewAdapter: RecyclerViewAdapter
    private lateinit var floatingActionButton: FloatingActionButton

    private lateinit var searchIcon: ImageView
    private lateinit var searchBackIcon: ImageView
    private lateinit var searchEditText: EditText
    private var searchJob: Job? = null

    private lateinit var databaseInstance: net.forestany.mediacollection.main.Database
    private lateinit var jsonSchema: String
    private lateinit var openMultipleFilesLauncher: ActivityResultLauncher<Intent>
    private var searchFilter = ""
    private var filters = mutableMapOf<String, String>()
    private lateinit var filterColumns: MutableMap<String, String>
    private var filterBadgeTextView: TextView? = null
    private var sorts = mutableMapOf<String, Boolean>()
    private lateinit var sortColumns: MutableMap<String, String>
    private var sortBadgeTextView: TextView? = null
    private var skipFiltersSortsReset = false
    private var isLoading = false
    private var dbEmpty = true

    companion object {
        private const val TAG = "MainActivity"

        const val RETURN_CODE_RELOAD = 6158
        const val RETURN_CODE_RELOAD_AFTER_RESET = 5893
        const val RETURN_CODE_INSERTED_AND_RELOAD = 7425
        const val RETURN_CODE_UPDATED_AND_RELOAD = 1286
        const val RETURN_CODE_DELETED_AND_RELOAD = 9526
        const val RETURN_CODE_FACTORY_RESET = 7894

        const val SETTINGS_STANDARD_BKP_FOLDER = "Documents/mediacollection_bkp"
        const val SETTINGS_STANDARD_TOOLBAR_OFFSET = "36"
        const val SETTINGS_STANDARD_TOOLBAR_WAIT_AUTOSEARCH = "750"
        const val SETTINGS_STANDARD_POSTER_DESIRED_WIDTH = "342"
        const val SETTINGS_STANDARD_POSTER_OVERVIEW_FIXED_HEIGHT = "800"
        const val SETTINGS_STANDARD_POSTER_TMDB_URL = "https://image.tmdb.org/t/p/w%1%2"
        const val SETTINGS_STANDARD_POSTER_MOVIEPOSTERDB_URL = "https://www.movieposterdb.com/search?q=%1&imdb=0"
        const val SETTINGS_STANDARD_TMDB_URL_MOVIES = "https://api.themoviedb.org/3/search/movie?query=%1&language=%2&api_key=%3"
        const val SETTINGS_STANDARD_TMDB_URL_MOVIE_DETAILS = "https://api.themoviedb.org/3/movie/%1?append_to_response=credits&language=%2&api_key=%3"
        const val SETTINGS_STANDARD_TMDB_URL_SERIES = "https://api.themoviedb.org/3/search/tv?query=%1&language=%2&api_key=%3"
        const val SETTINGS_STANDARD_TMDB_URL_SERIES_DETAILS = "https://api.themoviedb.org/3/tv/%1?append_to_response=credits&language=%2&api_key=%3"
        const val SETTINGS_STANDARD_TMDB_API_KEY = "1234567890abcdefghij1234567890ab"
        const val SETTINGS_STANDARD_SYNC_AUTOMATIC = "0"
        const val SETTINGS_STANDARD_SYNC_SERVER_IP = "123.456.789.012"
        const val SETTINGS_STANDARD_SYNC_SERVER_PORT = "12345"
        const val SETTINGS_STANDARD_SYNC_RECEIVE_BUFFER_SIZE = "16384"
        const val SETTINGS_STANDARD_SYNC_COMMON_PASSPHRASE = "1234567890abcdefghij1234567890abcdef"
        const val SETTINGS_STANDARD_SYNC_AUTH_USER = "auth user"
        const val SETTINGS_STANDARD_SYNC_AUTH_PASSPHRASE = "1234567890abcdefghij1234567890abcdef"
        const val SETTINGS_STANDARD_SYNC_TRUSTSTORE_FILENAME = "truststore"
        const val SETTINGS_STANDARD_SYNC_TRUSTSTORE_PASSWORD = "123456"
        const val SETTINGS_STANDARD_SYNC_RECEIVE_MAX_UNKNOWN_AMOUNT_IN_MIB = "1"
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        when (it.resultCode) {
            RESULT_OK -> {

            }

            RETURN_CODE_RELOAD -> {
                reload()
            }

            RETURN_CODE_INSERTED_AND_RELOAD -> {
                notifySnackbar(message = getString(R.string.main_return_record_inserted), view = findViewById(android.R.id.content))
                reload()
            }

            RETURN_CODE_UPDATED_AND_RELOAD -> {
                notifySnackbar(message = getString(R.string.main_return_record_updated), view = findViewById(android.R.id.content))
                reload()
            }

            RETURN_CODE_DELETED_AND_RELOAD -> {
                notifySnackbar(message = getString(R.string.main_return_record_deleted), view = findViewById(android.R.id.content))
                reload()
            }

            RETURN_CODE_RELOAD_AFTER_RESET -> {
                recyclerViewAdapter.clear()
            }

            RETURN_CODE_FACTORY_RESET -> {
                deactivateSearchModeWithoutRequery()
                showProgress()

                // reset shared preferences
                getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).edit(commit = true) { clear() }
                // delete database file
                net.forestany.forestj.lib.io.File.deleteFile(getDatabasePath("mediacollection.db").path)

                // check if .p12 truststore exists at app files directory
                if (net.forestany.forestj.lib.io.File.exists(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".p12")) {
                    // delete .p12 truststore at app files directory
                    net.forestany.forestj.lib.io.File.deleteFile(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".p12")
                }

                // check if .bks truststore exists at app files directory
                if (net.forestany.forestj.lib.io.File.exists(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".bks")) {
                    // delete .bks truststore at app files directory
                    net.forestany.forestj.lib.io.File.deleteFile(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".bks")
                }

                // restart app
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    hideProgress()
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                    Runtime.getRuntime().exit(0)
                }, 5000)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // set BouncyCastleProvider as first security provider for handling TLS within sockets
        java.security.Security.insertProviderAt(BouncyCastleProvider(), 1)
        // init forestj logging
        initLogging()

        // default settings
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        try {
            // create search instance
            GlobalInstance.get().searchInstance = net.forestany.mediacollection.search.Search(this)
            // load JSONMediaCollection.json schema from assets folder
            jsonSchema = net.forestany.mediacollection.search.Search.loadSchemaFromAssets(this, "JSONMediaCollection.json")

            // init layout element variables
            swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
            recyclerView = findViewById(R.id.recyclerView)
            floatingActionButton = findViewById(R.id.addActionButton)

            // swipe refresh layout to top listener
            swipeRefreshLayout.setOnRefreshListener {
                if (!isLoading) {
                    reload()
                }

                swipeRefreshLayout.isRefreshing = false
            }

            // settings for recycler view
            recyclerViewAdapter = RecyclerViewAdapter()
            recyclerViewAdapter.delegate = object : RecyclerViewAdapter.RecyclerViewAdapterDelegate {
                override fun onLoadMore() {
                    if (!isLoading) {
                        loadMore()
                    }
                }

                override fun onClickItem(itemBean: ItemBean) {
                    if (!isLoading) {
                        onItemClicked(itemBean.uuid)
                    }
                }
            }

            recyclerView.layoutManager = GridLayoutManager(this, 2)
            recyclerView.adapter = recyclerViewAdapter

            // settings toolbar
            val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar_main)
            toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_hamburger_menu)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(false) /* standard back/home button */
            supportActionBar?.setDisplayShowTitleEnabled(false)
            //supportActionBar?.title = getString(R.string.title_main_activity)

            // toolbar keyboard magic to align toolbar position while showing input keyboard
            window.decorView.rootView.observeKeyboard { visible, height ->
                val toolbarOffset = if (visible) height - this@MainActivity.dpToPx(GlobalInstance.get().toolbarOffset) else 0
                toolbar.translationY = -toolbarOffset.toFloat()
            }

            // set ui for toolbar search settings
            searchIcon = findViewById(R.id.search_icon)
            searchBackIcon = findViewById(R.id.search_back_icon)
            searchEditText = findViewById(R.id.search_edit_text)

            // toolbar search icon click listener
            searchIcon.setOnClickListener {
                if (!isLoading) {
                    toggleSearchMode(true)
                }
            }

            // toolbar search back icon click listener
            searchBackIcon.setOnClickListener {
                if (!isLoading) {
                    toggleSearchMode(false)
                }
            }

            // only allow some strict defined characters to be allowed to enter on the toolbar search edittext
            searchEditText.filters = arrayOf(InputFilter { source, start, end, _, _, _ ->
                for (i in start until end) {
                    val c = source[i]
                    // convert char to String for regex matching
                    if (!c.toString().matches(Regex("[a-zA-Z0-9öäüÖÄÜ =!.\\-+,;:_?ß]*"))) {
                        return@InputFilter "" // reject input
                    }
                }
                null // accept input
            })

            // execute search after the edittext value has not changed a configured amount of milliseconds in toolbar and that it is not empty
            searchEditText.doAfterTextChanged {
                // cancel current job, previous character input
                searchJob?.cancel()

                // start job to wait configured amount of milliseconds and then do the automatic search
                searchJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(GlobalInstance.get().toolbarWaitAutosearch.toLong())
                    val query = it.toString()

                    if (query.isNotEmpty()) {
                        onSearchQuerySettled(query)
                    }
                }
            }

            // deactivate standard back button
            onBackPressedDispatcher.addCallback(
                this,
                object : androidx.activity.OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                    }
                }
            )

            // floating action button onClick listener
            floatingActionButton.setOnClickListener {
                if (!isLoading) {
                    val intent = Intent(this, ItemViewActivity::class.java)
                    launcher.launch(intent)
                }
            }

            initSettings()

            // filter columns must be set with initSettings method
            if (this.filterColumns.isEmpty()) {
                errorSnackbar(message = getString(R.string.main_no_filter_columns), view = findViewById(android.R.id.content))
            }

            // sort columns must be set with initSettings method
            if (this.sortColumns.isEmpty()) {
                errorSnackbar(message = getString(R.string.main_no_sort_columns), view = findViewById(android.R.id.content))
            }

            setupFilePicker(this)

            // restart all settings of app
            //getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).edit(commit = true) { clear() }
            //net.forestany.forestj.lib.io.File.deleteFile(getDatabasePath("mediacollection.db").path)

            // create database instance
            databaseInstance = net.forestany.mediacollection.main.Database(getDatabasePath("mediacollection.db").path, cacheDir.absolutePath, this)

            // delete records with deleted timestamp older than 30 days
            for (o_mediaCollectionRecord in MediaCollectionRecord().getRecords(true)) {
                if ((o_mediaCollectionRecord.ColumnDeleted != null) && (o_mediaCollectionRecord.ColumnDeleted.isBefore(java.time.LocalDateTime.now().withNano(0).minusDays(30)))) {
                    o_mediaCollectionRecord.deleteRecord()
                } else {
                    // set flag for auto sync
                    dbEmpty = false
                }
            }

            // check for mediacollection backup folder
            ensureMediaCollectionBkpFolderExists(this)

            // load first data entries
            reload()
        } catch (e: Exception) {
            showProgress()
            errorSnackbar(message = e.message ?: "Exception in onCreate method.", view = findViewById(R.id.main))
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                finishAffinity()
                exitProcess(0)
            }, 15000)
        }

        Log.v(TAG, "onCreate $TAG")
    }

    private fun ensureMediaCollectionBkpFolderExists(context: Context) {
        val fileName = "_do_not_delete_these_files.txt"

        if (!fileExists(context, fileName)) {
            // set file destination settings
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, GlobalInstance.get().backupFolder)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            // create file
            context.contentResolver.insert(collection, values)?.let { uri ->
                // write content to file
                context.contentResolver.openOutputStream(uri, "w")?.use { outputStream ->
                    outputStream.write("This folder is used by the app to store backup data. Please do not delete.".toByteArray())
                } ?: throw Exception(getString(R.string.main_exception_open_output_stream))

                // update file IS_PENDING metadata
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } ?: throw Exception(getString(R.string.main_exception_contentresolver_insert))
        }
    }

    private fun fileExists(context: Context, fileName: String): Boolean {
        // look for file
        context.contentResolver
            .query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                arrayOf("${GlobalInstance.get().backupFolder}/", "%$fileName%"),
                null
            )
            ?.use { cursor ->
                // use amount of found items
                return cursor.count > 0
            }

        return false
    }

    private fun initLogging() {
        net.forestany.forestj.lib.Global.get().resetLog()

        val o_loggingConfigAll = net.forestany.forestj.lib.LoggingConfig()
        o_loggingConfigAll.level = java.util.logging.Level.OFF
        //o_loggingConfigAll.level = java.util.logging.Level.SEVERE
        //o_loggingConfigAll.level = java.util.logging.Level.WARNING
        //o_loggingConfigAll.level = java.util.logging.Level.INFO
        //o_loggingConfigAll.level = java.util.logging.Level.CONFIG
        //o_loggingConfigAll.level = java.util.logging.Level.FINE
        //o_loggingConfigAll.level = java.util.logging.Level.FINER
        //o_loggingConfigAll.level = java.util.logging.Level.FINEST
        //o_loggingConfigAll.level = java.util.logging.Level.ALL
        o_loggingConfigAll.useConsole = true

        o_loggingConfigAll.consoleLevel = java.util.logging.Level.OFF
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.SEVERE
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.WARNING
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.INFO
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.CONFIG
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.FINE
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.FINER
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.FINEST
        //o_loggingConfigAll.consoleLevel = java.util.logging.Level.ALL

        //o_loggingConfigAll.useFile = true
        //o_loggingConfigAll.fileLevel = java.util.logging.Level.SEVERE
        //o_loggingConfigAll.filePath = "C:\\Users\\Public\\Documents\\"
        //o_loggingConfigAll.fileLimit = 1000000 // ~ 1.0 MB
        //o_loggingConfigAll.fileCount = 25
        o_loggingConfigAll.loadConfig()

        net.forestany.forestj.lib.Global.get().by_logControl = net.forestany.forestj.lib.Global.OFF

        //net.forestany.forestj.lib.Global.get().by_logControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO).toByte()
        net.forestany.forestj.lib.Global.get().by_internalLogControl = net.forestany.forestj.lib.Global.OFF
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = net.forestany.forestj.lib.Global.SEVERE
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG + net.forestany.forestj.lib.Global.FINE + net.forestany.forestj.lib.Global.FINER).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG + net.forestany.forestj.lib.Global.FINE + net.forestany.forestj.lib.Global.FINER + net.forestany.forestj.lib.Global.FINEST).toByte()
        //net.forestany.forestj.lib.Global.get().by_internalLogControl = (net.forestany.forestj.lib.Global.SEVERE + net.forestany.forestj.lib.Global.WARNING + net.forestany.forestj.lib.Global.INFO + net.forestany.forestj.lib.Global.CONFIG + net.forestany.forestj.lib.Global.FINE + net.forestany.forestj.lib.Global.FINER + net.forestany.forestj.lib.Global.FINEST + net.forestany.forestj.lib.Global.MASS).toByte()
    }

    @SuppressLint("RestrictedApi")
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)

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

        // create filter menu item badge
        val menuItemFilter = menu!!.findItem(R.id.mI_filter)
        val filterActionView = View.inflate(this, R.layout.menu_item_filter, null)
        menuItemFilter?.actionView = filterActionView
        filterBadgeTextView = filterActionView.findViewById(R.id.filterBadge)
        filterActionView.setOnClickListener {
            onOptionsItemSelected(menuItemFilter)
        }
        updateBadge(filterBadgeTextView, filters.size)

        // create sort menu item badge
        val menuItemSort = menu.findItem(R.id.mI_sort)
        val sortActionView = View.inflate(this, R.layout.menu_item_sort, null)
        menuItemSort?.actionView = sortActionView
        sortBadgeTextView = sortActionView.findViewById(R.id.sortBadge)
        sortActionView.setOnClickListener {
            onOptionsItemSelected(menuItemSort)
        }
        updateBadge(sortBadgeTextView, sorts.size)

        return true
    }

    private fun toggleSearchMode(enabled: Boolean) {
        if (enabled) {
            // enable back icon button and search edittext
            searchIcon.visibility = View.GONE
            searchBackIcon.visibility = View.VISIBLE
            searchEditText.visibility = View.VISIBLE

            // focus edittext and open input keyboard
            searchEditText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(searchEditText, InputMethodManager.SHOW_IMPLICIT)
        } else {
            // enable search icon button and hide back icon button and hide search edittext
            searchBackIcon.visibility = View.GONE
            searchEditText.visibility = View.GONE
            searchIcon.visibility = View.VISIBLE

            // truncate edittext value
            searchEditText.setText("")
            // close input keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)

            // call method for doing some more logic when we hiding the search input in toolbar
            onSearchQueryHidden()
        }
    }

    private fun deactivateSearchModeWithoutRequery() {
        // enable search icon button and hide back icon button and hide search edittext
        searchBackIcon.visibility = View.GONE
        searchEditText.visibility = View.GONE
        searchIcon.visibility = View.VISIBLE

        // truncate edittext value and search filter
        searchEditText.setText("")
        searchFilter = ""
        // close input keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    private fun onSearchQuerySettled(query: String) {
        // save entered text as search query for filter and reload recycler view list
        searchFilter = query
        skipFiltersSortsReset = true
        reload()
    }

    private fun onSearchQueryHidden() {
        // truncate search query for filter and reload recycler view list
        searchFilter = ""
        skipFiltersSortsReset = true
        reload()
    }

    private fun View.observeKeyboard(onKeyboardVisibilityChanged: (Boolean, Int) -> Unit) {
        // logic to change toolbar height position, because it is necessary if we show input keyboard
        this.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            this.getWindowVisibleDisplayFrame(r)
            val screenHeight = this.rootView.height
            val keypadHeight = screenHeight - r.bottom
            onKeyboardVisibilityChanged(keypadHeight > screenHeight * 0.10, keypadHeight)
        }
    }

    private fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun updateBadge(textview: TextView?, count: Int) {
        textview?.let { badge ->
            if (count > 0) {
                badge.text = count.toString()
                badge.visibility = View.VISIBLE
            } else {
                badge.visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (!isLoading) {
            when (item.itemId) {
                R.id.mI_filter -> {
                    onFilterClicked()

                    return true
                }

                R.id.mI_sort -> {
                    onSortClicked()

                    return true
                }

                R.id.mI_sync -> {
                    onSyncClicked()

                    return true
                }

                R.id.mI_export -> {
                    onExportClicked()

                    return true
                }

                R.id.mI_import -> {
                    onImportClicked()

                    return true
                }

                R.id.mI_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    launcher.launch(intent)

                    return true
                }

                R.id.mI_statistic -> {
                    onStatisticClicked()

                    return true
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun onItemClicked(uuid: String) {
        val intent = Intent(this, ItemViewActivity::class.java)
        intent.putExtra("UUID", uuid)
        launcher.launch(intent)
    }

    private fun onFilterClicked() {
        val dialog = BottomSheetDialog(this)
        val view = View.inflate(this, R.layout.bottom_sheet_dialog_filter, null)

        val spinnerColumn = view.findViewById<Spinner>(R.id.spinnerFilterColumns)

        val spinnerColumnList = mutableListOf<String>()

        for (foo in filterColumns) {
            if (foo.value !in filters) {
                spinnerColumnList.add(foo.key)
            }
        }

        val adapterSpinnerColumn = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerColumnList) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val viewFoo = super.getDropDownView(position, convertView, parent) as TextView
                viewFoo.setTextColor(getColor(R.color.colorOnBackground))
                return viewFoo
            }
        }
        adapterSpinnerColumn.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColumn.adapter = adapterSpinnerColumn

        val filterItemValueEdittext = view.findViewById<EditText>(R.id.filterValue)
        val allowedCharsRegex = "[a-zA-Z0-9öäüÖÄÜ< >=!.\\-+,;:_?ß]*"

        val filter = InputFilter { source, start, end, _, _, _ ->
            for (i in start until end) {
                val c = source[i]
                // convert char to String for regex matching
                if (!c.toString().matches(Regex(allowedCharsRegex))) {
                    return@InputFilter "" // reject input
                }
            }
            null // accept input
        }

        filterItemValueEdittext.filters = arrayOf(filter)

        val addFilterItemButton = view.findViewById<ImageButton>(R.id.addFilterItemButton)
        addFilterItemButton.setOnClickListener {
            val selectedColumnSpinner = spinnerColumn.selectedItem.toString()
            val selectedColumn = filterColumns[selectedColumnSpinner]
            val filterValue = filterItemValueEdittext.text.toString()

            if ((selectedColumn.isNullOrBlank()) || (filterValue.isBlank())) {
                errorSnackbar(message = getString(R.string.main_choose_filter), view = findViewById(android.R.id.content), anchorView = findViewById(R.id.swipeRefreshLayout), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            } else {
                filters += selectedColumn to filterValue
                updateBadge(filterBadgeTextView, filters.size)
                dialog.dismiss()
                notifySnackbar(message = getString(R.string.main_filter_chosen, filterValue, selectedColumn), view = findViewById(android.R.id.content))
                skipFiltersSortsReset = true
                reload()
            }
        }

        val filterItems = mutableListOf<FilterItem>()

        for (filterItem in filters) {
            filterItems.add(FilterItem(filterItem.key, sortColumns.entries.find { it.value == filterItem.key }?.key ?: "null", filterItem.value))
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewFilterItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = FilterItemAdapter(filterItems) { selectedFilterItem ->
            val foo = mutableMapOf<String, String>()

            for (bar in filters) {
                if (!bar.key.contentEquals(selectedFilterItem.name)) {
                    foo += bar.key to bar.value
                }
            }

            filters = foo

            updateBadge(filterBadgeTextView, filters.size)
            dialog.dismiss()
            notifySnackbar(message = getString(R.string.main_filter_removed, selectedFilterItem.displayName), view = findViewById(android.R.id.content))
            skipFiltersSortsReset = true
            reload()
        }

        dialog.setContentView(view)

        dialog.show()
    }

    private fun onSortClicked() {
        val dialog = BottomSheetDialog(this)
        val view = View.inflate(this, R.layout.bottom_sheet_dialog_sort, null)

        val spinnerColumn = view.findViewById<Spinner>(R.id.spinnerSortColumns)

        val spinnerColumnList = mutableListOf<String>()

        for (foo in sortColumns) {
            if (foo.value !in sorts) {
                spinnerColumnList.add(foo.key)
            }
        }

        val adapterSpinnerColumn = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, spinnerColumnList) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val viewFoo = super.getDropDownView(position, convertView, parent) as TextView
                viewFoo.setTextColor(getColor(R.color.colorOnBackground))
                return viewFoo
            }
        }
        adapterSpinnerColumn.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerColumn.adapter = adapterSpinnerColumn

        val directionItems = mutableMapOf(
            getString(R.string.main_sort) to null,
            getString(R.string.main_sort_ascending) to true,
            getString(R.string.main_sort_descending) to false
        )

        val spinnerDirection = view.findViewById<Spinner>(R.id.spinnerDirections)
        val adapterSpinnerDirection = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, directionItems.keys.toList()) {
            override fun isEnabled(position: Int): Boolean {
                return position != 0
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val viewFoo = super.getDropDownView(position, convertView, parent) as TextView
                viewFoo.setTextColor(getColor(R.color.colorOnBackground))
                return viewFoo
            }
        }
        adapterSpinnerDirection.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDirection.adapter = adapterSpinnerDirection

        val addSortItemButton = view.findViewById<ImageButton>(R.id.addSortItemButton)
        addSortItemButton.setOnClickListener {
            val selectedColumnSpinner = spinnerColumn.selectedItem.toString()
            val selectedColumn = sortColumns[selectedColumnSpinner]
            val selectedDirectionSpinner = spinnerDirection.selectedItem.toString()
            val selectedDirection = directionItems[selectedDirectionSpinner]

            if ((selectedColumn.isNullOrBlank()) || (selectedDirection == null)) {
                errorSnackbar(message = getString(R.string.main_choose_sort), view = findViewById(android.R.id.content), anchorView = findViewById(R.id.swipeRefreshLayout), length = com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
            } else {
                sorts += selectedColumn to selectedDirection
                updateBadge(sortBadgeTextView, sorts.size)
                dialog.dismiss()
                notifySnackbar(
                    message = getString(
                        R.string.main_sort_chosen,
                        selectedColumn,
                        if (selectedDirection)
                            getString(R.string.main_sort_ascending)
                        else
                            getString(R.string.main_sort_descending)),
                    view = findViewById(android.R.id.content)
                )
                skipFiltersSortsReset = true
                reload()
            }
        }

        val sortItems = mutableListOf<SortItem>()

        for (sortItem in sorts) {
            sortItems.add(SortItem(sortItem.key, sortColumns.entries.find { it.value == sortItem.key }?.key ?: "null", sortItem.value))
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewSortItems)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = SortItemAdapter(sortItems) { selectedSortItem ->
            val foo = mutableMapOf<String, Boolean>()

            for (bar in sorts) {
                if (!bar.key.contentEquals(selectedSortItem.name)) {
                    foo += bar.key to bar.value
                }
            }

            sorts = foo

            updateBadge(sortBadgeTextView, sorts.size)
            dialog.dismiss()
            notifySnackbar(message = getString(R.string.main_sort_removed, selectedSortItem.displayName), view = findViewById(android.R.id.content))
            skipFiltersSortsReset = true
            reload()
        }

        dialog.setContentView(view)

        dialog.show()
    }

    private fun reload() {
        if (!(
            (sorts.isEmpty()) &&
            (filters.isEmpty()) &&
            (searchFilter.isEmpty())
        )) {
            if (skipFiltersSortsReset) {
                skipFiltersSortsReset = false
                reloadMore()
            } else {
                // check if we want to reset all filters and sorts with swipe refresh, without a confirmation
                if (!GlobalInstance.get().swipeRefreshDialog) {
                    filters = mutableMapOf()
                    sorts = mutableMapOf()

                    searchFilter = ""
                    searchBackIcon.visibility = View.GONE
                    searchEditText.visibility = View.GONE
                    searchIcon.visibility = View.VISIBLE
                    searchEditText.setText("")

                    updateBadge(filterBadgeTextView, filters.size)
                    updateBadge(sortBadgeTextView, sorts.size)

                    reloadMore()
                } else {
                    // we need a delay for your custom dialog style, because of setOnRefreshListener
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity, R.style.ConfirmDialogStyle)
                            .setTitle(getString(R.string.main_confirm_action))
                            .setMessage(getString(R.string.main_reset_filter_sort_question))
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                                filters = mutableMapOf()
                                sorts = mutableMapOf()

                                searchFilter = ""
                                searchBackIcon.visibility = View.GONE
                                searchEditText.visibility = View.GONE
                                searchIcon.visibility = View.VISIBLE
                                searchEditText.setText("")

                                updateBadge(filterBadgeTextView, filters.size)
                                updateBadge(sortBadgeTextView, sorts.size)

                                reloadMore()
                            }
                            .setNegativeButton(getString(R.string.text_no)) { _, _ ->
                                reloadMore()
                            }
                            .show()
                    }, 50)
                }
            }
        } else {
            skipFiltersSortsReset = false
            reloadMore()
        }
    }

    private fun reloadMore() {
        showProgress("#AA000000")

        // get data from sqlite
        Thread {
            val list = getData(0, 10)

            runOnUiThread {
                if (list.size < 1) {
                    notifySnackbar(message = getString(R.string.main_no_records_found), view = findViewById(android.R.id.content))
                }

                recyclerViewAdapter.reload(list)
                hideProgress()
            }
        }.start()
    }

    private fun loadMore() {
        showProgress("#AA000000")

        // get more data from sqlite
        Thread {
            val list = getData(recyclerViewAdapter.itemCount, 10)

            runOnUiThread {
                if (list.size < 1) {
                    notifySnackbar(message = getString(R.string.main_no_records_found), view = findViewById(android.R.id.content))
                }

                recyclerViewAdapter.loadMore(list)
                hideProgress()
            }
        }.start()
    }

    private fun showProgress(progressBackground: String = "#EE000000") {
        isLoading = true
        val loadingOverlayMain = findViewById<View>(R.id.loading_overlay_main)
        loadingOverlayMain.setBackgroundColor(progressBackground.toColorInt())
        loadingOverlayMain.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        isLoading = false
        findViewById<View>(R.id.loading_overlay_main).visibility = View.GONE
        findViewById<View>(R.id.progress_bar_main_text).visibility = View.GONE
    }

    private fun updateProgressText(current: Int, total: Int) {
        val percent = (current.toDouble() * 100) / total
        val progressText = "${String.format(java.util.Locale.getDefault(), "%.2f", percent)} %"
        val progressTextView = findViewById<TextView>(R.id.progress_bar_main_text)
        progressTextView.visibility = View.VISIBLE
        progressTextView.text = progressText
    }

    private fun getData(offset: Int, limit: Int): MutableList<ItemBean> {
        val list: MutableList<ItemBean> = mutableListOf()

        val o_recordInstance = MediaCollectionRecord()
        o_recordInstance.Page = (offset / 10) + 1
        o_recordInstance.Interval = limit
        //o_recordInstance.AutoTransaction = false

        if (sorts.isNotEmpty()) {
            // take sorts from main activity settings
            o_recordInstance.Sort = sorts
        } else {
            // no sorts selected, at least sort by FiledUnder and then Title
            o_recordInstance.Sort = mutableMapOf("FiledUnder" to true, "Title" to true)
        }

        val sqlFilters = mutableListOf<net.forestany.forestj.lib.sql.Filter>()

        for (filter in filters) {
            var filterOperator = "LIKE"
            var filterValue = "%${filter.value}%"

            if (filter.value.startsWith("==")) {
                filterOperator = "="
                filterValue = filter.value.substring(2)
            } else if (filter.value.startsWith("<=")) {
                filterOperator = "<="
                filterValue = filter.value.substring(2)
            } else if (filter.value.startsWith(">=")) {
                filterOperator = ">="
                filterValue = filter.value.substring(2)
            } else if (filter.value.startsWith("!=")) {
                filterOperator = "<>"
                filterValue = filter.value.substring(2)
            } else if (filter.value.startsWith("<>")) {
                filterOperator = "<>"
                filterValue = filter.value.substring(2)
            } else if (filter.value.startsWith("=")) {
                filterOperator = "="
                filterValue = filter.value.substring(1)
            } else if (filter.value.startsWith("<")) {
                filterOperator = "<"
                filterValue = filter.value.substring(1)
            } else if (filter.value.startsWith(">")) {
                filterOperator = ">"
                filterValue = filter.value.substring(1)
            } else if (filter.value.startsWith("..")) {
                filterOperator = "<="
                filterValue = filter.value.substring(2)
            } else if (filter.value.endsWith("..")) {
                filterOperator = ">="
                filterValue = filter.value.substring(0, filter.value.length - 2)
            } else if (filter.value.contains("..")) {
                val fromToFilter = filter.value.split("..")

                if (fromToFilter.size == 2) {
                    sqlFilters.add(net.forestany.forestj.lib.sql.Filter(filter.key, fromToFilter[0], ">=", "AND"))
                    filterOperator = "<="
                    filterValue = fromToFilter[1]
                }
            }

            val filterObject = net.forestany.forestj.lib.sql.Filter(filter.key, filterValue, filterOperator, "AND")
            sqlFilters.add(filterObject)
        }

        if (searchFilter.isNotEmpty()) {
            sqlFilters.add(net.forestany.forestj.lib.sql.Filter("Title", "%${searchFilter}%", "LIKE", "OR"))
            sqlFilters.add(net.forestany.forestj.lib.sql.Filter("OriginalTitle", "%${searchFilter}%", "LIKE", "OR"))
        }

        if (sqlFilters.isNotEmpty()) {
            o_recordInstance.Filters = sqlFilters
        }

        for (o_record in o_recordInstance.getRecords(false)) {
            if (o_record.ColumnDeleted != null) {
                continue
            }

            val itemBean = ItemBean()
            itemBean.uuid = o_record.ColumnUUID
            itemBean.title = o_record.ColumnTitle

            if ((o_record.ColumnPoster.isNullOrEmpty()) || (o_record.ColumnPoster.lowercase().contentEquals("null"))) {
                val inputStream = assets.open("no_image.jpg")
                itemBean.bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()
            } else {
                try {
                    val byteArray = net.forestany.forestj.lib.Helper.hexStringToBytes(o_record.ColumnPoster)
                    itemBean.bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
                } catch (_: Exception) {
                    val inputStream = assets.open("no_image.jpg")
                    itemBean.bitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                }
            }

            list.add(itemBean)
        }

        return list
    }

    private fun onExportClicked() {
        deactivateSearchModeWithoutRequery()
        showProgress()

        Thread {
            try {
                // create json media collection instance
                val o_jsonMediaCollection = JSONMediaCollection()
                o_jsonMediaCollection.Timestamp = java.time.LocalDateTime.now().withNano(0)

                // gather all language records
                val o_languageRecordInstance = LanguageRecord()

                for (o_languageRecord in o_languageRecordInstance.getRecords(true)) {
                    o_jsonMediaCollection.Languages.add(o_languageRecord)
                }

                // gather all mediacollection records, poster data in separate stringbuilder
                val o_postersStringBuilder = StringBuilder()
                val o_mediaCollectionRecordInstance = MediaCollectionRecord()

                for (o_mediaCollectionRecord in o_mediaCollectionRecordInstance.getRecords(true)) {
                    o_postersStringBuilder.append(o_mediaCollectionRecord.ColumnUUID + o_mediaCollectionRecord.ColumnPoster + System.lineSeparator())
                    // poster data not in json, decoding would take to much time
                    o_mediaCollectionRecord.ColumnPoster = null
                    o_jsonMediaCollection.Records.add(o_mediaCollectionRecord)
                }

                // encode to json
                val o_json = net.forestany.forestj.lib.io.JSON(mutableListOf(jsonSchema))
                val s_jsonEncodedList = o_json.jsonEncode(o_jsonMediaCollection)

                // save json data to file in shared documents folder in mediacollection_bkp
                saveTextToSharedDocumentsFolder(this, "bkp.json.txt", "text/plain", s_jsonEncodedList)

                if (o_postersStringBuilder.isNotEmpty()) {
                    // save poster data to file in shared documents folder in mediacollection_bkp
                    saveTextToSharedDocumentsFolder(this, "bkp_poster.dat.txt", "text/plain", o_postersStringBuilder.toString())
                }

                runOnUiThread {
                    notifySnackbar(message = getString(R.string.main_data_exported, GlobalInstance.get().backupFolder), view = findViewById(android.R.id.content))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    errorSnackbar(message = "Error: ${e.message ?: "Exception in onExportClicked method."}", view = findViewById(android.R.id.content))
                }
            } finally {
                runOnUiThread {
                    hideProgress()
                }
            }
        }.start()
    }

    private fun saveTextToSharedDocumentsFolder(context: Context, fileName: String, mimeType: String, text: String) {
        if (fileExists(context, fileName)) {
            // file exists, so we must query it to get file URI
            context.contentResolver
                .query(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY),
                    arrayOf(MediaStore.MediaColumns._ID),
                    "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
                    arrayOf("${GlobalInstance.get().backupFolder}/", "%$fileName%"),
                    null
                )
                ?.use { cursor ->
                    // get first item with our query settings
                    if (cursor.moveToFirst()) {
                        // get file URI
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val fileUri = ContentUris.withAppendedId(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), id)

                        // overwrite file content 'wt' -> 'write truncate'
                        context.contentResolver.openOutputStream(fileUri, "wt")?.use { outputStream ->
                            outputStream.write(text.toByteArray())
                        } ?: throw Exception(getString(R.string.main_exception_open_output_stream))
                    }
                }
        } else {
            // set file destination settings
            val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.Files.FileColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, GlobalInstance.get().backupFolder)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            // create file
            context.contentResolver.insert(collection, values)?.let { uri ->
                // write content to file
                context.contentResolver.openOutputStream(uri, "w")?.use { outputStream ->
                    outputStream.write(text.toByteArray())
                } ?: throw Exception(getString(R.string.main_exception_open_output_stream))

                // update file IS_PENDING metadata
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
            } ?: throw Exception(getString(R.string.main_exception_contentresolver_insert))
        }
    }

    private fun onImportClicked() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "application/json",
                "application/octet-stream",
                "text/json",
                "text/x-json",
                "text/plain"
            ))
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }

        openMultipleFilesLauncher.launch(intent)
    }

    private fun setupFilePicker(context: Context) {
        openMultipleFilesLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val clipData = result.data?.clipData
                val uriList = mutableListOf<android.net.Uri>()

                if (clipData != null) {
                    for (i in 0 until clipData.itemCount) {
                        uriList.add(clipData.getItemAt(i).uri)
                    }
                } else {
                    result.data?.data?.let { uriList.add(it) }
                }

                val jsonFile = uriList.find { getFileNameFromUri(context, it).endsWith(".json.txt", true) }
                val txtFile = uriList.find { getFileNameFromUri(context, it).endsWith(".dat.txt", true) }

                if (jsonFile != null && txtFile != null) {
                    val jsonContent = context.contentResolver.openInputStream(jsonFile)?.bufferedReader()?.use { it.readText() } ?: throw Exception(getString(R.string.main_find_json_failure))
                    val datContent = context.contentResolver.openInputStream(txtFile)?.bufferedReader()?.use { it.readText() } ?: throw Exception(getString(R.string.main_find_dat_failure))

                    handleImport(jsonContent, datContent)
                } else {
                    errorSnackbar(message = getString(R.string.main_select_json_and_dat), view = findViewById(android.R.id.content))
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

    private fun handleImport(jsonContent: String, datContent: String) {
        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.main_confirm_action))
            .setMessage(getString(R.string.main_import_question))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                deactivateSearchModeWithoutRequery()
                showProgress()

                Thread {
                    try {
                        var o_mediaCollectionRecordInstance = MediaCollectionRecord()
                        val o_languageRecordInstance = LanguageRecord()

                        // decode json data
                        val o_json = net.forestany.forestj.lib.io.JSON(mutableListOf<String?>(jsonSchema))
                        val o_jsonMediaCollection = o_json.jsonDecode(mutableListOf(jsonContent)) as JSONMediaCollection

                        if (o_mediaCollectionRecordInstance.truncateTable() < 0) {
                            throw Exception(getString(R.string.main_import_not_truncate_mediacollection))
                        }

                        if (o_languageRecordInstance.truncateTable() < 0) {
                            throw Exception(getString(R.string.main_import_not_truncate_languages))
                        }

                        if (o_jsonMediaCollection.Languages.size > 0) {
                            for (jsonLanguageRecord in o_jsonMediaCollection.Languages) {
                                if (jsonLanguageRecord.insertRecord() < 0) {
                                    Log.e(TAG, "Could not insert record with '${jsonLanguageRecord.ColumnLanguage}'.")
                                }
                            }
                        }

                        if (o_jsonMediaCollection.Records.size > 0) {
                            for (jsonMediaCollectionRecord in o_jsonMediaCollection.Records) {
                                if (jsonMediaCollectionRecord.insertRecord() < 0) {
                                    Log.e(TAG, "Could not insert record with '${jsonMediaCollectionRecord.ColumnTitle}'.")
                                }
                            }
                        }

                        // ---------------------------

                        // decode dat data
                        val contentList = datContent.split(Regex("\r\n|\r|\n")).filter { it.isNotBlank() }

                        for (s_foo in contentList) {
                            if (s_foo.length > 100) {
                                val s_uuid = s_foo.substring(0, 36)
                                o_mediaCollectionRecordInstance = MediaCollectionRecord()

                                if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(s_uuid) as List<Any>?)) {
                                    o_mediaCollectionRecordInstance.ColumnPoster = s_foo.substring(36)
                                    o_mediaCollectionRecordInstance.updateRecord()
                                }
                            }
                        }

                        runOnUiThread {
                            notifySnackbar(message = getString(R.string.main_import_finished), view = findViewById(android.R.id.content))
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            errorSnackbar(message = "Error: ${e.message ?: "Exception in handleImport method."}", view = findViewById(android.R.id.content))
                        }
                    } finally {
                        runOnUiThread {
                            hideProgress()
                            recyclerViewAdapter.clear()
                            reload()
                        }
                    }
                }.start()
            }
            .setNegativeButton(getString(R.string.text_no), null)
            .show()
    }

    private fun onSyncClicked() {
        var sslContext: javax.net.ssl.SSLContext? = null
        lateinit var authPassphrase: String
        lateinit var o_clientTask: net.forestany.forestj.lib.net.sock.task.send.https.TinyHttpsClient<javax.net.ssl.SSLSocket>

        // create sslContext, socket task and socket instance
        try {
            // encrypt authentication password
            val o_cryptography = net.forestany.forestj.lib.Cryptography(GlobalInstance.get().syncCommonPassphrase, net.forestany.forestj.lib.Cryptography.KEY256BIT)
            val a_encrypted = o_cryptography.encrypt(GlobalInstance.get().syncAuthPassphrase?.toByteArray(java.nio.charset.StandardCharsets.UTF_8) ?: throw Exception(getString(R.string.main_sync_auth_passphrase_missing)))
            authPassphrase = String(java.util.Base64.getEncoder().encode(a_encrypted), java.nio.charset.StandardCharsets.UTF_8)

            if (net.forestany.forestj.lib.io.File.exists(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".p12")) {
                // use .p12 truststore
                try {
                    sslContext = createMergedTrustManagerSSLContextInstance(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".p12", GlobalInstance.get().syncTruststorePassword ?: "no_pw")
                } catch (e: Exception) {
                    e.printStackTrace()

                    if (net.forestany.forestj.lib.io.File.exists(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".bks")) {
                        // .p12 truststore did not work, use .bks truststore
                        sslContext = try {
                            createMergedTrustManagerSSLContextInstance(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".bks", GlobalInstance.get().syncTruststorePassword ?: "no_pw")
                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                    }
                }
            } else if (net.forestany.forestj.lib.io.File.exists(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".bks")) {
                // use .bks truststore
                sslContext = try {
                    createMergedTrustManagerSSLContextInstance(filesDir.absolutePath + "/" + GlobalInstance.get().syncTruststoreFilename + ".bks", GlobalInstance.get().syncTruststorePassword ?: "no_pw")
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            // create https config class
            val o_clientConfig = net.forestany.forestj.lib.net.https.Config(
                "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}",
                net.forestany.forestj.lib.net.https.Mode.REST,
                net.forestany.forestj.lib.net.sock.recv.ReceiveType.SOCKET
            )

            // create https client task
            o_clientTask = net.forestany.forestj.lib.net.sock.task.send.https.TinyHttpsClient<javax.net.ssl.SSLSocket>(
                o_clientConfig
            )

            // need this hack until we can set amountCyclesToleratingDelay to 0
            o_clientTask.receiveMaxUnknownAmountInMiB = GlobalInstance.get().syncReceiveMaxUnknownAmountInMiB

            // create client socket instance
            val o_socketSend = net.forestany.forestj.lib.net.sock.send.SendTCP<javax.net.ssl.SSLSocket>(
                javax.net.ssl.SSLSocket::class.java,
                GlobalInstance.get().syncServerIp,
                GlobalInstance.get().syncServerPort,
                o_clientTask,
                30000,
                false,
                1,
                50,
                GlobalInstance.get().syncReceiveBufferSize,
                "127.0.0.1",
                0,
                sslContext
            )

            // set sending socket instance for https client
            o_clientConfig.setSendingSocketInstanceForHttpClient(o_socketSend)
        } catch (e: Exception) {
            errorSnackbar(message = "Error: ${e.message ?: "Exception in onSyncClicked method."}", view = findViewById(android.R.id.content))
            return
        }

        deactivateSearchModeWithoutRequery()
        showProgress()

        // start synchronization thread
        Thread {
            try {
                // Step 1: send all data to server
                // create json mediacollection instance
                var o_jsonMediaCollection = JSONMediaCollection()
                o_jsonMediaCollection.Timestamp = java.time.LocalDateTime.now().withNano(0)

                // get all language records
                val o_languageRecordInstance = LanguageRecord()

                for (o_languageRecord in o_languageRecordInstance.getRecords(true)) {
                    o_jsonMediaCollection.Languages.add(o_languageRecord)
                }

                // get all mediacollection records
                val o_mediaCollectionRecordInstance = MediaCollectionRecord()

                for (o_mediaCollectionRecord in o_mediaCollectionRecordInstance.getRecords(true)) {
                    // delete records with deleted timestamp older than 30 days
                    if ( (o_mediaCollectionRecord.ColumnDeleted != null) && (o_mediaCollectionRecord.ColumnDeleted.isBefore( java.time.LocalDateTime.now().withNano(0).minusDays(30) )) ) {
                        o_mediaCollectionRecord.deleteRecord()
                    } else {
                        // poster data not in json, decoding would take to much time
                        o_mediaCollectionRecord.ColumnPoster = null
                        o_jsonMediaCollection.Records.add(o_mediaCollectionRecord)
                    }
                }

                // encode to json
                val o_json = net.forestany.forestj.lib.io.JSON(mutableListOf(jsonSchema))
                val s_jsonEncodedList = o_json.jsonEncode(o_jsonMediaCollection)

                // send json data to server
                o_clientTask.setRequest(
                    "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/all",
                    net.forestany.forestj.lib.net.http.RequestType.POST
                )
                o_clientTask.contentType = net.forestany.forestj.lib.net.http.PostType.HTML
                o_clientTask.addRequestParameter(s_jsonEncodedList, "")
                o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                o_clientTask.authenticationPassword = authPassphrase
                o_clientTask.executeRequest()

                if (o_clientTask.returnCode != 200) {
                    throw Exception(getString(R.string.main_sync_exception_post_all, o_clientTask.returnCode, o_clientTask.returnMessage))
                }

                // validate response from sending json data to server
                try {
                    val serverResponsePostAll = cleanupResponse(o_clientTask.response)

                    // got list of record uuid
                    if (!serverResponsePostAll.contentEquals("null")) {
                        // convert server response to list of record uuid
                        val uuidsForPoster = serverResponsePostAll.split(net.forestany.forestj.lib.net.https.Config.HTTP_LINEBREAK)

                        // if list is not empty
                        if (uuidsForPoster.isNotEmpty()) {
                            var cnt = 1

                            // iterate each response line
                            for (s_uuid in uuidsForPoster) {
                                runOnUiThread {
                                    updateProgressText(cnt, uuidsForPoster.size)
                                }

                                if ((s_uuid.length != 36) && (s_uuid.length > 36) && (net.forestany.forestj.lib.Helper.isDateTime(s_uuid.substring(36)))) {
                                    // received uuid and deleted timestamp
                                    if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(s_uuid.substring(0, 36)) as List<Any>?)) {
                                        o_mediaCollectionRecordInstance.ColumnDeleted = net.forestany.forestj.lib.Helper.fromDateTimeString(s_uuid.substring(36))

                                        if (o_mediaCollectionRecordInstance.updateRecord(true) < 0) {
                                            errorSnackbar(message = getString(R.string.main_sync_exception_update_deleted, o_mediaCollectionRecordInstance.ColumnTitle), view = findViewById(android.R.id.content))
                                        }
                                    }
                                } else if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(s_uuid) as List<Any>?)) {
                                    // received uuid, try multiple times to post poster data to server, in case of connection issues
                                    for (tries in 0..2) {
                                        // post poster data with uuid
                                        o_clientTask.setRequest(
                                            "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/poster",
                                            net.forestany.forestj.lib.net.http.RequestType.POST
                                        )
                                        o_clientTask.contentType = net.forestany.forestj.lib.net.http.PostType.HTML
                                        o_clientTask.addRequestParameter("uuid", s_uuid)
                                        o_clientTask.addRequestParameter("posterdata", o_mediaCollectionRecordInstance.ColumnPoster)
                                        o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                                        o_clientTask.authenticationPassword = authPassphrase
                                        o_clientTask.executeRequest()

                                        if (o_clientTask.returnCode != 200) {
                                            // post poster data failed, try again
                                            continue
                                        }

                                        // validate response from posting poster data
                                        val serverResponsePostPoster = cleanupResponse(o_clientTask.response)

                                        // response should be an integer
                                        if (!net.forestany.forestj.lib.Helper.isInteger(serverResponsePostPoster)) {
                                            // post poster data failed, response is not an integer
                                            continue
                                        }

                                        if (serverResponsePostPoster.toInt() == o_mediaCollectionRecordInstance.ColumnPoster.length) {
                                            // post poster data successful, received bytes equal local bytes of poster data
                                            break
                                        } else {
                                            // post poster data failed, amount of bytes on both sides are not equal
                                            Thread.sleep(2000)
                                        }
                                    }
                                }

                                cnt++
                            }
                        }
                    }
                } catch (_: NullPointerException) {
                    // nothing to do if response hits a NullPointerException, go on to next step of synchronization
                }

                // Step 2: get all data from server
                o_clientTask.setRequest(
                    "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/all",
                    net.forestany.forestj.lib.net.http.RequestType.GET
                )
                o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                o_clientTask.authenticationPassword = authPassphrase
                o_clientTask.executeRequest()

                if (o_clientTask.returnCode != 200) {
                    throw Exception(getString(R.string.main_sync_exception_fetch_all, o_clientTask.returnCode, o_clientTask.returnMessage))
                }

                // decode received json data
                val serverResponseGetAll = cleanupResponse(o_clientTask.response)
                o_jsonMediaCollection = o_json.jsonDecode(mutableListOf(serverResponseGetAll)) as JSONMediaCollection

                // uuid list for getting poster data
                val a_uuidList = mutableListOf<String>()

                // iterate all received records
                if (o_jsonMediaCollection.Records.size > 0) {
                    for (jsonMediaCollectionRecord in o_jsonMediaCollection.Records) {
                        if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(jsonMediaCollectionRecord.ColumnUUID) as List<Any>?)) {
                            if (
                                (jsonMediaCollectionRecord.ColumnDeleted == null) && (o_mediaCollectionRecordInstance.ColumnDeleted == null) &&
                                (jsonMediaCollectionRecord.ColumnLastModified.isAfter(o_mediaCollectionRecordInstance.ColumnLastModified))
                            ) {
                                // received record found, received deleted is null on both sides, and received record last modified timestamp is newer -> update all
                                o_mediaCollectionRecordInstance.ColumnUUID = jsonMediaCollectionRecord.ColumnUUID
                                o_mediaCollectionRecordInstance.ColumnTitle = jsonMediaCollectionRecord.ColumnTitle
                                o_mediaCollectionRecordInstance.ColumnType = jsonMediaCollectionRecord.ColumnType
                                o_mediaCollectionRecordInstance.ColumnPublicationYear = jsonMediaCollectionRecord.ColumnPublicationYear
                                o_mediaCollectionRecordInstance.ColumnOriginalTitle = jsonMediaCollectionRecord.ColumnOriginalTitle
                                o_mediaCollectionRecordInstance.ColumnSubType = jsonMediaCollectionRecord.ColumnSubType
                                o_mediaCollectionRecordInstance.ColumnFiledUnder = jsonMediaCollectionRecord.ColumnFiledUnder
                                o_mediaCollectionRecordInstance.ColumnLastSeen = jsonMediaCollectionRecord.ColumnLastSeen
                                o_mediaCollectionRecordInstance.ColumnLengthInMinutes = jsonMediaCollectionRecord.ColumnLengthInMinutes
                                o_mediaCollectionRecordInstance.ColumnLanguages = jsonMediaCollectionRecord.ColumnLanguages
                                o_mediaCollectionRecordInstance.ColumnSubtitles = jsonMediaCollectionRecord.ColumnSubtitles
                                o_mediaCollectionRecordInstance.ColumnDirectors = jsonMediaCollectionRecord.ColumnDirectors
                                o_mediaCollectionRecordInstance.ColumnScreenwriters = jsonMediaCollectionRecord.ColumnScreenwriters
                                o_mediaCollectionRecordInstance.ColumnCast = jsonMediaCollectionRecord.ColumnCast
                                o_mediaCollectionRecordInstance.ColumnSpecialFeatures = jsonMediaCollectionRecord.ColumnSpecialFeatures
                                o_mediaCollectionRecordInstance.ColumnOther = jsonMediaCollectionRecord.ColumnOther
                                o_mediaCollectionRecordInstance.ColumnLastModified = jsonMediaCollectionRecord.ColumnLastModified

                                // update record - deleted is null on both sides, and received record last modified timestamp is newer
                                try {
                                    if (o_mediaCollectionRecordInstance.updateRecord(true) >= 0) {
                                        a_uuidList.add(o_mediaCollectionRecordInstance.ColumnUUID)
                                    }
                                } catch (o_exc: IllegalStateException) {
                                    // catch primary/unique violation and ignore it
                                }
                            } else if ((jsonMediaCollectionRecord.ColumnDeleted != null) && (!jsonMediaCollectionRecord.ColumnDeleted.equals(o_mediaCollectionRecordInstance.ColumnDeleted))) {
                                // received record found, received deleted is not null and local is not equal to it -> only update deleted
                                o_mediaCollectionRecordInstance.ColumnDeleted = jsonMediaCollectionRecord.ColumnDeleted

                                // update record - received deleted is not null and local deleted is not equal to it
                                try {
                                    o_mediaCollectionRecordInstance.updateRecord(true)
                                } catch (o_exc: IllegalStateException) {
                                    // catch primary/unique violation and ignore it
                                }
                            } else if (
                                (jsonMediaCollectionRecord.ColumnDeleted == null) && (o_mediaCollectionRecordInstance.ColumnDeleted == null) &&
                                (jsonMediaCollectionRecord.ColumnPoster.toInt() != o_mediaCollectionRecordInstance.ColumnPoster.length)
                            ) {
                                // received record found, both sides not deleted and it is not different by last modified timestamp, but poster data does not match

                                // save uuid to get poster data later
                                a_uuidList.add(jsonMediaCollectionRecord.ColumnUUID)
                            }
                        } else {
                            if (jsonMediaCollectionRecord.ColumnDeleted == null) {
                                // received record not found and deleted is null
                                try {
                                    // check with new record if record(s) already exists with OriginalTitle and PublicationYear
                                    val checkRecords = MediaCollectionRecord()
                                    checkRecords.Filters = mutableListOf(
                                        net.forestany.forestj.lib.sql.Filter("OriginalTitle", jsonMediaCollectionRecord.ColumnOriginalTitle, "=", "AND"),
                                        net.forestany.forestj.lib.sql.Filter("PublicationYear", jsonMediaCollectionRecord.ColumnPublicationYear, "=", "AND"),
                                        net.forestany.forestj.lib.sql.Filter("Deleted", null, "IS", "AND")
                                    )

                                    // query for same record(s)
                                    val result = checkRecords.getRecords(true)

                                    // we found record(s) which are identical and not deleted
                                    for (record in result) {
                                        record.ColumnDeleted = java.time.LocalDateTime.now().withNano(0)

                                        // update record - duplicate can be set with deleted timestamp
                                        try {
                                            record.updateRecord(true)
                                        } catch (o_exc: IllegalStateException) {
                                            // catch primary/unique violation and ignore it
                                        }
                                    }

                                    // insert record and save uuid to get poster data later
                                    if (jsonMediaCollectionRecord.insertRecord() > 0) {
                                        a_uuidList.add(jsonMediaCollectionRecord.ColumnUUID)
                                    }
                                } catch (o_exc: IllegalStateException) {
                                    // catch primary/unique violation and ignore it
                                }
                            }
                        }
                    }
                }

                // any changes (insert/update) happens?
                if (a_uuidList.isNotEmpty()) {
                    var cnt = 1

                    // iterate each uuid entry
                    for (s_uuid in a_uuidList) {
                        runOnUiThread {
                            updateProgressText(cnt, a_uuidList.size)
                        }

                        // try multiple times to get poster data from server, in case of connection issues
                        for (tries in 0..2) {
                            // get poster data from server
                            o_clientTask.setRequest(
                                "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/poster?uuid=$s_uuid",
                                net.forestany.forestj.lib.net.http.RequestType.GET
                            )
                            o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                            o_clientTask.authenticationPassword = authPassphrase
                            o_clientTask.executeRequest()

                            if (o_clientTask.returnCode != 200) {
                                // get poster data failed, try again
                                continue
                            }

                            // get poster bytes from response
                            val serverResponseGetPoster = cleanupResponse(o_clientTask.response)

                            // get mediacollection record with uuid entry
                            if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(s_uuid) as List<Any>?)) {
                                // update poster data
                                o_mediaCollectionRecordInstance.ColumnPoster = serverResponseGetPoster
                                o_mediaCollectionRecordInstance.updateRecord()
                            }

                            // get poster bytes length to check if we received all bytes
                            o_clientTask.setRequest(
                                "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/posterbyteslength?uuid=$s_uuid",
                                net.forestany.forestj.lib.net.http.RequestType.GET
                            )
                            o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                            o_clientTask.authenticationPassword = authPassphrase
                            o_clientTask.executeRequest()

                            if (o_clientTask.returnCode != 200) {
                                // get poster bytes length failed, try again
                                continue
                            }

                            // validate response from get poster bytes length
                            val serverResponseGetPosterLength = cleanupResponse(o_clientTask.response)

                            // response should be an integer
                            if (!net.forestany.forestj.lib.Helper.isInteger(serverResponseGetPosterLength)) {
                                // get poster bytes length failed, response is not an integer
                                continue
                            }

                            if (serverResponseGetPosterLength.toInt() == o_mediaCollectionRecordInstance.ColumnPoster.length) {
                                // get poster data successful, received bytes equal local bytes of poster data
                                break
                            } else {
                                // get poster data failed, amount of bytes on both sides are not equal
                                Thread.sleep(2000)
                            }

                        }

                        cnt++
                    }
                }

                runOnUiThread {
                    notifySnackbar(message = getString(R.string.main_sync_finished), view = findViewById(android.R.id.content))
                }
            } catch (e: Exception) {
                runOnUiThread {
                    errorSnackbar(message = "Error: ${e.message ?: "Exception in onSyncClicked method."}", view = findViewById(android.R.id.content))
                }
            } finally {
                runOnUiThread {
                    hideProgress()
                    reload()
                }
            }
        }.start()
    }

    private fun createMergedTrustManagerSSLContextInstance(pathToTruststore: String, truststorePassword: String): javax.net.ssl.SSLContext {
        // load default android ca truststore
        val defaultTrustStore = java.security.KeyStore.getInstance("AndroidCAStore").apply {
            load(null)
        }

        // load own BKS truststore
        val customTrustStore = java.security.KeyStore.getInstance("BKS").apply {
            java.io.FileInputStream(java.io.File(pathToTruststore)).use { input ->
                load(input, truststorePassword.toCharArray())
            }
        }

        // create merged truststore
        val mergedTrustStore = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType()).apply {
            load(null)

            // copy from default android ca truststore
            defaultTrustStore.aliases().iterator().forEach { alias ->
                setCertificateEntry("system-$alias", defaultTrustStore.getCertificate(alias))
            }

            // copy from own BKS truststore
            customTrustStore.aliases().iterator().forEach { alias ->
                setCertificateEntry("custom-$alias", customTrustStore.getCertificate(alias))
            }
        }

        // create TrustManagerFactory with merged truststore
        val tmf = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()).apply {
            init(mergedTrustStore)
        }

        // create SSLContext with TLS 1.3
        return javax.net.ssl.SSLContext.getInstance("TLSv1.3").apply {
            init(null, tmf.trustManagers, java.security.SecureRandom())
        }
    }

    private fun cleanupResponse(content: String?): String {
        // need this hack until we can set amountCyclesToleratingDelay to 0
        if (content == null) {
            return "null"
        }

        var s_foo = content.trimEnd('\u0000')

        if (s_foo.indexOf("HTTP/1.1 ") > 0) {
            s_foo = s_foo.substring(0, s_foo.indexOf("HTTP/1.1 "))
        }

        return s_foo
    }

    private fun onStatisticClicked() {
        var totalItems = 0
        var totalMovieItems = 0
        var totalMovieBlurayItems = 0
        var totalMovieDVDItems = 0
        var totalMovie4KItems = 0
        var totalSeriesItems = 0
        var totalSeriesBlurayItems = 0
        var totalSeriesDVDItems = 0
        var totalSeries4KItems = 0

        // get all mediacollection records
        val o_mediaCollectionRecordInstance = MediaCollectionRecord()

        for (o_mediaCollectionRecord in o_mediaCollectionRecordInstance.getRecords(true)) {
            // skip deleted records
            if (o_mediaCollectionRecord.ColumnDeleted != null) {
                continue
            }

            if (o_mediaCollectionRecord.ColumnType!!.contentEquals("Movie")) {
                if (o_mediaCollectionRecord.ColumnSubType.contains("bluray")) {
                    totalItems++
                    totalMovieItems++
                    totalMovieBlurayItems++
                }

                if (o_mediaCollectionRecord.ColumnSubType.contains("dvd")) {
                    totalItems++
                    totalMovieItems++
                    totalMovieDVDItems++
                }

                if (o_mediaCollectionRecord.ColumnSubType.contains("4k")) {
                    totalItems++
                    totalMovieItems++
                    totalMovie4KItems++
                }
            } else if (o_mediaCollectionRecord.ColumnType!!.contentEquals("Series")) {
                if (o_mediaCollectionRecord.ColumnSubType.contains("bluray")) {
                    totalItems++
                    totalSeriesItems++
                    totalSeriesBlurayItems++
                }

                if (o_mediaCollectionRecord.ColumnSubType.contains("dvd")) {
                    totalItems++
                    totalSeriesItems++
                    totalSeriesDVDItems++
                }

                if (o_mediaCollectionRecord.ColumnSubType.contains("4k")) {
                    totalItems++
                    totalSeriesItems++
                    totalSeries4KItems++
                }
            }
        }

        val message = """
            |%-15s %5d
            |
            |%-15s %5d
            |%-15s %5d
            |%-15s %5d
            |%-15s %5d
            |
            |%-15s %5d
            |%-15s %5d
            |%-15s %5d
            |%-15s %5d
        """.trimMargin().format(
            "Total Items:", totalItems,
            "Movie Items:", totalMovieItems,
            "    Bluray:", totalMovieBlurayItems,
            "    DVD:", totalMovieDVDItems,
            "    4K:", totalMovie4KItems,
            "Series Items:", totalSeriesItems,
            "    Bluray:", totalSeriesBlurayItems,
            "    DVD:", totalSeriesDVDItems,
            "    4K:", totalSeries4KItems
        )

        val textView = TextView(this).apply {
            text = message
            typeface = android.graphics.Typeface.MONOSPACE
            setPadding(40, 40, 40, 0)
            textSize = 16f
        }

        androidx.appcompat.app.AlertDialog.Builder(this@MainActivity, R.style.SelectionDialogStyle)
            .setTitle(getString(R.string.main_menu_statistic))
            .setView(textView)
            .setIcon(android.R.drawable.ic_dialog_info)
            .setPositiveButton(getString(R.string.text_ok)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun initSettings() {
        filterColumns = mutableMapOf(
            //"Column..." to "",
            getString(R.string.main_field_title) to "Title",
            getString(R.string.main_field_originaltitle) to "OriginalTitle",
            getString(R.string.main_field_publicationyear) to "PublicationYear",
            getString(R.string.main_field_type) to "Type",
            getString(R.string.main_field_subtype) to "SubType",
            getString(R.string.main_field_filedunder) to "FiledUnder",
            getString(R.string.main_field_lastseen) to "LastSeen",
            getString(R.string.main_field_lengthinminutes) to "LengthInMinutes",
            getString(R.string.main_field_languages) to "Languages",
            getString(R.string.main_field_subtitles) to "Subtitles",
            getString(R.string.main_field_directors) to "Directors",
            getString(R.string.main_field_screenwriters) to "Screenwriters",
            getString(R.string.main_field_cast) to "Cast",
            getString(R.string.main_field_specialfeatures) to "SpecialFeatures",
            getString(R.string.main_field_other) to "Other"
        )
        sortColumns = filterColumns

        val sharedPreferences = getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE)

        checkForAppUpdate(sharedPreferences)

        //sharedPreferences.all.forEach {
        //    Log.v(TAG, "${it.key} -> ${it.value}")
        //}

        if (
            (sharedPreferences.all.isEmpty()) ||
            (!sharedPreferences.contains("general_locale")) ||
            (!sharedPreferences.contains("show_media_title")) ||
            (!sharedPreferences.contains("backup_folder")) ||
            (!sharedPreferences.contains("swipe_refresh_dialog")) ||
            (!sharedPreferences.contains("toolbar_offset")) ||
            (!sharedPreferences.contains("toolbar_wait_autosearch")) ||
            (!sharedPreferences.contains("poster_desired_width")) ||
            (!sharedPreferences.contains("poster_overview_fixed_height")) ||
            (!sharedPreferences.contains("poster_tmdb_url")) ||
            (!sharedPreferences.contains("poster_movieposterdb_url")) ||
            (!sharedPreferences.contains("search_tmdb_url_movies")) ||
            (!sharedPreferences.contains("search_tmdb_url_movie_details")) ||
            (!sharedPreferences.contains("search_tmdb_url_series")) ||
            (!sharedPreferences.contains("search_tmdb_url_series_details")) ||
            (!sharedPreferences.contains("search_tmdb_url_target_language")) ||
            (!sharedPreferences.contains("search_tmdb_api_key")) ||
            (!sharedPreferences.contains("sync_automatic")) ||
            (!sharedPreferences.contains("sync_server_ip")) ||
            (!sharedPreferences.contains("sync_server_port")) ||
            (!sharedPreferences.contains("sync_receive_buffer_size")) ||
            (!sharedPreferences.contains("sync_common_passphrase")) ||
            (!sharedPreferences.contains("sync_auth_user")) ||
            (!sharedPreferences.contains("sync_auth_passphrase")) ||
            (!sharedPreferences.contains("sync_truststore_filename")) ||
            (!sharedPreferences.contains("sync_truststore_password")) ||
            (!sharedPreferences.contains("sync_receive_max_unknown_amount_in_mib"))
        ) {
            sharedPreferences.edit(commit = true) {
                if (!sharedPreferences.contains("general_locale")) {
                    val s_locale = java.util.Locale.getDefault().toString()

                    if ((s_locale.lowercase().startsWith("de")) || (s_locale.lowercase().startsWith("en"))) {
                        putString("general_locale", java.util.Locale.getDefault().toString().substring(0, 2))
                    } else {
                        putString("general_locale", "en")
                    }
                }

                if (!sharedPreferences.contains("general_locale")) {
                    val s_locale = java.util.Locale.getDefault().toString()

                    if (s_locale.lowercase().startsWith("de")) {
                        putString("search_tmdb_url_target_language", "de-DE")
                    } else if (s_locale.lowercase().startsWith("en")) {
                        putString("search_tmdb_url_target_language", "en-US")
                    } else {
                        putString("search_tmdb_url_target_language", "en-US")
                    }
                }

                if (!sharedPreferences.contains("show_media_title")) putBoolean("show_media_title", false)
                if (!sharedPreferences.contains("backup_folder")) putString("backup_folder", SETTINGS_STANDARD_BKP_FOLDER)
                if (!sharedPreferences.contains("swipe_refresh_dialog")) putBoolean("swipe_refresh_dialog", false)
                if (!sharedPreferences.contains("toolbar_offset")) putString("toolbar_offset", SETTINGS_STANDARD_TOOLBAR_OFFSET)
                if (!sharedPreferences.contains("toolbar_wait_autosearch")) putString("toolbar_wait_autosearch", SETTINGS_STANDARD_TOOLBAR_WAIT_AUTOSEARCH)
                if (!sharedPreferences.contains("poster_desired_width")) putString("poster_desired_width", SETTINGS_STANDARD_POSTER_DESIRED_WIDTH)
                if (!sharedPreferences.contains("poster_overview_fixed_height")) putString("poster_overview_fixed_height", SETTINGS_STANDARD_POSTER_OVERVIEW_FIXED_HEIGHT)
                if (!sharedPreferences.contains("poster_tmdb_url")) putString("poster_tmdb_url", SETTINGS_STANDARD_POSTER_TMDB_URL)
                if (!sharedPreferences.contains("poster_movieposterdb_url")) putString("poster_movieposterdb_url", SETTINGS_STANDARD_POSTER_MOVIEPOSTERDB_URL)
                if (!sharedPreferences.contains("search_tmdb_url_movies")) putString("search_tmdb_url_movies", SETTINGS_STANDARD_TMDB_URL_MOVIES)
                if (!sharedPreferences.contains("search_tmdb_url_movie_details")) putString("search_tmdb_url_movie_details", SETTINGS_STANDARD_TMDB_URL_MOVIE_DETAILS)
                if (!sharedPreferences.contains("search_tmdb_url_series")) putString("search_tmdb_url_series", SETTINGS_STANDARD_TMDB_URL_SERIES)
                if (!sharedPreferences.contains("search_tmdb_url_series_details")) putString("search_tmdb_url_series_details", SETTINGS_STANDARD_TMDB_URL_SERIES_DETAILS)
                if (!sharedPreferences.contains("search_tmdb_api_key")) putString("search_tmdb_api_key", SETTINGS_STANDARD_TMDB_API_KEY)
                if (!sharedPreferences.contains("sync_automatic")) putString("sync_automatic", SETTINGS_STANDARD_SYNC_AUTOMATIC)
                if (!sharedPreferences.contains("sync_server_ip")) putString("sync_server_ip", SETTINGS_STANDARD_SYNC_SERVER_IP)
                if (!sharedPreferences.contains("sync_server_port")) putString("sync_server_port", SETTINGS_STANDARD_SYNC_SERVER_PORT)
                if (!sharedPreferences.contains("sync_receive_buffer_size")) putString("sync_receive_buffer_size", SETTINGS_STANDARD_SYNC_RECEIVE_BUFFER_SIZE)
                if (!sharedPreferences.contains("sync_common_passphrase")) putString("sync_common_passphrase", SETTINGS_STANDARD_SYNC_COMMON_PASSPHRASE)
                if (!sharedPreferences.contains("sync_auth_user")) putString("sync_auth_user", SETTINGS_STANDARD_SYNC_AUTH_USER)
                if (!sharedPreferences.contains("sync_auth_passphrase")) putString("sync_auth_passphrase", SETTINGS_STANDARD_SYNC_AUTH_PASSPHRASE)
                if (!sharedPreferences.contains("sync_truststore_filename")) putString("sync_truststore_filename", SETTINGS_STANDARD_SYNC_TRUSTSTORE_FILENAME)
                if (!sharedPreferences.contains("sync_truststore_password")) putString("sync_truststore_password", SETTINGS_STANDARD_SYNC_TRUSTSTORE_PASSWORD)
                if (!sharedPreferences.contains("sync_receive_max_unknown_amount_in_mib")) putString("sync_receive_max_unknown_amount_in_mib", SETTINGS_STANDARD_SYNC_RECEIVE_MAX_UNKNOWN_AMOUNT_IN_MIB)
            }
        }

        assumeSharedPreferencesToGlobal(sharedPreferences)

        if (java.util.Locale.getDefault().toString().substring(0, 2) != sharedPreferences.all["general_locale"].toString()) {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags(
                    sharedPreferences.all["general_locale"].toString()
                )
            )
        }
    }

    private fun assumeSharedPreferencesToGlobal(sharedPreferences: SharedPreferences) {
        sharedPreferences.all.forEach {
            if (it.key!!.contentEquals("show_media_title")) GlobalInstance.get().showMediaTitle = it.value.toString().lowercase().contentEquals("true")
            if (it.key!!.contentEquals("backup_folder")) GlobalInstance.get().backupFolder = it.value.toString()
            if (it.key!!.contentEquals("swipe_refresh_dialog")) GlobalInstance.get().swipeRefreshDialog = it.value.toString().lowercase().contentEquals("true")
            if (it.key!!.contentEquals("toolbar_offset")) GlobalInstance.get().toolbarOffset = Integer.parseInt(it.value.toString())
            if (it.key!!.contentEquals("toolbar_wait_autosearch")) GlobalInstance.get().toolbarWaitAutosearch = Integer.parseInt(it.value.toString())
            if (it.key!!.contentEquals("poster_desired_width")) GlobalInstance.get().posterDesiredWidth = Integer.parseInt(it.value.toString())
            if (it.key!!.contentEquals("poster_overview_fixed_height")) GlobalInstance.get().posterOverviewFixedHeight = Integer.parseInt(it.value.toString())
            if (it.key!!.contentEquals("poster_tmdb_url")) GlobalInstance.get().posterTMDBUrl = it.value.toString()
            if (it.key!!.contentEquals("poster_movieposterdb_url")) GlobalInstance.get().posterMoviePosterDbUrl = it.value.toString()
            if (it.key!!.contentEquals("search_tmdb_url_movies")) GlobalInstance.get().tmdbUrlMovies = it.value.toString()
            if (it.key!!.contentEquals("search_tmdb_url_movie_details")) GlobalInstance.get().tmdbUrlMovieDetails = it.value.toString()
            if (it.key!!.contentEquals("search_tmdb_url_series")) GlobalInstance.get().tmdbUrlSeries = it.value.toString()
            if (it.key!!.contentEquals("search_tmdb_url_series_details")) GlobalInstance.get().tmdbUrlSeriesDetails = it.value.toString()
            if (it.key!!.contentEquals("search_tmdb_url_target_language")) GlobalInstance.get().tmdbUrlTargetLanguage = it.value.toString()
            if (it.key!!.contentEquals("search_tmdb_api_key")) GlobalInstance.get().tmdbApiKey = it.value.toString()
            if (it.key!!.contentEquals("sync_automatic")) GlobalInstance.get().syncAutomatic = it.value.toString().toLong()
            if (it.key!!.contentEquals("sync_server_ip")) GlobalInstance.get().syncServerIp = it.value.toString()
            if (it.key!!.contentEquals("sync_server_port")) GlobalInstance.get().syncServerPort = Integer.parseInt(it.value.toString())
            if (it.key!!.contentEquals("sync_receive_buffer_size")) GlobalInstance.get().syncReceiveBufferSize = Integer.parseInt(it.value.toString())
            if (it.key!!.contentEquals("sync_common_passphrase")) GlobalInstance.get().syncCommonPassphrase = it.value.toString()
            if (it.key!!.contentEquals("sync_auth_user")) GlobalInstance.get().syncAuthUser = it.value.toString()
            if (it.key!!.contentEquals("sync_auth_passphrase")) GlobalInstance.get().syncAuthPassphrase = it.value.toString()
            if (it.key!!.contentEquals("sync_truststore_filename")) GlobalInstance.get().syncTruststoreFilename = it.value.toString()
            if (it.key!!.contentEquals("sync_truststore_password")) GlobalInstance.get().syncTruststorePassword = it.value.toString()
            if (it.key!!.contentEquals("sync_receive_max_unknown_amount_in_mib")) GlobalInstance.get().syncReceiveMaxUnknownAmountInMiB = Integer.parseInt(it.value.toString())
            if (it.key!!.contentEquals("last_auto_synchronization_datetime")) GlobalInstance.get().lastAutoSynchronizationDateTime = it.value.toString()
        }
    }

    private fun getCurrentAppVersion(): String? {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            "unknown_version"
        }
    }

    private fun checkForAppUpdate(o_sharedPreferences: SharedPreferences) {
        val s_lastVersion: String = o_sharedPreferences.getString("last_version", "") ?: ""

        val s_currentVersion = getCurrentAppVersion()

        if (s_currentVersion.contentEquals("unknown_version")) {
            errorSnackbar(message = getString(R.string.main_app_unknown_version), view = findViewById(android.R.id.content))
        } else if (s_lastVersion.isEmpty()) {
            onFirstLaunchEver()
            o_sharedPreferences.edit { putString("last_version", s_currentVersion) }
        } else if (s_currentVersion != s_lastVersion) {
            onFirstLaunchAfterUpdate()
            o_sharedPreferences.edit { putString("last_version", s_currentVersion) }
        } else {
            Log.v(TAG, "app has not changed")
        }
    }

    private fun onFirstLaunchEver() {
        Log.v(TAG, "first launch ever")
    }

    private fun onFirstLaunchAfterUpdate() {
        Log.v(TAG, "first launch after update")
    }

    override fun onStart() {
        super.onStart()

        Log.v(TAG, "onStart $TAG")
    }

    override fun onResume() {
        super.onResume()

        // do auto synchronization if db is not empty and if it is set via settings and not null or 0
        if (
            (!dbEmpty) &&
            (GlobalInstance.get().syncAutomatic != null) &&
            (GlobalInstance.get().syncAutomatic != 0L) &&
            (
                (GlobalInstance.get().lastAutoSynchronizationDateTime == null) ||
                (net.forestany.forestj.lib.Helper.fromDateTimeString(GlobalInstance.get().lastAutoSynchronizationDateTime).isBefore(java.time.LocalDateTime.now().minusHours(GlobalInstance.get().syncAutomatic ?: 0L)))
            )
        ) {
            // wait 1 seconds, before doing auto sync
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                onSyncClicked()

                GlobalInstance.get().lastAutoSynchronizationDateTime = java.time.LocalDateTime.now().withNano(0).toString()

                getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE).edit(commit = true) {
                    putString("last_auto_synchronization_datetime", java.time.LocalDateTime.now().withNano(0).toString())
                }
            }, 1000)
        }

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

        assumeSharedPreferencesToGlobal(getSharedPreferences("${packageName}_preferences", Context.MODE_PRIVATE))

        Log.v(TAG, "onRestart $TAG")
    }

    override fun onDestroy() {
        super.onDestroy()

        Log.v(TAG, "onDestroy $TAG")
    }
}