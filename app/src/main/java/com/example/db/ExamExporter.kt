package com.example.db

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.ExamSession
import com.example.ShuffledPart1Question
import com.example.Part2Question
import com.example.Part3Question
import java.io.OutputStream

object ExamExporter {

    // =========================================================================
    // HTML MATH FORMATTING AND ILLUSTRATION HELPERS
    // =========================================================================
    fun formatMathForHtml(text: String): String {
        var res = text
        // Convert vector combining characters
        res = res.replace("n\u20D7", "vectơ <b>n</b>")
        res = res.replace("u\u20D7", "vectơ <b>u</b>")
        res = res.replace("v\u20D7", "vectơ <b>v</b>")
        res = res.replace("z\u0304", "<span style=\"text-decoration: overline;\">z</span>")

        // Superscripts
        res = res.replace("⁰", "<sup>0</sup>")
        res = res.replace("¹", "<sup>1</sup>")
        res = res.replace("²", "<sup>2</sup>")
        res = res.replace("³", "<sup>3</sup>")
        res = res.replace("⁴", "<sup>4</sup>")
        res = res.replace("⁵", "<sup>5</sup>")
        res = res.replace("⁶", "<sup>6</sup>")
        res = res.replace("⁷", "<sup>7</sup>")
        res = res.replace("⁸", "<sup>8</sup>")
        res = res.replace("⁹", "<sup>9</sup>")
        res = res.replace("ⁿ", "<sup>n</sup>")
        res = res.replace("ⁱ", "<sup>i</sup>")

        // Subscripts
        res = res.replace("₀", "<sub>0</sub>")
        res = res.replace("₁", "<sub>1</sub>")
        res = res.replace("₂", "<sub>2</sub>")
        res = res.replace("₃", "<sub>3</sub>")
        res = res.replace("₄", "<sub>4</sub>")
        res = res.replace("₅", "<sub>5</sub>")
        res = res.replace("₆", "<sub>6</sub>")
        res = res.replace("₇", "<sub>7</sub>")
        res = res.replace("₈", "<sub>8</sub>")
        res = res.replace("₉", "<sub>9</sub>")

        // Math signs
        res = res.replace("\u21D4", " &hArr; ")
        res = res.replace("\u21D2", " &rArr; ")
        res = res.replace("\u2192", " &rarr; ")
        res = res.replace("\u221A", " &radic; ")
        res = res.replace("\u222B", " &int; ")
        res = res.replace("\u03C0", " &pi; ")

        return res
    }

