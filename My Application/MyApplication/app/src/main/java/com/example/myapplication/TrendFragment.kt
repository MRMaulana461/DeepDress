package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class TrendFragment : Fragment() {

    private lateinit var weekInput: android.widget.EditText
    private lateinit var predictButton: android.widget.Button
    private lateinit var resultBox: android.widget.TextView
    private lateinit var lineChart: LineChart
    private lateinit var progressBar: android.widget.ProgressBar

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiUrl = "http://10.0.2.2:5000/predict_trend"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_trend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        weekInput = view.findViewById(R.id.weekInput)
        predictButton = view.findViewById(R.id.predictButton)
        resultBox = view.findViewById(R.id.resultBox)
        lineChart = view.findViewById(R.id.lineChart)
        progressBar = view.findViewById(R.id.progressBar)

        setupChart()

        predictButton.setOnClickListener {
            val daysText = weekInput.text.toString().trim()
            val days = daysText.toIntOrNull()

            if (days == null || days <= 0 || days > 60) {
                showToast("Masukkan jumlah hari yang valid (1-60)")
                return@setOnClickListener
            }

            requestPrediction(days)
        }
    }

    private fun setupChart() {
        lineChart.apply {
            // Interaksi dasar
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            description.isEnabled = false

            // Hilangkan pesan default no-data (kita gunakan placeholder TextView jika perlu)
            setNoDataText("")

            // offsets default agar tidak terlalu mepet ke tepi
            setExtraOffsets(12f, 12f, 12f, 12f)

            axisRight.isEnabled = false

            axisLeft.apply {
                textColor = Color.DKGRAY
                setDrawGridLines(true)
                axisMinimum = 0f
                granularity = 1f
                setLabelCount(6, false)
            }

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.DKGRAY
                setDrawGridLines(false)
                granularity = 1f
                setAvoidFirstLastClipping(true)
                // label count akan diatur di showGraph sesuai jumlah hari
                labelRotationAngle = 0f
            }

            legend.apply {
                isEnabled = true
                textColor = Color.DKGRAY
                isWordWrapEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                yOffset = 10f
            }

            // Default visible range: beri kemampuan user zoom/pan
            setVisibleXRangeMaximum(60f) // dapat menampilkan sampai 60 hari
        }
    }

    private fun requestPrediction(days: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                showLoading(true)
                resultBox.text = "Memproses prediksi..."
                lineChart.clear()
                lineChart.invalidate()
            }

            try {
                val jsonBody = JSONObject().apply {
                    put("days", days)
                }

                val body = jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                withContext(Dispatchers.Main) {
                    showLoading(false)
                }

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        resultBox.text = "Gagal: ${response.code}"
                        showToast("Gagal prediksi")
                    }
                    return@launch
                }

                val json = JSONObject(responseBody)
                val predictionArray = json.getJSONArray("prediction")
                val entries = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                // Buat entries dan labels dengan benar
                for (i in 0 until predictionArray.length()) {
                    val y = predictionArray.getDouble(i).toFloat()
                    entries.add(Entry(i.toFloat(), y))
                    labels.add("Hari ${i + 1}")
                }

                withContext(Dispatchers.Main) {
                    resultBox.text = """
                        Prediksi berhasil!
                        Lama Prediksi: $days hari
                        Status: ${json.getString("status")}
                    """.trimIndent()
                    showGraph(entries, labels)
                }

            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    resultBox.text = "Error jaringan: ${e.localizedMessage}"
                    showToast("Jaringan gagal")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    resultBox.text = "Error: ${e.localizedMessage}"
                    showToast("Terjadi error")
                }
            }
        }
    }

    // Ganti fungsi showGraph(...) dengan ini
    private fun showGraph(entries: List<Entry>, labels: List<String>) {
        if (entries.isEmpty()) {
            // Kosongkan chart (placeholder UI bisa di-handle terpisah)
            lineChart.clear()
            lineChart.invalidate()
            return
        }

        // Tentukan Y axis dengan cap 5000 tetapi jangan pangkas jika data > 5000
        val minY = entries.minByOrNull { it.y }?.y ?: 0f
        val maxY = entries.maxByOrNull { it.y }?.y ?: 1f
        val range = (maxY - minY)
        val padding = if (range > 0) range * 0.1f else 1f

        val CAP_MAX = 5000f
        val computedMax = maxY + padding
        val axisMax = if (computedMax <= CAP_MAX) CAP_MAX else computedMax

        lineChart.axisLeft.apply {
            axisMinimum = 0f
            axisMaximum = axisMax
            setLabelCount(6, false)
        }

        // X axis : sesuaikan min/max dengan jumlah entries (hari)
        val total = entries.size
        val maxIndex = (total - 1).coerceAtLeast(0)

        lineChart.xAxis.apply {
            axisMinimum = 0f
            axisMaximum = maxIndex.toFloat()
            granularity = 1f
            // tampilkan label secukupnya agar tidak berantakan
            setLabelCount(Math.min(total, 10), false)
            valueFormatter = IndexAxisValueFormatter(labels)
        }

        // Dataset prediksi
        val predictionDataSet = LineDataSet(entries, "Prediksi").apply {
            color = Color.parseColor("#1E90FF")
            lineWidth = 2.5f
            setCircleColor(Color.parseColor("#1E90FF"))
            circleRadius = 4f
            circleHoleRadius = 2f
            setDrawValues(false)
            setDrawFilled(false)
            mode = LineDataSet.Mode.LINEAR
        }

        val trendlineEntries = calculateLinearTrendline(entries)
        val trendlineDataSet = LineDataSet(trendlineEntries, "Trendline").apply {
            color = Color.RED
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            enableDashedLine(10f, 5f, 0f)
            mode = LineDataSet.Mode.LINEAR
        }

        // Set data
        lineChart.data = LineData(predictionDataSet, trendlineDataSet).apply {
            setValueTextSize(9f)
            setValueTextColor(Color.DKGRAY)
        }

        // Jangan ubah layoutParams / width di sini — biarkan XML & container mengatur ukuran.
        // Atur visible range agar default view wajar (user dapat geser/zoom)
        val visibleRange = Math.min(total.toFloat(), 15f) // default show 15 data kalau banyak
        lineChart.setVisibleXRangeMaximum(visibleRange)
        lineChart.setVisibleXRangeMinimum(Math.min(5f, visibleRange))

        // Gunakan fitScreen untuk reset zoom jika perlu, lalu moveViewToX ke 0 (awal)
        lineChart.fitScreen()
        // Tampilkan dari posisi awal (kiri). Jika Anda ingin lihat ujung kanan, ubah ke (total - visibleRange)
        lineChart.moveViewToX(0f)

        // offset tetap agar tidak nempel ke pinggir
        lineChart.setExtraOffsets(12f, 12f, 12f, 12f)

        lineChart.invalidate()
        lineChart.animateX(600)
    }

    private fun calculateLinearTrendline(entries: List<Entry>): List<Entry> {
        val n = entries.size
        if (n < 2) return emptyList()

        var sumX = 0f
        var sumY = 0f
        var sumXY = 0f
        var sumX2 = 0f

        for (entry in entries) {
            val x = entry.x
            val y = entry.y
            sumX += x
            sumY += y
            sumXY += x * y
            sumX2 += x * x
        }

        val denominator = n * sumX2 - sumX * sumX
        if (denominator == 0f) return emptyList()

        val slope = (n * sumXY - sumX * sumY) / denominator
        val intercept = (sumY - slope * sumX) / n

        return entries.map { entry ->
            Entry(entry.x, slope * entry.x + intercept)
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        predictButton.isEnabled = !show
        weekInput.isEnabled = !show
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
}