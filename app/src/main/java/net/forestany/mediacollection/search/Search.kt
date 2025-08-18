package net.forestany.mediacollection.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.forestany.mediacollection.R
import net.forestany.mediacollection.main.GlobalInstance
import net.forestany.mediacollection.main.Util
import net.forestany.mediacollection.search.data.MovieDetails
import net.forestany.mediacollection.search.data.MovieSearchResults
import net.forestany.mediacollection.search.data.SeriesDetails
import net.forestany.mediacollection.search.data.SeriesSearchResults

class Search(private val context: android.content.Context) {
    private var movieSearchSchema: String = loadSchemaFromAssets(context, "TMDBMovieSearchSchema.json")
    private var movieDetailsSchema: String = loadSchemaFromAssets(context, "TMDBMovieDetailsCreditsSchema.json")
    private var seriesSearchSchema: String = loadSchemaFromAssets(context, "TMDBSeriesSearchSchema.json")
    private var seriesDetailsSchema: String = loadSchemaFromAssets(context, "TMDBSeriesDetailsCreditsSchema.json")

    companion object {
        fun loadSchemaFromAssets(context: android.content.Context, fileName: String) : String {
            // prepare StringBuilder and BufferedReader
            val stringBuilder = StringBuilder()
            var bufferedReader: java.io.BufferedReader? = null

            try {
                // open BufferedReader with input stream of fileName in assets folder
                bufferedReader = java.io.BufferedReader(
                    java.io.InputStreamReader(context.assets.open(fileName))
                )

                var line: String?

                // add each line in StringBuilder
                while ((bufferedReader.readLine().also { line = it }) != null) {
                    stringBuilder.append(line)
                }
            } catch (_: java.io.IOException) {
                // ignore error with opening or reading file
            } finally {
                // closing BufferedReader instance
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close()
                    } catch (_: java.io.IOException) {
                        // ignore error with closing file
                    }
                }
            }

