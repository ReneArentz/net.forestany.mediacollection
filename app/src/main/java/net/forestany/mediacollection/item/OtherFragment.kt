package net.forestany.mediacollection.item

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import net.forestany.mediacollection.R
import net.forestany.mediacollection.main.Util.errorSnackbar

class OtherFragment : Fragment() {
    private val viewModel: ItemViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_other, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        try {
            val edittextSpecialFeatures = view.findViewById<TextInputEditText>(R.id.eT_SpecialFeatures)
            val edittextOther = view.findViewById<TextInputEditText>(R.id.eT_Other)
            val edittextLastModified = view.findViewById<TextInputEditText>(R.id.eT_LastModified)

            viewModel.record.observe(viewLifecycleOwner) { record ->
                if (!edittextSpecialFeatures.text.toString().contentEquals(record.ColumnSpecialFeatures ?: "")) {
                    edittextSpecialFeatures.setText(record.ColumnSpecialFeatures)
                }

                if (!edittextOther.text.toString().contentEquals(record.ColumnOther ?: "")) {
                    edittextOther.setText(record.ColumnOther)
                }

                // use own format for 'last modified'
                if (record.ColumnLastModified != null) {
                    if (!edittextLastModified.text.toString().contentEquals(record.ColumnLastModified.toString())) {
                        edittextLastModified.setText( record.ColumnLastModified.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")))
                    }
                } else {
                    edittextLastModified.setText(getString(R.string.itemview_otherfragment_not_modified))
                }
            }

            edittextSpecialFeatures.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnSpecialFeatures = text.toString() }
            }

            edittextOther.doAfterTextChanged { text ->
                viewModel.updateRecord { it.ColumnOther = text.toString() }
            }
        } catch (e: Exception) {
            errorSnackbar(message = e.message ?: "Exception in onViewCreated method.", view = requireView().findViewById(android.R.id.content))
        }
    }
}