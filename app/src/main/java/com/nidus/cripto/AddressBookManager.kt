package com.nidus.cripto.contacts

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Contact(
    val id: String,
    val name: String,
    val address: String,
    val isMainnet: Boolean
)

object AddressBookManager {

    private const val PREF_NAME = "nidus_address_book_prefs"
    private const val KEY_CONTACTS_JSON = "contacts_list_json"

    fun saveContact(context: Context, name: String, address: String, isMainnet: Boolean): Boolean {
        val cleanName = name.trim()
        val cleanAddress = address.trim()

        if (cleanAddress.isEmpty()) return false

        val currentContacts = getContacts(context).toMutableList()

        val existingIndex = currentContacts.indexOfFirst {
            it.address.equals(cleanAddress, ignoreCase = true) && it.isMainnet == isMainnet
        }

        if (cleanName.isEmpty()) {
            if (existingIndex != -1) {
                currentContacts.removeAt(existingIndex)
            } else {
                return true
            }
        } else {
            if (existingIndex != -1) {
                currentContacts[existingIndex] = currentContacts[existingIndex].copy(name = cleanName)
            } else {
                val newContact = Contact(
                    id = System.currentTimeMillis().toString(),
                    name = cleanName,
                    address = cleanAddress,
                    isMainnet = isMainnet
                )
                currentContacts.add(newContact)
            }
        }

        return persistContacts(context, currentContacts)
    }

    fun getContacts(context: Context, isMainnetFilter: Boolean? = null): List<Contact> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_CONTACTS_JSON, null) ?: return emptyList()

        val contactsList = mutableListOf<Contact>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val contact = Contact(
                    id = obj.optString("id", System.currentTimeMillis().toString()),
                    name = obj.optString("name", ""),
                    address = obj.optString("address", ""),
                    isMainnet = obj.optBoolean("isMainnet", true)
                )

                if (isMainnetFilter == null || contact.isMainnet == isMainnetFilter) {
                    contactsList.add(contact)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return contactsList.sortedBy { it.name.lowercase() }
    }

    fun deleteContact(context: Context, contactId: String): Boolean {
        val currentContacts = getContacts(context).toMutableList()
        val removed = currentContacts.removeIf { it.id == contactId }
        return if (removed) persistContacts(context, currentContacts) else false
    }

    private fun persistContacts(context: Context, contacts: List<Contact>): Boolean {
        return try {
            val jsonArray = JSONArray()
            for (contact in contacts) {
                val obj = JSONObject().apply {
                    put("id", contact.id)
                    put("name", contact.name)
                    put("address", contact.address)
                    put("isMainnet", contact.isMainnet)
                }
                jsonArray.put(obj)
            }

            val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CONTACTS_JSON, jsonArray.toString()).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}