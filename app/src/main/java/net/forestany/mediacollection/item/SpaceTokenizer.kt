package net.forestany.mediacollection.item

import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.widget.MultiAutoCompleteTextView

class SpaceTokenizer : MultiAutoCompleteTextView.Tokenizer {
    override fun findTokenStart(inputText: CharSequence, cursor: Int): Int {
        var i = cursor

        while (i > 0 && inputText[i - 1] != ' ') {
            i--
        }

        while (i < cursor && inputText[i] == ' ') {
            i++
        }

        return i
    }

    override fun findTokenEnd(inputText: CharSequence, cursor: Int): Int {
        var i = cursor
        val length = inputText.length

        while (i < length) {
            if (inputText[i] == ' ') {
                return i
            } else {
                i++
            }
        }

        return length
    }

    override fun terminateToken(inputText: CharSequence): CharSequence {
        var i = inputText.length

        while (i > 0 && inputText[i - 1] == ' ') {
            i--
        }

        if (i > 0 && inputText[i - 1] == ' ') {
            return inputText
        } else {
            if (inputText is Spanned) {
                val sp = SpannableString("$inputText ")
                TextUtils.copySpansFrom(
                    inputText, 0, inputText.length,
                    Any::class.java, sp, 0
                )

                return sp
            } else {
                return "$inputText "
            }
        }
    }
}