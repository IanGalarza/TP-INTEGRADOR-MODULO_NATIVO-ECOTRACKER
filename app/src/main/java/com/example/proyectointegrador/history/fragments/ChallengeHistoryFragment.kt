package com.example.proyectointegrador.history.fragments

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectointegrador.Detail.DetailActivity
import com.example.proyectointegrador.R
import com.example.proyectointegrador.history.adapter.ChallengeHistoryAdapter
import com.example.proyectointegrador.history.model.Challenge
import com.example.proyectointegrador.placeholder.PlaceholderContent
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import java.io.InputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChallengeHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var noHistoryText: TextView
    private lateinit var searchEditText: EditText
    private lateinit var statusSpinner: Spinner
    private lateinit var adapter: ChallengeHistoryAdapter

    private var allChallenges: List<Challenge> = emptyList()
    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_challenge_history, container, false)

        recyclerView = rootView.findViewById(R.id.history_recycler_view)
        progressBar = rootView.findViewById(R.id.history_progress_bar)
        noHistoryText = rootView.findViewById(R.id.no_history_text)
        searchEditText = rootView.findViewById(R.id.challenge_search_edittext)
        statusSpinner = rootView.findViewById(R.id.status_filter_spinner)

        adapter = ChallengeHistoryAdapter(
            emptyList(),
            { challenge ->
                val intent = Intent(requireContext(), DetailActivity::class.java)
                intent.putExtra("challengeId", challenge.id)
                startActivity(intent)
            },
            { challenge ->
                viewLifecycleOwner.lifecycleScope.launch {
                    generateReport(requireContext(), challenge)
                }
            }
        )
        recyclerView.adapter = adapter

        setupSearchListener()
        setupStatusFilter()
        loadChallengesFromFirestore()

        return rootView
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                searchRunnable = Runnable {
                    applyFilters()
                }
                handler.postDelayed(searchRunnable!!, 500) // 500ms debounce
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupStatusFilter() {
        val statusOptions = listOf(
            getString(R.string.status_all),
            getString(R.string.status_active),
            getString(R.string.status_completed),
            getString(R.string.status_cancelled)
        )

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.spinner_selected_item,
            statusOptions
        ).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
        }

        statusSpinner.adapter = adapter

        statusSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                applyFilters()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadChallengesFromFirestore() {
        val db = Firebase.firestore
        val currentUser = Firebase.auth.currentUser

        if (currentUser == null) {
            updateUIWithChallenges(emptyList())
            return
        }

        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        noHistoryText.visibility = View.GONE

        PlaceholderContent.ITEMS.clear()
        PlaceholderContent.ITEM_MAP.clear()

        db.collection("users")
            .document(currentUser.uid)
            .collection("active_challenges")
            .get()
            .addOnSuccessListener { documents ->
                progressBar.visibility = View.GONE

                val challenges = documents.mapNotNull { doc ->

                    // Agregar documento completo a PlaceholderContent

                    PlaceholderContent.addItemFromDocument(doc)

                    try {
                        val id = doc.id
                        val imageUrl = doc.getString("imageUrl") ?: ""
                        val title = doc.getString("title") ?: "Challenge without Title"
                        val status = doc.getString("status") ?: "ACTIVE"
                        val category = doc.getString("category") ?: ""
                        val description = doc.getString("description") ?: ""
                        val durationInDays = (doc.getLong("durationInDays") ?: 0L).toInt()
                        val startedAt = (doc.getTimestamp("startedAt") ?: doc.getDate("startedAt")?.let { com.google.firebase.Timestamp(it) })
                            ?.toDate() ?: Date()
                        val endDate = (doc.getTimestamp("endDate") ?: doc.getDate("endDate")?.let { com.google.firebase.Timestamp(it) })
                            ?.toDate() ?: Date()
                        val extraPoints = (doc.getLong("extraPoints") ?: 0L).toInt()

                        // Parseo de la lista de tareas
                        val tasksRaw = doc.get("tasks") as? List<Map<String, Any>> ?: emptyList()
                        val tasks = tasksRaw.map { taskMap ->
                            val taskId = taskMap["id"] as? String ?: ""
                            val taskTitle = taskMap["title"] as? String ?: ""
                            val points = (taskMap["points"] as? Long ?: 0L).toInt()
                            val completed = taskMap["completed"] as? Boolean ?: false
                            val comment = taskMap["comment"] as? String ?: ""
                            val photoUrl = taskMap["photoUrl"] as? String

                            // Parseo de location anidado
                            val locationMap = taskMap["location"] as? Map<String, Any>
                            val city = locationMap?.get("city") as? String ?: "Unknown"
                            val country = locationMap?.get("country") as? String ?: "Unknown"
                            val lat = (locationMap?.get("lat") as? Number)?.toDouble() ?: 0.0
                            val lng = (locationMap?.get("lng") as? Number)?.toDouble() ?: 0.0
                            val location = com.example.proyectointegrador.history.model.Location(
                                city = city,
                                country = country,
                                lat = lat,
                                lng = lng
                            )

                            com.example.proyectointegrador.history.model.TaskData(
                                id = taskId,
                                title = taskTitle,
                                points = points,
                                completed = completed,
                                comment = comment,
                                location = location,
                                photoUrl = photoUrl
                            )
                        }

                        com.example.proyectointegrador.history.model.Challenge(
                            id = id,
                            imageUrl = imageUrl,
                            title = title,
                            category = category,
                            description = description,
                            durationInDays = durationInDays,
                            startedAt = startedAt,
                            endDate = endDate,
                            extraPoints = extraPoints,
                            status = status,
                            tasks = tasks
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                allChallenges = challenges
                applyFilters()
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                noHistoryText.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            }
    }

    private fun applyFilters() {
        val query = searchEditText.text.toString().trim().lowercase()
        val selectedStatus = statusSpinner.selectedItem.toString()

        val statusInEnglish = when (selectedStatus) {
            getString(R.string.status_active) -> "ACTIVE"
            getString(R.string.status_completed) -> "COMPLETED"
            getString(R.string.status_cancelled) -> "CANCELLED"
            else -> "ALL"
        }

        val filtered = allChallenges.filter { challenge ->
            val matchesTitle = challenge.title.lowercase().contains(query)
            val matchesStatus = statusInEnglish == "ALL" || challenge.status == statusInEnglish
            matchesTitle && matchesStatus
        }

        updateUIWithChallenges(filtered)
    }

    private fun updateUIWithChallenges(challenges: List<Challenge>) {
        if (challenges.isEmpty()) {
            noHistoryText.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            adapter.updateData(challenges)
            recyclerView.visibility = View.VISIBLE
            noHistoryText.visibility = View.GONE
        }
    }

    suspend fun generateReport(context: Context, challenge: Challenge) {
        val pdfDocument = PdfDocument()
        val pageWidth = 400
        val pageHeight = 720
        val res = context.resources

        
        var pageNumber = 1
        var y = 40f

        fun startNewPage(): Pair<PdfDocument.Page, Canvas> {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber++).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            y = 40f
            return Pair(page, canvas)
        }

        var (page, canvas) = startNewPage()
        val paint = Paint().apply { isAntiAlias = true }

        // Portada
        challenge.imageUrl.takeIf { it.isNotBlank() }?.let { url ->
            fetchBitmapSync(url)?.let { bitmap ->
                val aspect = bitmap.width.toFloat() / bitmap.height
                val imgW = pageWidth - 40
                val imgH = (imgW / aspect).toInt().coerceAtMost(120)
                val scaled = Bitmap.createScaledBitmap(bitmap, imgW, imgH, true)
                canvas.drawBitmap(scaled, 20f, y, null)
                y += imgH + 20
            }
        }

        // Título challenge
        paint.color = Color.rgb(34, 139, 34)
        paint.textSize = 20f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(challenge.title, pageWidth / 2f, y, paint)
        y += 32f

        // "Chips" categoría y estado
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 12f
        paint.color = Color.rgb(224, 242, 241)
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(
            30f, y, 30f + paint.measureText(challenge.category) + 28f, y + 24f,
            12f, 12f, paint
        )
        paint.color = Color.rgb(0, 150, 136)
        paint.isFakeBoldText = true
        canvas.drawText("  ${challenge.category}  ", 32f, y + 17f, paint)

        val estadoColor = when (challenge.status.uppercase()) {
            "COMPLETED" -> Color.rgb(46, 125, 50)
            "ACTIVE" -> Color.rgb(255, 193, 7)
            "CANCELLED" -> Color.rgb(211, 47, 47)
            else -> Color.DKGRAY
        }
        val estadoTexto = when (challenge.status.uppercase()) {
            "COMPLETED" -> res.getString(R.string.status_completed)
            "ACTIVE" -> res.getString(R.string.status_active)
            "CANCELLED" -> res.getString(R.string.status_cancelled)
            else -> challenge.status.replaceFirstChar { it.uppercase() }
        }
        paint.textAlign = Paint.Align.RIGHT
        paint.color = Color.argb(32, Color.red(estadoColor), Color.green(estadoColor), Color.blue(estadoColor))
        canvas.drawRoundRect(
            pageWidth - 160f, y, pageWidth - 32f, y + 24f,
            12f, 12f, paint
        )
        paint.color = estadoColor
        paint.isFakeBoldText = true
        canvas.drawText("  $estadoTexto  ", pageWidth - 34f, y + 17f, paint)
        y += 40f

        // Info general
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 13f
        paint.color = Color.DKGRAY
        paint.isFakeBoldText = false
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText(
            res.getString(R.string.report_start_date, sdf.format(challenge.startedAt)),
            32f, y, paint
        )
        y += 18f
        canvas.drawText(
            res.getString(R.string.report_end_date, sdf.format(challenge.endDate)),
            32f, y, paint
        )
        y += 18f
        canvas.drawText(
            res.getString(R.string.challenge_duration, challenge.durationInDays),
            32f, y, paint
        )
        y += 18f
        canvas.drawText(
            res.getString(R.string.extra_points_label, challenge.extraPoints),
            32f, y, paint
        )
        y += 22f

        // Línea divisoria
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(24f, y, pageWidth - 24f, y, paint)
        y += 16f

        // Descripción challenge
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 14f
        paint.color = Color.BLACK
        paint.isFakeBoldText = false
        val descLines = challenge.description.chunked(56)
        for (line in descLines) {
            canvas.drawText(line, 32f, y, paint)
            y += 17f
        }
        y += 12f

        // TASKS
        paint.textSize = 16f
        paint.isFakeBoldText = true
        paint.color = Color.DKGRAY
        canvas.drawText(res.getString(R.string.report_tasks), 30f, y, paint)
        y += 28f

        for ((i, t) in challenge.tasks.withIndex()) {
            val textHeight = 62f
            val imageHeight = 90f
            val cardPadding = 10f
            val cardHeight = textHeight + imageHeight + cardPadding * 2
            if (y + cardHeight + 30f > pageHeight) {
                pdfDocument.finishPage(page)
                val result = startNewPage()
                page = result.first
                canvas = result.second
                y += 28f
            }

            // Card background
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(20, 0, 0, 0)
            val cardLeft = 30f
            val cardRight = pageWidth - 30f
            val cardTop = y
            val cardBottom = y + cardHeight + 10f
            canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, 16f, 16f, paint)

            // --- Text section ---
            val textStartY = y + cardPadding + 18f
            paint.textSize = 13.5f
            paint.isFakeBoldText = true
            paint.color = if (t.completed) Color.rgb(34, 139, 34) else Color.rgb(210, 40, 40)
            canvas.drawText("${i + 1}. ${t.title}", cardLeft + 10f, textStartY, paint)

            paint.textSize = 12f
            paint.isFakeBoldText = false
            paint.color = Color.DKGRAY
            val estadoTxt = if (t.completed)
                res.getString(R.string.report_task_status_completed)
            else
                res.getString(R.string.report_task_status_pending)
            canvas.drawText(
                res.getString(R.string.report_task_points_and_status, t.points, estadoTxt),
                cardLeft + 10f, textStartY + 15f, paint
            )

            // Comentario
            paint.color = Color.GRAY
            val commentText = if (t.comment.isNotEmpty()) t.comment else res.getString(R.string.report_task_no_comment)
            canvas.drawText(
                res.getString(R.string.report_task_comment, commentText),
                cardLeft + 10f, textStartY + 30f, paint
            )

            // Ubicación
            paint.color = Color.parseColor("#888888")
            val loc = if (t.location.city != "Unknown") "${t.location.city}, ${t.location.country}" else "-"
            canvas.drawText(
                res.getString(R.string.report_task_location, loc),
                cardLeft + 10f, textStartY + 45f, paint
            )

            // Imagen
            val extraImgMargin = 10f
            val imgY = cardTop + textHeight + cardPadding + extraImgMargin
            val imgWidth = cardRight - cardLeft - 20f
            val imgHeight = imageHeight
            val imgLeft = cardLeft + 10f

            val photoBitmap = t.photoUrl?.takeIf { it.isNotEmpty() }?.let { url -> fetchBitmapSync(url) }
            if (photoBitmap != null) {
                val scaled = Bitmap.createScaledBitmap(photoBitmap, imgWidth.toInt(), imgHeight.toInt(), true)
                canvas.drawBitmap(scaled, imgLeft, imgY, null)
            } else {
                // Placeholder
                paint.style = Paint.Style.STROKE
                paint.color = Color.LTGRAY
                paint.strokeWidth = 2f
                canvas.drawRect(imgLeft, imgY, imgLeft + imgWidth, imgY + imgHeight, paint)
                paint.textSize = 12f
                paint.style = Paint.Style.FILL
                paint.color = Color.LTGRAY
                paint.textAlign = Paint.Align.CENTER
                canvas.drawText(res.getString(R.string.report_task_no_photo), imgLeft + imgWidth / 2f, imgY + imgHeight / 2f + 6f, paint)
                paint.textAlign = Paint.Align.LEFT
            }

            // Línea separadora después de cada card
            paint.color = Color.LTGRAY
            paint.style = Paint.Style.STROKE
            canvas.drawLine(cardLeft, cardBottom + 6f, cardRight, cardBottom + 6f, paint)

            y += cardHeight + 20f
        }
        pdfDocument.finishPage(page)

        val filename = "${challenge.title.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val mimeType = "application/pdf"
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        var uri: Uri? = null
        try {
            uri = resolver.insert(collection, contentValues)
            if (uri == null) throw Exception("No se pudo crear el archivo")
            val outputStream: OutputStream? = resolver.openOutputStream(uri)
            if (outputStream == null) throw Exception("No se pudo abrir el archivo para escribir")

            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            Toast.makeText(context, res.getString(R.string.report_pdf_saved), Toast.LENGTH_SHORT).show()
            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(openIntent, res.getString(R.string.report_open_with)))

        } catch (e: Exception) {
            pdfDocument.close()
            Toast.makeText(context, res.getString(R.string.report_pdf_error, e.message ?: ""), Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    suspend fun fetchBitmapSync(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            val input: InputStream = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }
}
