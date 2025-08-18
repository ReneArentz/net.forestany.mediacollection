package net.forestany.mediacollection.settings

import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.preference.PreferenceManager
import net.forestany.mediacollection.R

class SliderDialogFragment : DialogFragment() {
    private lateinit var valueText: TextView
    private lateinit var seekBar: SeekBar
    private lateinit var sharedPrefs: SharedPreferences

    private val prefKey: String by lazy {
        requireArguments().getString(ARG_PREF_KEY) ?: error(getString(R.string.settings_slider_dialog_fragment_key_missing))
    }

    private val dialogTitle: String by lazy {
        requireArguments().getString(ARG_TITLE) ?: error(getString(R.string.settings_slider_dialog_fragment_title_missing))
    }

    private val minValue: Int by lazy {
        requireArguments().getInt(ARG_MIN, 0)
    }

    private val maxValue: Int by lazy {
        requireArguments().getInt(ARG_MAX, 100)
    }

    private val defaultValue: Int by lazy {
        requireArguments().getInt(ARG_DEFAULT, 50)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        try {
            val context = requireContext()
            sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

            val currentValue = sharedPrefs.getString(prefKey, defaultValue.toString())?.toInt() ?: 50

            valueText = TextView(context).apply {
                text = currentValue.toString()
                textSize = 18f
                setPadding(0, 0, 0, 16)
            }

            seekBar = SeekBar(context).apply {
                max = maxValue - minValue
                progress = currentValue - minValue
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        val foo = progress + minValue
                        valueText.text = foo.toString()
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            val container = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(50, 40, 50, 10)
                addView(valueText)
                addView(seekBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            }

            return AlertDialog.Builder(context)
                .setTitle(dialogTitle)
                .setView(container)
                .setPositiveButton(R.string.text_ok) { _, _ ->
                    val finalValue = seekBar.progress + minValue
                    sharedPrefs.edit { putString(prefKey, finalValue.toString()) }
                }
                .setNegativeButton(R.string.text_cancel, null)
                .create()
        } catch (e: Exception) {
            return AlertDialog.Builder(context)
                .setTitle(e.message ?: "Exception in onCreateDialog method.")
                .setPositiveButton(R.string.text_ok, null)
                .create()
        }
    }

    companion object {
        private const val ARG_PREF_KEY = "pref_key"
        private const val ARG_TITLE = "title"
        private const val ARG_MIN = "min"
        private const val ARG_MAX = "max"
        private const val ARG_DEFAULT = "default"

        fun newInstance(
            prefKey: String,
            title: String,
            min: Int = 0,
            max: Int = 100,
            defaultValue: Int = 50
        ): SliderDialogFragment {
            val args = Bundle().apply {
                putString(ARG_PREF_KEY, prefKey)
                putString(ARG_TITLE, title)
                putInt(ARG_MIN, min)
                putInt(ARG_MAX, max)
                putInt(ARG_DEFAULT, defaultValue)
            }
            return SliderDialogFragment().apply { arguments = args }
        }
    }
}