    fun getHtmlIllustration(part: Int, questionId: Int): String {
        return when {
            part == 1 && questionId == 1 -> """
                <table style="border-collapse: collapse; width: 100%; border: 1px solid #000; font-family: 'Times New Roman', serif; font-size: 11pt; margin: 15px 0;">
                  <tr style="border-bottom: 1px solid #000;">
                    <td style="width: 15%; border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center;">x</td>
                    <td style="padding: 6px; text-align: center; width: 15%;">-&infin;</td>
                    <td style="padding: 6px; text-align: center; width: 5%;"></td>
                    <td style="padding: 6px; text-align: center; width: 15%; font-weight: bold;">0</td>
                    <td style="padding: 6px; text-align: center; width: 15%;"></td>
                    <td style="padding: 6px; text-align: center; width: 15%; font-weight: bold;">2</td>
                    <td style="padding: 6px; text-align: center; width: 5%;"></td>
                    <td style="padding: 6px; text-align: center; width: 15%;">+&infin;</td>
                  </tr>
                  <tr style="border-bottom: 1px solid #000;">
                    <td style="border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center;">y'</td>
                    <td style="padding: 6px; text-align: center;"></td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: blue;">+</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold;">0</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: red;">-</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold;">0</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: blue;">+</td>
                    <td style="padding: 6px; text-align: center;"></td>
                  </tr>
                  <tr>
                    <td style="border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center; vertical-align: middle;">y</td>
                    <td colspan="7" style="padding: 10px;">
                      <table style="width: 100%; border-collapse: collapse; border: none;">
                        <tr>
                          <td style="width: 20%; text-align: left; vertical-align: bottom;">-&infin;</td>
                          <td style="width: 5%; text-align: center; vertical-align: bottom; color: #1a73e8;">&nearr;</td>
                          <td style="width: 25%; text-align: center; vertical-align: top; font-weight: bold; color: green;">2 (CĐ)</td>
                          <td style="width: 5%; text-align: center; vertical-align: top; color: #1a73e8;">&searr;</td>
                          <td style="width: 25%; text-align: center; vertical-align: bottom; font-weight: bold; color: red;">-2 (CT)</td>
                          <td style="width: 5%; text-align: center; vertical-align: bottom; color: #1a73e8;">&nearr;</td>
                          <td style="width: 20%; text-align: right; vertical-align: top;">+&infin;</td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
            """.trimIndent()

            part == 1 && questionId == 2 -> """
                <div style="font-family: monospace; white-space: pre; border: 1px solid #ccc; padding: 10px; background: #fafafa; margin: 15px 0;">
                Đồ thị hàm số y = (2x - 1)/(x - 1) với:
                - Tiệm cận đứng (TCĐ): x = 1
                - Tiệm cận ngang (TCN): y = 2
                - Giao điểm với trục tung: (0; 1)
                - Giao điểm với trục hoành: (0.5; 0)
                </div>
            """.trimIndent()

            part == 2 && questionId == 1 -> """
                <table style="border-collapse: collapse; width: 100%; border: 1px solid #000; font-family: 'Times New Roman', serif; font-size: 11pt; margin: 15px 0;">
                  <tr style="border-bottom: 1px solid #000;">
                    <td style="width: 15%; border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center;">x</td>
                    <td style="padding: 6px; text-align: center; width: 25%;">-&infin;</td>
                    <td style="padding: 6px; text-align: center; width: 20%; font-weight: bold; color: red;">1</td>
                    <td style="padding: 6px; text-align: center; width: 25%;">+&infin;</td>
                  </tr>
                  <tr style="border-bottom: 1px solid #000;">
                    <td style="border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center;">y'</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: red;">-</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: red;">||</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: red;">-</td>
                  </tr>
                  <tr>
                    <td style="border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center; vertical-align: middle;">y</td>
                    <td colspan="3" style="padding: 10px;">
                      <table style="width: 100%; border-collapse: collapse; border: none;">
                        <tr>
                          <td style="width: 10%; text-align: left; vertical-align: top;">1</td>
                          <td style="width: 35%; text-align: center; vertical-align: middle; color: #1a73e8;">&searr;</td>
                          <td style="width: 10%; text-align: right; vertical-align: bottom;">-&infin;</td>
                          <td style="width: 10%; border-left: 2px double red; text-align: left; vertical-align: top; padding-left: 10px;">+&infin;</td>
                          <td style="width: 35%; text-align: center; vertical-align: middle; color: #1a73e8;">&searr;</td>
                          <td style="width: 10%; text-align: right; vertical-align: bottom;">1</td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
            """.trimIndent()

            part == 2 && questionId == 3 -> """
                <table style="border-collapse: collapse; width: 100%; border: 1px solid #000; font-family: 'Times New Roman', serif; font-size: 11pt; margin: 15px 0;">
                  <tr style="border-bottom: 1px solid #000;">
                    <td style="width: 15%; border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center;">x</td>
                    <td style="padding: 6px; text-align: center; width: 15%;">-&infin;</td>
                    <td style="padding: 6px; text-align: center; width: 15%; font-weight: bold;">-1</td>
                    <td style="padding: 6px; text-align: center; width: 15%; font-weight: bold;">0</td>
                    <td style="padding: 6px; text-align: center; width: 15%; font-weight: bold;">1</td>
                    <td style="padding: 6px; text-align: center; width: 15%;">+&infin;</td>
                  </tr>
                  <tr style="border-bottom: 1px solid #000;">
                    <td style="border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center;">y'</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: red;">-</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold;">0</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: blue;">+</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold;">0</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: red;">-</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold;">0</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: blue;">+</td>
                  </tr>
                  <tr>
                    <td style="border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center; vertical-align: middle;">y</td>
                    <td colspan="7" style="padding: 10px;">
                      <table style="width: 100%; border-collapse: collapse; border: none;">
                        <tr>
                          <td style="width: 15%; text-align: left; vertical-align: top;">+&infin;</td>
                          <td style="width: 5%; text-align: center; vertical-align: middle; color: #1a73e8;">&searr;</td>
                          <td style="width: 20%; text-align: center; vertical-align: bottom; font-weight: bold; color: green;">2 (CT)</td>
                          <td style="width: 5%; text-align: center; vertical-align: middle; color: #1a73e8;">&nearr;</td>
                          <td style="width: 20%; text-align: center; vertical-align: top; font-weight: bold; color: blue;">3 (CĐ)</td>
                          <td style="width: 5%; text-align: center; vertical-align: middle; color: #1a73e8;">&searr;</td>
                          <td style="width: 20%; text-align: center; vertical-align: bottom; font-weight: bold; color: green;">2 (CT)</td>
                          <td style="width: 5%; text-align: center; vertical-align: middle; color: #1a73e8;">&nearr;</td>
                          <td style="width: 15%; text-align: right; vertical-align: top;">+&infin;</td>
                        </tr>
                      </table>
                    </td>
                  </tr>
                </table>
            """.trimIndent()

            part == 2 && questionId == 5 -> """
                <div style="font-family: monospace; white-space: pre; border: 1px solid #ccc; padding: 10px; background: #fafafa; margin: 15px 0;">
                Sơ đồ hình hộp chữ nhật ABCD.A'B'C'D' có:
                - AB = 1 (chiều rộng)
                - AD = 2 (chiều dài)
                - AA' = 3 (chiều cao)
                </div>
            """.trimIndent()

            part == 2 && questionId == 6 -> """
                <table style="border-collapse: collapse; width: 100%; border: 1px solid #000; font-family: 'Times New Roman', serif; font-size: 11pt; margin: 15px 0;">
                  <tr style="border-bottom: 1px solid #000;">
                    <td style="width: 15%; border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center;">x</td>
                    <td style="padding: 6px; text-align: center; width: 25%;">-&infin;</td>
                    <td style="padding: 6px; text-align: center; width: 20%; font-weight: bold;">-1</td>
                    <td style="padding: 6px; text-align: center; width: 20%; font-weight: bold;">2</td>
                    <td style="padding: 6px; text-align: center; width: 25%;">+&infin;</td>
                  </tr>
                  <tr>
                    <td style="border-right: 1px solid #000; padding: 6px; font-weight: bold; text-align: center;">f'(x)</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: red;">-</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold;">0</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: blue;">+</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold;">0</td>
                    <td style="padding: 6px; text-align: center; font-weight: bold; color: red;">-</td>
                  </tr>
                </table>
            """.trimIndent()

            part == 3 && questionId == 4 -> """
                <div style="font-family: monospace; white-space: pre; border: 1px solid #ccc; padding: 10px; background: #fafafa; margin: 15px 0;">
                Sơ đồ hình hộp chữ nhật ngoại tiếp bởi mặt cầu:
                - Kích thước hộp: 1 x 2 x 3
                - Bán kính mặt cầu R = d / 2
                - Đường chéo d = &radic;(1&sup2; + 2&sup2; + 3&sup2;) = &radic;14
                </div>
            """.trimIndent()

            else -> ""
        }
    }

