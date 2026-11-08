package com.Lia.assistant.action

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.Lia.assistant.data.UserMemoryStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class IndexedContact(
    val id: String,
    val displayName: String,
    val normalizedName: String,
    val rawNumbers: List<String>,
    val internationalNumbers: List<String>,
    val aliases: Set<String> = emptySet()
)

sealed class UniversalContactResult {
    data class SingleMatch(
        val contact: IndexedContact,
        val chosenNumber: String,
        val formattedNumber: String
    ) : UniversalContactResult()

    data class AmbiguousMatch(
        val query: String,
        val candidates: List<IndexedContact>
    ) : UniversalContactResult()

    data class NotFound(val query: String) : UniversalContactResult()
    object NoPermission : UniversalContactResult()
}

object UniversalContactIndex {
    private const val TAG = "UniversalContactIndex"

    private val scope = CoroutineScope(Dispatchers.IO)
    private val contactsList = mutableListOf<IndexedContact>()
    private var isInitialized = false
    private var contentObserverRegistered = false

    private val _indexedCount = MutableStateFlow(0)
    val indexedCount: StateFlow<Int> = _indexedCount.asStateFlow()

    // Common Hindi/Hinglish to English transliteration map
    private val TRANSLITERATION_MAP = mapOf(
        "केशव" to "keshav",
        "keshav" to "keshav",
        "keshaw" to "keshav",
        "kesav" to "keshav",
        "केशवशर्मा" to "keshav sharma",
        "केशवकुमार" to "keshav kumar",
        "राहुल" to "rahul",
        "अमन" to "aman",
        "रोहित" to "rohit",
        "सुमित" to "sumit",
        "अमित" to "amit",
        "विकास" to "vikas",
        "प्रशांत" to "prashant",
        "दीपक" to "deepak",
        "सोनू" to "sonu",
        "मोनू" to "monu",
        "शिवा" to "shiva",
        "शिवाय" to "shivay",
        "मम्मी" to "mummy",
        "पापा" to "papa",
        "भैया" to "bhaiya",
        "दीदी" to "didi"
    )

    private val DEFAULT_ALIASES = mapOf(
        "mummy" to setOf("maa", "mom", "mother", "mataji", "ammi"),
        "papa" to setOf("dad", "father", "pitaji", "abbu", "bapuji"),
        "bhaiya" to setOf("bhai", "bro", "brother", "bhaijaan"),
        "didi" to setOf("sister", "sis", "behan", "aapi"),
        "wife" to setOf("patni", "biwi"),
        "husband" to setOf("pati")
    )

    fun initialize(context: Context) {
        if (isInitialized) return
        isInitialized = true

        loadContacts(context)
        registerObserver(context)
    }

    fun refresh(context: Context) {
        loadContacts(context)
    }

    private fun registerObserver(context: Context) {
        if (contentObserverRegistered) return
        try {
            val resolver = context.applicationContext.contentResolver
            resolver.registerContentObserver(
                ContactsContract.Contacts.CONTENT_URI,
                true,
                object : ContentObserver(Handler(Looper.getMainLooper())) {
                    override fun onChange(selfChange: Boolean) {
                        super.onChange(selfChange)
                        Log.i(TAG, "Contacts changed on device. Reindexing...")
                        loadContacts(context)
                    }
                }
            )
            contentObserverRegistered = true
        } catch (e: Throwable) {
            Log.w(TAG, "Unable to register Contacts ContentObserver: ${e.message}")
        }
    }

