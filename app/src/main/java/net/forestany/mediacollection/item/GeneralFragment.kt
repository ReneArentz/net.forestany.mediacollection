package net.forestany.mediacollection.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import net.forestany.mediacollection.R
import net.forestany.mediacollection.main.Util.errorSnackbar

class GeneralFragment : Fragment() {
    private val viewModel: ItemViewModel by activityViewModels()
    private lateinit var textInputEditTextPublicationYear: TextInputEditText
    private lateinit var textInputEditTextOriginalTitle: TextInputEditText

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_general, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            textInputEditTextPublicationYear = view.findViewById(R.id.eT_PublicationYear)
            textInputEditTextOriginalTitle = view.findViewById(R.id.eT_OriginalTitle)

            val edittextTitle = view.findViewById<TextInputEditText>(R.id.eT_Title)
            val edittextOriginalTitle = textInputEditTextOriginalTitle
            val edittextPublicationYear = textInputEditTextPublicationYear
            val radiogroupSubType = view.findViewById<RadioGroup>(R.id.radio_SubType)
            val checkboxBluray = view.findViewById<CheckBox>(R.id.checkbox_bluray)
            val checkboxDVD = view.findViewById<CheckBox>(R.id.checkbox_dvd)
            val checkbox4K = view.findViewById<CheckBox>(R.id.checkbox_4k)
            val edittextFiledUnder = view.findViewById<TextInputEditText>(R.id.eT_FiledUnder)

            viewModel.record.observe(viewLifecycleOwner) { record ->
                if (!edittextTitle.text.toString().contentEquals(record.ColumnTitle ?: "")) {
                    edittextTitle.setText(record.ColumnTitle)
                }

                radiogroupSubType.check(
                    when (record.ColumnType ?: "") {
                        "Movie" -> R.id.radio_movie
                        "Series" -> R.id.radio_series
                        else -> 0
                    }
                )

                if (!edittextFiledUnder.text.toString().contentEquals(record.ColumnFiledUnder ?: "")) {
                    edittextFiledUnder.setText(record.ColumnFiledUnder)
                }

                if (!edittextPublicationYear.text.toString().contentEquals(record.ColumnPublicationYear.toString())) {
                    edittextPublicationYear.setText(record.ColumnPublicationYear.toString())
                }

                if (!edittextOriginalTitle.text.toString().contentEquals(record.ColumnOriginalTitle ?: "")) {
                    edittextOriginalTitle.setText(record.ColumnOriginalTitle)
                }

                val s_foo = record.ColumnSubType ?: ""
                checkboxBluray.isChecked = "bluray" in s_foo
                checkboxDVD.isChecked = "dvd" in s_foo
                checkbox4K.isChecked = "4k" in s_foo
            }

            edittextTitle.doAfterTextChanged { text ->
                viewModel.updateRecord {
                    it.ColumnTitle = text.toString()

                    // auto fill 'filed under' when entering a 'title' if this is a new record
                    if ((!net.forestany.forestj.lib.Helper.isStringEmpty(it.ColumnTitle)) && ((it.ColumnUUID) == null)) {
                        it.ColumnFiledUnder = it.ColumnTitle.substring(0, 1).uppercase()
                    }
                }
            }

            radiogroupSubType.setOnCheckedChangeListener { _, checkedId ->
                val selectedConsole = when (checkedId) {
                    R.id.radio_movie -> "Movie"
                    R.id.radio_series -> "Series"
                    else -> "NULL"
                }

                viewModel.updateRecord { it.ColumnType = selectedConsole }
            }

            edittextFiledUnder.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnFiledUnder = text.toString() }
            }

            edittextPublicationYear.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnPublicationYear = (text.toString().toIntOrNull() ?: 0).toShort() }
            }

            edittextOriginalTitle.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnOriginalTitle = text.toString() }
            }

            // use year number picker for 'publication year'
            edittextPublicationYear.setOnClickListener {
                val picker = NumberPicker(context).apply {
                    val valueYear = (edittextPublicationYear.text.toString().toIntOrNull() ?: 0)
                    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
                    minValue = 1900
                    maxValue = currentYear + 1

                    value = if (valueYear < 1900 || valueYear > currentYear + 1) {
                        currentYear
                    } else {
                        valueYear
                    }
                }

                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.itemview_generalfragment_select_year))
                    .setView(picker)
                    .setPositiveButton(getString(R.string.text_ok)) { _, _ ->
                        edittextPublicationYear.setText(picker.value.toString())
                        viewModel.updateRecord { it.ColumnPublicationYear = (edittextPublicationYear.text.toString().toIntOrNull() ?: 0).toShort() }
                    }
                    .setNegativeButton(getString(R.string.text_cancel), null)
                    .show()
            }

            val checkboxListener = CompoundButton.OnCheckedChangeListener { _, _ ->
                val selected = mutableListOf<String>()
                if (checkboxBluray.isChecked) selected.add("bluray")
                if (checkboxDVD.isChecked) selected.add("dvd")
                if (checkbox4K.isChecked) selected.add("4k")

                if (selected.size < 1) selected.add("NULL")

                viewModel.updateRecord { it.ColumnSubType = selected.joinToString("+") }
            }

            checkboxBluray.setOnCheckedChangeListener(checkboxListener)
            checkboxDVD.setOnCheckedChangeListener(checkboxListener)
            checkbox4K.setOnCheckedChangeListener(checkboxListener)
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onViewCreated method.", view = requireView().findViewById(android.R.id.content))
        }
    }

    fun setPublicationYearAndOriginalTitleVisibility(hide: Boolean) {
        textInputEditTextPublicationYear.isEnabled = !hide
        textInputEditTextOriginalTitle.isEnabled = !hide
    }
}