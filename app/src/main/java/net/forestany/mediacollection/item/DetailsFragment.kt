package net.forestany.mediacollection.item

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import net.forestany.mediacollection.R
import net.forestany.mediacollection.main.LanguageRecord
import net.forestany.mediacollection.main.Util.errorSnackbar

class DetailsFragment : Fragment() {
    private val viewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val o_languages = mutableListOf<String>()

            for (o_language: LanguageRecord in LanguageRecord().records) {
                o_languages.add(o_language.ColumnLanguage)
            }

            val edittextLastSeen = view.findViewById<TextInputEditText>(R.id.eT_LastSeen)
            val edittextLengthInMinutes = view.findViewById<TextInputEditText>(R.id.eT_LengthInMinutes)
            val textviewTimeConversion = view.findViewById<TextView>(R.id.tV_TimeConversion)

            val multitextviewLanguages = view.findViewById<MultiAutoCompleteTextView>(R.id.eT_Languages)
            multitextviewLanguages.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.item_dropdown,
                    R.id.dropdown_item_text,
                    o_languages
                )
            )
            multitextviewLanguages.setDropDownBackgroundResource(R.color.colorSurface)
            multitextviewLanguages.setTokenizer(SpaceTokenizer())

            val multitextviewSubtitles = view.findViewById<MultiAutoCompleteTextView>(R.id.eT_Subtitles)
            multitextviewSubtitles.setAdapter(
                ArrayAdapter(
                    requireContext(),
                    R.layout.item_dropdown,
                    R.id.dropdown_item_text,
                    o_languages
                )
            )
            multitextviewSubtitles.setDropDownBackgroundResource(R.color.colorSurface)
            multitextviewSubtitles.setTokenizer(SpaceTokenizer())

            val edittextDirectors = view.findViewById<TextInputEditText>(R.id.eT_Directors)
            val edittextScreenwriters = view.findViewById<TextInputEditText>(R.id.eT_Screenwriters)
            val edittextCast = view.findViewById<TextInputEditText>(R.id.eT_Cast)

            viewModel.record.observe(viewLifecycleOwner) { record ->
                if (record.ColumnLastSeen != null) {
                    if (!edittextLastSeen.text.toString().contentEquals(record.ColumnLastSeen.toString())) {
                        edittextLastSeen.setText(record.ColumnLastSeen.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                    }
                } else {
                    edittextLastSeen.setText(getString(R.string.itemview_detailsfragment_not_seen))
                }

                if (
                    (!edittextLengthInMinutes.text.toString().trim().contentEquals(record.ColumnLengthInMinutes.toString())) &&
                    (record.ColumnLengthInMinutes.toInt() != 0)
                ) {
                    if (record.ColumnLengthInMinutes < 1) {
                        edittextLengthInMinutes.setText("")
                    } else {
                        edittextLengthInMinutes.setText(record.ColumnLengthInMinutes.toString())
                    }
                }

                if (!multitextviewLanguages.text.toString().contentEquals(record.ColumnLanguages ?: "")) {
                    multitextviewLanguages.setText(record.ColumnLanguages)
                }

                if (!multitextviewSubtitles.text.toString().contentEquals(record.ColumnSubtitles ?: "")) {
                    multitextviewSubtitles.setText(record.ColumnSubtitles)
                }

                if ((record.ColumnDirectors != null) && (!edittextDirectors.text.toString().contentEquals(record.ColumnDirectors))) {
                    edittextDirectors.setText(record.ColumnDirectors.toString())
                }

                if ((record.ColumnScreenwriters != null) && (!edittextScreenwriters.text.toString().contentEquals(record.ColumnScreenwriters))) {
                    edittextScreenwriters.setText(record.ColumnScreenwriters.toString())
                }

                if ((record.ColumnCast != null) && (!edittextCast.text.toString().contentEquals(record.ColumnCast))) {
                    edittextCast.setText(record.ColumnCast.toString())
                }
            }

            // only update last seen with long click on it
            edittextLastSeen.setOnLongClickListener {
                viewModel.updateRecord { it.ColumnLastSeen = java.time.LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0) }
                return@setOnLongClickListener true
            }

            // handle special textView time conversion from integer minutes to 'Xh Ymin'
            edittextLengthInMinutes.doAfterTextChanged { text ->
                val minutes = text.toString().trim().toShortOrNull() ?: 0

                if (minutes.toInt() == 0) {
                    textviewTimeConversion.text = ""
                    textviewTimeConversion.visibility = View.INVISIBLE
                } else {
                    val hours = minutes / 60
                    val remainingMinutes = minutes % 60

                    textviewTimeConversion.text = if (hours > 0) {
                        "${hours}h ${remainingMinutes}min"
                    } else {
                        "${remainingMinutes}min"
                    }
                    textviewTimeConversion.visibility = View.VISIBLE
                }

                viewModel.updateRecord { it.ColumnLengthInMinutes = minutes }
            }

            multitextviewLanguages.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnLanguages = text.toString() }
            }

            // use languages dropdown helper
            multitextviewLanguages.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // when a white space is typed, force showDropDown after slight delay
                    if (!s.isNullOrEmpty() && s.last() == ' ') {
                        multitextviewLanguages.postDelayed({
                            multitextviewLanguages.showDropDown()
                        }, 100)
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            multitextviewSubtitles.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnSubtitles = text.toString() }
            }

            // use subtitles dropdown helper
            multitextviewSubtitles.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // when a white space is typed, force showDropDown after slight delay
                    if (!s.isNullOrEmpty() && s.last() == ' ') {
                        multitextviewSubtitles.postDelayed({
                            multitextviewSubtitles.showDropDown()
                        }, 100)
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })

            edittextDirectors.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnDirectors = text.toString() }
            }

            edittextScreenwriters.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnScreenwriters = text.toString() }
            }

            edittextCast.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnCast = text.toString() }
            }
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onViewCreated method.", view = requireView().findViewById(android.R.id.content))
        }
    }
}
