package com.example.db

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.util.zip.ZipInputStream
import com.example.BuildConfig
import com.example.Part1Question
import com.example.Part2Question
import com.example.Part2SubQuestion
import com.example.Part3Question
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

object ExamParser {
    // Read text from PDF URI
    fun readTextFromPdfUri(context: Context, uri: Uri): String {
        return try {
            PDFBoxResourceLoader.init(context)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            } ?: ""
        } catch (e: Exception) {
            Log.e("ExamParser", "Error reading PDF via PDFBox: ${e.message}", e)
            ""
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (idx != -1) {
                            result = cursor.getString(idx)
                        }
                    }
                }
            } catch (e: Exception) {
                // fallback
            }
        }
        if (result == null) {
            result = uri.path
        }
        return result ?: ""
    }

    // Map Symbol font hex codes to Unicode characters and LaTeX math symbols
    private fun mapSymbolCharToUnicode(charHex: String): String {
        val code = charHex.toIntOrNull(16) ?: return ""
        val baseCode = if (code in 0xF000..0xF0FF) code - 0xF000 else code
        return when (baseCode) {
            0x21 -> "!"
            0x23 -> "#"
            0x25 -> "%"
            0x2B -> "+"
            0x2D -> "-"
            0x2F -> "/"
            0x3C -> "<"
            0x3D -> "="
            0x3E -> ">"
            0x41 -> "A"
            0x42 -> "B"
            0x44 -> "Δ"
            0x47 -> "Γ"
            0x4C -> "Λ"
            0x50 -> "Π"
            0x51 -> "Θ"
            0x53 -> "Σ"
            0x57 -> "Ω"
            0x61 -> "α"
            0x62 -> "β"
            0x63 -> "χ"
            0x64 -> "δ"
            0x65 -> "ε"
            0x66 -> "φ"
            0x67 -> "γ"
            0x68 -> "η"
            0x69 -> "ι"
            0x6B -> "κ"
            0x6C -> "λ"
            0x6D -> "μ"
            0x6E -> "ν"
            0x6F -> "ο"
            0x70 -> "π"
            0x71 -> "θ"
            0x72 -> "ρ"
            0x73 -> "σ"
            0x74 -> "τ"
            0x75 -> "υ"
            0x77 -> "ω"
            0x78 -> "ξ"
            0x79 -> "ψ"
            0x7A -> "ζ"
            0xB1 -> "±"
            0xB2 -> "≤"
            0xB3 -> "≥"
            0xB4 -> "×"
            0xB8 -> "÷"
            0xB9 -> "≠"
            0xBA -> "≡"
            0xBB -> "≈"
            0xBC -> "…"
            0xC6 -> "∅"
            0xC7 -> "∩"
            0xC8 -> "∪"
            0xCE -> "∈"
            0xCF -> "∉"
            0xD0 -> "∠"
            0xD1 -> "∇"
            0xD5 -> "∏"
            0xD6 -> "√"
            0xDB -> "⇔"
            0xDC -> "⇐"
            0xDE -> "⇒"
            0xE5 -> "∑"
            0xF2 -> "∫"
            else -> try { baseCode.toChar().toString() } catch (e: Exception) { "" }
        }
    }

    // Convert Word EQ fields and MathType field code strings into LaTeX
    private fun convertEqFieldToLatex(raw: String): String {
        var s = raw.trim()
        if (!s.contains("EQ", ignoreCase = true) && !s.contains("\\f(") && !s.contains("\\r(") && !s.contains("\\s")) {
            return raw
        }
        s = s.replace(Regex("^EQ\\s*", RegexOption.IGNORE_CASE), "")
        
        // Replace \f(num,den) -> \frac{num}{den}
        val fracRegex = Regex("\\\\f\\s*\\(([^,]+),([^)]+)\\)")
        s = fracRegex.replace(s) { match ->
            "\\frac{${match.groupValues[1].trim()}}{${match.groupValues[2].trim()}}"
        }
        
        // Replace \r(deg,rad) or \r(rad)
        val rad2Regex = Regex("\\\\r\\s*\\(([^,]+),([^)]+)\\)")
        s = rad2Regex.replace(s) { match ->
            "\\sqrt[${match.groupValues[1].trim()}]{${match.groupValues[2].trim()}}"
        }
        val rad1Regex = Regex("\\\\r\\s*\\(([^)]+)\\)")
        s = rad1Regex.replace(s) { match ->
            "\\sqrt{${match.groupValues[1].trim()}}"
        }

        // Replace \s\upX(val) -> ^{val}
        val upRegex = Regex("\\\\s\\\\up\\d*\\s*\\(([^)]+)\\)")
        s = upRegex.replace(s) { match ->
            "^{${match.groupValues[1].trim()}}"
        }

        // Replace \s\doX(val) -> _{val}
        val doRegex = Regex("\\\\s\\\\do\\d*\\s*\\(([^)]+)\\)")
        s = doRegex.replace(s) { match ->
            "_{${match.groupValues[1].trim()}}"
        }

        // Replace \i(sub,sup,expr)
        val intRegex = Regex("\\\\i\\s*\\(([^,]+),([^,]+),([^)]+)\\)")
        s = intRegex.replace(s) { match ->
            "\\int_{${match.groupValues[1].trim()}}^{${match.groupValues[2].trim()}} ${match.groupValues[3].trim()}"
        }

        return if (s.isNotBlank()) " $s " else raw
    }

    // Read text from a DOCX (Word Document) URI with full MathType & OMML support
    fun readTextFromDocxUri(context: Context, uri: Uri): String {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry
                val sb = java.lang.StringBuilder()
                while (entry != null) {
                    if (entry.name == "word/document.xml" || entry.name.startsWith("word/header") || entry.name.startsWith("word/footer")) {
                        val bos = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(2048)
                        var len: Int
                        while (zipInputStream.read(buffer).also { len = it } != -1) {
                            bos.write(buffer, 0, len)
                        }
                        val bytes = bos.toByteArray()
                        
                        val parser = Xml.newPullParser()
                        parser.setInput(java.io.ByteArrayInputStream(bytes), "UTF-8")
                        var eventType = parser.eventType
                        var inMath = false
                        while (eventType != XmlPullParser.END_DOCUMENT) {
                            val name = parser.name
                            val localName = name?.substringAfterLast(":")?.lowercase() ?: ""

                            if (eventType == XmlPullParser.START_TAG) {
                                // Extract attributes (descr, title, alt) containing MathType equation text or LaTeX
                                for (i in 0 until parser.attributeCount) {
                                    val attrName = parser.getAttributeName(i).lowercase().substringAfterLast(":")
                                    if (attrName == "descr" || attrName == "title" || attrName == "alt" || attrName == "alttext") {
                                        var attrVal = parser.getAttributeValue(i)?.trim() ?: ""
                                        if (attrVal.isNotBlank()) {
                                            attrVal = attrVal
                                                .replace(Regex("^MathType\\s*WebTeX:\\s*", RegexOption.IGNORE_CASE), "")
                                                .replace(Regex("^MathType:\\s*", RegexOption.IGNORE_CASE), "")
                                                .replace(Regex("^LaTeX:\\s*", RegexOption.IGNORE_CASE), "")
                                                .replace(Regex("^Equation:\\s*", RegexOption.IGNORE_CASE), "")
                                                .trim()
                                            if (attrVal.isNotBlank() && !attrVal.contains("OLEObject", ignoreCase = true) && !attrVal.contains("EMBED", ignoreCase = true)) {
                                                if (attrVal.contains("\\") || attrVal.contains("=") || attrVal.contains("+") || attrVal.contains("-") || attrVal.contains("^") || attrVal.contains("_")) {
                                                    sb.append(" $").append(attrVal).append("$ ")
                                                }
                                            }
                                        }
                                    }
                                }

                                when (localName) {
                                    "omath", "omathpara" -> {
                                        inMath = true
                                        sb.append(" $")
                                    }
                                    "num" -> sb.append("\\frac{")
                                    "den" -> sb.append("}{")
                                    "sup" -> sb.append("^{")
                                    "sub" -> sb.append("_{")
                                    "rad" -> sb.append("\\sqrt{")
                                    "t" -> {
                                        sb.append(parser.nextText())
                                    }
                                    "instrtext" -> {
                                        val fieldTxt = parser.nextText()
                                        val converted = convertEqFieldToLatex(fieldTxt)
                                        sb.append(converted)
                                    }
                                    "tab" -> {
                                        sb.append("   ") // 3 spaces for option column alignment
                                    }
                                    "tc" -> {
                                        sb.append(" | ") // table column separator
                                    }
                                    "sym" -> {
                                        val charAttr = parser.getAttributeValue(null, "w:char") 
                                            ?: parser.getAttributeValue(null, "char")
                                            ?: if (parser.attributeCount > 0) parser.getAttributeValue(0) else null
                                        if (charAttr != null) {
                                            val symbolStr = mapSymbolCharToUnicode(charAttr)
                                            sb.append(symbolStr)
                                        }
                                    }
                                }
                            } else if (eventType == XmlPullParser.END_TAG) {
                                when (localName) {
                                    "omath", "omathpara" -> {
                                        inMath = false
                                        sb.append("$ ")
                                    }
                                    "den", "sup", "sub", "rad" -> {
                                        sb.append("}")
                                    }
                                    "p", "tr" -> {
                                        sb.append("\n")
                                    }
                                }
                            }
                            eventType = parser.next()
                        }
                        if (entry.name == "word/document.xml") break
                    }
                    entry = zipInputStream.nextEntry
                }
                sb.toString()
            } ?: ""
        } catch (e: Exception) {
            Log.e("ExamParser", "Error reading DOCX: ${e.message}", e)
            ""
        }
    }

    // Extract embedded equation/diagram images from word/media/ inside DOCX
    fun extractImagesFromDocx(context: Context, uri: Uri, maxImages: Int = 3): List<Pair<String, ByteArray>> {
        val result = mutableListOf<Pair<String, ByteArray>>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry
                while (entry != null && result.size < maxImages) {
                    val name = entry.name.lowercase()
                    if (name.startsWith("word/media/") && (name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".webp"))) {
                        val bos = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(2048)
                        var len: Int
                        while (zipInputStream.read(buffer).also { len = it } != -1) {
                            bos.write(buffer, 0, len)
                        }
                        val bytes = bos.toByteArray()
                        if (bytes.size in 100..3_000_000) {
                            val mime = when {
                                name.endsWith(".png") -> "image/png"
                                name.endsWith(".webp") -> "image/webp"
                                else -> "image/jpeg"
                            }
                            result.add(Pair(mime, bytes))
                        }
                    }
                    entry = zipInputStream.nextEntry
                }
            }
        } catch (e: Exception) {
            Log.e("ExamParser", "Error extracting DOCX images: ${e.message}", e)
        }
        return result
    }

    // Read text from a legacy binary DOC (Word 97-2003) URI with dual-pass scanner
    fun readTextFromDocUri(context: Context, uri: Uri): String {
        return try {
            val bytes = getBytesFromUri(context, uri) ?: return ""
            val sb = StringBuilder()
            var i = 0
            val len = bytes.size
            
            // Pass 1: 16-bit UTF-16LE scanner
            var currentRun16 = StringBuilder()
            while (i < len - 1) {
                val b1 = bytes[i].toInt() and 0xFF
                val b2 = bytes[i + 1].toInt() and 0xFF
                val charVal = (b2 shl 8) or b1
                val isValidChar = (charVal in 0x0020..0x007E) || 
                                  (charVal in 0x00A0..0x024F) || 
                                  (charVal in 0x0370..0x03FF) || // Greek letters
                                  (charVal in 0x2000..0x22FF) || // Math operators & symbols
                                  (charVal in 0x1EA0..0x1EF9) || // Vietnamese
                                  charVal == 0x000A || charVal == 0x000D
                if (isValidChar) {
                    if (charVal == 0x000D || charVal == 0x000A) {
                        currentRun16.append('\n')
                    } else {
                        currentRun16.append(charVal.toChar())
                    }
                    i += 2
                } else {
                    if (currentRun16.length >= 3) {
                        sb.append(currentRun16.toString()).append(" ")
                    }
                    currentRun16 = StringBuilder()
                    i++
                }
            }
            if (currentRun16.length >= 3) {
                sb.append(currentRun16.toString())
            }

            // Pass 2: 8-bit ASCII / CP1258 scanner for embedded MathType strings & EQ field codes
            var j = 0
            var currentRun8 = StringBuilder()
            while (j < len) {
                val b = bytes[j].toInt() and 0xFF
                val isValid8 = (b in 0x20..0x7E) || b == 0x0A || b == 0x0D
                if (isValid8) {
                    if (b == 0x0A || b == 0x0D) {
                        currentRun8.append('\n')
                    } else {
                        currentRun8.append(b.toChar())
                    }
                } else {
                    val str8 = currentRun8.toString()
                    if (str8.length >= 4 && (str8.contains("EQ") || str8.contains("\\f(") || str8.contains("Math") || str8.contains("DSMT4") || str8.contains("=") || str8.contains("+") || str8.contains("Câu"))) {
                        val converted = convertEqFieldToLatex(str8)
                        sb.append("\n").append(converted).append("\n")
                    }
                    currentRun8 = StringBuilder()
                }
                j++
            }

            val extracted = sb.toString()
                .replace(Regex("\n{3,}"), "\n\n")
                .replace(Regex(" +"), " ")
            if (extracted.length > 30) extracted else ""
        } catch (e: Exception) {
            Log.e("ExamParser", "Error reading legacy DOC: ${e.message}", e)
            ""
        }
    }

    // Read text from URI (automatically detects TXT vs PDF vs DOCX vs DOC)
    fun readTextFromUri(context: Context, uri: Uri): String {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val fileName = getFileName(context, uri)
        
        val isPdf = fileName.endsWith(".pdf", ignoreCase = true) || mimeType.contains("pdf", ignoreCase = true) || uri.path?.endsWith(".pdf", ignoreCase = true) == true
        val isDocx = fileName.endsWith(".docx", ignoreCase = true) || 
                     mimeType.contains("officedocument", ignoreCase = true) ||
                     uri.path?.endsWith(".docx", ignoreCase = true) == true
        val isDoc = fileName.endsWith(".doc", ignoreCase = true) || 
                    mimeType.contains("msword", ignoreCase = true) ||
                    uri.path?.endsWith(".doc", ignoreCase = true) == true

        if (isPdf) {
            return readTextFromPdfUri(context, uri)
        }
        if (isDocx) {
            val text = readTextFromDocxUri(context, uri)
            if (text.isNotBlank()) return text
        }
        if (isDoc) {
            val text = readTextFromDocUri(context, uri)
            if (text.isNotBlank()) return text
        }

        val stringBuilder = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line).append("\n")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ExamParser", "Error reading URI: ${e.message}", e)
        }
        return stringBuilder.toString()
    }

    // Get bytes from URI
    fun getBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val byteBuffer = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var len: Int
                while (inputStream.read(buffer).also { len = it } != -1) {
                    byteBuffer.write(buffer, 0, len)
                }
                byteBuffer.toByteArray()
            }
        } catch (e: Exception) {
            Log.e("ExamParser", "Error reading bytes from URI: ${e.message}", e)
            null
        }
    }

    // Call Gemini API to parse the exam paper text and optional attached document (PDF / DOCX / DOC)
    suspend fun parseWithGemini(context: Context, text: String, pdfUri: Uri? = null, onStatusUpdate: ((String) -> Unit)? = null): ParseResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext ParseResult.Error("Phím API chưa được thiết lập. Hãy thiết lập trong cấu hình Secrets.")
        }

        val prompt = """
            Bạn là một chuyên gia số hóa đề thi Toán học Trung học Phổ thông (THPT) lớp 12 tại Việt Nam.
            Hãy phân tích tài liệu đề thi đính kèm (hoặc văn bản đề thi dưới đây) và trích xuất thành cấu trúc JSON chính xác theo yêu cầu.
            
            QUY TẮC PHÂN TÍCH VÀ SỐ HÓA ĐẶC BIỆT CHUYÊN SÂU (ĐỂ HIỂN THỊ ĐẸP TRÊN ANDROID):
            1. CÔNG THỨC TOÁN HỌC (CỰC KỲ QUAN TRỌNG):
               - Chuyển đổi TẤT CẢ các công thức toán học, ký hiệu, biểu thức trong đề thi (kể cả chỉ số dưới, chỉ số trên, phân số, tích phân, căn thức, giới hạn, v.v.) sang định dạng LaTeX chuẩn chỉnh, bọc trong cặp dấu ${'$'}...${'$'} (ví dụ: ${'$'}y = x^3 - 3x${'$'}, ${'$'}\int_{0}^{2} f(x) dx${'$'}, ${'$'}\frac{a}{b}${'$'}).
               - Tuyệt đối KHÔNG xuất ký tự biểu tượng bì thư, ký tự lạ, hay mã font bị lỗi. Nếu gặp công thức toán bị thiếu hoặc lỗi font trong bản gốc, hãy suy luận toán học chính xác để khôi phục công thức hoàn chỉnh!
            2. QUY TẮC ĐỊNH DẠNG LaTeX CHI TIẾT CHO ANDROID PARSER:
               - Phân số: BẮT BUỘC dùng định dạng chuẩn \frac{tử_số}{mẫu_số} (ví dụ: \frac{a}{b}, \frac{x^2+1}{2x}). Không dùng khoảng trắng giữa lệnh \frac và dấu ngoặc nhọn.
               - Lũy thừa / Số mũ: Dùng ký tự ^ (ví dụ: x^2, x^{n+1}, 2^x). Nếu số mũ có nhiều hơn 1 ký tự, bắt buộc bọc trong dấu ngoặc nhọn {}.
               - Chỉ số dưới: Dùng ký tự _ (ví dụ: x_1, y_A, u_{n+1}). Nếu chỉ số dưới có nhiều hơn 1 ký tự, bắt buộc bọc trong dấu ngoặc nhọn {}.
               - Tích phân: Dùng dạng \int_{cận_dưới}^{cận_trên} (ví dụ: \int_{0}^{2} f(x)dx).
               - DOUBLE-ESCAPE CỰC KỲ QUAN TRỌNG: Tất cả các ký tự gạch chéo ngược (\) trong mã LaTeX nằm trong chuỗi JSON BẮT BUỘC phải được double-escaped thành \\ trong văn bản JSON trả về (ví dụ: viết "\\frac{a}{b}" thay vì "\frac{a}{b}", viết "\\theta" thay vì "\theta", viết "\\in" thay vì "\in"). Do đó hãy nhân đôi mọi dấu gạch chéo ngược (\) thành \\ trong tất cả các giá trị chuỗi của đối tượng JSON.
            3. HÌNH VẼ, ĐỒ THỊ, BẢNG BIẾN THIÊN VÀ BẢNG BIỂU:
               - BẢNG THỐNG KÊ, TẦN SỐ & BẢNG SỐ LIỆU: BẮT BUỘC định dạng bảng dưới dạng Bảng Markdown chuẩn dùng ký tự `|` để phân cách các cột (ví dụ: `| Khoảng điểm | [0; 25) | [25; 50) | [50; 75) |\n| Tần số | 5 | 12 | 8 |`) để ứng dụng tự động dựng thành Bảng số liệu đẹp mắt.
               - Bảng biến thiên: Mô tả chi tiết chiều biến thiên, các điểm cực trị, dấu đạo hàm (+/-), giới hạn tại vô cực hoặc dạng bảng Markdown.
               - Đồ thị, hình vẽ hình học: Phân tích kỹ nội dung hình vẽ và mô tả cực kỳ chi tiết bằng từ ngữ hoặc mô tả hình học nhúng trực tiếp vào nội dung câu hỏi (trường "content") trong ngoặc vuông `[Mô tả đồ thị: ...]` hoặc `[Mô tả hình vẽ: ...]`.
            4. BỎ DẠNG BÀI TẬP SỐ PHỨC (CỰC KỲ QUAN TRỌNG):
               - Tuyệt đối BỎ QUA HOÀN TOÀN và KHÔNG trích xuất bất kỳ câu hỏi nào thuộc dạng bài tập Số phức (nghĩa là các câu chứa số phức, đơn vị ảo i, phần thực, phần ảo, môđun số phức |z|, số phức liên hợp).
               - Chỉ trích xuất các chủ đề: Giải tích (Hàm số, Tích phân, Mũ & Logarit), Đại số (Phương trình, Bất phương trình, Cấp số), Hình học (Oxyz, Khối đa diện, Nón - Trụ - Cầu) và Thống kê & Xác suất.
            5. HÃY TRÍCH XUẤT tối đa tất cả câu hỏi phù hợp có trong tài liệu và phân loại vào đúng 3 phần:
               - Phần I (part1): Trắc nghiệm nhiều lựa chọn (4 lựa chọn, chọn 1 đáp án đúng).
               - Phần II (part2): Trắc nghiệm Đúng/Sai (Mỗi câu có 4 ý a, b, c, d, mỗi ý chọn Đúng hoặc Sai).
               - Phần III (part3): Trắc nghiệm trả lời ngắn (Điền đáp số là số nguyên hoặc số thập phân).
            6. LOẠI BỎ THÔNG TIN KHÔNG LIÊN QUAN: Loại bỏ hoàn toàn tiêu đề sở/trường, mã đề, biểu mẫu thí sinh, số trang...

            Văn bản tham khảo trích xuất từ file (nếu có):
            $text

            Yêu cầu bắt buộc về phản hồi JSON:
            Trả về duy nhất một đối tượng JSON (không bọc trong thẻ markdown ```json hay bất cứ thứ gì khác) có cấu trúc sau:
            {
              "part1": [
                {
                  "content": "Nội dung câu hỏi kèm mô tả hình vẽ/bảng biểu chi tiết...",
                  "options": ["Lựa chọn A", "Lựa chọn B", "Lựa chọn C", "Lựa chọn D"],
                  "correctIndex": 0,
                  "explanation": "Lời giải thích chi tiết...",
                  "topic": "Giải tích" hoặc "Đại số" hoặc "Hình học" hoặc "Thống kê",
                  "difficulty": "Dễ" hoặc "Trung bình" hoặc "Khó"
                }
              ],
              "part2": [
                {
                  "content": "Nội dung câu hỏi chính kèm mô tả...",
                  "subQuestions": [
                    {"statement": "Mệnh đề a...", "isTrue": true},
                    {"statement": "Mệnh đề b...", "isTrue": false},
                    {"statement": "Mệnh đề c...", "isTrue": true},
                    {"statement": "Mệnh đề d...", "isTrue": false}
                  ],
                  "explanation": "Lời giải thích chi tiết...",
                  "topic": "Giải tích" hoặc "Đại số" hoặc "Hình học" hoặc "Thống kê",
                  "difficulty": "Dễ" hoặc "Trung bình" hoặc "Khó"
                }
              ],
              "part3": [
                {
                  "content": "Nội dung câu hỏi ngắn...",
                  "correctAnswer": 12.5,
                  "explanation": "Lời giải thích chi tiết...",
                  "topic": "Giải tích" hoặc "Đại số" hoặc "Hình học" hoặc "Thống kê",
                  "difficulty": "Dễ" hoặc "Trung bình" hoặc "Khó"
                }
              ]
            }

            Nếu phần nào không có câu hỏi trong văn bản đề thi, hãy trả về mảng rỗng tương ứng.
        """.trimIndent()

        val jsonRequest = JSONObject().apply {
            val contentsArr = JSONArray().apply {
                put(JSONObject().apply {
                    val partsArr = JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        
                        // Attach document file for multimodal vision/file analysis if pdfUri/selectedUri is provided.
                        if (pdfUri != null) {
                            val fileName = getFileName(context, pdfUri)
                            val uriMime = context.contentResolver.getType(pdfUri) ?: ""
                            val isPdf = fileName.endsWith(".pdf", ignoreCase = true) || uriMime.contains("pdf", ignoreCase = true)
                            val isDocx = fileName.endsWith(".docx", ignoreCase = true) || uriMime.contains("officedocument", ignoreCase = true)
                            
                            if (isPdf) {
                                val bytes = getBytesFromUri(context, pdfUri)
                                if (bytes != null && bytes.size <= 8 * 1024 * 1024) {
                                    val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                    put(JSONObject().apply {
                                        put("inlineData", JSONObject().apply {
                                            put("mimeType", "application/pdf")
                                            put("data", base64Data)
                                        })
                                    })
                                }
                            } else if (isDocx) {
                                val docxImages = extractImagesFromDocx(context, pdfUri, maxImages = 3)
                                for ((imgMime, imgBytes) in docxImages) {
                                    val base64Data = android.util.Base64.encodeToString(imgBytes, android.util.Base64.NO_WRAP)
                                    put(JSONObject().apply {
                                        put("inlineData", JSONObject().apply {
                                            put("mimeType", imgMime)
                                            put("data", base64Data)
                                        })
                                    })
                                }
                            }
                        }
                    }
                    put("parts", partsArr)
                })
            }
            put("contents", contentsArr)
            
            // Set responseMimeType to application/json for structured output
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
            })
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()

        val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaTypeOrNull())
        
        // Multi-model fallback list to bypass single-model rate limits on 429
        val availableModels = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-pro-preview")
        
        var attempt = 0
        val maxAttempts = 6
        var delayMs = 4000L
        var lastErrorCode = -1
        var lastErrorMsg = "Giao tiếp AI thất bại."

        while (attempt < maxAttempts) {
            attempt++
            val currentModel = availableModels[(attempt - 1) % availableModels.size]
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$currentModel:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val code = response.code
                val responseStr = response.body?.string()

                if (response.isSuccessful && responseStr != null) {
                    val jsonObj = JSONObject(responseStr)
                    val candidates = jsonObj.optJSONArray("candidates")
                    if (candidates == null || candidates.length() == 0) {
                        return@withContext ParseResult.Error("Không nhận được phản hồi từ AI.")
                    }
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    if (content == null) {
                        return@withContext ParseResult.Error("Phản hồi từ AI không chứa nội dung.")
                    }
                    val parts = content.optJSONArray("parts")
                    if (parts == null || parts.length() == 0) {
                        return@withContext ParseResult.Error("Nội dung phản hồi từ AI rỗng.")
                    }
                    val responseText = parts.getJSONObject(0).getString("text")
                    return@withContext parseJsonResult(responseText)
                } else {
                    response.close()
                    lastErrorCode = code
                    lastErrorMsg = "Mã lỗi $code"

                    if (code == 429 || code == 503 || code == 504 || code == 500) {
                        val nextModel = availableModels[attempt % availableModels.size]
                        val reason = when (code) {
                            429 -> "Giới hạn tần suất (Too Many Requests - 429)"
                            503 -> "Dịch vụ tạm thời không khả dụng (Service Unavailable - 503)"
                            504 -> "Hết thời gian chờ Gateway (Gateway Timeout - 504)"
                            else -> "Lỗi máy chủ (Internal Server Error - 500)"
                        }

                        if (attempt < maxAttempts) {
                            Log.w("ExamParser", "Gemini API ($currentModel) failed with $reason. Retrying with $nextModel in ${delayMs / 1000}s (Lần $attempt/$maxAttempts)...")
                            onStatusUpdate?.invoke("AI bị $reason ở mô hình $currentModel. Đang tự động chuyển sang $nextModel và thử lại sau ${delayMs / 1000}s (Lần $attempt/$maxAttempts)...")
                            delay(delayMs)
                            delayMs = (delayMs * 1.5).toLong().coerceAtMost(12000L)
                        }
                    } else {
                        // For other codes (like 400, 403), do not retry as they are permanent
                        return@withContext ParseResult.Error("Giao tiếp AI thất bại: Mã lỗi $code ($currentModel).")
                    }
                }
            } catch (e: Exception) {
                lastErrorMsg = e.localizedMessage ?: "Lỗi kết nối"
                if (attempt < maxAttempts) {
                    val nextModel = availableModels[attempt % availableModels.size]
                    Log.w("ExamParser", "Network error on attempt $attempt ($currentModel): ${e.message}. Retrying with $nextModel in ${delayMs / 1000}s...")
                    onStatusUpdate?.invoke("Lỗi kết nối mạng: ${e.localizedMessage}. Đang tự động thử lại với $nextModel sau ${delayMs / 1000}s...")
                    delay(delayMs)
                    delayMs = (delayMs * 1.5).toLong().coerceAtMost(12000L)
                } else {
                    Log.e("ExamParser", "Error calling Gemini after $attempt attempts: ${e.message}", e)
                    return@withContext ParseResult.Error("Lỗi kết nối AI: ${e.localizedMessage}")
                }
            }
        }

        return@withContext ParseResult.Error("Giao tiếp AI thất bại sau $maxAttempts lần thử. Lỗi cuối cùng: $lastErrorMsg. Hãy thử lại sau một chút.")
    }

    // Sanitize raw JSON string to handle unescaped or partially escaped backslashes safely
    private fun sanitizeRawJson(rawJson: String): String {
        var s = rawJson
            .replace("\u000C", "\\\\f") // form feed -> \\f
            .replace("\u0008", "\\\\b") // backspace -> \\b
            
        // Replace tab character (\u0009) followed by common math suffixes
        s = s.replace("\u0009heta", "\\\\theta")
             .replace("\u0009imes", "\\\\times")
             .replace("\u0009o", "\\\\to")
             .replace("\u0009an", "\\\\tan")
             .replace("\u0009riangle", "\\\\triangle")
             
        // Replace carriage return (\u000D) followed by common math suffixes
        s = s.replace("\u000Dight", "\\\\right")
             .replace("\u000Dho", "\\\\rho")
             .replace("\rightharpoonup", "\\\\rightharpoonup")
             .replace("\right", "\\\\right")
             .replace("\rho", "\\\\rho")

        // Parse character by character to catch single backslashes that would be invalid JSON escapes
        val sb = StringBuilder()
        var i = 0
        val len = s.length
        while (i < len) {
            val c = s[i]
            if (c == '\\') {
                if (i + 1 < len) {
                    val next = s[i + 1]
                    // Standard valid escape characters in JSON
                    val isValidEscape = when (next) {
                        '"', '\\', '/', 'b', 'f', 'n', 'r', 't' -> true
                        'u' -> {
                            // \uXXXX (4 hex digits)
                            if (i + 5 < len) {
                                (2..5).all { offset ->
                                    val hc = s[i + offset]
                                    hc in '0'..'9' || hc in 'a'..'f' || hc in 'A'..'F'
                                }
                            } else false
                        }
                        else -> false
                    }
                    if (isValidEscape) {
                        sb.append('\\')
                        sb.append(next)
                        i += 2
                    } else {
                        // Duplicate single backslash to double backslash
                        sb.append("\\\\")
                        sb.append(next)
                        i += 2
                    }
                } else {
                    sb.append("\\\\")
                    i++
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun isComplexNumberQuestion(text: String): Boolean {
        val lower = text.lowercase()
        return lower.contains("số phức") || 
               lower.contains("phần thực") || 
               lower.contains("phần ảo") || 
               lower.contains("môđun của z") || 
               lower.contains("môđun của số phức") || 
               lower.contains("số phức liên hợp") || 
               (lower.contains("môđun") && lower.contains("z"))
    }

    // Parse the structured JSON output
    fun parseJsonResult(jsonStr: String): ParseResult {
        try {
            // In case it's wrapped in markdown backticks
            var cleanJson = jsonStr.trim()
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.removePrefix("```json")
                if (cleanJson.endsWith("```")) {
                    cleanJson = cleanJson.removeSuffix("```")
                }
            } else if (cleanJson.startsWith("```")) {
                cleanJson = cleanJson.removePrefix("```")
                if (cleanJson.endsWith("```")) {
                    cleanJson = cleanJson.removeSuffix("```")
                }
            }
            cleanJson = cleanJson.trim()

            // Preprocess and sanitize unescaped LaTeX backslashes
            cleanJson = sanitizeRawJson(cleanJson)

            val root = JSONObject(cleanJson)
            
            val p1List = mutableListOf<Part1Question>()
            val p2List = mutableListOf<Part2Question>()
            val p3List = mutableListOf<Part3Question>()

            var baseId = System.currentTimeMillis().toInt() / 1000

            // Part 1
            if (root.has("part1")) {
                val p1Arr = root.getJSONArray("part1")
                for (i in 0 until p1Arr.length()) {
                    val obj = p1Arr.getJSONObject(i)
                    val content = obj.getString("content")
                    if (isComplexNumberQuestion(content)) continue

                    val optsArr = obj.getJSONArray("options")
                    val options = mutableListOf<String>()
                    for (j in 0 until optsArr.length()) {
                        options.add(optsArr.getString(j))
                    }
                    
                    p1List.add(
                        Part1Question(
                            id = baseId++,
                            content = content,
                            originalOptions = options,
                            correctIndex = obj.optInt("correctIndex", 0),
                            explanation = obj.optString("explanation", "Chưa có lời giải chi tiết."),
                            topic = obj.optString("topic", "Giải tích"),
                            difficulty = obj.optString("difficulty", "Dễ")
                        )
                    )
                }
            }

            // Part 2
            if (root.has("part2")) {
                val p2Arr = root.getJSONArray("part2")
                for (i in 0 until p2Arr.length()) {
                    val obj = p2Arr.getJSONObject(i)
                    val content = obj.getString("content")
                    if (isComplexNumberQuestion(content)) continue

                    val subArr = obj.getJSONArray("subQuestions")
                    val subQuestions = mutableListOf<Part2SubQuestion>()
                    for (j in 0 until subArr.length()) {
                        val subObj = subArr.getJSONObject(j)
                        subQuestions.add(
                            Part2SubQuestion(
                                statement = subObj.getString("statement"),
                                isTrue = subObj.optBoolean("isTrue", true)
                            )
                        )
                    }
                    p2List.add(
                        Part2Question(
                            id = baseId++,
                            content = content,
                            subQuestions = subQuestions,
                            explanation = obj.optString("explanation", "Chưa có lời giải chi tiết."),
                            topic = obj.optString("topic", "Giải tích"),
                            difficulty = obj.optString("difficulty", "Trung bình")
                        )
                    )
                }
            }

            // Part 3
            if (root.has("part3")) {
                val p3Arr = root.getJSONArray("part3")
                for (i in 0 until p3Arr.length()) {
                    val obj = p3Arr.getJSONObject(i)
                    val content = obj.getString("content")
                    if (isComplexNumberQuestion(content)) continue

                    p3List.add(
                        Part3Question(
                            id = baseId++,
                            content = content,
                            correctAnswer = obj.optDouble("correctAnswer", 0.0),
                            explanation = obj.optString("explanation", "Chưa có lời giải chi tiết."),
                            topic = obj.optString("topic", "Giải tích"),
                            difficulty = obj.optString("difficulty", "Khó")
                        )
                    )
                }
            }

            return ParseResult.Success(p1List, p2List, p3List)
        } catch (e: Exception) {
            Log.e("ExamParser", "Error parsing JSON result: ${e.message}", e)
            return ParseResult.Error("Lỗi phân tích cú pháp dữ liệu: ${e.localizedMessage}")
        }
    }

    // Offline parser using Regex
    fun parseOffline(text: String): ParseResult {
        if (text.isBlank()) {
            return ParseResult.Error("Nội dung trống, không thể phân tích.")
        }

        val p1List = mutableListOf<Part1Question>()
        val p2List = mutableListOf<Part2Question>()
        val p3List = mutableListOf<Part3Question>()
        var baseId = (System.currentTimeMillis().toInt() / 1000) + 500

        try {
            val normalizedText = text.replace("\r\n", "\n")
            val qBlocks = normalizedText.split(Regex("(?i)Câu\\s+\\d+[:\\.]"))
            
            if (qBlocks.size <= 1) {
                return ParseResult.Error("Không tìm thấy câu hỏi dạng 'Câu 1.', 'Câu 2.' trong văn bản. Vui lòng kiểm tra định dạng hoặc phân tích bằng AI.")
            }

            for (i in 1 until qBlocks.size) {
                val block = qBlocks[i].trim()
                if (block.isBlank()) continue

                val hasSubquestions = block.contains(Regex("[a-d]\\)\\s+")) || block.contains(Regex("[a-d]\\.\\s+"))
                val optionsMatch = Regex("(?s)([A-D])\\s*\\.\\s*(.*?)(?=\\s*[A-D]\\s*\\.|$)")
                val foundOptions = optionsMatch.findAll(block).toList()

                if (!hasSubquestions && foundOptions.size >= 2) {
                    // Part 1: MCQ
                    val firstOptIndex = block.indexOf(foundOptions[0].value)
                    val content = if (firstOptIndex > 0) block.substring(0, firstOptIndex).trim() else "Câu hỏi trắc nghiệm"
                    
                    val options = mutableListOf<String>()
                    var correctIdx = 0
                    foundOptions.forEachIndexed { idx, match ->
                        val optText = match.groups[2]?.value?.trim() ?: ""
                        options.add(optText)
                        if (match.value.endsWith("*") || optText.endsWith("*")) {
                            correctIdx = idx
                        }
                    }

                    val solutionRegex = Regex("(?i)Chọn\\s+([A-D])|Đáp\\s*án\\s*:\\s*([A-D])")
                    val solMatch = solutionRegex.find(block)
                    if (solMatch != null) {
                        val ansChar = (solMatch.groups[1]?.value ?: solMatch.groups[2]?.value ?: "A").uppercase()
                        correctIdx = when (ansChar) {
                            "A" -> 0
                            "B" -> 1
                            "C" -> 2
                            "D" -> 3
                            else -> correctIdx
                        }
                    }

                    p1List.add(
                        Part1Question(
                            id = baseId++,
                            content = content,
                            originalOptions = if (options.size < 4) {
                                val padded = options.toMutableList()
                                while (padded.size < 4) {
                                    padded.add("Phương án ${'A' + padded.size}")
                                }
                                padded
                            } else options.take(4),
                            correctIndex = correctIdx,
                            explanation = block.substringAfter("Lời giải").substringAfter("Hướng dẫn").trim().ifBlank { "Chưa có lời giải chi tiết." },
                            topic = determineTopic(content),
                            difficulty = "Dễ"
                        )
                    )
                } else if (hasSubquestions) {
                    // Part 2: True/False
                    val firstSubIndex = block.indexOfFirst { it == 'a' || it == 'a' }
                    val content = if (firstSubIndex > 10) block.substring(0, firstSubIndex).trim() else block.split("\n")[0]
                    
                    val subs = mutableListOf<Part2SubQuestion>()
                    val subMatches = Regex("(?m)^\\s*([a-d])(?:\\)|\\.)\\s*(.*)").findAll(block)
                    subMatches.forEach { match ->
                        val label = match.groups[1]?.value ?: "a"
                        val statement = match.groups[2]?.value?.trim() ?: ""
                        val isTrue = !statement.contains("Sai") && !statement.contains("sai")
                        subs.add(
                             Part2SubQuestion(
                                statement = "Ý $label) $statement",
                                isTrue = isTrue
                             )
                        )
                    }

                    if (subs.isNotEmpty()) {
                        p2List.add(
                            Part2Question(
                                id = baseId++,
                                content = content,
                                subQuestions = if (subs.size < 4) {
                                    val padded = subs.toMutableList()
                                    while (padded.size < 4) {
                                        padded.add(Part2SubQuestion("Ý phụ ${'a' + padded.size}...", true))
                                    }
                                    padded
                                } else subs.take(4),
                                explanation = block.substringAfter("Lời giải").substringAfter("Hướng dẫn").trim().ifBlank { "Chưa có lời giải chi tiết." },
                                topic = determineTopic(content),
                                difficulty = "Trung bình"
                            )
                        )
                    }
                } else {
                    // Part 3: Short Answer
                    val content = block.split("\n")[0].trim()
                    var ansValue = 0.0
                    val numMatch = Regex("-?\\d+(?:\\.\\d+)?").find(block.substringAfter("Đáp số").substringAfter("Đáp án"))
                    if (numMatch != null) {
                        ansValue = numMatch.value.toDoubleOrNull() ?: 0.0
                    }
                    
                    p3List.add(
                        Part3Question(
                            id = baseId++,
                            content = block,
                            correctAnswer = ansValue,
                            explanation = block.substringAfter("Lời giải").substringAfter("Hướng dẫn").trim().ifBlank { "Chưa có lời giải chi tiết." },
                            topic = determineTopic(content),
                            difficulty = "Khó"
                        )
                    )
                }
            }

            if (p1List.isEmpty() && p2List.isEmpty() && p3List.isEmpty()) {
                return ParseResult.Error("Không tìm thấy câu hỏi hợp lệ. Xin kiểm tra cấu trúc.")
            }

            return ParseResult.Success(p1List, p2List, p3List)
        } catch (e: Exception) {
            Log.e("ExamParser", "Offline parsing error: ${e.message}", e)
            return ParseResult.Error("Lỗi khi phân tích ngoại tuyến: ${e.localizedMessage}")
        }
    }

    // Helper to extract nested curly braces
    fun extractCurlyBracesContent(text: String, startIndex: Int): List<String> {
        val results = mutableListOf<String>()
        var i = startIndex
        while (i < text.length) {
            val start = text.indexOf('{', i)
            if (start == -1) break
            
            var braceCount = 1
            var end = start + 1
            while (end < text.length && braceCount > 0) {
                if (text[end] == '{') braceCount++
                else if (text[end] == '}') braceCount--
                end++
            }
            if (braceCount == 0) {
                results.add(text.substring(start + 1, end - 1))
                i = end
            } else {
                break
            }
        }
        return results
    }

    // Offline LaTeX math questions parser
    fun parseLatex(text: String): ParseResult {
        if (text.isBlank()) {
            return ParseResult.Error("Nội dung LaTeX trống, không thể phân tích.")
        }

        val p1List = mutableListOf<Part1Question>()
        val p2List = mutableListOf<Part2Question>()
        val p3List = mutableListOf<Part3Question>()
        var baseId = (System.currentTimeMillis().toInt() / 1000) + 1000

        try {
            val normalizedText = text.replace("\r\n", "\n")
            
            // Find all question blocks in LaTeX environments: ex, question, cau, exercise, problem
            val blockRegex = Regex("(?s)\\\\begin\\{(ex|question|cau|exercise|problem)\\}(.*?)\\\\end\\{\\1\\}")
            val matches = blockRegex.findAll(normalizedText).toList()

            val blocksToProcess = if (matches.isNotEmpty()) {
                matches.map { it.groups[2]?.value ?: "" }
            } else {
                // Fallback: Split by \item if we have a list of questions, or split by Câu
                if (normalizedText.contains("\\item")) {
                    normalizedText.split("\\item").drop(1)
                } else {
                    normalizedText.split(Regex("(?i)Câu\\s+\\d+[:\\.]")).drop(1)
                }
            }

            if (blocksToProcess.isEmpty()) {
                return ParseResult.Error("Không tìm thấy câu hỏi LaTeX nào. Định dạng đề cần sử dụng các môi trường \\begin{ex}...\\end{ex}, \\begin{question}...\\end{question}, hoặc sử dụng các từ khóa \\item / Câu.")
            }

            for (rawBlock in blocksToProcess) {
                val block = rawBlock.trim()
                if (block.isBlank()) continue

                // Clean comments starting with % (except for %[...] annotations)
                val lines = block.lines().map { line ->
                    if (line.trim().startsWith("%") && !line.trim().startsWith("%[")) {
                        ""
                    } else {
                        line
                    }
                }
                val cleanedBlock = lines.joinToString("\n").trim()

                // Detect question type
                val isPart2 = cleanedBlock.contains("\\choiceTF")
                val isPart3 = cleanedBlock.contains("\\gex") || cleanedBlock.contains("Đáp số:") || cleanedBlock.contains("đáp số")

                if (isPart2) {
                    // Part 2: True/False
                    val choiceTfIndex = cleanedBlock.indexOf("\\choiceTF")
                    var content = if (choiceTfIndex > 0) cleanedBlock.substring(0, choiceTfIndex).trim() else cleanedBlock
                    // Remove leading metadata like %[Câu 1] or similar
                    content = content.replace(Regex("%\\[.*?\\]"), "").trim()

                    val subs = mutableListOf<Part2SubQuestion>()
                    val braceContents = extractCurlyBracesContent(cleanedBlock, choiceTfIndex + 9)
                    
                    val statements = if (braceContents.size >= 4) {
                        braceContents.take(4)
                    } else {
                        // fallback regex if braces extract failed
                        val fallbackMatches = Regex("(?s)\\{\\s*(\\\\True)?\\s*(.*?)\\}").findAll(cleanedBlock.substring(choiceTfIndex + 9))
                        fallbackMatches.map { (it.groups[1]?.value ?: "") + " " + (it.groups[2]?.value ?: "") }.toList()
                    }

                    statements.forEachIndexed { index, rawStatement ->
                        var isTrue = rawStatement.contains("\\True") || rawStatement.contains("\\true")
                        var statementText = rawStatement.replace("\\True", "").replace("\\true", "").trim()
                        val letter = ('a' + index)
                        subs.add(
                            Part2SubQuestion(
                                statement = "Ý $letter) $statementText",
                                isTrue = isTrue
                            )
                        )
                    }

                    // Fill to 4 sub-questions if needed
                    while (subs.size < 4) {
                        val letter = ('a' + subs.size)
                        subs.add(Part2SubQuestion("Ý $letter) Mệnh đề chưa được cấu hình", false))
                    }

                    p2List.add(
                        Part2Question(
                            id = baseId++,
                            content = content,
                            subQuestions = subs,
                            explanation = cleanedBlock.substringAfter("\\loigiaichitiet").substringAfter("\\loigia").substringAfter("Lời giải").substringAfter("Hướng dẫn").trim().substringBefore("\\end").trim().ifBlank { "Chưa có lời giải chi tiết." },
                            topic = determineTopic(content),
                            difficulty = "Trung bình"
                        )
                    )
                } else if (isPart3) {
                    // Part 3: Short Answer
                    var content = cleanedBlock
                    if (content.contains("\\gex")) {
                        val gexIndex = content.indexOf("\\gex")
                        content = content.substring(0, gexIndex).trim()
                    }
                    content = content.replace(Regex("%\\[.*?\\]"), "").trim()

                    var ansValue = 0.0
                    if (cleanedBlock.contains("\\gex")) {
                        val gexIndex = cleanedBlock.indexOf("\\gex")
                        val braces = extractCurlyBracesContent(cleanedBlock, gexIndex + 4)
                        if (braces.isNotEmpty()) {
                            ansValue = braces[0].trim().toDoubleOrNull() ?: 0.0
                        }
                    } else {
                        val numMatch = Regex("-?\\d+(?:\\.\\d+)?").find(cleanedBlock.substringAfter("Đáp số").substringAfter("Đáp án"))
                        if (numMatch != null) {
                            ansValue = numMatch.value.toDoubleOrNull() ?: 0.0
                        }
                    }

                    p3List.add(
                        Part3Question(
                            id = baseId++,
                            content = content,
                            correctAnswer = ansValue,
                            explanation = cleanedBlock.substringAfter("\\loigiaichitiet").substringAfter("\\loigia").substringAfter("Lời giải").substringAfter("Hướng dẫn").trim().substringBefore("\\end").trim().ifBlank { "Chưa có lời giải chi tiết." },
                            topic = determineTopic(content),
                            difficulty = "Khó"
                        )
                    )
                } else {
                    // Part 1: MCQ (Default)
                    val choiceIndex = cleanedBlock.indexOf("\\choice")
                    var content = if (choiceIndex > 0) cleanedBlock.substring(0, choiceIndex).trim() else cleanedBlock
                    content = content.replace(Regex("%\\[.*?\\]"), "").trim()

                    val options = mutableListOf<String>()
                    var correctIdx = 0

                    if (choiceIndex != -1) {
                        val braceContents = extractCurlyBracesContent(cleanedBlock, choiceIndex + 7)
                        val rawOptions = if (braceContents.size >= 4) {
                            braceContents.take(4)
                        } else {
                            // try matching standard options within braces
                            val fallbackMatches = Regex("(?s)\\{\\s*(\\\\True)?\\s*(.*?)\\}").findAll(cleanedBlock.substring(choiceIndex + 7))
                            fallbackMatches.map { (it.groups[1]?.value ?: "") + " " + (it.groups[2]?.value ?: "") }.toList()
                        }

                        rawOptions.forEachIndexed { index, rawOpt ->
                            val isTrue = rawOpt.contains("\\True") || rawOpt.contains("\\true")
                            val optText = rawOpt.replace("\\True", "").replace("\\true", "").trim()
                            options.add(optText)
                            if (isTrue) {
                                correctIdx = index
                            }
                        }
                    }

                    // Fallback to regex split if no options extracted
                    if (options.isEmpty()) {
                        val optionsMatch = Regex("(?s)([A-D])\\s*\\.\\s*(.*?)(?=\\s*[A-D]\\s*\\.|$)")
                        val foundOptions = optionsMatch.findAll(cleanedBlock).toList()
                        foundOptions.forEachIndexed { idx, match ->
                            val optText = match.groups[2]?.value?.trim() ?: ""
                            options.add(optText)
                            if (match.value.endsWith("*") || optText.endsWith("*")) {
                                correctIdx = idx
                            }
                        }
                    }

                    // Fill to 4 options
                    while (options.size < 4) {
                        options.add("Phương án ${'A' + options.size}...")
                    }

                    p1List.add(
                        Part1Question(
                            id = baseId++,
                            content = content,
                            originalOptions = options,
                            correctIndex = correctIdx,
                            explanation = cleanedBlock.substringAfter("\\loigiaichitiet").substringAfter("\\loigia").substringAfter("Lời giải").substringAfter("Hướng dẫn").trim().substringBefore("\\end").trim().ifBlank { "Chưa có lời giải chi tiết." },
                            topic = determineTopic(content),
                            difficulty = "Dễ"
                        )
                    )
                }
            }

            if (p1List.isEmpty() && p2List.isEmpty() && p3List.isEmpty()) {
                return ParseResult.Error("Không tìm thấy câu hỏi LaTeX hợp lệ. Hãy kiểm tra các thẻ môi trường hoặc cấu trúc lựa chọn.")
            }

            return ParseResult.Success(p1List, p2List, p3List)
        } catch (e: Exception) {
            Log.e("ExamParser", "LaTeX parsing error: ${e.message}", e)
            return ParseResult.Error("Lỗi khi phân tích LaTeX: ${e.localizedMessage}")
        }
    }

    private fun determineTopic(content: String): String {
        val lower = content.lowercase()
        return if (lower.contains("không gian") || lower.contains("oxyz") || lower.contains("mặt cầu") || lower.contains("thể tích") || lower.contains("chóp") || lower.contains("lăng trụ") || lower.contains("hình nón") || lower.contains("trụ xoay")) {
            "Hình học không gian"
        } else if (lower.contains("tam giác") || lower.contains("đường tròn") || lower.contains("vectơ") || lower.contains("vector") || lower.contains("elip") || lower.contains("đường thẳng") || lower.contains("hình học phẳng")) {
            "Hình học phẳng"
        } else if (lower.contains("tổ hợp") || lower.contains("xác suất") || lower.contains("chỉnh hợp") || lower.contains("hoán vị") || lower.contains("nhị thức") || lower.contains("phép đếm")) {
            "Tổ hợp & Xác suất"
        } else if (lower.contains("phương trình") || lower.contains("bất phương trình") || lower.contains("hệ phương trình") || lower.contains("đại số") || lower.contains("ma trận")) {
            "Đại số"
        } else {
            "Giải tích"
        }
    }
}

sealed class ParseResult {
    data class Success(
        val part1: List<Part1Question>,
        val part2: List<Part2Question>,
        val part3: List<Part3Question>
    ) : ParseResult()
    data class Error(val message: String) : ParseResult()
}
