package net.forestany.mediacollection.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.forestany.mediacollection.R
import androidx.core.net.toUri

class MCForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private lateinit var notificationManager: NotificationManager
    private lateinit var builder: NotificationCompat.Builder

    companion object {
        private const val TAG = "MCForegroundService"
        private const val NOTIFICATION_ID = 1
        const val ACTION_SYNC = "net.forestany.mediacollection.SYNC"
        const val ACTION_IMPORT = "net.forestany.mediacollection.IMPORT"
        const val IMPORT_FILE_JSON = "json_file_content"
        const val IMPORT_FILE_DATA = "data_file_content"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val title = when (intent?.action) {
            ACTION_SYNC -> {
                getString(R.string.main_foreground_service_progress_title_synchronise)
            }
            ACTION_IMPORT -> {
                getString(R.string.main_foreground_service_progress_title_import)
            }
            else -> ""
        }

        builder = createNotificationBuilder(title)
        startForeground(NOTIFICATION_ID, builder.build())

        when (intent?.action) {
            ACTION_SYNC -> {
                serviceScope.launch {
                    performSync()
                    finishNotification(getString(R.string.main_sync_finished))
                }
            }
            ACTION_IMPORT -> {
                val jsonFile = intent.getStringExtra(IMPORT_FILE_JSON) ?: ""
                val txtFile = intent.getStringExtra(IMPORT_FILE_DATA) ?: ""

                serviceScope.launch {
                    performImport(jsonFile, txtFile)
                    finishNotification(getString(R.string.main_import_finished))
                }
            }
            else -> Log.w(TAG, "Unknown action for foreground service")
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotificationBuilder(title: String): NotificationCompat.Builder {
        val channelId = "media_collection_service_channel"

        val channel = NotificationChannel(
            channelId,
            title,
            NotificationManager.IMPORTANCE_LOW
        )

        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(getString(R.string.main_foreground_service_starting))
            .setSmallIcon(R.drawable.ic_foreground_service)
            .setOnlyAlertOnce(true)
    }

    private fun updateProgress(progress: Int, max: Int, message: String, indeterminate: Boolean = false) {
        if (notificationManager.areNotificationsEnabled()) {
            if ((progress == 0) && (max == 0)) {
                builder.setContentText(message)
                    .setProgress(max, progress, indeterminate)
                    .setOngoing(true)
            } else {
                val progressPercent = ((progress.toFloat() / max) * 100)
                builder.setContentText("$message ${String.format(java.util.Locale.getDefault(), "%.2f", progressPercent)} %")
                    .setProgress(100, progressPercent.toInt(), indeterminate)
                    .setOngoing(true)
            }

            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private suspend fun finishNotification(message: String) {
        if (notificationManager.areNotificationsEnabled()) {
            kotlinx.coroutines.delay(2500)
            builder.setContentText(message)
                .setProgress(0, 0, false)
                .setOngoing(false)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            kotlinx.coroutines.delay(2500)
            stopForeground(STOP_FOREGROUND_DETACH)
            stopSelf()
        }
    }

    private fun performSync() {
        var sslContext: javax.net.ssl.SSLContext? = null
        lateinit var authPassphrase: String
        lateinit var o_clientTask: net.forestany.forestj.lib.net.sock.task.send.https.TinyHttpsClient<javax.net.ssl.SSLSocket>

        // load JSONMediaCollectionSmall.json schema from assets folder
        val jsonSmallSchema = net.forestany.mediacollection.search.Search.loadSchemaFromAssets(this, "JSONMediaCollectionSmall.json")
        // load JSONMediaCollectionRecord.json schema from assets folder
        val jsonRecordSchema = net.forestany.mediacollection.search.Search.loadSchemaFromAssets(this, "JSONMediaCollectionRecord.json")

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
            return
        }

        try {
            // prepare json instances
            val o_json = net.forestany.forestj.lib.io.JSON(mutableListOf(jsonSmallSchema))
            val o_jsonRecord = net.forestany.forestj.lib.io.JSON(mutableListOf(jsonRecordSchema))

            //region Step 1: send all data to server
            // create json media collection instance
            var o_jsonMediaCollection = JSONMediaCollectionSmall()
            o_jsonMediaCollection.Timestamp = java.time.LocalDateTime.now().withNano(0)

            // get all language records
            val o_languageRecordInstance = LanguageRecord()

            for (o_languageRecord in o_languageRecordInstance.getRecords(true)) {
                o_jsonMediaCollection.Languages.add(o_languageRecord)
            }

            // get all media collection records
            val o_mediaCollectionRecordSmallInstance = MediaCollectionRecordSmall()
            o_mediaCollectionRecordSmallInstance.Columns = listOf(
                "Id",
                "UUID",
                "PublicationYear",
                "OriginalTitle",
                "LastModified",
                "Deleted",
                "Poster"
            )

            for (o_mediaCollectionRecord in o_mediaCollectionRecordSmallInstance.getRecords(true)) {
                // delete records with deleted timestamp older than 30 days
                if ( (o_mediaCollectionRecord.ColumnDeleted != null) && (o_mediaCollectionRecord.ColumnDeleted.isBefore( java.time.LocalDateTime.now().withNano(0).minusDays(30) )) ) {
                    o_mediaCollectionRecord.deleteRecord()
                } else {
                    // poster data not in json, decoding would take to much time, encapsulate just the amount of data
                    val posterData: String? = o_mediaCollectionRecord.ColumnPoster

                    if ( posterData.isNullOrBlank() || (posterData.lowercase().contentEquals("null")) ) {
                        o_mediaCollectionRecord.ColumnPoster = "0"
                    } else {
                        o_mediaCollectionRecord.ColumnPoster = posterData.length.toString()
                    }

                    o_jsonMediaCollection.Records.add(o_mediaCollectionRecord)
                }
            }

            // encode to json
            val s_jsonEncoded = o_json.jsonEncode(o_jsonMediaCollection)

            // send json data to server
            o_clientTask.setRequest(
                "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/all",
                net.forestany.forestj.lib.net.http.RequestType.POST
            )
            o_clientTask.contentType = net.forestany.forestj.lib.net.http.PostType.HTML
            o_clientTask.addRequestParameter(s_jsonEncoded, "")
            o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
            o_clientTask.authenticationPassword = authPassphrase

            updateProgress(0, 0, getString(R.string.main_sync_sending_record_overview), true)

            o_clientTask.executeRequest()

            if (o_clientTask.returnCode != 200) {
                throw Exception(getString(R.string.main_sync_exception_post_all, o_clientTask.returnCode, o_clientTask.returnMessage))
            }

            var cnt = 1

            // validate response from sending json data to server
            try {
                val serverResponsePostAll = cleanupResponse(o_clientTask.response)

                updateProgress(0, 0, getString(R.string.main_sync_sent_record_overview), true)

                // got list of record answers
                if (!serverResponsePostAll.contentEquals("null")) {
                    // convert server response to list of record uuid
                    val serverResponses = serverResponsePostAll.split(net.forestany.forestj.lib.net.https.Config.HTTP_LINEBREAK)

                    // if list is not empty
                    if (serverResponses.isNotEmpty()) {
                        // iterate each response line
                        for (response in serverResponses) {
                            updateProgress(cnt++, serverResponses.size, getString(R.string.main_sync_sending_records))

                            if (!response.contains(";")) {
                                continue
                            }

                            val responseCommands = response.split(";")
                            val command = responseCommands[1].lowercase()

                            if ( (responseCommands[0].length == 36) && ((command.contentEquals("insert")) || (command.contentEquals("update")) || (command.contentEquals("updatewithposter"))) ) {
                                // Insert || Update || UpdateWithPoster command
                                val o_mediaCollectionRecordInstance = MediaCollectionRecord()

                                if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(responseCommands[0]) as List<Any>?)) {
                                    val posterData: String? = o_mediaCollectionRecordInstance.ColumnPoster

                                    if ( posterData.isNullOrBlank() || (posterData.lowercase().contentEquals("null")) ) {
                                        o_mediaCollectionRecordInstance.ColumnPoster = "0"
                                    } else {
                                        o_mediaCollectionRecordInstance.ColumnPoster = posterData.length.toString()
                                    }

                                    // encode to json
                                    val s_jsonEncodedRecord = o_jsonRecord.jsonEncode(o_mediaCollectionRecordInstance)

                                    if ((command.contentEquals("insert"))) {
                                        // post record data for insert
                                        o_clientTask.setRequest(
                                            "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/insert",
                                            net.forestany.forestj.lib.net.http.RequestType.POST
                                        )
                                        o_clientTask.contentType = net.forestany.forestj.lib.net.http.PostType.HTML
                                        o_clientTask.addRequestParameter(s_jsonEncodedRecord, "")
                                        o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                                        o_clientTask.authenticationPassword = authPassphrase
                                        o_clientTask.executeRequest()
                                    } else if ((command.contentEquals("update")) || (command.contentEquals("updatewithposter"))) {
                                        // post record data for update
                                        o_clientTask.setRequest(
                                            "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/update",
                                            net.forestany.forestj.lib.net.http.RequestType.POST
                                        )
                                        o_clientTask.contentType = net.forestany.forestj.lib.net.http.PostType.HTML
                                        o_clientTask.addRequestParameter(s_jsonEncodedRecord, "")
                                        o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                                        o_clientTask.authenticationPassword = authPassphrase
                                        o_clientTask.executeRequest()
                                    }

                                    if (o_clientTask.returnCode == 200) {
                                        // validate response from posting data
                                        val serverResponseInsert = cleanupResponse(o_clientTask.response)

                                        if (serverResponseInsert.contentEquals(o_mediaCollectionRecordInstance.ColumnUUID)) {
                                            // post successful
                                            if ((command.contentEquals("insert")) || (command.contentEquals("updatewithposter"))) {
                                                // post poster data with uuid
                                                o_clientTask.setRequest(
                                                    "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/poster",
                                                    net.forestany.forestj.lib.net.http.RequestType.POST
                                                )
                                                o_clientTask.contentType = net.forestany.forestj.lib.net.http.PostType.HTML
                                                o_clientTask.addRequestParameter("uuid", o_mediaCollectionRecordInstance.ColumnUUID)
                                                o_clientTask.addRequestParameter("posterdata", Util.compress(posterData ?: ""))
                                                o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                                                o_clientTask.authenticationPassword = authPassphrase
                                                o_clientTask.executeRequest()

                                                if (o_clientTask.returnCode == 200) {
                                                    // validate response from posting poster data
                                                    val serverResponsePostPoster = cleanupResponse(o_clientTask.response)

                                                    // response should be an integer
                                                    if (net.forestany.forestj.lib.Helper.isInteger(serverResponsePostPoster)) {
                                                        if (serverResponsePostPoster.toInt() != (posterData?.length ?: 0)) {
                                                            // post uncompressed poster data with uuid as another try
                                                            o_clientTask.setRequest(
                                                                "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/poster",
                                                                net.forestany.forestj.lib.net.http.RequestType.POST
                                                            )
                                                            o_clientTask.contentType = net.forestany.forestj.lib.net.http.PostType.HTML
                                                            o_clientTask.addRequestParameter("uuid", o_mediaCollectionRecordInstance.ColumnUUID)
                                                            o_clientTask.addRequestParameter("uncompressed", "true")
                                                            o_clientTask.addRequestParameter("posterdata", posterData)
                                                            o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                                                            o_clientTask.authenticationPassword = authPassphrase
                                                            o_clientTask.executeRequest()

                                                            if (o_clientTask.returnCode == 200) {
                                                                // validate response from posting poster data
                                                                val serverResponsePostPosterUncompressed = cleanupResponse(o_clientTask.response)

                                                                // response should be an integer
                                                                if (net.forestany.forestj.lib.Helper.isInteger(serverResponsePostPosterUncompressed)) {
                                                                    if (serverResponsePostPosterUncompressed.toInt() != (posterData?.length ?: 0)) {
                                                                        Log.e(TAG, "sent poster bytes not successful: received size on server '${serverResponsePostPosterUncompressed.toInt()}' does not match with local size '${(posterData?.length ?: 0)}'")
                                                                    }
                                                                } else {
                                                                    Log.e(TAG, "server response for posting poster data is not an integer: '$serverResponsePostPosterUncompressed'")
                                                                }
                                                            } else {
                                                                Log.e(TAG, "request failed: ${o_clientTask.returnCode}|${o_clientTask.returnMessage}")
                                                            }
                                                        }
                                                    } else {
                                                        Log.e(TAG, "server response for posting poster data is not an integer: '$serverResponsePostPoster'")
                                                    }
                                                } else {
                                                    Log.e(TAG, "request failed: ${o_clientTask.returnCode}|${o_clientTask.returnMessage}")
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, "insert or update post request was not successful; $serverResponseInsert != ${o_mediaCollectionRecordInstance.ColumnUUID}")
                                        }
                                    } else {
                                        Log.e(TAG, "request failed: ${o_clientTask.returnCode}|${o_clientTask.returnMessage}")
                                    }
                                }
                            } else if ( (responseCommands[0].length == 36) && (command.contentEquals("delete")) && (responseCommands.size == 3) ) {
                                // Delete command
                                val o_mediaCollectionRecordInstance = MediaCollectionRecord()

                                if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(responseCommands[0]) as List<Any>?)) {
                                    o_mediaCollectionRecordInstance.ColumnDeleted = net.forestany.forestj.lib.Helper.fromDateTimeString(responseCommands[2])

                                    if (o_mediaCollectionRecordInstance.updateRecord(true) < 0) {
                                        Log.e(TAG, "setting deleted timestamp failed for '${o_mediaCollectionRecordInstance.ColumnOriginalTitle}'")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (_: NullPointerException) {
                // nothing to do if response hits a NullPointerException, go on to next step of synchronization
                Log.e(TAG, "NullPointerException within synchronisation in step 1")
            }
            //endregion

            //region Step 2: get record overview from server
            updateProgress(0, 0, getString(R.string.main_sync_receiving_record_overview), true)

            o_clientTask.setRequest(
                "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/all",
                net.forestany.forestj.lib.net.http.RequestType.GET
            )
            o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
            o_clientTask.authenticationPassword = authPassphrase
            o_clientTask.executeRequest()

            updateProgress(0, 0, getString(R.string.main_sync_received_record_overview), true)

            if (o_clientTask.returnCode != 200) {
                throw Exception(getString(R.string.main_sync_exception_fetch_all, o_clientTask.returnCode, o_clientTask.returnMessage))
            }

            updateProgress(0, 0, getString(R.string.main_sync_decoding_record_overview), true)

            // decode received json data
            val serverResponseGetAll = cleanupResponse(o_clientTask.response)
            o_jsonMediaCollection = o_json.jsonDecode(mutableListOf(serverResponseGetAll)) as JSONMediaCollectionSmall

            updateProgress(0, 0, getString(R.string.main_sync_decoded_record_overview), true)

            // list for note to retrieve record data
            val getRequestList = mutableListOf<String>()

            // iterate all received records
            if (o_jsonMediaCollection.Records.size > 0) {
                val o_mediaCollectionRecordInstance = MediaCollectionRecord()
                cnt = 1

                for (jsonMediaCollectionRecordSmall in o_jsonMediaCollection.Records) {
                    updateProgress(cnt++, o_jsonMediaCollection.Records.size, getString(R.string.main_sync_verify_received_records))

                    if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(jsonMediaCollectionRecordSmall.ColumnUUID) as List<Any>?)) {
                        val columnPosterLength = if (o_mediaCollectionRecordInstance.ColumnPoster == null) 0 else o_mediaCollectionRecordInstance.ColumnPoster.length

                        if (
                            (jsonMediaCollectionRecordSmall.ColumnDeleted == null) && (o_mediaCollectionRecordInstance.ColumnDeleted == null) &&
                            (jsonMediaCollectionRecordSmall.ColumnLastModified.isAfter(o_mediaCollectionRecordInstance.ColumnLastModified))
                        ) {
                            // received record found, received deleted is null on both sides, and received record last modified timestamp is newer -> update all

                            // note UUID with 'UpdateWithPoster' if amounts of poster bytes on both sides do not match, otherwise just 'Update' because received last modified timestamp is newer
                            if ((jsonMediaCollectionRecordSmall.ColumnPoster.toInt()) != columnPosterLength) {
                                getRequestList.add(jsonMediaCollectionRecordSmall.ColumnUUID + ";" + "UpdateWithPoster")
                            } else {
                                getRequestList.add(jsonMediaCollectionRecordSmall.ColumnUUID + ";" + "Update")
                            }
                        } else if ((jsonMediaCollectionRecordSmall.ColumnDeleted != null) && (!jsonMediaCollectionRecordSmall.ColumnDeleted.equals(o_mediaCollectionRecordInstance.ColumnDeleted))) {
                            // received record found, received deleted is not null and local is not equal to it -> only update deleted
                            o_mediaCollectionRecordInstance.ColumnDeleted = jsonMediaCollectionRecordSmall.ColumnDeleted

                            // update record - received deleted is not null and local deleted is not equal to it
                            try {
                                o_mediaCollectionRecordInstance.updateRecord(true)
                            } catch (o_exc: IllegalStateException) {
                                // catch primary/unique violation and ignore it
                                Log.e(TAG, "primary/unique violation updating record with delete timestamp: ${o_exc.message}")
                            }
                        } else if (
                            (jsonMediaCollectionRecordSmall.ColumnDeleted == null) && (o_mediaCollectionRecordInstance.ColumnDeleted == null) &&
                            ((jsonMediaCollectionRecordSmall.ColumnPoster.toInt()) != columnPosterLength)
                        ) {
                            Log.d(TAG, "overview verification: poster bytes length do not match for '${o_mediaCollectionRecordInstance.ColumnOriginalTitle}' -> server'${jsonMediaCollectionRecordSmall.ColumnPoster.toInt()}' != local'$columnPosterLength'")

                            // note UUID with 'UpdateWithPoster' if amounts of poster bytes on both sides do not match
                            getRequestList.add(jsonMediaCollectionRecordSmall.ColumnUUID + ";" + "UpdateWithPoster")
                        }
                    } else {
                        if (jsonMediaCollectionRecordSmall.ColumnDeleted == null) {
                            // received record not found and deleted is null
                            try {
                                // check with new record if record(s) already exists with OriginalTitle and PublicationYear
                                val checkRecords = MediaCollectionRecord()
                                checkRecords.Filters = mutableListOf(
                                    net.forestany.forestj.lib.sql.Filter("OriginalTitle", jsonMediaCollectionRecordSmall.ColumnOriginalTitle, "=", "AND"),
                                    net.forestany.forestj.lib.sql.Filter("PublicationYear", jsonMediaCollectionRecordSmall.ColumnPublicationYear, "=", "AND"),
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
                                        Log.e(TAG, "primary/unique violation updating record with delete timestamp: ${o_exc.message}")
                                    }
                                }

                                // note UUID with 'Insert'
                                getRequestList.add(jsonMediaCollectionRecordSmall.ColumnUUID + ";" + "Insert")
                            } catch (o_exc: IllegalStateException) {
                                // catch select exception and ignore it
                                Log.e(TAG, "issue while searching for identical records: ${o_exc.message}")
                            }
                        }
                    }
                }
            }
            //endregion

            //region Step 3: request record if they are new or changes happened
            if (getRequestList.isNotEmpty()) {
                cnt = 1

                // iterate each uuid entry
                for (request in getRequestList) {
                    updateProgress(cnt++, getRequestList.size, getString(R.string.main_sync_request_records))

                    if (!request.contains(";")) {
                        continue
                    }

                    val requestCommands = request.split(";")
                    val command = requestCommands[1].lowercase()

                    if ( (requestCommands[0].length == 36) && (command.contentEquals("insert")) ) {
                        // get record data from server for insert
                        o_clientTask.setRequest(
                            "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/record?uuid=${requestCommands[0]}",
                            net.forestany.forestj.lib.net.http.RequestType.GET
                        )
                        o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                        o_clientTask.authenticationPassword = authPassphrase
                        o_clientTask.executeRequest()

                        if (o_clientTask.returnCode == 200) {
                            // decode received json data
                            val serverRequestGetRecord = cleanupResponse(o_clientTask.response)
                            val jsonMediaCollectionRecord = o_jsonRecord.jsonDecode(mutableListOf(serverRequestGetRecord)) as MediaCollectionRecord

                            // insert record
                            if (jsonMediaCollectionRecord.insertRecord() < 1) {
                                Log.w(TAG, "could not insert record '${jsonMediaCollectionRecord.ColumnOriginalTitle}'")
                                continue
                            }
                        } else {
                            Log.e(TAG, "request failed: ${o_clientTask.returnCode}|${o_clientTask.returnMessage}")
                        }
                    } else if ( (requestCommands[0].length == 36) && ((command.contentEquals("update")) || (command.contentEquals("updatewithposter"))) ) {
                        val o_mediaCollectionRecordInstance = MediaCollectionRecord()

                        // get local record
                        if (!o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(requestCommands[0]) as List<Any>?)) {
                            Log.e(TAG, "could not find record with uuid '${requestCommands[0]}'")
                            continue
                        }

                        // get record data from server for update
                        o_clientTask.setRequest(
                            "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/record?uuid=${requestCommands[0]}",
                            net.forestany.forestj.lib.net.http.RequestType.GET
                        )
                        o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                        o_clientTask.authenticationPassword = authPassphrase
                        o_clientTask.executeRequest()

                        if (o_clientTask.returnCode == 200) {
                            // decode received json data
                            val serverRequestGetRecord = cleanupResponse(o_clientTask.response)
                            val jsonMediaCollectionRecord = o_jsonRecord.jsonDecode(mutableListOf(serverRequestGetRecord)) as MediaCollectionRecord

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

                            // clear poster data if we want to update record with poster
                            if (command.contentEquals("updatewithposter")) {
                                o_mediaCollectionRecordInstance.ColumnPoster = null
                            }

                            // update record
                            try {
                                if (o_mediaCollectionRecordInstance.updateRecord(true) < 0) {
                                    Log.w(TAG, "could not update record '${o_mediaCollectionRecordInstance.ColumnOriginalTitle}'")
                                    continue
                                }
                            } catch (o_exc: IllegalStateException) {
                                // catch primary/unique violation and ignore it
                                Log.e(TAG, "primary/unique violation updating record: ${o_exc.message}")
                            }
                        } else {
                            Log.e(TAG, "request failed: ${o_clientTask.returnCode}|${o_clientTask.returnMessage}")
                        }
                    }

                    // get or update poster from server if command is 'Insert' or 'UpdateWithPoster'
                    if ( (requestCommands[0].length == 36) && ((command.contentEquals("insert")) || (command.contentEquals("updatewithposter"))) ) {
                        // get poster data from server
                        o_clientTask.setRequest(
                            "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/poster?uuid=${requestCommands[0]}",
                            net.forestany.forestj.lib.net.http.RequestType.GET
                        )
                        o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                        o_clientTask.authenticationPassword = authPassphrase
                        o_clientTask.executeRequest()

                        if (o_clientTask.returnCode == 200) {
                            // get poster bytes from response
                            var serverResponseGetPoster: String? = cleanupResponse(o_clientTask.response)

                            // check for 'null' in case that server side has no poster stored
                            serverResponseGetPoster = if ((serverResponseGetPoster?.length == 4) && (serverResponseGetPoster.lowercase().contentEquals("null"))) {
                                null
                            } else {
                                try {
                                    Util.decompress(serverResponseGetPoster)
                                } catch (exc: Exception) {
                                    Log.e(TAG, "decompress failed for '${requestCommands[0]}'")
                                    null
                                }
                            }

                            // get media collection record with uuid entry
                            var o_mediaCollectionRecordInstance = MediaCollectionRecord()

                            if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(requestCommands[0]) as List<Any>?)) {
                                // update poster data
                                o_mediaCollectionRecordInstance.ColumnPoster = serverResponseGetPoster

                                if (o_mediaCollectionRecordInstance.updateRecord() < 0) {
                                    Log.w(TAG, "could not update record with new poster data '${o_mediaCollectionRecordInstance.ColumnOriginalTitle}'")
                                }
                            }

                            // get poster bytes length to check if we received all bytes
                            o_clientTask.setRequest(
                                "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/posterbyteslength?uuid=${requestCommands[0]}",
                                net.forestany.forestj.lib.net.http.RequestType.GET
                            )
                            o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                            o_clientTask.authenticationPassword = authPassphrase
                            o_clientTask.executeRequest()

                            if (o_clientTask.returnCode == 200) {
                                // validate response from get poster bytes length
                                val serverResponseGetPosterLength = cleanupResponse(o_clientTask.response)
                                val localPosterLength = if (o_mediaCollectionRecordInstance.ColumnPoster == null) 0 else o_mediaCollectionRecordInstance.ColumnPoster.length

                                // response should be an integer
                                if (net.forestany.forestj.lib.Helper.isInteger(serverResponseGetPosterLength)) {
                                    if (serverResponseGetPosterLength.toInt() != localPosterLength) {
                                        Log.w(TAG, "verify poster data: poster bytes length do not match for '${o_mediaCollectionRecordInstance.ColumnOriginalTitle}' -> server'${serverResponseGetPosterLength.toInt()}' != local'$localPosterLength'")

                                        // get uncompressed poster data from server as another try
                                        o_clientTask.setRequest(
                                            "https://${GlobalInstance.get().syncServerIp}:${GlobalInstance.get().syncServerPort}/poster?uuid=${requestCommands[0]}&uncompressed=true",
                                            net.forestany.forestj.lib.net.http.RequestType.GET
                                        )
                                        o_clientTask.authenticationUser = GlobalInstance.get().syncAuthUser
                                        o_clientTask.authenticationPassword = authPassphrase
                                        o_clientTask.executeRequest()

                                        if (o_clientTask.returnCode == 200) {
                                            // get poster bytes from response
                                            var serverResponseGetPosterUncompressed: String? = cleanupResponse(o_clientTask.response)

                                            // check for 'null' in case that server side has no poster stored
                                            if ((serverResponseGetPosterUncompressed?.length == 4) && (serverResponseGetPosterUncompressed.lowercase().contentEquals("null"))) {
                                                serverResponseGetPosterUncompressed = null
                                            }

                                            // get media collection record with uuid entry
                                            o_mediaCollectionRecordInstance = MediaCollectionRecord()

                                            if (o_mediaCollectionRecordInstance.getOneRecord(mutableListOf("UUID"), mutableListOf(requestCommands[0]) as List<Any>?)) {
                                                // update poster data
                                                o_mediaCollectionRecordInstance.ColumnPoster = serverResponseGetPosterUncompressed

                                                if (o_mediaCollectionRecordInstance.updateRecord() < 0) {
                                                    Log.w(TAG, "could not update record with new poster data '${o_mediaCollectionRecordInstance.ColumnOriginalTitle}'")
                                                }
                                            }
                                        } else {
                                            Log.e(TAG, "request failed: ${o_clientTask.returnCode}|${o_clientTask.returnMessage}")
                                        }
                                    }
                                } else {
                                    Log.e(TAG, "server response for request poster data length is not an integer: '$serverResponseGetPosterLength'")
                                }
                            } else {
                                Log.e(TAG, "request failed: ${o_clientTask.returnCode}|${o_clientTask.returnMessage}")
                            }
                        } else {
                            Log.e(TAG, "request failed: ${o_clientTask.returnCode}|${o_clientTask.returnMessage}")
                        }
                    }
                }
            }
            //endregion
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message ?: "Exception in performSync method."}; ${Log.getStackTraceString(e)}")
        }
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

    private fun performImport(jsonFile: String, txtFile: String) {
        try {
            updateProgress(0, 0, getString(R.string.main_import_status), true)

            val jsonContent = contentResolver.openInputStream(jsonFile.toUri())?.bufferedReader()?.use { it.readText() } ?: throw Exception(getString(R.string.main_find_json_failure))
            val dataContent = contentResolver.openInputStream(txtFile.toUri())?.bufferedReader()?.use { it.readText() } ?: throw Exception(getString(R.string.main_find_dat_failure))

            // load JSONMediaCollection.json schema from assets folder
            val jsonSchema = net.forestany.mediacollection.search.Search.loadSchemaFromAssets(this, "JSONMediaCollection.json")

            var o_mediaCollectionRecordInstance = MediaCollectionRecord()
            val o_languageRecordInstance = LanguageRecord()

            // ---------------------------

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

            // decode data
            val contentList = dataContent.split(Regex("\r\n|\r|\n")).filter { it.isNotBlank() }

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
        } catch (e: Exception) {
            Log.e(TAG, "Error: ${e.message ?: "Exception in performImport method."}; ${Log.getStackTraceString(e)}")
        }
    }
}
