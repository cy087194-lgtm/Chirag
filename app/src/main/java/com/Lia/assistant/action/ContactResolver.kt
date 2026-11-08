package com.Lia.assistant.action

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

sealed class ContactLookupResult {
    data class Found(val displayName: String, val phoneNumber: String) : ContactLookupResult()
    object NoPermission : ContactLookupResult()
    object NotFound : ContactLookupResult()
}

object ContactResolver {

    fun isDirectPhoneNumber(query: String): Boolean {
        val trimmed = query.trim()
        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length < 5) return false

        // Check if query only contains digits and standard phone symbols
        val validChars = setOf('+', '-', ' ', '(', ')', '.')
        return trimmed.all { it.isDigit() || validChars.contains(it) }
    }

    fun lookup(context: Context, query: String): ContactLookupResult {
        val q = query.trim()
        if (q.isEmpty()) return ContactLookupResult.NotFound

        if (isDirectPhoneNumber(q)) {
            return ContactLookupResult.Found(displayName = q, phoneNumber = q)
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return ContactLookupResult.NoPermission
        }

        val contentResolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$q%")
        val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC LIMIT 5"

        val candidates = mutableListOf<Pair<String, String>>()

        try {
            contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIdx) ?: ""
                    val number = cursor.getString(numberIdx) ?: ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        candidates.add(Pair(name, number))
                    }
                }
            }
        } catch (_: Exception) {
            return ContactLookupResult.NotFound
        }

        if (candidates.isEmpty()) {
            return ContactLookupResult.NotFound
        }

        // Rank candidates: exact > prefix > contains
        val ranked = rankCandidates(q, candidates)
        val best = ranked.firstOrNull() ?: return ContactLookupResult.NotFound
        return ContactLookupResult.Found(displayName = best.first, phoneNumber = best.second)
    }

    fun rankCandidates(query: String, candidates: List<Pair<String, String>>): List<Pair<String, String>> {
        val qLower = query.lowercase().trim()
        return candidates.sortedBy { (name, _) ->
            val nameLower = name.lowercase().trim()
            when {
                nameLower == qLower -> 0
                nameLower.startsWith(qLower) -> 1
                else -> 2
            }
        }
    }
}
