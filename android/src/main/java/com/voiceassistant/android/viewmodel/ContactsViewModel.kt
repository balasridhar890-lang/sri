package com.voiceassistant.android.viewmodel

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voiceassistant.android.datastore.AppPreferences
import com.voiceassistant.android.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UI state for contacts screen
 */
data class ContactsState(
    val isLoading: Boolean = true,
    val contacts: List<Contact> = emptyList(),
    val filteredContacts: List<Contact> = emptyList(),
    val searchQuery: String = "",
    val error: String? = null,
    val hasPermission: Boolean = false
)

/**
 * Contact information
 */
data class Contact(
    val id: String,
    val displayName: String,
    val phoneNumbers: List<String> = emptyList(),
    val emailAddresses: List<String> = emptyList(),
    val photoUri: String? = null
)

/**
 * ViewModel for contacts list
 */
@Singleton
class ContactsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val preferencesRepository: PreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ContactsState())
    val uiState: StateFlow<ContactsState> = _uiState.asStateFlow()
    
    init {
        loadContacts()
    }
    
    /**
     * Load contacts from device
     */
    fun loadContacts() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // Check if we have permission to read contacts
                val hasPermission = hasContactPermission()
                if (!hasPermission) {
                    _uiState.value = ContactsState(
                        isLoading = false,
                        error = "Contacts permission required",
                        hasPermission = false
                    )
                    return@launch
                }
                
                val contacts = getContactsFromDevice()
                
                _uiState.value = ContactsState(
                    isLoading = false,
                    contacts = contacts,
                    filteredContacts = contacts,
                    hasPermission = true
                )
            } catch (e: Exception) {
                _uiState.value = ContactsState(
                    isLoading = false,
                    error = e.message ?: "Failed to load contacts"
                )
            }
        }
    }
    
    /**
     * Search contacts by name or phone number
     */
    fun searchContacts(query: String) {
        val currentState = _uiState.value
        val filteredContacts = if (query.isEmpty()) {
            currentState.contacts
        } else {
            currentState.contacts.filter { contact ->
                contact.displayName.contains(query, ignoreCase = true) ||
                contact.phoneNumbers.any { phone ->
                    phone.contains(query, ignoreCase = true)
                }
            }
        }
        
        _uiState.value = currentState.copy(
            searchQuery = query,
            filteredContacts = filteredContacts
        )
    }
    
    /**
     * Clear search and show all contacts
     */
    fun clearSearch() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            searchQuery = "",
            filteredContacts = currentState.contacts
        )
    }
    
    /**
     * Refresh contacts list
     */
    fun refreshContacts() {
        loadContacts()
    }
    
    /**
     * Check if we have contact reading permission
     */
    private fun hasContactPermission(): Boolean {
        return try {
            android.content.pm.PackageManager.PERMISSION_GRANTED == 
            context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get contacts from device contacts database
     */
    private fun getContactsFromDevice(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        val contentResolver = context.contentResolver
        
        try {
            // Query for contact IDs and display names
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME,
                ContactsContract.Contacts.PHOTO_URI
            )
            
            val cursor: Cursor? = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
            )
            
            cursor?.use { contactCursor ->
                while (contactCursor.moveToNext()) {
                    val contactId = contactCursor.getString(
                        contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                    )
                    val displayName = contactCursor.getString(
                        contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
                    )
                    val photoUri = contactCursor.getString(
                        contactCursor.getColumnIndexOrThrow(ContactsContract.Contacts.PHOTO_URI)
                    )
                    
                    // Get phone numbers for this contact
                    val phoneNumbers = getPhoneNumbers(contentResolver, contactId)
                    
                    // Get email addresses for this contact
                    val emailAddresses = getEmailAddresses(contentResolver, contactId)
                    
                    // Only add contacts with phone numbers
                    if (phoneNumbers.isNotEmpty()) {
                        contacts.add(
                            Contact(
                                id = contactId,
                                displayName = displayName,
                                phoneNumbers = phoneNumbers,
                                emailAddresses = emailAddresses,
                                photoUri = photoUri
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            throw Exception("Failed to read contacts: ${e.message}")
        }
        
        return contacts
    }
    
    /**
     * Get phone numbers for a contact
     */
    private fun getPhoneNumbers(contentResolver: ContentResolver, contactId: String): List<String> {
        val phoneNumbers = mutableListOf<String>()
        
        try {
            val phoneCursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )
            
            phoneCursor?.use { cursor ->
                while (cursor.moveToNext()) {
                    val phoneNumber = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    )
                    phoneNumbers.add(phoneNumber)
                }
            }
        } catch (e: Exception) {
            // Ignore individual phone number retrieval errors
        }
        
        return phoneNumbers
    }
    
    /**
     * Get email addresses for a contact
     */
    private fun getEmailAddresses(contentResolver: ContentResolver, contactId: String): List<String> {
        val emailAddresses = mutableListOf<String>()
        
        try {
            val emailCursor: Cursor? = contentResolver.query(
                ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                arrayOf(contactId),
                null
            )
            
            emailCursor?.use { cursor ->
                while (cursor.moveToNext()) {
                    val emailAddress = cursor.getString(
                        cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.ADDRESS)
                    )
                    emailAddresses.add(emailAddress)
                }
            }
        } catch (e: Exception) {
            // Ignore individual email retrieval errors
        }
        
        return emailAddresses
    }
    
    /**
     * Request contact permission
     */
    fun requestContactPermission() {
        // This would typically trigger a permission request flow
        // For now, just reload to check permission status
        loadContacts()
    }
}