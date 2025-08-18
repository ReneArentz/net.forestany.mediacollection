package net.forestany.mediacollection.main

class GlobalInstance {
    companion object {
        @Volatile
        private var instance: GlobalInstance? = null

        fun get(): GlobalInstance {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = GlobalInstance()
                    }
                }
            }

            return instance!!
        }
    }

    var fJGlob: net.forestany.forestj.lib.Global = net.forestany.forestj.lib.Global.get()
    var tempRecord: MediaCollectionRecord? = null
    var searchInstance: net.forestany.mediacollection.search.Search? = null

    var showMediaTitle = false
    var backupFolder: String? = null
    var swipeRefreshDialog = false
    var toolbarOffset = 0
    var toolbarWaitAutosearch = 0
    var posterDesiredWidth = 0
    var posterOverviewFixedHeight = 0
    var posterTMDBUrl: String? = null
    var posterMoviePosterDbUrl: String? = null
    var tmdbUrlMovies: String? = null
    var tmdbUrlMovieDetails: String? = null
    var tmdbUrlSeries: String? = null
    var tmdbUrlSeriesDetails: String? = null
    var tmdbUrlTargetLanguage: String? = null
    var tmdbApiKey: String? = null
    var syncAutomatic: Long? = null
    var syncServerIp: String? = null
    var syncServerPort = 0
    var syncReceiveBufferSize = 0
    var syncCommonPassphrase: String? = null
    var syncAuthUser: String? = null
    var syncAuthPassphrase: String? = null
    var syncTruststoreFilename: String? = null
    var syncTruststorePassword: String? = null
    var syncReceiveMaxUnknownAmountInMiB = 0
    var lastAutoSynchronizationDateTime: String? = null
}