    private fun loadContacts(context: Context) {
        scope.launch {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d(TAG, "READ_CONTACTS permission not granted yet")
                return@launch
            }

            val appCtx = context.applicationContext
            val countryCode = detectCountryCode(appCtx)
            val tempMap = mutableMapOf<String, MutableIndexedBuilder>()

            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            try {
                appCtx.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                    while (cursor.moveToNext()) {
                        val id = cursor.getString(idIdx) ?: ""
                        val name = cursor.getString(nameIdx)?.trim() ?: ""
                        val rawNumber = cursor.getString(numIdx)?.trim() ?: ""

                        if (name.isNotEmpty() && rawNumber.isNotEmpty()) {
                            val builder = tempMap.getOrPut(id) {
                                MutableIndexedBuilder(
                                    id = id,
                                    displayName = name,
                                    normalizedName = normalizeSpelling(name)
                                )
                            }
                            builder.rawNumbers.add(rawNumber)
                            builder.internationalNumbers.add(formatInternational(rawNumber, countryCode))
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error querying contacts: ${e.message}", e)
            }

            // Also incorporate saved user memories for custom aliases
            val customAliases = extractCustomAliasesFromMemory(appCtx)

            synchronized(contactsList) {
                contactsList.clear()
                tempMap.values.forEach { builder ->
                    val aliases = mutableSetOf<String>()
                    val norm = builder.normalizedName

                    DEFAULT_ALIASES.forEach { (key, synonyms) ->
                        if (norm.contains(key) || synonyms.any { norm.contains(it) }) {
                            aliases.add(key)
                            aliases.addAll(synonyms)
                        }
                    }

                    customAliases[builder.id]?.let { aliases.addAll(it) }

                    contactsList.add(
                        IndexedContact(
                            id = builder.id,
                            displayName = builder.displayName,
                            normalizedName = builder.normalizedName,
                            rawNumbers = builder.rawNumbers.distinct(),
                            internationalNumbers = builder.internationalNumbers.distinct(),
                            aliases = aliases
                        )
                    )
                }
                _indexedCount.value = contactsList.size
            }

            Log.i(TAG, "Indexed ${contactsList.size} contacts successfully in memory")
        }
    }

    fun findContact(context: Context, query: String): UniversalContactResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return UniversalContactResult.NoPermission
        }

        val rawQuery = query.trim()
        if (rawQuery.isEmpty()) return UniversalContactResult.NotFound(query)

        // If direct phone number passed
        val digitsOnly = rawQuery.filter { it.isDigit() }
        if (digitsOnly.length >= 7 && rawQuery.all { it.isDigit() || it in "+- ()" }) {
            val countryCode = detectCountryCode(context)
            val intl = formatInternational(rawQuery, countryCode)
            return UniversalContactResult.SingleMatch(
                contact = IndexedContact(
                    id = "direct",
                    displayName = rawQuery,
                    normalizedName = rawQuery,
                    rawNumbers = listOf(rawQuery),
                    internationalNumbers = listOf(intl)
                ),
                chosenNumber = rawQuery,
                formattedNumber = intl
            )
        }

        val normalizedQuery = normalizeSpelling(rawQuery)

        val matches = synchronized(contactsList) {
            // Check direct exact match
            val exact = contactsList.filter { it.normalizedName == normalizedQuery }
            if (exact.isNotEmpty()) return@synchronized exact

            // Check aliases
            val aliasMatches = contactsList.filter { it.aliases.contains(normalizedQuery) }
            if (aliasMatches.isNotEmpty()) return@synchronized aliasMatches

            // Check prefix / starts with
            val prefix = contactsList.filter { it.normalizedName.startsWith(normalizedQuery) }
            if (prefix.isNotEmpty()) return@synchronized prefix

            // Check contains
            val contains = contactsList.filter { it.normalizedName.contains(normalizedQuery) }
            if (contains.isNotEmpty()) return@synchronized contains

            // Fuzzy match (levenshtein or phonetic)
            contactsList.filter {
                isFuzzyMatch(normalizedQuery, it.normalizedName)
            }
        }

