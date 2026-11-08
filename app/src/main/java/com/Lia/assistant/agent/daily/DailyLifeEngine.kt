package com.Lia.assistant.agent.daily

import android.content.Context
import com.Lia.assistant.brain.ModelRouter
import com.Lia.assistant.brain.TaskType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DailyLifeEngine {

    /**
     * T5: Recipe from Fridge Photo with voice cooking timer
     */
    suspend fun getRecipeFromFridge(context: Context, ingredientsList: String): String {
        val prompt = """
            The user has these available ingredients: "$ingredientsList".
            Suggest 2 delicious Indian home-style recipes that can be prepared with these items.
            Provide:
            1. Dish Name & prep time.
            2. Step-by-step cooking steps with timing instructions for timers.
            3. Pro cooking tips.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T5: Fitness, Workout Rep Count & Water Reminder
     */
    suspend fun getFitnessPlanOrRepCount(context: Context, query: String): String {
        val prompt = """
            Act as an encouraging personal fitness coach.
            Request: "$query"
            Provide clear workout breakdown (sets, reps, rest intervals), hydration guidance, and diet tip.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T5: Travel (Route, Train/Flight live status, Itinerary)
     */
    suspend fun getTravelOrTransitStatus(context: Context, transitQuery: String): String {
        val prompt = """
            Help with travel intelligence: "$transitQuery"
            If it's a train (e.g. 12424 / Rajdhani): explain running status route, major stops, and delay trends.
            If it's an itinerary: generate a day-by-day sightseeing and culinary guide.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.DEEP_RESEARCH, prompt)
    }

    /**
     * T5: Shopping Price Comparison across Amazon, Flipkart, Blinkit
     */
    suspend fun compareProductPrices(context: Context, productName: String): String {
        val prompt = """
            Compare shopping price ranges and customer reviews for product: "$productName".
            Provide:
            - Typical price range on Amazon India, Flipkart, and quick-commerce (Blinkit/Zepto).
            - Top 3 pros and top 2 common complaints from verified buyers.
            - Value for money verdict.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.DEEP_RESEARCH, prompt)
    }

    /**
     * T5: News, Live Cricket Score & Morning Briefing
     */
    suspend fun getMorningBriefingOrCricket(context: Context, request: String): String {
        val today = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
        val prompt = """
            Today is $today.
            Provide a concise, energetic morning briefing / cricket update for Indian user:
            Request: "$request"
            Include:
            1. Latest Cricket Match Score / Status.
            2. Top 3 National & Global Headlines.
            3. Thought for the day in Hindi.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.DEEP_RESEARCH, prompt)
    }

    /**
     * T5: Festival, Prayer & Panchang Reminders
     */
    suspend fun getPanchangAndFestivals(context: Context, query: String): String {
        val today = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val prompt = """
            Provide Hindu Panchang and festival dates for query: "$query". Reference date: $today.
            Include: Tithi, Vaar, Nakshatra, auspicious Muhurat timings, and upcoming festivals.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.DEEP_RESEARCH, prompt)
    }

    /**
     * T5: Song Recognition & YouTube Music Finder
     */
    suspend fun identifySong(context: Context, lyricOrClue: String): String {
        val prompt = """
            Identify this song from the lyrics or audio clue: "$lyricOrClue".
            Provide: Song Title, Singer/Artist, Movie/Album, Release Year, and a recommended YouTube search link.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }
}
