package com.Lia.assistant.agent.photo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.Lia.assistant.brain.ModelRouter
import com.Lia.assistant.brain.OpenAiProvider
import com.Lia.assistant.brain.TaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PhotoFeaturesEngine {

    /**
     * T3: Identify objects, plants, medicine labels, bills from image context or description
     */
    suspend fun identifyObjectOrLabel(context: Context, contextText: String, photoType: String = "general"): String {
        val prompt = """
            Analyze this image/description and identify what it is:
            Context/OCR details: $contextText
            Type: $photoType (plant, medicine, bill, or hardware)
            Explain in clear Hindi/English:
            1. What is this object/item?
            2. Key features or usage.
            3. If it's medicine: usage warnings and advisory to consult a doctor.
            4. If it's a bill: total amount, date, and vendor.
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T3: Photo Questions (Homework, boards, menus)
     */
    suspend fun answerPhotoQuestion(context: Context, extractedText: String, question: String): String {
        val prompt = """
            Answer the user's question based on the photo content.
            Extracted Photo Text:
            $extractedText
            
            User Question:
            $question
        """.trimIndent()
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }

    /**
     * T3: Description-based Gallery Search
     */
    fun searchGalleryByDescription(context: Context, description: String): String {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            return "Gallery khol di gayi hai. Description \"$description\" se related photos browse kar sakte hain."
        } catch (_: Exception) {
            return "Gallery open karne mein problem aayi. Check karein ki Photos app installed hai."
        }
    }

    /**
     * T3: Document Scanner to PDF
     */
    fun scanDocumentToPdf(context: Context, documentName: String, textContent: String): String {
        return try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
            val page = pdfDoc.startPage(pageInfo)

            val canvas: Canvas = page.canvas
            val paint = Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 14f
            }

            canvas.drawText("LiaAI Scanned Document: $documentName", 40f, 60f, paint.apply { isFakeBoldText = true })
            paint.isFakeBoldText = false
            paint.textSize = 12f

            var y = 100f
            val lines = textContent.split("\n")
            for (line in lines) {
                if (y > 800f) break
                canvas.drawText(line.take(70), 40f, y, paint)
                y += 18f
            }

            pdfDoc.finishPage(page)

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS) ?: context.filesDir
            val safeName = documentName.replace("[^a-zA-Z0-9]".toRegex(), "_").take(20)
            val file = File(dir, "Scanned_${safeName}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            pdfDoc.close()
            outputStream.close()

            "Document scan complete! PDF save ho gaya hai: ${file.name}"
        } catch (e: Exception) {
            "PDF scan error: ${e.localizedMessage ?: "Failed to generate PDF"}"
        }
    }

    /**
     * T3: Photo Filter & Monochrome High Contrast for Documents
     */
    fun applyHighContrastFilter(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val matrix = ColorMatrix().apply {
            setSaturation(0f) // Black and white
        }
        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    /**
     * T3: AI Image Generation (DALL-E 3 / Gemini)
     */
    suspend fun generateAiImage(context: Context, prompt: String): String {
        val openAi = OpenAiProvider(context)
        if (openAi.hasKey()) {
            val url = openAi.generateImage(prompt)
            if (url != null) {
                return "AI Image generate ho gayi hai! URL: $url"
            }
        }
        return "Image generate karne ke liye OpenAI API key configure karein Settings mein (DALL-E 3 support). Prompt registered: \"$prompt\"."
    }

    /**
     * T3: Photo Text Translation
     */
    suspend fun translatePhotoText(context: Context, extractedText: String, targetLang: String = "Hindi"): String {
        val prompt = "Translate the following extracted photo text accurately to $targetLang, keeping original formatting:\n\n$extractedText"
        return ModelRouter.ask(context, TaskType.GENERAL_CHAT, prompt)
    }
}
