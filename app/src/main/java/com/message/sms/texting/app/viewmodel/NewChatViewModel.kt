package com.message.sms.texting.app.viewmodel

import android.content.Context
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactItem(
    val name: String,
    val number: String,
    val photoUri: String?,
    val isStarred: Boolean = false
)

class NewChatViewModel : ViewModel() {

    private val _groupedContacts = MutableStateFlow<Map<Char, List<ContactItem>>>(emptyMap())
    val groupedContacts: StateFlow<Map<Char, List<ContactItem>>> = _groupedContacts

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadContacts(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            val contactsMap = mutableMapOf<Char, MutableList<ContactItem>>()
            
            try {
                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI,
                    ContactsContract.CommonDataKinds.Phone.STARRED
                )

                val cursor = context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    projection,
                    null,
                    null,
                    "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
                )

                cursor?.use {
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)
                    val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
                    
                    val seenNumbers = mutableSetOf<String>()

                    while (it.moveToNext()) {
                        val name = it.getString(nameIndex) ?: continue
                        val rawNumber = it.getString(numIndex) ?: continue
                        val photoUri = if (photoIndex != -1) it.getString(photoIndex) else null
                        val isStarred = if (starredIndex != -1) it.getInt(starredIndex) == 1 else false

                        // Normalize number to avoid duplicates
                        val normalizedNumber = rawNumber.replace(Regex("[^0-9+]"), "")
                        if (normalizedNumber.isBlank() || seenNumbers.contains(normalizedNumber)) continue
                        
                        seenNumbers.add(normalizedNumber)

                        val firstLetter = name.firstOrNull()?.uppercaseChar() ?: '#'
                        val groupChar = if (firstLetter.isLetter()) firstLetter else '#'
                        
                        val contact = ContactItem(
                            name = name,
                            number = rawNumber, // Keep raw number for display formatting
                            photoUri = photoUri,
                            isStarred = isStarred
                        )
                        
                        if (isStarred) {
                            if (!contactsMap.containsKey('*')) {
                                contactsMap['*'] = mutableListOf()
                            }
                            contactsMap['*']?.add(contact)
                        }

                        if (!contactsMap.containsKey(groupChar)) {
                            contactsMap[groupChar] = mutableListOf()
                        }
                        contactsMap[groupChar]?.add(contact)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Sort keys so '*' is first, then A-Z, then '#'
            val sortedKeys = contactsMap.keys.sortedWith { c1, c2 ->
                when {
                    c1 == '*' && c2 != '*' -> -1
                    c1 != '*' && c2 == '*' -> 1
                    c1 == '#' && c2 != '#' -> 1
                    c1 != '#' && c2 == '#' -> -1
                    else -> c1.compareTo(c2)
                }
            }
            val sortedMap = mutableMapOf<Char, List<ContactItem>>()
            for (k in sortedKeys) {
                sortedMap[k] = contactsMap[k]!!
            }

            _groupedContacts.value = sortedMap
            _isLoading.value = false
        }
    }
}
