package net.forestany.mediacollection.item

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.forestany.mediacollection.main.MediaCollectionRecord

class ItemViewModel : ViewModel()  {
    private val _record = MutableLiveData<MediaCollectionRecord>().apply {
        value = MediaCollectionRecord()
    }

    val record: LiveData<MediaCollectionRecord> = _record

    fun setRecord(record: MediaCollectionRecord) {
        _record.value = record
    }

    fun updateRecord(column: (MediaCollectionRecord) -> Unit) {
        _record.value?.let {
            column(it)
            _record.value = it
        }
    }
}