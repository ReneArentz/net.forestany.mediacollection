package net.forestany.mediacollection.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import androidx.core.content.edit
import net.forestany.mediacollection.R

class PasswordDialogFragment : DialogFragment() {
    private lateinit var editText: EditText
    private lateinit var sharedPrefs: SharedPreferences

    private val prefKey: String by lazy {
        requireArguments().getString(ARG_PREF_KEY) ?: error(getString(R.string.settings_password_dialog_fragment_key_missing))
    }

    private val dialogTitle: String by lazy {
        requireArguments().getString(ARG_TITLE) ?: error(getString(R.string.settings_password_dialog_fragment_title_missing))
    }

    private val positiveText: String by lazy {
        requireArguments().getString(ARG_POSITIVE_TEXT) ?: getString(R.string.text_ok)
    }

    private val negativeText: String by lazy {
        requireArguments().getString(ARG_NEGATIVE_TEXT) ?: getString(R.string.text_cancel)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        try {
            val context = requireContext()
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

            editText = EditText(context).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                setText(sharedPrefs.getString(prefKey, ""))
            }

            val toggle = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_view)
                setPadding(40, 20, 40, 20)
                setOnClickListener {
                    if ((editText.inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                        editText.transformationMethod = PasswordTransformationMethod.getInstance()
                    } else {
                        editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                        editText.transformationMethod = null
                    }

                    editText.setSelection(editText.text.length)
                }
            }

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(50, 40, 50, 10)
                addView(editText, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(toggle)
            }

            return AlertDialog.Builder(context)
                .setTitle(dialogTitle)
                .setView(container)
                .setPositiveButton(positiveText) { _, _ ->
                    val newValue = editText.text.toString()
                    sharedPrefs.edit { putString(prefKey, newValue) }
                }
                .setNegativeButton(negativeText, null)
                .create()
        } catch (e: Exception) {
            return AlertDialog.Builder(context)
                .setTitle(e.message ?: "Exception in onCreateDialog method.")
                .setPositiveButton(positiveText, null)
                .create()
        }
    }

    companion object {
        private const val ARG_PREF_KEY = "pref_key"
        private const val ARG_TITLE = "title"
        private const val ARG_POSITIVE_TEXT = "positive_text"
        private const val ARG_NEGATIVE_TEXT = "negative_text"

        fun newInstance(
            prefKey: String? = null,
            title: String? = null,
            positiveText: String? = null,
            negativeText: String? = null,
        ): PasswordDialogFragment {
            val args = Bundle().apply {
                putString(ARG_PREF_KEY, prefKey)
                putString(ARG_TITLE, title)
                putString(ARG_POSITIVE_TEXT, positiveText)
                putString(ARG_NEGATIVE_TEXT, negativeText)
            }

            return PasswordDialogFragment().apply { arguments = args }
        }
    }
}