package com.example.cityapp.presentation.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.cityapp.domain.model.IncidentItem
import com.example.cityapp.domain.model.Route
import com.example.cityapp.domain.model.Waybill
import com.example.cityapp.presentation.incident.IncidentApiType
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportWriter {

    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val MARGIN = 48f
    private const val LINE_BELOW_BLOCK = 6f

    private fun titlePaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 15f
        isFakeBoldText = true
    }

    private fun sectionPaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f
        isFakeBoldText = true
    }

    private fun bodyPaint() = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 10.5f
    }

    private fun layoutOf(text: String, paint: TextPaint, widthPx: Int): StaticLayout =
        StaticLayout.Builder.obtain(text, 0, text.length, paint, widthPx)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float): Float {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
        return y + layout.height + LINE_BELOW_BLOCK
    }

    private class PageSession(private val pdf: PdfDocument) {
        private var pageNum = 1
        private lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y = MARGIN

        init {
            openPage()
        }

        private fun openPage() {
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
            page = pdf.startPage(info)
            canvas = page.canvas
            y = MARGIN
        }

        fun finish() {
            pdf.finishPage(page)
        }

        fun advancePage() {
            pdf.finishPage(page)
            pageNum++
            openPage()
        }

        fun ensureSpace(neededHeight: Float) {
            if (y + neededHeight > PAGE_H - MARGIN) {
                advancePage()
            }
        }

        fun drawParagraph(text: String, paint: TextPaint, contentW: Int) {
            if (text.isBlank()) return
            val layout = layoutOf(text, paint, contentW)
            ensureSpace(layout.height + LINE_BELOW_BLOCK + 4f)
            y = drawLayout(canvas, layout, MARGIN, y)
        }
    }

    private fun fmtNow(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    private fun waybillStatusUa(status: String): String = when (status) {
        "assigned" -> "Призначено"
        "in_progress" -> "В дорозі"
        "completed" -> "Завершено"
        "cancelled" -> "Скасовано"
        else -> status
    }

    private fun incidentStatusUa(status: String): String = when (status) {
        "open" -> "Відкрито"
        "resolved" -> "Закрито"
        "completed" -> "Завершено"
        else -> status
    }

    private fun coordPair(lat: Double, lng: Double) =
        "${String.format(Locale.US, "%.5f", lat)}, ${String.format(Locale.US, "%.5f", lng)}"

    fun writeWaybillPdf(
        output: OutputStream,
        waybill: Waybill,
        route: Route?,
        driverDisplayName: String?,
        vehicleDetails: String?
    ): Result<Unit> = runCatching {
        val pdf = PdfDocument()
        val session = PageSession(pdf)
        val contentW = (PAGE_W - 2 * MARGIN).toInt()
        val title = titlePaint()
        val section = sectionPaint()
        val body = bodyPaint()

        session.drawParagraph("Дорожній лист", title, contentW)
        session.drawParagraph("Сформовано: ${fmtNow()}", body, contentW)
        driverDisplayName?.takeIf { it.isNotBlank() }?.let {
            session.drawParagraph("Водій: $it", body, contentW)
        }
        session.drawParagraph("ID листа: ${waybill.id}", body, contentW)
        session.drawParagraph("Маршрут №${waybill.routeNumber}", body, contentW)
        route?.routeName?.takeIf { it.isNotBlank() }?.let {
            session.drawParagraph("Напрямок (назва маршруту): $it", body, contentW)
        }
        session.drawParagraph("Статус: ${waybillStatusUa(waybill.status)}", body, contentW)
        val vehicleLine = buildString {
            append("Борт / транспорт: ${waybill.vehicleId}")
            vehicleDetails?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
        }
        session.drawParagraph(vehicleLine, body, contentW)
        waybill.startedAt?.let { session.drawParagraph("Початок рейсу: $it", body, contentW) }
        waybill.completedAt?.let { session.drawParagraph("Завершено: $it", body, contentW) }
        if (waybill.notes.isNotBlank()) {
            session.drawParagraph("Примітки до рейсу: ${waybill.notes}", body, contentW)
        }
        waybill.deletedAt?.let {
            session.drawParagraph("Архівовано (прибрано з активних): $it", body, contentW)
        }
        waybill.deletionReasonCode?.let {
            session.drawParagraph("Код причини архіву: $it", body, contentW)
        }
        waybill.deletionReasonNote?.takeIf { it.isNotBlank() }?.let {
            session.drawParagraph("Примітка до архіву: $it", body, contentW)
        }

        session.y += 8f
        session.drawParagraph("Графік зупинок", section, contentW)
        val stops = route?.stops.orEmpty().sortedBy { it.stopNumber }
        if (stops.isEmpty()) {
            session.drawParagraph(
                "Список зупинок недоступний (немає даних маршруту на сервері або порожній графік).",
                body,
                contentW
            )
        } else {
            stops.forEach { s ->
                val line =
                    "${s.stopNumber}. ${s.plannedTime} — ${s.name} · ${coordPair(s.lat, s.lng)}"
                session.drawParagraph(line, body, contentW)
            }
        }

        session.y += 8f
        session.drawParagraph(
            "Застосунок RoutePulse. Дані рейсу та зупинок можуть бути узагальненими; " +
                "офіційний розклад уточнюйте у перевізника.",
            body,
            contentW
        )

        session.finish()
        pdf.writeTo(output)
        pdf.close()
    }

    fun writeIncidentPdf(
        output: OutputStream,
        incident: IncidentItem,
        waybill: Waybill?,
        route: Route?,
        driverDisplayName: String?,
        vehicleDetails: String?
    ): Result<Unit> = runCatching {
        val pdf = PdfDocument()
        val session = PageSession(pdf)
        val contentW = (PAGE_W - 2 * MARGIN).toInt()
        val title = titlePaint()
        val section = sectionPaint()
        val body = bodyPaint()

        session.drawParagraph("Інцидент (звіт)", title, contentW)
        session.drawParagraph("Сформовано: ${fmtNow()}", body, contentW)
        driverDisplayName?.takeIf { it.isNotBlank() }?.let {
            session.drawParagraph("Водій (автор запису): $it", body, contentW)
        }

        session.y += 6f
        session.drawParagraph("Дані інциденту", section, contentW)
        session.drawParagraph("ID інциденту: ${incident.id}", body, contentW)
        session.drawParagraph(
            "Тип: ${IncidentApiType.fromApi(incident.type).labelUa}",
            body,
            contentW
        )
        session.drawParagraph("Статус: ${incidentStatusUa(incident.status)}", body, contentW)
        incident.reportedAt?.let { session.drawParagraph("Час події (за записом): $it", body, contentW) }
        if (incident.stopLabel.isNotBlank()) {
            session.drawParagraph("Зупинка поруч: ${incident.stopLabel}", body, contentW)
        }
        session.drawParagraph(
            "Можливість самостійного руху: ${if (incident.canMoveIndependently) "так" else "ні"}",
            body,
            contentW
        )
        session.drawParagraph("Опис:\n${incident.description.ifBlank { "—" }}", body, contentW)
        session.drawParagraph(
            "Координати: ${coordPair(incident.lat, incident.lng)}",
            body,
            contentW
        )
        incident.photoUrl?.takeIf { it.isNotBlank() }?.let {
            session.drawParagraph("Посилання на фото: $it", body, contentW)
        }
        if (incident.isModified || incident.versionHistoryCount > 0) {
            session.drawParagraph(
                "Запис змінювався (версій у історії: ${incident.versionHistoryCount}).",
                body,
                contentW
            )
        }
        incident.lastEditedAt?.let {
            session.drawParagraph("Останнє редагування: $it", body, contentW)
        }
        incident.deletedAt?.let {
            session.drawParagraph("Архівовано: $it", body, contentW)
        }
        incident.deletionReasonCode?.let {
            session.drawParagraph("Причина архіву (код): $it", body, contentW)
        }
        incident.deletionReasonNote?.takeIf { it.isNotBlank() }?.let {
            session.drawParagraph("Примітка до архіву: $it", body, contentW)
        }

        session.y += 8f
        session.drawParagraph("Пов’язаний дорожній лист і рейс", section, contentW)
        session.drawParagraph("ID дорожнього листа: ${incident.waybillId}", body, contentW)

        if (waybill != null) {
            session.drawParagraph("Маршрут №${waybill.routeNumber}", body, contentW)
            session.drawParagraph("Статус листа: ${waybillStatusUa(waybill.status)}", body, contentW)
            val vLine = buildString {
                append("Борт: ${waybill.vehicleId}")
                vehicleDetails?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
            }
            session.drawParagraph(vLine, body, contentW)
            waybill.startedAt?.let { session.drawParagraph("Початок рейсу: $it", body, contentW) }
            waybill.completedAt?.let { session.drawParagraph("Завершено: $it", body, contentW) }
            if (waybill.notes.isNotBlank()) {
                session.drawParagraph("Примітки листа: ${waybill.notes}", body, contentW)
            }
        } else {
            session.drawParagraph(
                "Повний запис дорожнього листа не завантажено (немає зв’язку або доступу).",
                body,
                contentW
            )
        }

        route?.let { r ->
            session.y += 6f
            session.drawParagraph(
                "Маршрут (довідково): №${r.routeNumber} — ${r.routeName}",
                body,
                contentW
            )
            session.drawParagraph("Графік зупинок на момент експорту", section, contentW)
            val stops = r.stops.sortedBy { it.stopNumber }
            if (stops.isEmpty()) {
                session.drawParagraph("Зупинки відсутні в даних.", body, contentW)
            } else {
                stops.forEach { s ->
                    session.drawParagraph(
                        "${s.stopNumber}. ${s.plannedTime} — ${s.name}",
                        body,
                        contentW
                    )
                }
            }
        }

        session.y += 8f
        session.drawParagraph(
            "RoutePulse · експорт PDF для внутрішнього обліку; перевіряйте дані за офіційними системами перевізника.",
            body,
            contentW
        )

        session.finish()
        pdf.writeTo(output)
        pdf.close()
    }
}