            // return file content as string
            return stringBuilder.toString()
        }
    }

    private suspend fun fetchURL(sUrl: String): Pair<Int, String?> = withContext(Dispatchers.IO) {
        try {
            // prepare web request GET
            val o_webRequestGet: net.forestany.forestj.lib.net.http.Request = net.forestany.forestj.lib.net.http.Request(net.forestany.forestj.lib.net.http.RequestType.GET, sUrl, "", 0)
            val s_foo = o_webRequestGet.executeWebRequest()
            // return request response code and response content
            return@withContext Pair(o_webRequestGet.responseCode, s_foo)
        } catch (e: Exception) {
            return@withContext Pair(500, "Error: ${e.message}")
        }
    }

    suspend fun searchMovieByTitle(sMovieTitle: String) : MovieSearchResults? {
        // get list of movies by title
        val fetchURLResult = fetchURL(
            Util.replacePlaceholders(
                GlobalInstance.get().tmdbUrlMovies,
                sMovieTitle,
                GlobalInstance.get().tmdbUrlTargetLanguage,
                GlobalInstance.get().tmdbApiKey
            ) ?: throw Exception(context.getString(R.string.itemview_movie_url_null))
        )

        // if not successful, throw code and message as exception
        if (fetchURLResult.first != 200) {
            throw Exception("${fetchURLResult.first}|${fetchURLResult.second}")
        }

        // process json txt data, for cases we are not prepared at the moment
        val s_foo = processJson(fetchURLResult.second, true)

        // decode received response from json to MovieSearchResults
        val o_json = net.forestany.forestj.lib.io.JSON(mutableListOf<String?>(movieSearchSchema))
        val o_movieSearchResults = o_json.jsonDecode(mutableListOf(s_foo)) as MovieSearchResults

        // if we have no results, return null
        if (o_movieSearchResults.TotalResults < 1) {
            return null
        }

        return o_movieSearchResults
    }

    suspend fun getMovieById(iMovieId: Int) : MovieDetails {
        // get movie details by movie-id
        val fetchURLResult = fetchURL(
            Util.replacePlaceholders(
                GlobalInstance.get().tmdbUrlMovieDetails,
                iMovieId.toString(),
                GlobalInstance.get().tmdbUrlTargetLanguage,
                GlobalInstance.get().tmdbApiKey
            ) ?: throw Exception(context.getString(R.string.itemview_movie_details_url_null))
        )

        // if not successful, throw code and message as exception
        if (fetchURLResult.first != 200) {
            throw Exception("${fetchURLResult.first}|${fetchURLResult.second}")
        }

        // process json txt data, for cases we are not prepared at the moment
        val s_foo = processJson(fetchURLResult.second)

        // decode received response from json to MovieDetails
        val o_json = net.forestany.forestj.lib.io.JSON(mutableListOf<String?>(movieDetailsSchema))
        val o_movieDetails = o_json.jsonDecode(mutableListOf(s_foo)) as MovieDetails

        return o_movieDetails
    }

    suspend fun searchSeriesByTitle(sSeriesTitle: String) : SeriesSearchResults? {
        // get list of series by title
        val fetchURLResult = fetchURL(
            Util.replacePlaceholders(
                GlobalInstance.get().tmdbUrlSeries,
                sSeriesTitle,
                GlobalInstance.get().tmdbUrlTargetLanguage,
                GlobalInstance.get().tmdbApiKey
            ) ?: throw Exception(context.getString(R.string.itemview_series_url_null))
        )

        // if not successful, throw code and message as exception
        if (fetchURLResult.first != 200) {
            throw Exception("${fetchURLResult.first}|${fetchURLResult.second}")
        }

        // process json txt data, for cases we are not prepared at the moment
        val s_foo = processJson(fetchURLResult.second, true)

        // decode received response from json to SeriesSearchResults
        val o_json = net.forestany.forestj.lib.io.JSON(mutableListOf<String?>(seriesSearchSchema))
        val o_seriesSearchResults = o_json.jsonDecode(mutableListOf(s_foo)) as SeriesSearchResults

        if (o_seriesSearchResults.TotalResults < 1) {
            return null
        }

        return o_seriesSearchResults
    }

    suspend fun getSeriesById(iSeriesId: Int) : SeriesDetails {
        // get series details by series-id
        val fetchURLResult = fetchURL(
            Util.replacePlaceholders(
                GlobalInstance.get().tmdbUrlSeriesDetails,
                iSeriesId.toString(),
                GlobalInstance.get().tmdbUrlTargetLanguage,
                GlobalInstance.get().tmdbApiKey
            ) ?: throw Exception(context.getString(R.string.itemview_series_details_url_null))
        )

        // if not successful, throw code and message as exception
        if (fetchURLResult.first != 200) {
            throw Exception("${fetchURLResult.first}|${fetchURLResult.second}")
        }

        // process json txt data, for cases we are not prepared at the moment
        val s_foo = processJson(fetchURLResult.second)

        // decode received response from json to SeriesDetails
        val o_json = net.forestany.forestj.lib.io.JSON(mutableListOf<String?>(seriesDetailsSchema))
        val o_seriesDetails = o_json.jsonDecode(mutableListOf(s_foo)) as SeriesDetails

        return o_seriesDetails
    }

    private fun processJson(jsonString: String?, expectGenreIds: Boolean = false): String {
        var s_foo = jsonString ?: ""

        s_foo = cleanFromUnsupportedFields(s_foo)

        if (expectGenreIds) {
            s_foo = cutOutElementsWithoutGenreIds(s_foo)
        }

        s_foo = processCastArray(s_foo)
        s_foo = processCrewArray(s_foo)
        // we do not need to handle empty arrays anymore
        //s_foo = handleEmptyCastAndCrew(s_foo)

        // debug purpose in logcat
        //var start = 0
        //while (start < s_foo.length) {
        //    val end = minOf(start + 800, s_foo.length)
        //    Log.v(s_foo.substring(start, end))
        //    start = end
        //}

        return s_foo
    }

    private fun cleanFromUnsupportedFields(jsonString: String) : String {
        var s_foo = jsonString
        var i_indexVariantOneStart: Int
        var i_indexVariantOneEnd: Int
        var i_indexVariantTwoStart: Int
        var i_indexVariantTwoEnd: Int

        // remove origin_country array, because it is not part of the json schema
        do {
            i_indexVariantOneStart = s_foo.indexOf("\"origin_country\": [")
            i_indexVariantOneEnd = s_foo.indexOf("],", i_indexVariantOneStart)
            i_indexVariantTwoStart = s_foo.indexOf("\"origin_country\":[")
            i_indexVariantTwoEnd = s_foo.indexOf("],", i_indexVariantTwoStart)

            if ((i_indexVariantOneStart > 0) && (i_indexVariantOneEnd > 0)) {
                s_foo = s_foo.substring(0, i_indexVariantOneStart) + s_foo.substring(i_indexVariantOneEnd + 2)
            } else if ((i_indexVariantTwoStart > 0) && (i_indexVariantTwoEnd > 0)) {
                s_foo = s_foo.substring(0, i_indexVariantTwoStart) + s_foo.substring(i_indexVariantTwoEnd + 2)
            }
        } while ((i_indexVariantOneStart + i_indexVariantTwoStart) > 0)

        // remove languages array, because it is not part of the json schema
        do {
            i_indexVariantOneStart = s_foo.indexOf("\"languages\": [")
            i_indexVariantOneEnd = s_foo.indexOf("],", i_indexVariantOneStart)
            i_indexVariantTwoStart = s_foo.indexOf("\"languages\":[")
            i_indexVariantTwoEnd = s_foo.indexOf("],", i_indexVariantTwoStart)

            if ((i_indexVariantOneStart > 0) && (i_indexVariantOneEnd > 0)) {
                s_foo = s_foo.substring(0, i_indexVariantOneStart) + s_foo.substring(i_indexVariantOneEnd + 2)
            } else if ((i_indexVariantTwoStart > 0) && (i_indexVariantTwoEnd > 0)) {
                s_foo = s_foo.substring(0, i_indexVariantTwoStart) + s_foo.substring(i_indexVariantTwoEnd + 2)
            }
        } while ((i_indexVariantOneStart + i_indexVariantTwoStart) > 0)

        // replace "type" with "seriestype", because "type" is a central element of json schema definitions
        if (s_foo.indexOf("\"type\":") > 0) {
            s_foo = s_foo.replace("\"type\":", "\"seriestype\":")
        }

        return s_foo
    }

    private fun processCastArray(jsonString: String): String {
        // process cast array, so that we only have 15 objects
        val castStart = jsonString.indexOf("\"cast\":[") + 7
        if (castStart == 6) return jsonString

        val castContentStart = castStart + 1
        var braceCount = 0
        var objectsFound = 0
        var currentPos = castContentStart
        var lastObjectEnd = castContentStart

        // gather objects until we have 15 or at the end of the array
        while (objectsFound <= 15 && currentPos < jsonString.length) {
            when (jsonString[currentPos]) {
                '{' -> {
                    braceCount++
                    if (braceCount == 1) {
                        objectsFound++
                    }
                }
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        // remember end of last object
                        lastObjectEnd = currentPos
                    }
                }
                ']' -> {
                    if (braceCount == 0) {
                        return jsonString
                    }
                }
            }
            currentPos++
        }

        // find end of cast array
        var arrayEnd = castStart + 1
        var arrayBraceCount = 1
        while (arrayBraceCount > 0 && arrayEnd < jsonString.length) {
            when (jsonString[arrayEnd]) {
                '[' -> arrayBraceCount++
                ']' -> arrayBraceCount--
            }
            arrayEnd++
        }

        // substring to 15 objects or less and go on from end of cast array
        return jsonString.substring(0, lastObjectEnd + 1) +
            "]" +
            jsonString.substring(arrayEnd)
    }

    private fun processCrewArray(jsonString: String): String {
        // process crew array, so that we only have "Director", "Screenplay", "Story", "Writer"
        val crewStart = jsonString.indexOf("\"crew\":[") + 7
        if (crewStart == 6) return jsonString

        val crewContentStart = crewStart + 1
        var currentPos = crewContentStart
        var braceCount = 0
        var currentJobValue: String? = null
        var objectStart = -1
        val filteredObjects = mutableListOf<String>()

        // gather objects we want to have
        while (currentPos < jsonString.length) {
            when (jsonString[currentPos]) {
                '{' -> {
                    if (braceCount == 0) {
                        objectStart = currentPos
                    }
                    braceCount++
                }
                '}' -> {
                    braceCount--
                    if (braceCount == 0) {
                        if (currentJobValue in setOf("Director", "Screenplay", "Story", "Writer")) {
                            // add object to filtered objects
                            filteredObjects.add(jsonString.substring(objectStart, currentPos + 1))
                        }
                        currentJobValue = null
                    }
                }
                '"' -> {
                    if (currentPos + 7 < jsonString.length &&
                        jsonString.substring(currentPos, currentPos + 7) == "\"job\":\"") {
                        val valueStart = currentPos + 7
                        val valueEnd = jsonString.indexOf('"', valueStart)
                        if (valueEnd != -1) {
                            currentJobValue = jsonString.substring(valueStart, valueEnd)
                        }
                    }
                }
                ']' -> {
                    if (braceCount == 0) {
                        break
                    }
                }
            }
            currentPos++
        }

        // find end of crew array
        var arrayEnd = crewStart + 1
        var arrayBraceCount = 1
        while (arrayBraceCount > 0 && arrayEnd < jsonString.length) {
            when (jsonString[arrayEnd]) {
                '[' -> arrayBraceCount++
                ']' -> arrayBraceCount--
            }
            arrayEnd++
        }

        // substring crew array start, all filtered objects and go on from end of crew array
        return jsonString.substring(0, crewContentStart) +
                filteredObjects.joinToString(",") +
                "]" +
                jsonString.substring(arrayEnd)
    }

    private fun cutOutElementsWithoutGenreIds(jsonString: String): String {
        val result = StringBuilder()
        var i = 1 // skip first '{'
        val n = jsonString.length - 1 // ignore last '}'

        while (i < n) {
            // check the current json block
            if (jsonString[i] == '{') {
                var braceCount = 1
                var j = i + 1

                // look for end of current json block
                while (j < n && braceCount > 0) {
                    if (jsonString[j] == '{') braceCount++
                    else if (jsonString[j] == '}') braceCount--
                    j++
                }

                // we have our json block until position j
                if (braceCount == 0) {
                    val block = jsonString.substring(i, j)

                    // check if "genre_ids" is in this block
                    if ("\"genre_ids\":" in block) {
                        // add block to result
                        result.append(block)

                        // include a trailing comma if it exists
                        if (j < n && jsonString[j] == ',') {
                            result.append(',')
                            j++
                        }
                    }

                    // continue at position j, because we added the block to result
                    i = j
                } else {
                    // unbalanced braces – just copy character and move on
                    result.append(jsonString[i])
                    i++
                }
            } else {
                // add to result
                result.append(jsonString[i])
                i++
            }
        }

        // add '{' and '}' back to result
        var s_foo = "{${result.toString().trim()}}"

        // very dirty hack
        s_foo = s_foo.replace("},]", "}]")

        return s_foo
    }

    @Suppress("unused")
    private fun handleEmptyCastAndCrew(jsonString: String): String {
        /*
        cast example:
        "cast":[{"adult":false,"gender":1,"id":1,"known_for_department":"Nothing","name":"Max Mustermann","original_name":"Max Mustermann","popularity":0.0,"profile_path":"/none.jpg","cast_id":1,"character":"Mustermann","credit_id":"abcdefg","order":1}]

        crew example:
        "crew":[{"adult":false,"gender":1,"id":1,"known_for_department":"Nothing","name":"Max Mustermann","original_name":"Max Mustermann","popularity":0.0,"profile_path":"/none.jpg","credit_id":"abcdefg","department":"Nothing","job":"Nothing"}]
        */

        var s_foo = jsonString

        // we have empty cast array
        if (s_foo.indexOf("\"cast\":[]") > 0) {
            // create dummy cast array with one entry
            s_foo = s_foo.replace("\"cast\":[]", "\"cast\":[{\"adult\":false,\"gender\":1,\"id\":1,\"known_for_department\":\"Nothing\",\"name\":\"Max Mustermann\",\"original_name\":\"Max Mustermann\",\"popularity\":0.0,\"profile_path\":\"/none.jpg\",\"cast_id\":1,\"character\":\"Mustermann\",\"credit_id\":\"abcdefg\",\"order\":1}]")
        }

        // we have empty crew array
        if (s_foo.indexOf("\"crew\":[]") > 0) {
            // create dummy crew array with one entry
            s_foo = s_foo.replace("\"crew\":[]", "\"crew\":[{\"adult\":false,\"gender\":1,\"id\":1,\"known_for_department\":\"Nothing\",\"name\":\"Max Mustermann\",\"original_name\":\"Max Mustermann\",\"popularity\":0.0,\"profile_path\":\"/none.jpg\",\"credit_id\":\"abcdefg\",\"department\":\"Nothing\",\"job\":\"Nothing\"}]")
        }

        return s_foo
    }
}