        return when {
            matches.isEmpty() -> UniversalContactResult.NotFound(rawQuery)
            matches.size == 1 -> {
                val contact = matches.first()
                val chosen = contact.internationalNumbers.firstOrNull()
                    ?: contact.rawNumbers.firstOrNull() ?: ""
                UniversalContactResult.SingleMatch(
                    contact = contact,
                    chosenNumber = chosen,
                    formattedNumber = chosen
                )
            }
            else -> {
                // If there is an exact first-name match, prioritize
                val exactFirst = matches.filter {
                    it.displayName.split(" ").firstOrNull()?.equals(rawQuery, ignoreCase = true) == true
                }
                if (exactFirst.size == 1) {
                    val contact = exactFirst.first()
                    val chosen = contact.internationalNumbers.firstOrNull() ?: contact.rawNumbers.first()
                    UniversalContactResult.SingleMatch(contact, chosen, chosen)
                } else {
                    UniversalContactResult.AmbiguousMatch(rawQuery, matches.take(4))
                }
            }
        }
    }

    /**
     * Normalizes Hinglish/Hindi/English spellings to a standard ASCII key.
     */
    fun normalizeSpelling(input: String): String {
        var str = input.lowercase().trim()

        // Strip honorifics & relationship suffixes first
        str = str.replace(Regex("\\b(ji|bhai|bhaiya|sir|mam|dr|bro)\\b"), "").trim()

        // Check dictionary replacement
        TRANSLITERATION_MAP[str]?.let { return it }

        // Common spelling variants
        str = str.replace("w", "v")
            .replace("sav", "shav")
            .replace("shh", "sh")
            .replace("ee", "i")
            .replace("oo", "u")
            .replace("aa", "a")
            .replace("ph", "f")

        return str
    }

    private fun isFuzzyMatch(query: String, target: String): Boolean {
        if (query.length < 3) return false
        val qWords = query.split(" ")
        val tWords = target.split(" ")
        return qWords.all { qw -> tWords.any { tw -> tw.startsWith(qw) || levenshteinDistance(qw, tw) <= 1 } }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[s1.length][s2.length]
    }

    fun formatInternational(phoneNumber: String, defaultCountryCode: String): String {
        var digits = phoneNumber.filter { it.isDigit() }
        if (phoneNumber.startsWith("+")) {
            return digits // Already has country code
        }
        if (digits.startsWith("0")) {
            digits = digits.substring(1)
        }
        if (digits.length == 10 && (defaultCountryCode == "91" || defaultCountryCode.isEmpty())) {
            return "91$digits"
        }
        if (digits.length <= 10 && defaultCountryCode.isNotEmpty()) {
            return "$defaultCountryCode$digits"
        }
        return digits
    }

    private fun detectCountryCode(context: Context): String {
        return try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            val simCountry = tm?.simCountryIso?.uppercase(Locale.US) ?: ""
            when (simCountry) {
                "IN" -> "91"
                "US", "CA" -> "1"
                "GB" -> "44"
                "AE" -> "971"
                else -> "91" // Default India
            }
        } catch (_: Throwable) {
            "91"
        }
    }

    private fun extractCustomAliasesFromMemory(context: Context): Map<String, Set<String>> {
        val result = mutableMapOf<String, MutableSet<String>>()
        try {
            val memories = UserMemoryStore.get(context).getAllMemories()
            memories.forEach { mem ->
                val text = mem.content.lowercase()
                if (text.contains("mummy") || text.contains("mom")) {
                    // Extract name if formatted like "mummy ka naam Sunita hai"
                    val words = text.split(" ")
                    words.forEach { w ->
                        if (w.length > 3 && w !in listOf("mummy", "naam", "hai", "mera")) {
                            result.getOrPut(w) { mutableSetOf() }.add("mummy")
                        }
                    }
                }
            }
        } catch (_: Throwable) {}
        return result
    }

    private class MutableIndexedBuilder(
        val id: String,
        val displayName: String,
        val normalizedName: String
    ) {
        val rawNumbers = mutableListOf<String>()
        val internationalNumbers = mutableListOf<String>()
    }
}