    // =========================================================================
    // 1. GENERATE WORD COMPATIBLE HTML (.doc)
    // =========================================================================
    fun generateWordHtml(
        session: ExamSession,
        includeAnswers: Boolean,
        department: String = "SỞ GIÁO DỤC VÀ ĐÀO TẠO",
        school: String = "TRƯỜNG THPT CHUYÊN TOÁN MIXER",
        examTitle: String = "KỲ THI THỬ TỐT NGHIỆP THPT QUỐC GIA",
        subject: String = "Môn thi: TOÁN HỌC (Lớp 12)",
        year: String = "NĂM HỌC 2026 - 2027",
        timeLimit: String = "90",
        instructions: String = "Thời gian làm bài: 90 phút (Không kể thời gian phát đề)"
    ): String {
        val sb = StringBuilder()
        sb.append("\uFEFF") // Write UTF-8 BOM so MS Word imports symbols perfectly
        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>${session.examCode}</title>
                <style>
                    body {
                        font-family: "Times New Roman", Times, serif;
                        line-height: 1.5;
                        margin: 40px;
                        font-size: 12pt;
                        color: #000000;
                    }
                    .header-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 30px;
                    }
                    .header-cell {
                        vertical-align: top;
                        width: 50%;
                    }
                    .title {
                        text-align: center;
                        font-size: 16pt;
                        font-weight: bold;
                        margin-top: 10px;
                        margin-bottom: 5px;
                        text-transform: uppercase;
                    }
                    .subtitle {
                        text-align: center;
                        font-size: 12pt;
                        font-style: italic;
                        margin-bottom: 25px;
                    }
                    .student-info {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 25px;
                    }
                    .student-info td {
                        padding: 6px 0;
                        font-size: 12pt;
                    }
                    .part-title {
                        font-size: 13pt;
                        font-weight: bold;
                        margin-top: 25px;
                        margin-bottom: 15px;
                        text-transform: uppercase;
                        border-bottom: 1px solid #000;
                        padding-bottom: 3px;
                    }
                    .question {
                        margin-top: 15px;
                        margin-bottom: 12px;
                        text-align: justify;
                    }
                    .question-num {
                        font-weight: bold;
                    }
                    .options-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-left: 15px;
                        margin-bottom: 15px;
                    }
                    .options-table td {
                        width: 25%;
                        padding: 4px 5px;
                        vertical-align: top;
                    }
                    .sub-question {
                        margin-left: 25px;
                        margin-bottom: 8px;
                    }
                    .sub-letter {
                        font-weight: bold;
                    }
                    .short-answer-line {
                        margin-top: 8px;
                        margin-bottom: 12px;
                        font-style: italic;
                    }
                    .solution-box {
                        background-color: #f6f8fa;
                        border-left: 4px solid #1a73e8;
                        padding: 10px 15px;
                        margin: 10px 0 20px 20px;
                        font-size: 11pt;
                    }
                    .correct-ans {
                        font-weight: bold;
                        color: #137333;
                    }
                    .footer {
                        text-align: center;
                        font-size: 10pt;
                        margin-top: 40px;
                        border-top: 1px dashed #ccc;
                        padding-top: 10px;
                        color: #666;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        // 1.1 Header Info Table
        sb.append("""
            <table class="header-table">
                <tr>
                    <td class="header-cell" style="text-align: center;">
                        <strong>$department</strong><br>
                        <strong>$school</strong>
                    </td>
                    <td class="header-cell" style="text-align: center;">
                        <strong>$examTitle</strong><br>
                        <strong>$year</strong><br>
                        <span style="font-size: 11pt;">$subject</span>
                    </td>
                </tr>
            </table>
        """.trimIndent())

        // Title
        sb.append("<div class=\"title\">ĐỀ THI TRẮC NGHIỆM KHÁCH QUAN</div>")
        sb.append("<div class=\"subtitle\">$instructions</div>")

        // Student line
        val displayStudentName = session.studentName.ifBlank { "........................................................................" }
        val displayStudentClass = session.studentClass.ifBlank { "........................" }
        sb.append("""
            <table class="student-info">
                <tr>
                    <td style="width: 60%;">Họ và tên thí sinh: <strong>$displayStudentName</strong></td>
                    <td style="width: 40%;">Lớp: <strong>$displayStudentClass</strong></td>
                </tr>
                <tr>
                    <td colspan="2">Số báo danh / Mã đề: <strong>${session.examCode}</strong></td>
                </tr>
            </table>
            <hr style="border: 0; border-top: 2px double #000; margin-bottom: 25px;">
        """.trimIndent())

        // ==========================================
        // PHẦN I
        // ==========================================
        if (session.part1Questions.isNotEmpty()) {
            sb.append("<div class=\"part-title\">PHẦN I. Câu trắc nghiệm nhiều phương án lựa chọn (Chọn một phương án đúng)</div>")
            sb.append("<p style=\"font-style: italic; margin-bottom: 15px;\">Thí sinh trả lời từ câu 1 đến câu ${session.part1Questions.size}. Mỗi câu hỏi chỉ chọn một phương án.</p>")
            
            session.part1Questions.forEachIndexed { index, q ->
                sb.append("<div class=\"question\">")
                sb.append("<span class=\"question-num\">Câu ${index + 1}.</span> ${formatMathForHtml(q.question.content)}")
                sb.append("</div>")

                // Embed illustration table/graph
                val illustrationHtml = getHtmlIllustration(1, q.question.id)
                if (illustrationHtml.isNotEmpty()) {
                    sb.append(illustrationHtml)
                }

                // Options (4 columns standard table layout)
                sb.append("<table class=\"options-table\"><tr>")
                q.options.forEachIndexed { optIdx, optText ->
                    val prefix = when (optIdx) {
                        0 -> "A. "
                        1 -> "B. "
                        2 -> "C. "
                        3 -> "D. "
                        else -> ""
                    }
                    sb.append("<td><strong>$prefix</strong>${formatMathForHtml(optText)}</td>")
                }
                sb.append("</tr></table>")

                // Detailed solution if requested
                if (includeAnswers) {
                    val correctChar = when (q.correctIndex) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        3 -> "D"
                        else -> "?"
                    }
                    sb.append("<div class=\"solution-box\">")
                    sb.append("<span class=\"correct-ans\">Đáp án đúng: $correctChar</span><br>")
                    sb.append("<strong>Hướng dẫn giải chi tiết:</strong><br>")
                    sb.append(formatMathForHtml(q.question.explanation).replace("\n", "<br>"))
                    sb.append("</div>")
                }
            }
        }

        // ==========================================
        // PHẦN II
        // ==========================================
        if (session.part2Questions.isNotEmpty()) {
            sb.append("<div class=\"part-title\">PHẦN II. Câu trắc nghiệm đúng sai (Mỗi câu hỏi chọn Đúng hoặc Sai cho mỗi ý a, b, c, d)</div>")
            sb.append("<p style=\"font-style: italic; margin-bottom: 15px;\">Thí sinh trả lời từ câu 1 đến câu ${session.part2Questions.size}. Trong mỗi ý a), b), c), d) ở mỗi câu, thí sinh chọn Đúng hoặc Sai.</p>")

            session.part2Questions.forEachIndexed { index, q ->
                sb.append("<div class=\"question\">")
                sb.append("<span class=\"question-num\">Câu ${index + 1}.</span> ${formatMathForHtml(q.content)}")
                sb.append("</div>")

                // Embed illustration table/graph
                val illustrationHtml = getHtmlIllustration(2, q.id)
                if (illustrationHtml.isNotEmpty()) {
                    sb.append(illustrationHtml)
                }

                // Subquestions list
                q.subQuestions.forEachIndexed { subIdx, sub ->
                    val letter = when (subIdx) {
                        0 -> "a"
                        1 -> "b"
                        2 -> "c"
                        3 -> "d"
                        else -> "•"
                    }
                    sb.append("<div class=\"sub-question\">")
                    sb.append("<span class=\"sub-letter\">$letter)</span> ${formatMathForHtml(sub.statement)} ")
                    if (!includeAnswers) {
                        sb.append("<span style=\"font-size: 10pt; color: #555;\">&nbsp;&nbsp;[ &nbsp; ] Đúng &nbsp;&nbsp; [ &nbsp; ] Sai</span>")
                    }
                    sb.append("</div>")
                }

                // Solution details
                if (includeAnswers) {
                    sb.append("<div class=\"solution-box\">")
                    sb.append("<strong>Đáp án đúng:</strong><br>")
                    q.subQuestions.forEachIndexed { subIdx, sub ->
                        val letter = when (subIdx) {
                            0 -> "a"
                            1 -> "b"
                            2 -> "c"
                            3 -> "d"
                            else -> "•"
                        }
                        val status = if (sub.isTrue) "ĐÚNG" else "SAI"
                        sb.append("- Ý $letter): <strong style=\"color:#137333;\">$status</strong><br>")
                    }
                    sb.append("<br><strong>Hướng dẫn giải chi tiết:</strong><br>")
                    sb.append(formatMathForHtml(q.explanation).replace("\n", "<br>"))
                    sb.append("</div>")
                }
            }
        }

        // ==========================================
        // PHẦN III
        // ==========================================
        if (session.part3Questions.isNotEmpty()) {
            sb.append("<div class=\"part-title\">PHẦN III. Câu trắc nghiệm trả lời ngắn (Điền đáp số đúng)</div>")
            sb.append("<p style=\"font-style: italic; margin-bottom: 15px;\">Thí sinh trả lời từ câu 1 đến câu ${session.part3Questions.size}. Điền kết quả vào ô trống tương ứng.</p>")

            session.part3Questions.forEachIndexed { index, q ->
                sb.append("<div class=\"question\">")
                sb.append("<span class=\"question-num\">Câu ${index + 1}.</span> ${formatMathForHtml(q.content)}")
                sb.append("</div>")

                // Embed illustration table/graph
                val illustrationHtml = getHtmlIllustration(3, q.id)
                if (illustrationHtml.isNotEmpty()) {
                    sb.append(illustrationHtml)
                }

                if (!includeAnswers) {
                    sb.append("<div class=\"short-answer-line\">Đáp số: ............................................................</div>")
                }

                if (includeAnswers) {
                    sb.append("<div class=\"solution-box\">")
                    sb.append("<span class=\"correct-ans\">Đáp số đúng: ${formatMathForHtml(q.correctAnswer.toString())}</span><br>")
                    sb.append("<strong>Hướng dẫn giải chi tiết:</strong><br>")
                    sb.append(formatMathForHtml(q.explanation).replace("\n", "<br>"))
                    sb.append("</div>")
                }
            }
        }

        // End signature
        sb.append("""
            <br><br>
            <div style="text-align: center; font-weight: bold; font-style: italic; font-size: 13pt;">
                --- HẾT ---
            </div>
            <div class="subtitle" style="margin-top: 5px;">Thí sinh không được sử dụng tài liệu. Giám thị không giải thích gì thêm.</div>
            
            <div class="footer">
                Được biên soạn tự động và phân phối bởi AI Studio MathMixer • Thiết kế bởi Trịnh Như Quỳnh
            </div>
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }

    // =========================================================================
    // PDF MATH SANITIZER AND ILLUSTRATION HELPERS
    // =========================================================================
    fun sanitizeMathForPdf(text: String): String {
        return text
            .replace("n\u20D7", "vectơ n")
            .replace("u\u20D7", "vectơ u")
            .replace("v\u20D7", "vectơ v")
            .replace("z\u0304", "z̄")
            .replace("\u207F", "^n")
            .replace("\u2070", "^0")
            .replace("\u2071", "^i")
            .replace("\u2074", "^4")
            .replace("\u2075", "^5")
            .replace("\u2076", "^6")
            .replace("\u2077", "^7")
            .replace("\u2078", "^8")
            .replace("\u2079", "^9")
            .replace("\u2080", "_0")
            .replace("\u2081", "_1")
            .replace("\u2082", "_2")
            .replace("\u2083", "_3")
            .replace("\u2084", "_4")
            .replace("\u2085", "_5")
            .replace("\u2086", "_6")
            .replace("\u2087", "_7")
            .replace("\u2088", "_8")
            .replace("\u2089", "_9")
            .replace("\u222B", "∫")
            .replace("\u21D4", " <=> ")
            .replace("\u21D2", " => ")
            .replace("\u2192", " -> ")
            .replace("\u221A", "√")
            .replace("\u03C0", "π")
    }

    fun getIllustrationTextForPdf(questionId: Int, part: Int): List<String>? {
        return when {
            part == 1 && questionId == 1 -> listOf(
                "    [BẢNG BIẾN THIÊN MINH HỌA]:",
                "    x   | -∞         0         2         +∞",
                "    y'  |      +     0    -    0    +",
                "    y   | -∞  ↗   2 (CĐ)  ↘  -2 (CT)  ↗  +∞"
            )
            part == 1 && questionId == 2 -> listOf(
                "    [ĐỒ THỊ MINH HỌA]:",
                "    Hàm số: y = (2x - 1) / (x - 1)",
                "    - Tiệm cận đứng (TCĐ): x = 1",
                "    - Tiệm cận ngang (TCN): y = 2"
            )
            part == 2 && questionId == 1 -> listOf(
                "    [BẢNG BIẾN THIÊN MINH HỌA]:",
                "    x   | -∞         1         +∞",
                "    y'  |      -    ||    -",
                "    y   |  1  ↘  -∞ || +∞  ↘  1"
            )
            part == 2 && questionId == 3 -> listOf(
                "    [BẢNG BIẾN THIÊN MINH HỌA]:",
                "    x   | -∞         -1         0         1         +∞",
                "    y'  |      -     0    +    0    -    0    +",
                "    y   | +∞  ↘   2 (CT)  ↗  3 (CĐ)  ↘  2 (CT)  ↗  +∞"
            )
            part == 2 && questionId == 5 -> listOf(
                "    [SƠ ĐỒ HÌNH KHÔNG GIAN]:",
                "    Hình hộp chữ nhật ABCD.A'B'C'D' có:",
                "    - AB = 1 (chiều rộng)",
                "    - AD = 2 (chiều dài)",
                "    - AA' = 3 (chiều cao)"
            )
            part == 2 && questionId == 6 -> listOf(
                "    [BẢNG XÉT DẤU ĐẠO HÀM]:",
                "    x     | -∞         -1         2         +∞",
                "    f'(x) |      -      0    +    0    -"
            )
            part == 3 && questionId == 4 -> listOf(
                "    [SƠ ĐỒ KHÔNG GIAN MINH HỌA]:",
                "    Mặt cầu ngoại tiếp hình hộp chữ nhật ABCD.A'B'C'D' có:",
                "    - Bán kính R = d / 2 (với d là đường chéo hình hộp)",
                "    - d = √(AB² + AD² + AA'²)"
            )
            else -> null
        }
    }

    // =========================================================================
    // 2. GENERATE NATIVE ANDROID PDF (.pdf)
    // =========================================================================
    fun exportToPdf(
        context: Context,
        session: ExamSession,
        includeAnswers: Boolean,
        outputStream: OutputStream,
        department: String = "SỞ GIÁO DỤC VÀ ĐÀO TẠO",
        school: String = "TRƯỜNG THPT CHUYÊN TOÁN MIXER",
        examTitle: String = "KỲ THI THỬ TỐT NGHIỆP THPT QUỐC GIA",
        subject: String = "Môn thi: TOÁN HỌC (Lớp 12)",
        year: String = "NĂM HỌC 2026 - 2027",
        timeLimit: String = "90",
        instructions: String = "Thời gian làm bài: 90 phút (Không kể thời gian phát đề)"
    ) {
        val pdfDocument = PdfDocument()

        // Page setup dimensions for A4 (595 x 842 points)
        val pageWidth = 595
        val pageHeight = 842
        val margin = 50
        val contentWidth = pageWidth - (margin * 2)

        // Setup Paints with robust Sans-Serif system font for full Vietnamese + math Unicode support
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 11.5f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }

        val sectionTitlePaint = TextPaint().apply {
            color = Color.DKGRAY
            textSize = 12f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            isAntiAlias = true
        }

        val solutionPaint = TextPaint().apply {
            color = Color.rgb(26, 115, 232) // blue shade
            textSize = 9.5f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            isAntiAlias = true
        }

        val footerPaint = TextPaint().apply {
            color = Color.GRAY
            textSize = 8f
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
            isAntiAlias = true
        }

        var pageCount = 1
        var currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCount).create())
        var canvas = currentPage.canvas
        var currentY = margin

        // Helper to handle page breaks
        fun checkAndCreateNewPage(heightNeeded: Int) {
            if (currentY + heightNeeded > pageHeight - margin - 30) {
                // Draw footer before finishing page
                canvas.drawText("Trang $pageCount", (pageWidth - margin - 50).toFloat(), (pageHeight - margin + 15).toFloat(), footerPaint)
                canvas.drawText("Mã đề thi: ${session.examCode}", margin.toFloat(), (pageHeight - margin + 15).toFloat(), footerPaint)
                canvas.drawLine(margin.toFloat(), (pageHeight - margin + 5).toFloat(), (pageWidth - margin).toFloat(), (pageHeight - margin + 5).toFloat(), Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })

                pdfDocument.finishPage(currentPage)
                pageCount++
                currentPage = pdfDocument.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageCount).create())
                canvas = currentPage.canvas
                currentY = margin
            }
        }

        // Helper to draw layout text with automatic mathematical character sanitization
        fun drawTextLayout(text: String, paint: TextPaint, spacing: Int = 12, isCentered: Boolean = false) {
            val sanitized = sanitizeMathForPdf(text)
            val alignment = if (isCentered) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL
            val staticLayout = StaticLayout.Builder.obtain(sanitized, 0, sanitized.length, paint, contentWidth)
                .setAlignment(alignment)
                .setLineSpacing(0f, 1.15f)
                .setIncludePad(true)
                .build()

            checkAndCreateNewPage(staticLayout.height)
            canvas.save()
            canvas.translate(margin.toFloat(), currentY.toFloat())
            staticLayout.draw(canvas)
            canvas.restore()

            currentY += staticLayout.height + spacing
        }

        // DRAW FIRST PAGE HEADER
        drawTextLayout(department, headerPaint, 4, true)
        drawTextLayout("$school - $year", headerPaint, 15, true)

        drawTextLayout("$examTitle - $subject", sectionTitlePaint, 6, true)
        drawTextLayout("$instructions | Số báo danh / Mã đề: ${session.examCode}", textPaint, 15, true)

        val sName = if (session.studentName.isBlank()) "..................................................." else session.studentName
        val sClass = if (session.studentClass.isBlank()) ".................." else session.studentClass
        drawTextLayout("Họ và tên thí sinh: $sName  |  Lớp: $sClass", boldTextPaint, 20, false)

        canvas.drawLine(margin.toFloat(), (currentY - 10).toFloat(), (pageWidth - margin).toFloat(), (currentY - 10).toFloat(), Paint().apply { color = Color.BLACK; strokeWidth = 1.5f })

        // ==========================================
        // 2.1 PART 1 DRAWING
        // ==========================================
        if (session.part1Questions.isNotEmpty()) {
            drawTextLayout("PHẦN I. Câu trắc nghiệm nhiều phương án lựa chọn (Chọn một phương án đúng)", sectionTitlePaint, 8)
            drawTextLayout("Thí sinh trả lời từ câu 1 đến câu ${session.part1Questions.size}. Mỗi câu hỏi chỉ chọn một phương án.", textPaint, 15)

            session.part1Questions.forEachIndexed { idx, q ->
                val fullQText = "Câu ${idx + 1}. ${q.question.content}"
                drawTextLayout(fullQText, textPaint, 6)

                // Render matching math diagram/table in PDF if applicable
                val illus = getIllustrationTextForPdf(q.question.id, 1)
                if (illus != null) {
                    illus.forEach { line ->
                        drawTextLayout(line, boldTextPaint, 4)
                    }
                    currentY += 6
                }

                // Layout options: A, B, C, D
                q.options.forEachIndexed { optIdx, option ->
                    val letter = when (optIdx) {
                        0 -> "A. "
                        1 -> "B. "
                        2 -> "C. "
                        3 -> "D. "
                        else -> "• "
                    }
                    drawTextLayout("    $letter$option", textPaint, 4)
                }
                currentY += 4 // extra gap after options

                if (includeAnswers) {
                    val correctLetter = when (q.correctIndex) {
                        0 -> "A"
                        1 -> "B"
                        2 -> "C"
                        3 -> "D"
                        else -> "Chưa rõ"
                    }
                    drawTextLayout("    -> Đáp án đúng: $correctLetter", boldTextPaint, 4)
                    drawTextLayout("    [Lời giải chi tiết]: ${q.question.explanation}", solutionPaint, 10)
                }
            }
        }

        // ==========================================
        // 2.2 PART 2 DRAWING
        // ==========================================
        if (session.part2Questions.isNotEmpty()) {
            drawTextLayout("PHẦN II. Câu trắc nghiệm đúng sai (Chọn Đúng hoặc Sai cho mỗi ý)", sectionTitlePaint, 8)
            drawTextLayout("Trong mỗi ý a), b), c), d) ở mỗi câu, thí sinh chọn Đúng hoặc Sai.", textPaint, 15)

            session.part2Questions.forEachIndexed { idx, q ->
                drawTextLayout("Câu ${idx + 1}. ${q.content}", textPaint, 6)

                // Render matching math diagram/table in PDF if applicable
                val illus = getIllustrationTextForPdf(q.id, 2)
                if (illus != null) {
                    illus.forEach { line ->
                        drawTextLayout(line, boldTextPaint, 4)
                    }
                    currentY += 6
                }

                q.subQuestions.forEachIndexed { subIdx, sub ->
                    val subLetter = when (subIdx) {
                        0 -> "a) "
                        1 -> "b) "
                        2 -> "c) "
                        3 -> "d) "
                        else -> "• "
                    }
                    val suffix = if (includeAnswers) {
                        " -> [${if (sub.isTrue) "ĐÚNG" else "SAI"}]"
                    } else {
                        "  [  ] Đúng   [  ] Sai"
                    }
                    drawTextLayout("    $subLetter${sub.statement}   $suffix", textPaint, 4)
                }
                currentY += 4

                if (includeAnswers) {
                    drawTextLayout("    [Lời giải chi tiết]: ${q.explanation}", solutionPaint, 10)
                }
            }
        }

        // ==========================================
        // 2.3 PART 3 DRAWING
        // ==========================================
        if (session.part3Questions.isNotEmpty()) {
            drawTextLayout("PHẦN III. Câu trắc nghiệm trả lời ngắn (Điền đáp số đúng)", sectionTitlePaint, 8)
            drawTextLayout("Thí sinh trả lời từ câu 1 đến câu ${session.part3Questions.size}.", textPaint, 15)

            session.part3Questions.forEachIndexed { idx, q ->
                drawTextLayout("Câu ${idx + 1}. ${q.content}", textPaint, 6)

                // Render matching math diagram/table in PDF if applicable
                val illus = getIllustrationTextForPdf(q.id, 3)
                if (illus != null) {
                    illus.forEach { line ->
                        drawTextLayout(line, boldTextPaint, 4)
                    }
                    currentY += 6
                }

                if (includeAnswers) {
                    drawTextLayout("    -> Đáp số đúng: ${q.correctAnswer}", boldTextPaint, 4)
                    drawTextLayout("    [Lời giải chi tiết]: ${q.explanation}", solutionPaint, 10)
                } else {
                    drawTextLayout("    Đáp số: ............................................................", textPaint, 10)
                }
            }
        }

        // DRAW HẾT
        drawTextLayout("--- HẾT ---", boldTextPaint, 10, true)
        drawTextLayout("Giám thị không giải thích gì thêm.", textPaint, 10, true)

        // Draw last page footer
        canvas.drawText("Trang $pageCount", (pageWidth - margin - 50).toFloat(), (pageHeight - margin + 15).toFloat(), footerPaint)
        canvas.drawText("Mã đề thi: ${session.examCode}", margin.toFloat(), (pageHeight - margin + 15).toFloat(), footerPaint)
        canvas.drawLine(margin.toFloat(), (pageHeight - margin + 5).toFloat(), (pageWidth - margin).toFloat(), (pageHeight - margin + 5).toFloat(), Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f })

        pdfDocument.finishPage(currentPage)
        pdfDocument.writeTo(outputStream)
        pdfDocument.close()
    }
}
