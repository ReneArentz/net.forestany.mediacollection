package net.forestany.mediacollection.main

import android.graphics.Color
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.core.graphics.toColorInt
import com.google.android.material.snackbar.Snackbar
import net.forestany.mediacollection.R
import java.util.zip.Deflater
import java.util.zip.Inflater


object Util {
    fun customSnackbar(
        view: View,
        message: String,
        textColor: Int = Color.WHITE,
        backgroundColor: Int = Color.BLACK,
        actionTextColor: Int = Color.WHITE,
        actionBackgroundColor: Int = Color.DKGRAY,
        anchorView: View? = null,
        length: Int = Snackbar.LENGTH_INDEFINITE
    ) {
        val snackbar = Snackbar.make(view, message, length)

        if (anchorView != null) {
            snackbar.setAnchorView(anchorView)
        }

        snackbar.setBackgroundTint(backgroundColor)

        val textView = snackbar.view.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_text
        )

        textView.apply {
            setTextColor(textColor)
            textSize = 16f
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.MARQUEE
            marqueeRepeatLimit = -1
            isSelected = true /* marquee feature */
            setHorizontallyScrolling(true)
        }

        snackbar.setAction(view.context.getString(R.string.text_ok)) { snackbar.dismiss() }

        val actionView = snackbar.view.findViewById<TextView>(
            com.google.android.material.R.id.snackbar_action
        )

        actionView?.apply {
            setTextColor(actionTextColor)
            textSize = 18f
            setPadding(24, 8, 24, 8)
            setBackgroundColor(actionBackgroundColor)
        }

        snackbar.show()
    }

    fun notifySnackbar(
        view: View,
        message: String,
        anchorView: View? = null,
        length: Int = Snackbar.LENGTH_INDEFINITE
    ) {
        customSnackbar(
            view,
            message,
            Color.WHITE,
            "#0F5132".toColorInt(),
            Color.WHITE,
            "#1FA868".toColorInt(),
            anchorView,
            length
        )
    }

    fun errorSnackbar(
        view: View,
        message: String,
        anchorView: View? = null,
        length: Int = Snackbar.LENGTH_INDEFINITE
    ) {
        customSnackbar(
            view,
            message,
            Color.WHITE,
            "#DC3545".toColorInt(),
            Color.WHITE,
            "#FF7992".toColorInt(),
            anchorView,
            length
        )
    }

    fun replacePlaceholders(input: String?, vararg args: String?): String? {
        if (input == null) return null

        var result: String = input

        for (i in 1..9) {
            val placeholder = "%$i"

            if (i <= args.size) {
                result = result.replace(placeholder, args[i - 1] ?: "")
            }
        }

        return result
    }

    // compress a hex string to Base64
    fun compress(p_s_hexString: String): String {
        // convert hex string to byte array
        val input = net.forestany.forestj.lib.Helper.hexStringToBytes(p_s_hexString)

        // compress
        val deflater = Deflater()
        deflater.setInput(input)
        deflater.finish()

        val buffer = ByteArray(input.size)
        val compressedDataLength = deflater.deflate(buffer)
        deflater.end()

        val compressedBytes: ByteArray = buffer.copyOf(compressedDataLength)

        // encode as Base64 string
        return java.util.Base64.getEncoder().encodeToString(compressedBytes)
    }

    // decompress Base64 back to hex string
    fun decompress(p_s_base64Compressed: String?): String {
        // decode Base64 to compressed bytes
        val compressedData: ByteArray = java.util.Base64.getDecoder().decode(p_s_base64Compressed)

        // decompress
        val inflater = Inflater()
        inflater.setInput(compressedData)

        val buffer = ByteArray(10_000_000) // big enough buffer
        val length = inflater.inflate(buffer)
        inflater.end()

        val result: ByteArray = buffer.copyOf(length)

        // convert back to hex string
        return net.forestany.forestj.lib.Helper.bytesToHexString(result, false)
    }
}