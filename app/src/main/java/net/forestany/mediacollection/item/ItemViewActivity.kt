package net.forestany.mediacollection.item

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.scale
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.forestany.mediacollection.MainActivity
import net.forestany.mediacollection.R
import net.forestany.mediacollection.main.MediaCollectionRecord
import net.forestany.mediacollection.main.GlobalInstance
import net.forestany.mediacollection.main.LanguageRecord
import net.forestany.mediacollection.main.Util
import net.forestany.mediacollection.main.Util.errorSnackbar
import net.forestany.mediacollection.main.Util.notifySnackbar
import net.forestany.mediacollection.search.MediaItem
import net.forestany.mediacollection.search.MediaItemAdapter
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ItemViewActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ItemViewActivity"
    }

    private val viewModel: ItemViewModel by viewModels()
    private var isLoading = false
    private lateinit var record: MediaCollectionRecord

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // default settings
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_item_view)

            // settings toolbar
            setSupportActionBar(findViewById(R.id.toolbar_item))
            supportActionBar?.setDisplayHomeAsUpEnabled(false) /* standard back/home button */
            supportActionBar?.setDisplayShowTitleEnabled(false)
            //supportActionBar?.title = getString(R.string.title_item_activity)

            // deactivate standard back button
            onBackPressedDispatcher.addCallback(
                this,
                object : androidx.activity.OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        /* execute anything, e.g. finish() - if nothing is here, nothing happens pushing main back button */
                        finish()
                    }
                }
            )

            // initialize media collection record instance
            record = MediaCollectionRecord()

            // get UUID extra from MainActivity
            val s_uuid = intent.getStringExtra("UUID")
            var disablePublicationYearAndOriginalTitle = false

            // if UUID extra is not null or empty
            if (!s_uuid.isNullOrEmpty()) {
                // get record with uuid
                if (!record.getOneRecord(listOf("UUID"), listOf(s_uuid))) {
                    errorSnackbar(message = getString(R.string.itemview_record_not_loaded), view = findViewById(android.R.id.content))
                    // reinitialize record instance
                    record = MediaCollectionRecord()
                } else {
                    disablePublicationYearAndOriginalTitle = true
                }
            }

            // give record instance to view model
            viewModel.setRecord(record)

            val viewPager: ViewPager2 = findViewById(R.id.view_pager)
            val tabLayout: TabLayout = findViewById(R.id.tab_layout)

            val adapter = ItemViewPagerAdapter(this)
            viewPager.adapter = adapter
            // somehow this is a hack that elements are not losing focus
            viewPager.setOffscreenPageLimit(5)

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> getString(R.string.itemview_tab_title_general)
                    1 -> getString(R.string.itemview_tab_title_details)
                    2 -> getString(R.string.itemview_tab_title_poster)
                    3 -> getString(R.string.itemview_tab_title_other)
                    else -> "Tab"
                }
            }.attach()

            // disable form elements for external poster update for an existing record
            if ( (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") && (!record.ColumnUUID.isNullOrEmpty()) ) {
                disablePublicationYearAndOriginalTitle = true
            }

            // handle disabling form elements on general fragment 'f0'
            viewPager.post {
                val tag = getFragmentTag(viewPager.id, viewPager.currentItem)

                if (tag.contentEquals("f0")) {
                    val fragment = supportFragmentManager.findFragmentByTag(tag) as GeneralFragment?
                    fragment?.setPublicationYearAndOriginalTitleVisibility(disablePublicationYearAndOriginalTitle)
                }
            }

            handleIncomingShare(intent)

            Log.v(TAG, "onCreate $TAG")
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onCreate method.", view = findViewById(android.R.id.content))
        }
    }

    private fun getFragmentTag(viewPagerId: Int, position: Int): String {
        return "f$position"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.item_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        /* hide soft input keyboard if option is selected */
        val view = currentFocus ?: View(this)
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)

        if (!isLoading) {
            when (item.itemId) {
                R.id.mI_autoFill -> {
                    autoFill()

                    return true
                }

                R.id.mI_delete -> {
                    deleteDataRecord()

                    return true
                }

                R.id.mI_save -> {
                    saveDataRecord()

                    return true
                }
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun showLoading() {
        isLoading = true
        findViewById<View>(R.id.loading_overlay_item_view).visibility = View.VISIBLE
    }

    private fun hideLoading() {
        findViewById<View>(R.id.loading_overlay_item_view).visibility = View.GONE
    }

    private fun autoFill() {
        // check if we have a title for search
        if ((viewModel.record.value?.ColumnTitle.isNullOrEmpty()) || (viewModel.record.value?.ColumnTitle?.lowercase().contentEquals("null"))) {
            // go to general tab
            (findViewById<ViewPager2>(R.id.view_pager)!!).currentItem = 0

            errorSnackbar(message = getString(R.string.itemview_enter_title), view = findViewById(android.R.id.content))
            return
        }

        // check if we have a type for search
        if ((viewModel.record.value?.ColumnType.isNullOrEmpty()) || (viewModel.record.value?.ColumnType?.lowercase().contentEquals("null"))) {
            // go to general tab
            (findViewById<ViewPager2>(R.id.view_pager)!!).currentItem = 0

            errorSnackbar(message = getString(R.string.itemview_enter_type), view = findViewById(android.R.id.content))
            return
        }

        // encode title into utf-8 string for use within url
        val urlEncodedTitle = java.net.URLEncoder.encode(viewModel.record.value?.ColumnTitle, "UTF-8")
        val mediaItems = mutableListOf<MediaItem>()
        var exceptionThrown = false

        showLoading()

        // do search and auto fill within lifecycle scope
        lifecycleScope.launch {
            try {
                var typeIsMovie = false

                if (viewModel.record.value?.ColumnType?.contentEquals("Movie") == true) // look for movie
                {
                    typeIsMovie = true

                    // search for movies
                    val a_foo = GlobalInstance.get().searchInstance?.searchMovieByTitle(urlEncodedTitle) ?: throw Exception(getString(R.string.itemview_no_results_title, viewModel.record.value?.ColumnTitle))

                    // add search results to media item list
                    for (mediaItem in a_foo.Results) {
                        // bitmap for poster preview
                        var bitmap: Bitmap? = null

                        // load poster preview if poster path is not null or empty
                        if (!mediaItem.PosterPath.isNullOrEmpty()) {
                            try {
                                // load poster of media item as bitmap
                                bitmap = withContext(Dispatchers.IO) {
                                    downloadImageReturnBitmap(
                                        Util.replacePlaceholders(
                                            GlobalInstance.get().posterTMDBUrl,
                                            GlobalInstance.get().posterDesiredWidth.toString(),
                                            mediaItem.PosterPath
                                        )
                                    )
                                }
                            } catch (_: Exception) {
                                // is ok, we handle null bitmap with 'no_image.jpg'
                            }
                        }

                        // no poster path or downloading poster of media item failed, so we just show 'no_image.jpg'
                        if (bitmap == null) {
                            val inputStream = this@ItemViewActivity.assets.open("no_image.jpg")
                            bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream.close()
                        }

                        mediaItems.add(
                            MediaItem(
                                mediaItem.Id,
                                mediaItem.Title,
                                mediaItem.OriginalTitle,
                                if (mediaItem.ReleaseDate.isNullOrBlank()) {
                                    getString(R.string.itemview_no_date)
                                } else {
                                    net.forestany.forestj.lib.Helper
                                        .fromDateString(mediaItem.ReleaseDate)
                                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                },
                                bitmap
                            )
                        )
                    }
                } else if (viewModel.record.value?.ColumnType?.contentEquals("Series") == true) // look for series
                {
                    // search for series
                    val a_foo = GlobalInstance.get().searchInstance?.searchSeriesByTitle(urlEncodedTitle) ?: throw Exception(getString(R.string.itemview_no_results_title, viewModel.record.value?.ColumnTitle))

                    // add search results to media item list
                    for (mediaItem in a_foo.Results) {
                        // bitmap for poster preview
                        var bitmap: Bitmap? = null

                        // load poster preview if poster path is not null or empty
                        if (!mediaItem.PosterPath.isNullOrEmpty()) {
                            try {
                                // load poster of media item as bitmap
                                bitmap = withContext(Dispatchers.IO) {
                                    downloadImageReturnBitmap(
                                        Util.replacePlaceholders(
                                            GlobalInstance.get().posterTMDBUrl,
                                            GlobalInstance.get().posterDesiredWidth.toString(),
                                            mediaItem.PosterPath
                                        )
                                    )
                                }
                            } catch (_: Exception) {
                                // is ok, we handle null bitmap with 'no_image.jpg'
                            }
                        }

                        // no poster path or downloading poster of media item failed, so we just show 'no_image.jpg'
                        if (bitmap == null) {
                            val inputStream = this@ItemViewActivity.assets.open("no_image.jpg")
                            bitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream.close()
                        }

                        mediaItems.add(
                            MediaItem(
                                mediaItem.Id,
                                mediaItem.Name,
                                mediaItem.OriginalName,
                                if (mediaItem.FirstAirDate.isNullOrBlank()) {
                                    getString(R.string.itemview_no_date)
                                } else {
                                    net.forestany.forestj.lib.Helper
                                        .fromDateString(mediaItem.FirstAirDate)
                                        .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"))
                                },
                                bitmap
                            )
                        )
                    }
                }

                // show search results in media item list with BottomSheetDialog
                val dialog = BottomSheetDialog(this@ItemViewActivity)
                val view = View.inflate(this@ItemViewActivity, R.layout.bottom_sheet_dialog_media_items, null)
                val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewMediaItems)
                recyclerView.layoutManager = LinearLayoutManager(this@ItemViewActivity)

                // cancel listener for disabling loading screen
                dialog.setOnCancelListener {
                    isLoading = false
                    hideLoading()
                }

                // what happens if we select a media item
                recyclerView.adapter = MediaItemAdapter(mediaItems) { selectedMediaItem ->
                    // closing BottomSheetDialog
                    dialog.dismiss()

                    // do another lifecycle scope for getting movie or series details
                    lifecycleScope.launch {
                        try {
                            if (typeIsMovie) // handle movie
                            {
                                // look for movie details
                                val o_foo = GlobalInstance.get().searchInstance?.getMovieById(selectedMediaItem.id) ?: throw Exception(getString(R.string.itemview_no_results_movie_id, selectedMediaItem.id))

                                // gather languages
                                var s_languages = ""

                                for (language in o_foo.SpokenLanguages) {
                                    s_languages += language.EnglishName + " "
                                }

                                if (s_languages.isNotEmpty()) {
                                    s_languages = s_languages.substring(0, s_languages.length - 1)
                                }

                                var s_directors = ""
                                var s_screenwriters = ""
                                var s_cast = ""

                                // handle credits info
                                if (o_foo.Credits != null) {
                                    // gather cast
                                    if (o_foo.Credits.Cast != null) {
                                        // accumulate cast items, but only if it is not exceeding 255 characters altogether
                                        for (castItem in o_foo.Credits.Cast) {
                                            if ((!s_cast.contains(castItem.Name)) && (s_cast.length + castItem.Name.length <= 255)) {
                                                s_cast += castItem.Name + ","
                                            }
                                        }

                                        if (s_cast.isNotEmpty()) {
                                            s_cast = s_cast.substring(0, s_cast.length - 1)
                                        }
                                    }

                                    // gather directors and screenwriters
                                    if (o_foo.Credits.Crew != null) {
                                        for (crewItem in o_foo.Credits.Crew) {
                                            if (crewItem.Job.lowercase().contentEquals("director")) {
                                                if (!s_directors.contains(crewItem.Name)) {
                                                    s_directors += crewItem.Name + ","
                                                }
                                            }

                                            if (
                                                (crewItem.Job.lowercase().contentEquals("screenplay")) ||
                                                (crewItem.Job.lowercase().contentEquals("story")) ||
                                                (crewItem.Job.lowercase().contentEquals("writer"))
                                            ) {
                                                if (!s_screenwriters.contains(crewItem.Name)) {
                                                    s_screenwriters += crewItem.Name + ","
                                                }
                                            }
                                        }

                                        if (s_directors.isNotEmpty()) {
                                            s_directors = s_directors.substring(0, s_directors.length - 1)
                                        }

                                        if (s_screenwriters.isNotEmpty()) {
                                            s_screenwriters = s_screenwriters.substring(0, s_screenwriters.length - 1)
                                        }
                                    }
                                }

                                // get poster
                                var s_posterBytes = ""

                                if (o_foo.PosterPath.isNotEmpty()) {
                                    try {
                                        s_posterBytes = withContext(Dispatchers.IO) {
                                            downloadImage(
                                                Util.replacePlaceholders(
                                                    GlobalInstance.get().posterTMDBUrl,
                                                    GlobalInstance.get().posterDesiredWidth.toString(),
                                                    o_foo.PosterPath
                                                )
                                            ) ?: ""
                                        }
                                    } catch (e: Exception) {
                                        throw Exception("Error while downloading poster image; ${e.message}")
                                    }
                                }

                                //choose subtitles
                                val initialList = s_languages.split(" ").filter { it.isNotBlank() }.toMutableList()

                                // ensure "German" is the first item and not duplicated
                                if (!initialList.contains("German")) {
                                    initialList.add(0, "German")
                                } else {
                                    initialList.remove("German")
                                    initialList.add(0, "German")
                                }

                                // prepare list and array for dialog
                                val allLanguages = initialList.toList()
                                val selectedLanguages = BooleanArray(allLanguages.size)

                                // open dialog to select subtitles
                                androidx.appcompat.app.AlertDialog.Builder(this@ItemViewActivity, R.style.SelectionDialogStyle)
                                    .setTitle(getString(R.string.itemview_auto_fill_subtitles))
                                    .setMultiChoiceItems(allLanguages.toTypedArray(), selectedLanguages) { _, which, isChecked ->
                                        selectedLanguages[which] = isChecked
                                    }
                                    .setPositiveButton(getString(R.string.text_ok)) { _, _ ->
                                        // fill subtitles variable with result
                                        val s_subtitles = allLanguages.filterIndexed { index, _ -> selectedLanguages[index] }.joinToString(" ")

                                        // update subtitles in viewpager details fragment
                                        viewModel.updateRecord {
                                            it.ColumnSubtitles = s_subtitles
                                        }
                                    }
                                    .show()

                                // fill item view activity fields with gathered movie details
                                viewModel.updateRecord {
                                    it.ColumnTitle = o_foo.Title

                                    it.ColumnPublicationYear = if (o_foo.ReleaseDate.isNullOrEmpty()) {
                                        0
                                    } else {
                                        net.forestany.forestj.lib.Helper
                                            .fromDateString(o_foo.ReleaseDate)
                                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy"))
                                            .toShort()
                                    }
                                    it.ColumnLengthInMinutes = o_foo.Runtime.toShort()
                                    it.ColumnLanguages = s_languages
                                    it.ColumnDirectors = s_directors
                                    it.ColumnScreenwriters = s_screenwriters
                                    it.ColumnCast = s_cast
                                    it.ColumnPoster = s_posterBytes
                                }

                                // update 'original title' after 'title' has been set, because of auto fill link while typing in 'title', so a little delay too
                                kotlinx.coroutines.delay(100)

                                viewModel.updateRecord {
                                    it.ColumnOriginalTitle = o_foo.OriginalTitle
                                }
                            }
                            else // handle series
                            {
                                // look for series details
                                val o_foo = GlobalInstance.get().searchInstance?.getSeriesById(selectedMediaItem.id) ?: throw Exception(getString(R.string.itemview_no_results_series_id, selectedMediaItem.id))

                                // gather languages
                                var s_languages = ""

                                for (language in o_foo.SpokenLanguages) {
                                    s_languages += language.EnglishName + " "
                                }

                                if (s_languages.isNotEmpty()) {
                                    s_languages = s_languages.substring(0, s_languages.length - 1)
                                }

                                var s_directors = ""
                                var s_screenwriters = ""
                                var s_cast = ""

                                if (o_foo.Credits != null) {
                                    // gather cast
                                    if (o_foo.Credits.Cast != null) {
                                        // accumulate cast items, but only if it is not exceeding 255 characters altogether
                                        for (castItem in o_foo.Credits.Cast) {
                                            if ((!s_cast.contains(castItem.Name)) && (s_cast.length + castItem.Name.length <= 255)) {
                                                s_cast += castItem.Name + ","
                                            }
                                        }

                                        if (s_cast.isNotEmpty()) {
                                            s_cast = s_cast.substring(0, s_cast.length - 1)
                                        }
                                    }

                                    // gather directors and screenwriters
                                    if (o_foo.Credits.Crew != null) {
                                        for (crewItem in o_foo.Credits.Crew) {
                                            if (crewItem.Job.lowercase().contentEquals("director")) {
                                                if (!s_directors.contains(crewItem.Name)) {
                                                    s_directors += crewItem.Name + ","
                                                }
                                            }

                                            if (
                                                (crewItem.Job.lowercase().contentEquals("screenplay")) ||
                                                (crewItem.Job.lowercase().contentEquals("story")) ||
                                                (crewItem.Job.lowercase().contentEquals("writer"))
                                            ) {
                                                if (!s_screenwriters.contains(crewItem.Name)) {
                                                    s_screenwriters += crewItem.Name + ","
                                                }
                                            }
                                        }

                                        if (s_directors.isNotEmpty()) {
                                            s_directors = s_directors.substring(0, s_directors.length - 1)
                                        }

                                        if (s_screenwriters.isNotEmpty()) {
                                            s_screenwriters = s_screenwriters.substring(0, s_screenwriters.length - 1)
                                        }
                                    }

                                    // use createdby as directors
                                    if (s_directors.isEmpty()) {
                                        for (createdBy in o_foo.CreatedBys) {
                                            if (!s_directors.contains(createdBy.Name)) {
                                                s_directors += createdBy.Name + ","
                                            }
                                        }

                                        if (s_directors.isNotEmpty()) {
                                            s_directors = s_directors.substring(0, s_directors.length - 1)
                                        }
                                    }
                                }

                                // get poster
                                var s_posterBytes = ""

                                if (o_foo.PosterPath.isNotEmpty()) {
                                    try {
                                        s_posterBytes = withContext(Dispatchers.IO) {
                                            downloadImage(
                                                Util.replacePlaceholders(
                                                    GlobalInstance.get().posterTMDBUrl,
                                                    GlobalInstance.get().posterDesiredWidth.toString(),
                                                    o_foo.PosterPath
                                                )
                                            ) ?: ""
                                        }
                                    } catch (e: Exception) {
                                        throw Exception("Error while downloading poster image; ${e.message}")
                                    }
                                }

                                //choose subtitles
                                val initialList = s_languages.split(" ").filter { it.isNotBlank() }.toMutableList()

                                // ensure "German" is the first item and not duplicated
                                if (!initialList.contains("German")) {
                                    initialList.add(0, "German")
                                } else {
                                    initialList.remove("German")
                                    initialList.add(0, "German")
                                }

                                // prepare list and array for dialog
                                val allLanguages = initialList.toList()
                                val selectedLanguages = BooleanArray(allLanguages.size)

                                // open dialog to select subtitles
                                androidx.appcompat.app.AlertDialog.Builder(this@ItemViewActivity, R.style.SelectionDialogStyle)
                                    .setTitle(getString(R.string.itemview_auto_fill_subtitles))
                                    .setMultiChoiceItems(allLanguages.toTypedArray(), selectedLanguages) { _, which, isChecked ->
                                        selectedLanguages[which] = isChecked
                                    }
                                    .setPositiveButton(getString(R.string.text_ok)) { _, _ ->
                                        // fill subtitles variable with result
                                        val s_subtitles = allLanguages.filterIndexed { index, _ -> selectedLanguages[index] }.joinToString(" ")

                                        // update subtitles in viewpager details fragment
                                        viewModel.updateRecord {
                                            it.ColumnSubtitles = s_subtitles
                                        }
                                    }
                                    //.setNegativeButton("Cancel", null)
                                    .show()

                                // fill item view activity fields with gathered series details
                                viewModel.updateRecord {
                                    it.ColumnTitle = o_foo.Name

                                    it.ColumnPublicationYear = if (o_foo.FirstAirDate.isNullOrEmpty()) {
                                        0
                                    } else {
                                        net.forestany.forestj.lib.Helper
                                            .fromDateString(o_foo.FirstAirDate)
                                            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy"))
                                            .toShort()
                                    }
                                    it.ColumnLengthInMinutes = 0
                                    it.ColumnLanguages = s_languages
                                    it.ColumnDirectors = s_directors
                                    it.ColumnScreenwriters = s_screenwriters
                                    it.ColumnCast = s_cast
                                    it.ColumnPoster = s_posterBytes
                                }

                                // update 'original title' after 'title' has been set, because of auto fill link while typing in 'title', so a little delay too
                                kotlinx.coroutines.delay(100)

                                viewModel.updateRecord {
                                    it.ColumnOriginalTitle = o_foo.OriginalName
                                }
                            }

                            notifySnackbar(message = getString(R.string.itemview_auto_fill_done), view = findViewById(android.R.id.content))
                        } catch (e: Exception) {
                            errorSnackbar(message = "Error: ${e.message ?: "Exception in autoFill method."}", view = findViewById(android.R.id.content))
                        } finally {
                            isLoading = false
                            hideLoading()
                        }
                    }
                }

                // set content view and show BottomSheetDialog
                dialog.setContentView(view)
                dialog.show()
            } catch (e: Exception) {
                errorSnackbar(message = "Error: ${e.message ?: "Exception in autoFill method."}", view = findViewById(android.R.id.content))
                hideLoading()
                exceptionThrown = true
            } finally {
                isLoading = !exceptionThrown
            }
        }
    }

    private fun saveDataRecord() {
        try {
            // check required values for saving a record
            if ((record.ColumnTitle.isNullOrEmpty()) || (record.ColumnTitle.lowercase().contentEquals("null"))) {
                (findViewById<ViewPager2>(R.id.view_pager)!!).currentItem = 0

                throw Exception(getString(R.string.itemview_enter_title))
            } else if ((record.ColumnOriginalTitle.isNullOrEmpty()) || (record.ColumnOriginalTitle.lowercase().contentEquals("null"))) {
                (findViewById<ViewPager2>(R.id.view_pager)!!).currentItem = 0

                throw Exception(getString(R.string.itemview_enter_originaltitle))
            } else if ((record.ColumnType.isNullOrEmpty()) || (record.ColumnType.lowercase().contentEquals("null"))) {
                (findViewById<ViewPager2>(R.id.view_pager)!!).currentItem = 0

                throw Exception(getString(R.string.itemview_enter_type))
            } else if ((record.ColumnSubType.isNullOrEmpty()) || (record.ColumnSubType.lowercase().contentEquals("null"))) {
                (findViewById<ViewPager2>(R.id.view_pager)!!).currentItem = 0

                throw Exception(getString(R.string.itemview_enter_medium))
            } else if ((record.ColumnFiledUnder.isNullOrEmpty()) || (record.ColumnFiledUnder.lowercase().contentEquals("null"))) {
                (findViewById<ViewPager2>(R.id.view_pager)!!).currentItem = 0

                throw Exception(getString(R.string.itemview_enter_filedunder))
            } else if ((record.ColumnPublicationYear < 1900)) {
                (findViewById<ViewPager2>(R.id.view_pager)!!).currentItem = 0

                throw Exception(getString(R.string.itemview_enter_publication_year))
            } else {
                // only new records
                if (net.forestany.forestj.lib.Helper.isStringEmpty(record.ColumnUUID)) {
                    // check with new record if record(s) already exists with OriginalTitle and PublicationYear
                    val checkRecords = MediaCollectionRecord()
                    checkRecords.Filters = mutableListOf(
                        net.forestany.forestj.lib.sql.Filter("OriginalTitle", record.ColumnOriginalTitle, "=", "AND"),
                        net.forestany.forestj.lib.sql.Filter("PublicationYear", record.ColumnPublicationYear, "=", "AND"),
                        net.forestany.forestj.lib.sql.Filter("Deleted", null, "IS", "AND")
                    )

                    // query for same record(s)
                    val result = checkRecords.getRecords(true)

                    // we found record(s) which are identical and not deleted
                    if (result.size > 0) {
                        throw Exception(getString(R.string.itemview_enter_new_record_already_in_db))
                    }
                }

                // get languages as list
                val a_languages = mutableListOf<String>()

                for (o_language: LanguageRecord in LanguageRecord().records) {
                    a_languages.add(o_language.ColumnLanguage)
                }

                // check languages
                var a_foo = mutableListOf<String>()

                if (!record.ColumnLanguages.isNullOrEmpty()) {
                    for (s_bar in record.ColumnLanguages.split(" ")) {
                        if ((s_bar.lowercase().contentEquals("null")) || (s_bar.trim().isBlank())) {
                            continue
                        } else if (s_bar.lowercase().contentEquals("deutsch")) {
                            if (!a_foo.contains("German")) a_foo.add("German")
                        } else if (s_bar.lowercase().contentEquals("englisch")) {
                            if (!a_foo.contains("English")) a_foo.add("English")
                        } else if (s_bar.lowercase().contentEquals("japanisch")) {
                            if (!a_foo.contains("Japanese")) a_foo.add("Japanese")
                        } else if (s_bar.lowercase().contentEquals("französisch")) {
                            if (!a_foo.contains("French")) a_foo.add("French")
                        } else if (s_bar.lowercase().contentEquals("spanisch")) {
                            if (!a_foo.contains("Spanish")) a_foo.add("Spanish")
                        } else if (s_bar.lowercase().contentEquals("koreanisch")) {
                            if (!a_foo.contains("Korean")) a_foo.add("Korean")
                        } else if (s_bar.lowercase().contentEquals("italienisch")) {
                            if (!a_foo.contains("Italian")) a_foo.add("Italian")
                        } else if (s_bar.lowercase().contentEquals("hindi")) {
                            if (!a_foo.contains("Hindi")) a_foo.add("Hindi")
                        } else if (s_bar.lowercase().contentEquals("russisch")) {
                            if (!a_foo.contains("Russian")) a_foo.add("Russian")
                        } else if (s_bar.lowercase().contentEquals("schwedisch")) {
                            if (!a_foo.contains("Swedish")) a_foo.add("Swedish")
                        } else if (s_bar.lowercase().contentEquals("thailändisch")) {
                            if (!a_foo.contains("Thai")) a_foo.add("Thai")
                        } else {
                            if (!a_foo.contains(s_bar)) a_foo.add(s_bar)

                            // new language?
                            if (!a_languages.contains(s_bar)) {
                                val o_languageRecord = LanguageRecord()
                                o_languageRecord.ColumnLanguage = s_bar
                                o_languageRecord.insertRecord()

                                a_languages.add(s_bar)
                            }
                        }
                    }
                }

                if (a_foo.size > 0) {
                    record.ColumnLanguages = a_foo.joinToString(" ")
                } else {
                    record.ColumnLanguages = null
                }

                // check subtitles
                a_foo = mutableListOf()

                if (!record.ColumnSubtitles.isNullOrEmpty()) {
                    for (s_bar in record.ColumnSubtitles.split(" ")) {
                        if ((s_bar.lowercase().contentEquals("null")) || (s_bar.trim().isBlank())) {
                            continue
                        } else if (s_bar.lowercase().contentEquals("deutsch")) {
                            if (!a_foo.contains("German")) a_foo.add("German")
                        } else if (s_bar.lowercase().contentEquals("englisch")) {
                            if (!a_foo.contains("English")) a_foo.add("English")
                        } else if (s_bar.lowercase().contentEquals("japanisch")) {
                            if (!a_foo.contains("Japanese")) a_foo.add("Japanese")
                        } else if (s_bar.lowercase().contentEquals("französisch")) {
                            if (!a_foo.contains("French")) a_foo.add("French")
                        } else if (s_bar.lowercase().contentEquals("spanisch")) {
                            if (!a_foo.contains("Spanish")) a_foo.add("Spanish")
                        } else if (s_bar.lowercase().contentEquals("koreanisch")) {
                            if (!a_foo.contains("Korean")) a_foo.add("Korean")
                        } else if (s_bar.lowercase().contentEquals("italienisch")) {
                            if (!a_foo.contains("Italian")) a_foo.add("Italian")
                        } else if (s_bar.lowercase().contentEquals("hindi")) {
                            if (!a_foo.contains("Hindi")) a_foo.add("Hindi")
                        } else if (s_bar.lowercase().contentEquals("russisch")) {
                            if (!a_foo.contains("Russian")) a_foo.add("Russian")
                        } else if (s_bar.lowercase().contentEquals("schwedisch")) {
                            if (!a_foo.contains("Swedish")) a_foo.add("Swedish")
                        } else if (s_bar.lowercase().contentEquals("thailändisch")) {
                            if (!a_foo.contains("Thai")) a_foo.add("Thai")
                        } else {
                            if (!a_foo.contains(s_bar)) a_foo.add(s_bar)

                            // new language?
                            if (!a_languages.contains(s_bar)) {
                                val o_languageRecord = LanguageRecord()
                                o_languageRecord.ColumnLanguage = s_bar
                                o_languageRecord.insertRecord()
                            }
                        }
                    }
                }

                if (a_foo.size > 0) {
                    record.ColumnSubtitles = a_foo.joinToString(" ")
                } else {
                    record.ColumnSubtitles = null
                }

                // update last modified column
                record.ColumnLastModified = java.time.LocalDateTime.now().withNano(0)

                if (net.forestany.forestj.lib.Helper.isStringEmpty(record.ColumnUUID)) { // new record
                    // new uuid
                    record.ColumnUUID = net.forestany.forestj.lib.Helper.generateUUID()

                    // insert record
                    record.insertRecord()
                    setResult(MainActivity.RETURN_CODE_INSERTED_AND_RELOAD)
                    finish()
                } else { // update record
                    // update record with unique columns
                    record.updateRecord(true)
                    setResult(MainActivity.RETURN_CODE_UPDATED_AND_RELOAD)
                    finish()
                }
            }
        } catch (_: IllegalStateException) {
            errorSnackbar(message = getString(R.string.itemview_unique_violation), view = findViewById(android.R.id.content))
        } catch (e: Exception) {
            errorSnackbar(message = "Error: ${e.message ?: "Exception in saveDataRecord method."}", view = findViewById(android.R.id.content))
        }
    }

    private fun deleteDataRecord() {
        androidx.appcompat.app.AlertDialog.Builder(this@ItemViewActivity, R.style.AlertDialogStyle)
            .setTitle(getString(R.string.itemview_delete))
            .setMessage(getString(R.string.itemview_delete_question))
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(getString(R.string.text_yes)) { _, _ ->
                try {
                    record.ColumnDeleted = java.time.LocalDateTime.now().withNano(0)

                    if ((record.ColumnId < 1) || (record.ColumnDeleted == null) || (record.updateRecord(true) < 1)) {
                        throw Exception(getString(R.string.itemview_could_not_delete))
                    }

                    setResult(MainActivity.RETURN_CODE_DELETED_AND_RELOAD)
                    finish()
                } catch (e: Exception) {
                    errorSnackbar(message = "Error: ${e.message ?: "Exception in deleteDataRecord method."}", view = findViewById(android.R.id.content))
                }
            }
            .setNegativeButton(getString(R.string.text_no), null)
            .setCancelable(false)
            .show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShare(intent)
    }

    private fun handleIncomingShare(intent: Intent?) {
        // check right action and intent extra text is 'text/plain'
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            // get shared url
            val url = intent.getStringExtra(Intent.EXTRA_TEXT)

            // url must start with https and be a '.jpg', '.jpeg', '.png' or '.webp'
            if ( (url != null) && (url.startsWith("https")) && (
                (url.endsWith(".jpg")) || (url.endsWith(".jpeg")) || (url.endsWith(".png")) || (url.endsWith(".webp"))
            ) ) {
                // check if we saved record as temp
                if (GlobalInstance.get().tempRecord != null) {
                    // download new poster in lifecycle scope
                    lifecycleScope.launch {
                        try {
                            // download poster and get hex bytes as string
                            val s_foo = withContext(Dispatchers.IO) { downloadImage(url) }

                            // update poster record column
                            GlobalInstance.get().tempRecord?.ColumnPoster = s_foo
                            // go to general fragment to force a poster refresh
                            (findViewById<ViewPager2>(R.id.view_pager)!!).currentItem = 0
                            notifySnackbar(message = getString(R.string.itemview_poster_updated), view = findViewById(android.R.id.content))
                        } catch (e: Exception) {
                            errorSnackbar(message = "Error: ${e.message ?: "Exception in handleIncomingShare method."}", view = findViewById(android.R.id.content))
                        }
                    }
                } else {
                    errorSnackbar(message = getString(R.string.itemview_poster_no_temp), view = findViewById(android.R.id.content))
                }
            } else if (url != null) {
                errorSnackbar(message = getString(R.string.itemview_poster_invalid_url, url.substring(url.length - 20)), view = findViewById(android.R.id.content))
            }

            // restore record because we left the app looking for a poster
            if (GlobalInstance.get().tempRecord != null) {
                viewModel.setRecord(GlobalInstance.get().tempRecord!!)
            }
        }
    }

    private fun downloadImage(imageUrl: String?) : String? {
        try {
            if (imageUrl == null) return null

            // call image url with HttpURLConnection
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()

            // get input stream and bytes
            val inputStream = connection.inputStream
            val byteArray = inputStream.readBytes()

            // use jpeg compress format as standard
            var compressFormat = android.graphics.Bitmap.CompressFormat.JPEG

            if (imageUrl.endsWith(".png")) { // change to png compress format
                compressFormat = android.graphics.Bitmap.CompressFormat.PNG
            } else if (imageUrl.endsWith(".webp")) { // change to webp compress format
                compressFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    android.graphics.Bitmap.CompressFormat.WEBP_LOSSLESS
                } else {
                    android.graphics.Bitmap.CompressFormat.WEBP
                }
            }

            // compress image to desired width
            var bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
            val factor = GlobalInstance.get().posterDesiredWidth / bitmap.width.toFloat()
            bitmap = bitmap.scale(GlobalInstance.get().posterDesiredWidth, (bitmap.height * factor).toInt())
            val stream = ByteArrayOutputStream()
            bitmap.compress(compressFormat, 100, stream)
            val byteArrayScaled = stream.toByteArray()

            // return compressed image as hex bytes string
            return net.forestany.forestj.lib.Helper.bytesToHexString(byteArrayScaled, false)
        } catch (_: Exception) {
            return null
        }
    }

    private fun downloadImageReturnBitmap(imageUrl: String?) : Bitmap? {
        try {
            if (imageUrl == null) return null

            // call image url with HttpURLConnection
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()

            // get input stream and bytes
            val inputStream = connection.inputStream
            val byteArray = inputStream.readBytes()
            return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
        } catch (_: Exception) {
            return null
        }
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