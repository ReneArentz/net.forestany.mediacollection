package net.forestany.mediacollection.item

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import net.forestany.mediacollection.R
import net.forestany.mediacollection.main.GlobalInstance
import androidx.core.net.toUri
import net.forestany.mediacollection.main.Util

class PosterFragment : Fragment() {
    private val viewModel: ItemViewModel by activityViewModels()
    private var posterBytes: String = ""
    private var viewInstance: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_poster, container, false)
    }

    override fun onResume() {
        super.onResume()

        if (viewInstance != null) {
            val poster = viewInstance!!.findViewById<ImageView>(R.id.iV_Poster)

            // update poster on resume
            if (!posterBytes.contentEquals(viewModel.record.value?.ColumnPoster ?: "")) {
                if ((viewModel.record.value?.ColumnPoster ?: "").isNotEmpty()) {
                    try {
                        // cast hex string bytes to bitmap
                        val byteArray = net.forestany.forestj.lib.Helper.hexStringToBytes(viewModel.record.value?.ColumnPoster)
                        poster.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
                    } catch (_: Exception) {
                        // error while casting hey string bytes to bitmap -> use standard image
                        val inputStream = viewInstance!!.context.assets.open("no_image.jpg")
                        poster.setImageBitmap(BitmapFactory.decodeStream(inputStream))
                        inputStream.close()
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewInstance = view

        try {
            val poster = view.findViewById<ImageView>(R.id.iV_Poster)

            // set default image
            val inputStream = view.context.assets.open("no_image.jpg")
            poster.setImageBitmap(BitmapFactory.decodeStream(inputStream))
            inputStream.close()

            val button = view.findViewById<Button>(R.id.button)

            viewModel.record.observe(viewLifecycleOwner) { record ->
                // update poster if it has changed and is not empty
                if (!posterBytes.contentEquals(record.ColumnPoster ?: "")) {
                    if (record.ColumnPoster.isNotEmpty()) {
                        try {
                            // cast hex string bytes to bitmap
                            val byteArray = net.forestany.forestj.lib.Helper.hexStringToBytes(record.ColumnPoster)
                            poster.setImageBitmap(BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size))
                        } catch (_: Exception) {
                            // error while casting hey string bytes to bitmap -> use standard image
                            val inputStreamException = view.context.assets.open("no_image.jpg")
                            poster.setImageBitmap(BitmapFactory.decodeStream(inputStreamException))
                            inputStreamException.close()
                        }
                    }
                }
            }

            button.setOnClickListener {
                /* save record before leaving */
                GlobalInstance.get().tempRecord = viewModel.record.value

                // use 'original title' as query filter if it is not null or empty
                val s_foo = if ((GlobalInstance.get().tempRecord != null) && (!GlobalInstance.get().tempRecord?.ColumnOriginalTitle.isNullOrEmpty())) {
                    Util.replacePlaceholders(GlobalInstance.get().posterMoviePosterDbUrl, GlobalInstance.get().tempRecord?.ColumnOriginalTitle?.lowercase()!!.replace(" ", "+")) ?: "https://duckduckgo.com/"
                } else {
                    GlobalInstance.get().posterMoviePosterDbUrl?.substring(0, GlobalInstance.get().posterMoviePosterDbUrl?.lastIndexOf("/") ?: 0) ?: "https://duckduckgo.com/"
                }

                // open browser to query for a poster
                val intent = Intent(Intent.ACTION_VIEW, s_foo.toUri())
                startActivity(intent)
            }
        } catch (_: Exception) {
            // nothing to do
        }
    }
}