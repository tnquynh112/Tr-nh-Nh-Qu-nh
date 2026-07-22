package com.example.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ExamViewModel
import com.example.Part1Question
import com.example.Part2Question
import com.example.Part3Question
import com.example.Part2SubQuestion
import com.example.db.ExamParser
import com.example.db.ParseResult
import com.example.db.MathQuestionEntity
import android.provider.OpenableColumns
import kotlinx.coroutines.launch

data class BulkParseResult(
    val part1: List<Part1Question>,
    val part2: List<Part2Question>,
    val part3: List<Part3Question>,
    val errors: List<String>
)

fun determineTopicLocal(content: String): String {
    val lower = content.lowercase()
    return when {
        lower.contains("đạo hàm") || lower.contains("cực trị") || lower.contains("tiệm cận") || lower.contains("biến thiên") || lower.contains("hàm số") -> "Giải tích"
        lower.contains("tích phân") || lower.contains("nguyên hàm") -> "Giải tích"
        lower.contains("mũ") || lower.contains("logarit") || lower.contains("lũy thừa") -> "Đại số"
        lower.contains("tọa độ") || lower.contains("mặt cầu") || lower.contains("mặt phẳng") || lower.contains("đường thẳng") -> "Hình học không gian"
        lower.contains("thể tích") || lower.contains("khối đa diện") || lower.contains("khối lăng trụ") || lower.contains("khối chóp") -> "Hình học không gian"
        lower.contains("xác suất") || lower.contains("tổ hợp") || lower.contains("chỉnh hợp") -> "Tổ hợp & Xác suất"
        else -> "Giải tích"
    }
}

fun parseBulkText(text: String): BulkParseResult {
    val p1List = mutableListOf<Part1Question>()
    val p2List = mutableListOf<Part2Question>()
    val p3List = mutableListOf<Part3Question>()
    val errors = mutableListOf<String>()
    var baseId = (System.currentTimeMillis().toInt() / 1000) + 2000

    if (text.isBlank()) return BulkParseResult(p1List, p2List, p3List, errors)

    // First try splitting by blocks [MCQ], [TF], [SA]
    val blocks = text.split(Regex("(?=\\[(MCQ|TF|SA)\\])"))
    var processedBlocksCount = 0
    
    for (block in blocks) {
        val trimmedBlock = block.trim()
        if (trimmedBlock.isBlank()) continue
        
        if (trimmedBlock.startsWith("[MCQ]") || trimmedBlock.startsWith("[TF]") || trimmedBlock.startsWith("[SA]")) {
            processedBlocksCount++
            val lines = trimmedBlock.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val type = lines.firstOrNull() ?: ""
            
            if (type == "[MCQ]") {
                var content = ""
                val options = mutableListOf<String>()
                var correctIdx = 0
                var explanation = ""
                var topic = ""
                var difficulty = "Dễ"
                
                var contentPhase = true
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    val isOption = line.startsWith("A.", ignoreCase = true) || line.startsWith("B.", ignoreCase = true) || line.startsWith("C.", ignoreCase = true) || line.startsWith("D.", ignoreCase = true) ||
                                   line.startsWith("A)", ignoreCase = true) || line.startsWith("B)", ignoreCase = true) || line.startsWith("C)", ignoreCase = true) || line.startsWith("D)", ignoreCase = true)
                    
                    val isAnswer = line.startsWith("Đáp án:", ignoreCase = true) || line.startsWith("Đáp án đúng:", ignoreCase = true) || line.startsWith("Correct:", ignoreCase = true)
                    val isExplanation = line.startsWith("Giải thích:", ignoreCase = true) || line.startsWith("Lời giải:", ignoreCase = true) || line.startsWith("Explanation:", ignoreCase = true)
                    val isTopic = line.startsWith("Chủ đề:", ignoreCase = true) || line.startsWith("Topic:", ignoreCase = true)
                    val isDifficulty = line.startsWith("Độ khó:", ignoreCase = true) || line.startsWith("Difficulty:", ignoreCase = true)

                    if (isOption) {
                        contentPhase = false
                        val optText = line.substring(2).trim().removePrefix(".").removePrefix(")").trim()
                        options.add(optText)
                    } else if (isAnswer) {
                        contentPhase = false
                        val ansChar = line.substringAfter(":").trim().uppercase()
                        correctIdx = when {
                            ansChar.startsWith("A") -> 0
                            ansChar.startsWith("B") -> 1
                            ansChar.startsWith("C") -> 2
                            ansChar.startsWith("D") -> 3
                            else -> 0
                        }
                    } else if (isExplanation) {
                        contentPhase = false
                        explanation = line.substringAfter(":").trim()
                    } else if (isTopic) {
                        contentPhase = false
                        topic = line.substringAfter(":").trim()
                    } else if (isDifficulty) {
                        contentPhase = false
                        difficulty = line.substringAfter(":").trim()
                    } else if (contentPhase) {
                        if (content.isEmpty()) content = line else content += "\n" + line
                    } else {
                        if (explanation.isNotEmpty()) {
                            explanation += "\n" + line
                        }
                    }
                }
                
                if (content.isBlank()) {
                    errors.add("Khối MCQ thứ ${processedBlocksCount}: Thiếu nội dung câu hỏi.")
                    continue
                }
                if (options.size < 4) {
                    while (options.size < 4) {
                        options.add("Lựa chọn ${'A' + options.size}")
                    }
                }
                if (topic.isBlank()) {
                    topic = determineTopicLocal(content)
                }
                
                p1List.add(
                    Part1Question(
                        id = baseId++,
                        content = content,
                        originalOptions = options.take(4),
                        correctIndex = correctIdx,
                        explanation = explanation.ifBlank { "Chưa có lời giải chi tiết." },
                        topic = topic,
                        difficulty = difficulty
                    )
                )
            } else if (type == "[TF]") {
                var content = ""
                val subQuestions = mutableListOf<Part2SubQuestion>()
                var explanation = ""
                var topic = ""
                var difficulty = "Trung bình"
                
                var contentPhase = true
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    val isSub = line.startsWith("-") || line.startsWith("*") || line.startsWith("+") ||
                                line.startsWith("a)", ignoreCase = true) || line.startsWith("b)", ignoreCase = true) || line.startsWith("c)", ignoreCase = true) || line.startsWith("d)", ignoreCase = true) ||
                                line.startsWith("a.", ignoreCase = true) || line.startsWith("b.", ignoreCase = true) || line.startsWith("c.", ignoreCase = true) || line.startsWith("d.", ignoreCase = true)
                    
                    val isExplanation = line.startsWith("Giải thích:", ignoreCase = true) || line.startsWith("Lời giải:", ignoreCase = true) || line.startsWith("Explanation:", ignoreCase = true)
                    val isTopic = line.startsWith("Chủ đề:", ignoreCase = true) || line.startsWith("Topic:", ignoreCase = true)
                    val isDifficulty = line.startsWith("Độ khó:", ignoreCase = true) || line.startsWith("Difficulty:", ignoreCase = true)

                    if (isSub) {
                        contentPhase = false
                        val cleanLine = line.trim().removePrefix("-").removePrefix("*").removePrefix("+").trim()
                        val parts = cleanLine.split("|")
                        val statement = parts[0].trim().removePrefix("a)").removePrefix("b)").removePrefix("c)").removePrefix("d)").removePrefix("a.").removePrefix("b.").removePrefix("c.").removePrefix("d.").trim()
                        val isTrueText = parts.getOrNull(1)?.trim()?.lowercase() ?: "đúng"
                        val isTrue = isTrueText == "đúng" || isTrueText == "true" || isTrueText == "t" || isTrueText == "đ" || isTrueText == "d"
                        subQuestions.add(
                            Part2SubQuestion(
                                statement = "Ý ${'a' + subQuestions.size}) $statement",
                                isTrue = isTrue
                            )
                        )
                    } else if (isExplanation) {
                        contentPhase = false
                        explanation = line.substringAfter(":").trim()
                    } else if (isTopic) {
                        contentPhase = false
                        topic = line.substringAfter(":").trim()
                    } else if (isDifficulty) {
                        contentPhase = false
                        difficulty = line.substringAfter(":").trim()
                    } else if (contentPhase) {
                        if (content.isEmpty()) content = line else content += "\n" + line
                    } else {
                        if (explanation.isNotEmpty()) {
                            explanation += "\n" + line
                        }
                    }
                }
                
                if (content.isBlank()) {
                    errors.add("Khối Đúng/Sai thứ ${processedBlocksCount}: Thiếu nội dung câu hỏi chính.")
                    continue
                }
                if (subQuestions.size < 4) {
                    while (subQuestions.size < 4) {
                        subQuestions.add(Part2SubQuestion("Ý phụ ${'a' + subQuestions.size}...", true))
                    }
                }
                if (topic.isBlank()) {
                    topic = determineTopicLocal(content)
                }
                
                p2List.add(
                    Part2Question(
                        id = baseId++,
                        content = content,
                        subQuestions = subQuestions.take(4),
                        explanation = explanation.ifBlank { "Chưa có lời giải chi tiết." },
                        topic = topic,
                        difficulty = difficulty
                    )
                )
            } else if (type == "[SA]") {
                var content = ""
                var correctAnswer = 0.0
                var explanation = ""
                var topic = ""
                var difficulty = "Khó"
                
                var contentPhase = true
                for (i in 1 until lines.size) {
                    val line = lines[i]
                    val isAnswer = line.startsWith("Đáp án:", ignoreCase = true) || line.startsWith("Correct:", ignoreCase = true)
                    val isExplanation = line.startsWith("Giải thích:", ignoreCase = true) || line.startsWith("Lời giải:", ignoreCase = true) || line.startsWith("Explanation:", ignoreCase = true)
                    val isTopic = line.startsWith("Chủ đề:", ignoreCase = true) || line.startsWith("Topic:", ignoreCase = true)
                    val isDifficulty = line.startsWith("Độ khó:", ignoreCase = true) || line.startsWith("Difficulty:", ignoreCase = true)

                    if (isAnswer) {
                        contentPhase = false
                        val ansStr = line.substringAfter(":").trim()
                        correctAnswer = ansStr.toDoubleOrNull() ?: 0.0
                    } else if (isExplanation) {
                        contentPhase = false
                        explanation = line.substringAfter(":").trim()
                    } else if (isTopic) {
                        contentPhase = false
                        topic = line.substringAfter(":").trim()
                    } else if (isDifficulty) {
                        contentPhase = false
                        difficulty = line.substringAfter(":").trim()
                    } else if (contentPhase) {
                        if (content.isEmpty()) content = line else content += "\n" + line
                    } else {
                        if (explanation.isNotEmpty()) {
                            explanation += "\n" + line
                        }
                    }
                }
                
                if (content.isBlank()) {
                    errors.add("Khối Trả lời ngắn thứ ${processedBlocksCount}: Thiếu nội dung câu hỏi.")
                    continue
                }
                if (topic.isBlank()) {
                    topic = determineTopicLocal(content)
                }
                
                p3List.add(
                    Part3Question(
                        id = baseId++,
                        content = content,
                        correctAnswer = correctAnswer,
                        explanation = explanation.ifBlank { "Chưa có lời giải chi tiết." },
                        topic = topic,
                        difficulty = difficulty
                    )
                )
            }
        }
    }
    
    // Fallback line-by-line format
    if (processedBlocksCount == 0) {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        lines.forEachIndexed { lineIdx, line ->
            if (line.startsWith("#") || line.startsWith("//")) return@forEachIndexed
            
            val parts = line.split("|").map { it.trim() }
            if (parts.size < 3) {
                errors.add("Dòng ${lineIdx + 1}: Thiếu thông tin phân tách bởi '|' (Cần ít nhất Loại | Câu hỏi | Đáp án).")
                return@forEachIndexed
            }
            
            val type = parts[0].uppercase()
            when (type) {
                "P1", "PART1", "MCQ" -> {
                    if (parts.size < 7) {
                        errors.add("Dòng ${lineIdx + 1}: Trắc nghiệm MCQ cần ít nhất 7 trường (Loại | Câu hỏi | A | B | C | D | Đáp án).")
                        return@forEachIndexed
                    }
                    val content = parts[1]
                    val options = listOf(parts[2], parts[3], parts[4], parts[5])
                    val ansChar = parts[6].uppercase()
                    val correctIdx = when {
                        ansChar.startsWith("A") -> 0
                        ansChar.startsWith("B") -> 1
                        ansChar.startsWith("C") -> 2
                        ansChar.startsWith("D") -> 3
                        else -> 0
                    }
                    val explanation = parts.getOrNull(7) ?: "Chưa có lời giải chi tiết."
                    val topic = parts.getOrNull(8)?.ifBlank { determineTopicLocal(content) } ?: determineTopicLocal(content)
                    val difficulty = parts.getOrNull(9)?.ifBlank { "Dễ" } ?: "Dễ"
                    
                    p1List.add(
                        Part1Question(
                            id = baseId++,
                            content = content,
                            originalOptions = options,
                            correctIndex = correctIdx,
                            explanation = explanation,
                            topic = topic,
                            difficulty = difficulty
                        )
                    )
                }
                "P2", "PART2", "TF" -> {
                    if (parts.size < 10) {
                        errors.add("Dòng ${lineIdx + 1}: Đúng/Sai cần ít nhất 10 trường (Loại | Câu hỏi | 4 cặp mệnh đề và kết quả Đ/S).")
                        return@forEachIndexed
                    }
                    val content = parts[1]
                    val subQuestions = mutableListOf<Part2SubQuestion>()
                    
                    for (pairIdx in 0..3) {
                        val subText = parts.getOrNull(2 + pairIdx * 2) ?: "Mệnh đề phụ"
                        val isTrueVal = parts.getOrNull(3 + pairIdx * 2)?.lowercase() ?: "đúng"
                        val isTrue = isTrueVal == "đúng" || isTrueVal == "true" || isTrueVal == "t" || isTrueVal == "đ" || isTrueVal == "d"
                        subQuestions.add(
                            Part2SubQuestion(
                                statement = "Ý ${'a' + pairIdx}) $subText",
                                isTrue = isTrue
                            )
                        )
                    }
                    
                    val explanation = parts.getOrNull(10) ?: "Chưa có lời giải chi tiết."
                    val topic = parts.getOrNull(11)?.ifBlank { determineTopicLocal(content) } ?: determineTopicLocal(content)
                    val difficulty = parts.getOrNull(12)?.ifBlank { "Trung bình" } ?: "Trung bình"
                    
                    p2List.add(
                        Part2Question(
                            id = baseId++,
                            content = content,
                            subQuestions = subQuestions,
                            explanation = explanation,
                            topic = topic,
                            difficulty = difficulty
                        )
                    )
                }
                "P3", "PART3", "SA" -> {
                    val content = parts[1]
                    val ansValue = parts[2].toDoubleOrNull() ?: 0.0
                    val explanation = parts.getOrNull(3) ?: "Chưa có lời giải chi tiết."
                    val topic = parts.getOrNull(4)?.ifBlank { determineTopicLocal(content) } ?: determineTopicLocal(content)
                    val difficulty = parts.getOrNull(5)?.ifBlank { "Khó" } ?: "Khó"
                    
                    p3List.add(
                        Part3Question(
                            id = baseId++,
                            content = content,
                            correctAnswer = ansValue,
                            explanation = explanation,
                            topic = topic,
                            difficulty = difficulty
                        )
                    )
                }
                else -> {
                    errors.add("Dòng ${lineIdx + 1}: Không nhận dạng được loại câu hỏi (Cần bắt đầu bằng P1, P2 hoặc P3).")
                }
            }
        }
    }
    
    return BulkParseResult(p1List, p2List, p3List, errors)
}

// Helper to get actual display name from uri
fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx != -1) {
                    result = cursor.getString(idx)
                }
            }
        } catch (e: Exception) {
            // fallback
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "Document.txt"
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ExamImportTab(
    viewModel: ExamViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Custom imported questions preview from database
    val customP1 by viewModel.customPart1.collectAsState()
    val customP2 by viewModel.customPart2.collectAsState()
    val customP3 by viewModel.customPart3.collectAsState()
    
    var importMode by remember { mutableStateOf(0) } // 0: Normal Text, 1: LaTeX Code, 2: Manual Form
    var inputText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorStatus by remember { mutableStateOf(false) }

    // Bulk Import State Variables
    var bulkInputText by remember { mutableStateOf("") }
    var parsedBulkResult by remember { mutableStateOf<BulkParseResult?>(null) }

    val bulkPipeTemplate = """# Mẫu 1: Trắc nghiệm Phần 1 (MCQ)
P1 | Cho hàm số f(x) = x^2 - 4x + 3. Cực tiểu của hàm số đạt tại x bằng bao nhiêu? | x = 1 | x = 2 | x = 3 | x = 4 | B | Đạo hàm f'(x) = 2x - 4. Cho f'(x) = 0 => x = 2. f''(2) = 2 > 0 nên x = 2 là điểm cực tiểu. | Giải tích | Dễ

# Mẫu 2: Đúng/Sai Phần 2 (TF)
P2 | Xét tính đúng sai của các khẳng định sau về hàm số y = x^3 - 3x: | Hàm số đồng biến trên khoảng (-1; 1) | Sai | Hàm số đạt cực đại tại x = -1 | Đúng | Hàm số có hai điểm cực trị | Đúng | Hàm số đồng biến trên toàn bộ R | Sai | Đạo hàm y' = 3x^2 - 3. Nghiệm x = +/-1. Hàm số nghịch biến trên (-1; 1) và đồng biến trên các khoảng còn lại. | Giải tích | Trung bình

# Mẫu 3: Trả lời ngắn Phần 3 (SA)
P3 | Tìm số nghiệm nguyên của phương trình log2(x) = 3 | 8 | log2(x) = 3 <=> x = 2^3 = 8. Vậy có 1 nghiệm nguyên duy nhất là x = 8. | Đại số | Khó"""

    val bulkBlockTemplate = """[MCQ]
Cho hàm số f(x) = x^2 - 4x + 3. Cực tiểu của hàm số đạt tại x bằng bao nhiêu?
A. x = 1
B. x = 2
C. x = 3
D. x = 4
Đáp án: B
Giải thích: Đạo hàm f'(x) = 2x - 4 = 0 <=> x = 2.
Chủ đề: Giải tích
Độ khó: Dễ

[TF]
Xét tính đúng sai của các khẳng định sau về hàm số y = x^3 - 3x:
- Hàm số đồng biến trên khoảng (-1; 1) | Sai
- Hàm số đạt cực đại tại x = -1 | Đúng
- Hàm số có hai điểm cực trị | Đúng
- Hàm số đồng biến trên toàn bộ R | Sai
Giải thích: Đạo hàm y' = 3x^2 - 3. Nghiệm x = +/-1.
Chủ đề: Giải tích
Độ khó: Trung bình

[SA]
Tìm số nghiệm nguyên của phương trình log2(x) = 3
Đáp án: 8
Giải thích: log2(x) = 3 <=> x = 2^3 = 8.
Chủ đề: Đại số
Độ khó: Khó"""

    // Manual Question Form State Variables
    var manualQuestionText by remember { mutableStateOf("") }
    var manualOptionA by remember { mutableStateOf("") }
    var manualOptionB by remember { mutableStateOf("") }
    var manualOptionC by remember { mutableStateOf("") }
    var manualOptionD by remember { mutableStateOf("") }
    var manualCorrectIndex by remember { mutableStateOf(0) }
    var manualTopic by remember { mutableStateOf("Giải tích") }
    var manualDifficulty by remember { mutableStateOf("Dễ") }
    var manualExplanation by remember { mutableStateOf("") }

    // Expanded states for local database list
    var showP1List by remember { mutableStateOf(false) }
    var showP2List by remember { mutableStateOf(false) }
    var showP3List by remember { mutableStateOf(false) }

    // LaTeX Templates
    val latexMcqTemplate = """\begin{ex}%[Câu 1]
Cho hàm số ${'$'}y=f(x)${'$'} liên tục trên $\mathbb{R}$ và có đạo hàm ${'$'}f'(x) = x(x-1)^2(x+2)^3${'$'}. Số điểm cực trị của hàm số là:
\choice
{1}
{\True 2}
{3}
{4}
\loigiaichitiet{Đạo hàm ${'$'}f'(x)${'$'} đổi dấu khi đi qua các nghiệm đơn hoặc nghiệm bội lẻ: ${'$'}x = 0${'$'} (nghiệm đơn) và ${'$'}x = -2${'$'} (nghiệm bội 3). Tại ${'$'}x = 1${'$'}, đạo hàm không đổi dấu vì là nghiệm bội chẵn. Vậy hàm số có 2 điểm cực trị.}
\end{ex}"""

    val latexTfTemplate = """\begin{ex}%[Câu 2]
Cho hàm số bậc ba ${'$'}y = f(x)${'$'} có đồ thị như hình vẽ. Xét tính đúng sai của các mệnh đề sau:
\choiceTF
{\True Hàm số đồng biến trên khoảng ${'$'}(0; 2)${'$'}.}
{Hàm số có cực trị tại ${'$'}x = 3${'$'}.}
{\True Giá trị cực đại của hàm số là ${'$'}y = 4${'$'}.}
{Hàm số đồng biến trên toàn bộ $\mathbb{R}$.}
\loigiaichitiet{Dựa vào đồ thị ta thấy hàm số đồng biến từ khoảng ${'$'}(0; 2)${'$'}, điểm cực đại đạt tại ${'$'}x = 0${'$'} và cực tiểu đạt tại ${'$'}x = 2${'$'}, giá trị cực đại là ${'$'}y = f(0) = 4${'$'}.}
\end{ex}"""

    val latexShortTemplate = """\begin{ex}%[Câu 3]
Trong không gian ${'$'}Oxyz${'$'}, cho mặt cầu ${'$'}(S): (x-1)^2 + (y+2)^2 + z^2 = 25${'$'}. Bán kính ${'$'}R${'$'} của mặt cầu là bao nhiêu?
\gex{5}
\loigiaichitiet{Phương trình mặt cầu có dạng ${'$'}(x-a)^2 + (y-b)^2 + (z-c)^2 = R^2 \implies R^2 = 25 \implies R = 5${'$'}.}
\end{ex}"""

    val latexMixedTemplate = """\begin{ex}%[Câu 1]
Cho hàm số ${'$'}y=f(x)${'$'} liên tục trên $\mathbb{R}$ và có đạo hàm ${'$'}f'(x) = x(x-1)^2(x+2)^3${'$'}. Số điểm cực trị của hàm số là:
\choice
{1}
{\True 2}
{3}
{4}
\end{ex}

\begin{ex}%[Câu 2]
Cho hàm số bậc ba ${'$'}y = f(x)${'$'} có đồ thị như hình vẽ. Xét tính đúng sai của các mệnh đề sau:
\choiceTF
{\True Mệnh đề 1: Hàm số đồng biến trên khoảng ${'$'}(0; 2)${'$'}.}
{Mệnh đề 2: Hàm số có cực trị tại ${'$'}x = 3${'$'}.}
{\True Mệnh đề 3: Giá trị cực đại của hàm số là ${'$'}y = 4${'$'}.}
{Mệnh đề 4: Hàm số đồng biến trên toàn bộ $\mathbb{R}$.}
\end{ex}

\begin{ex}%[Câu 3]
Trong không gian ${'$'}Oxyz${'$'}, cho mặt cầu ${'$'}(S): (x-1)^2 + (y+2)^2 + z^2 = 25${'$'}. Bán kính ${'$'}R${'$'} của mặt cầu là bao nhiêu?
\gex{5}
\end{ex}"""
    
    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fileName = getFileName(context, uri)
                selectedFileName = fileName
                selectedUri = uri
                
                val isPdf = fileName.endsWith(".pdf", ignoreCase = true) || 
                            context.contentResolver.getType(uri)?.contains("pdf", ignoreCase = true) == true
                val isDocx = fileName.endsWith(".docx", ignoreCase = true) ||
                             context.contentResolver.getType(uri)?.contains("officedocument", ignoreCase = true) == true
                val isDoc = fileName.endsWith(".doc", ignoreCase = true) ||
                            context.contentResolver.getType(uri)?.contains("msword", ignoreCase = true) == true

                val extractedText = ExamParser.readTextFromUri(context, uri)
                if (extractedText.isNotBlank()) {
                    inputText = extractedText
                    val typeStr = if (isPdf) "PDF" else if (isDocx) "Word (.docx)" else if (isDoc) "Word (.doc)" else "văn bản"
                    Toast.makeText(context, "Đã đọc tài liệu $typeStr thành công! Sẵn sàng phân tích.", Toast.LENGTH_SHORT).show()
                } else {
                    inputText = "Tập tin: $fileName\n\n[Đã nạp tệp $fileName thành công. Vui lòng nhấn nút 'Phân tích AI ✨' bên dưới để AI tiến hành trích xuất công thức toán, bảng biểu và hình vẽ trực tiếp từ tệp gốc.]"
                    Toast.makeText(context, "Đã nạp file $fileName! Hãy dùng 'Phân tích AI' để trích xuất.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi đọc tập tin: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_header_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload Icon",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nạp & Quản lý Ngân hàng đề",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = "Xây dựng cơ sở dữ liệu câu hỏi cục bộ bằng Văn bản thường hoặc định dạng LaTeX chuyên nghiệp.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        }

        // Mode Switcher (Normal vs LaTeX vs Bulk vs Manual)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = importMode == 0,
                    onClick = {
                        importMode = 0
                        inputText = ""
                        selectedFileName = null
                    },
                    label = { Text("Văn bản", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.TextFields, null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("mode_text_chip")
                )
                FilterChip(
                    selected = importMode == 1,
                    onClick = {
                        importMode = 1
                        inputText = ""
                        selectedFileName = null
                    },
                    label = { Text("Mã LaTeX", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Code, null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("mode_latex_chip")
                )
                FilterChip(
                    selected = importMode == 3,
                    onClick = {
                        importMode = 3
                    },
                    label = { Text("Dán Hàng Loạt", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.ContentPaste, null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("mode_bulk_chip")
                )
                FilterChip(
                    selected = importMode == 2,
                    onClick = {
                        importMode = 2
                    },
                    label = { Text("Tạo Thủ Công", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Add, null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("mode_manual_chip")
                )
                FilterChip(
                    selected = importMode == 4,
                    onClick = {
                        importMode = 4
                    },
                    label = { Text("Ngân Hàng Room DB", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Storage, null, modifier = Modifier.size(14.dp)) },
                    modifier = Modifier.testTag("mode_database_chip")
                )
            }
        }

        if (importMode == 0 || importMode == 1) {
            // Main parser inputs
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = if (importMode == 1) "Nhập mã LaTeX toán học (.tex)" else "Nhập đề thi văn bản thường",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // LaTeX Template selectors
                        if (importMode == 1) {
                            Text(
                                text = "Chọn cấu trúc mẫu nhanh để xem định dạng hoặc thử nghiệm:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SuggestionChip(
                                    onClick = {
                                        inputText = latexMcqTemplate
                                        Toast.makeText(context, "Đã nạp mẫu MCQ Phần I", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("Trắc nghiệm (Phần I)") }
                                )
                                SuggestionChip(
                                    onClick = {
                                        inputText = latexTfTemplate
                                        Toast.makeText(context, "Đã nạp mẫu Đúng/Sai Phần II", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("Đúng / Sai (Phần II)") }
                                )
                                SuggestionChip(
                                    onClick = {
                                        inputText = latexShortTemplate
                                        Toast.makeText(context, "Đã nạp mẫu Trả lời ngắn Phần III", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("Trả lời ngắn (Phần III)") }
                                )
                                SuggestionChip(
                                    onClick = {
                                        inputText = latexMixedTemplate
                                        Toast.makeText(context, "Đã nạp mẫu đề Hỗn hợp", Toast.LENGTH_SHORT).show()
                                    },
                                    label = { Text("Đề mẫu Hỗn hợp") }
                                )
                            }
                        }
                        
                        // Choose file card
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { filePickerLauncher.launch("*/*") },
                            colors = CardDefaults.outlinedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = "Tải tập tin",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (selectedFileName != null) "Tập tin đã chọn" else if (importMode == 1) "Tải lên tệp LaTeX (.tex, .txt)" else "Tải lên tệp tài liệu (.txt, .pdf, .docx)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = selectedFileName ?: "Nhấp để duyệt tập tin từ thiết bị của bạn",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (selectedFileName != null) {
                                    IconButton(
                                        onClick = {
                                            selectedFileName = null
                                            selectedUri = null
                                            inputText = ""
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Hủy chọn file",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Hoặc dán trực tiếp nội dung vào đây:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .testTag("text_import_field"),
                            placeholder = { 
                                if (importMode == 1) {
                                    Text("Nhập hoặc dán mã LaTeX tại đây...\nVí dụ:\n\\begin{ex}\nTìm cực trị...\n\\choice\n{1}\n{\\True 2}\n{3}\n{4}\n\\end{ex}")
                                } else {
                                    Text("Dán văn bản câu hỏi tại đây...\nVí dụ:\nCâu 1. Tìm cực trị của hàm số...\nA. x = 1\nB. x = 2...")
                                }
                            },
                            maxLines = 500,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = if (importMode == 1) FontFamily.Monospace else FontFamily.Default
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Control Buttons Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // AI Parser Button (Gemini)
                            Button(
                                onClick = {
                                    if (inputText.isBlank()) {
                                        Toast.makeText(context, "Vui lòng dán văn bản hoặc tải file!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    statusMessage = "AI đang phân tích câu hỏi toán học, vui lòng đợi..."
                                    isErrorStatus = false
                                    coroutineScope.launch {
                                        val result = ExamParser.parseWithGemini(context, inputText, selectedUri) { retryStatus ->
                                            statusMessage = retryStatus
                                        }
                                        isLoading = false
                                        when (result) {
                                            is ParseResult.Success -> {
                                                viewModel.addImportedQuestions(result.part1, result.part2, result.part3)
                                                statusMessage = "Đã nạp câu hỏi thành công bằng AI! Đã tích hợp vào ngân hàng:\n- ${result.part1.size} câu Trắc nghiệm (P1)\n- ${result.part2.size} câu Đúng/Sai (P2)\n- ${result.part3.size} câu Trả lời ngắn (P3)"
                                                isErrorStatus = false
                                            }
                                            is ParseResult.Error -> {
                                                statusMessage = result.message
                                                isErrorStatus = true
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .weight(1.2f)
                                    .testTag("ai_parse_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Gemini AI",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Phân tích AI ✨", fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            // Offline Parser Button
                            ElevatedButton(
                                onClick = {
                                    if (inputText.isBlank()) {
                                        Toast.makeText(context, "Vui lòng dán văn bản hoặc tải file!", Toast.LENGTH_SHORT).show()
                                        return@ElevatedButton
                                    }
                                    isLoading = true
                                    statusMessage = "Đang phân tích ngoại tuyến..."
                                    isErrorStatus = false
                                    coroutineScope.launch {
                                        val result = if (importMode == 1) {
                                            ExamParser.parseLatex(inputText)
                                        } else {
                                            ExamParser.parseOffline(inputText)
                                        }
                                        isLoading = false
                                        when (result) {
                                            is ParseResult.Success -> {
                                                viewModel.addImportedQuestions(result.part1, result.part2, result.part3)
                                                statusMessage = "Phân tích hoàn tất! Đã nạp vào ngân hàng:\n- ${result.part1.size} câu MCQ (P1)\n- ${result.part2.size} câu Đúng/Sai (P2)\n- ${result.part3.size} câu Trả lời ngắn (P3)"
                                                isErrorStatus = false
                                            }
                                            is ParseResult.Error -> {
                                                statusMessage = result.message
                                                isErrorStatus = true
                                            }
                                        }
                                    }
                                },
                                enabled = !isLoading,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("offline_parse_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (importMode == 1) Icons.Default.SettingsEthernet else Icons.Default.FlashOn,
                                    contentDescription = "Offline Fast",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (importMode == 1) "Phân tích LaTeX" else "Phân tích Nhanh", fontWeight = FontWeight.Bold, maxLines = 1, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        } else if (importMode == 3) {
            // Bulk Paste Form
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().testTag("bulk_import_card"),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Nhập câu hỏi hàng loạt cực nhanh",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "Hỗ trợ 2 định dạng: phân tách bằng dấu gạch đứng (|) hoặc viết khối trắc nghiệm rõ ràng. Hãy chọn một mẫu bên dưới để thử nghiệm nhanh.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Sample quick selections
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SuggestionChip(
                                onClick = {
                                    bulkInputText = bulkPipeTemplate
                                    parsedBulkResult = parseBulkText(bulkPipeTemplate)
                                    Toast.makeText(context, "Đã nạp mẫu phân cách |", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("Mẫu phân cách (|)") }
                            )
                            SuggestionChip(
                                onClick = {
                                    bulkInputText = bulkBlockTemplate
                                    parsedBulkResult = parseBulkText(bulkBlockTemplate)
                                    Toast.makeText(context, "Đã nạp mẫu Khối Văn Bản", Toast.LENGTH_SHORT).show()
                                },
                                label = { Text("Mẫu Khối Văn Bản") }
                            )
                        }

                        OutlinedTextField(
                            value = bulkInputText,
                            onValueChange = { 
                                bulkInputText = it 
                                parsedBulkResult = parseBulkText(it)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .testTag("bulk_import_text_field"),
                            placeholder = { 
                                Text("Dán văn bản câu hỏi hàng loạt tại đây...\nVí dụ dạng gạch đứng:\nP1 | Nội dung... | A | B | C | D | Đáp án đúng\n\nHoặc dạng khối:\n[MCQ]\nCâu hỏi...\nA. ...\nĐáp án: A")
                            },
                            maxLines = 1000,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Parse Summary section
                        parsedBulkResult?.let { result ->
                            val totalParsed = result.part1.size + result.part2.size + result.part3.size
                            if (totalParsed > 0 || result.errors.isNotEmpty()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "Kết quả phân tích tạm thời:",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (result.part1.isNotEmpty()) {
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text("P1 (MCQ): ${result.part1.size}") },
                                                    leadingIcon = { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                                )
                                            }
                                            if (result.part2.isNotEmpty()) {
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text("P2 (Đ/S): ${result.part2.size}") },
                                                    leadingIcon = { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                                )
                                            }
                                            if (result.part3.isNotEmpty()) {
                                                AssistChip(
                                                    onClick = {},
                                                    label = { Text("P3 (Ngắn): ${result.part3.size}") },
                                                    leadingIcon = { Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp)) }
                                                )
                                            }
                                        }

                                        if (result.errors.isNotEmpty()) {
                                            Text(
                                                text = "Cảnh báo / Lỗi định dạng (${result.errors.size}):",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                result.errors.take(5).forEach { err ->
                                                    Text(
                                                        text = "• $err",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                if (result.errors.size > 5) {
                                                    Text(
                                                        text = "... và ${result.errors.size - 5} cảnh báo khác.",
                                                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.error),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    bulkInputText = ""
                                    parsedBulkResult = null
                                },
                                modifier = Modifier.weight(1f).testTag("bulk_clear_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Xóa sạch")
                            }

                            val totalQuestions = (parsedBulkResult?.part1?.size ?: 0) +
                                                 (parsedBulkResult?.part2?.size ?: 0) +
                                                 (parsedBulkResult?.part3?.size ?: 0)

                            Button(
                                onClick = {
                                    val result = parsedBulkResult
                                    if (result == null || totalQuestions == 0) {
                                        Toast.makeText(context, "Chưa có câu hỏi nào hợp lệ để nhập!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    viewModel.addImportedQuestions(
                                        result.part1,
                                        result.part2,
                                        result.part3
                                    )

                                    Toast.makeText(
                                        context,
                                        "Đã nhập thành công $totalQuestions câu hỏi vào ngân hàng cục bộ!",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // Reset input and results
                                    bulkInputText = ""
                                    parsedBulkResult = null
                                },
                                enabled = totalQuestions > 0,
                                modifier = Modifier.weight(1.5f).testTag("bulk_save_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lưu $totalQuestions câu hỏi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else {
            // Manual Question Form Card
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().testTag("manual_question_form_card"),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Tạo câu hỏi trắc nghiệm mới",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        // 1. Question Text
                        OutlinedTextField(
                            value = manualQuestionText,
                            onValueChange = { manualQuestionText = it },
                            label = { Text("Nội dung câu hỏi") },
                            placeholder = { Text("Ví dụ: Tìm cực trị của hàm số y = x^2 - 4x...") },
                            modifier = Modifier.fillMaxWidth().testTag("manual_question_text"),
                            minLines = 3,
                            maxLines = 6,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // 2. Options and Correct Option selector
                        Text(
                            text = "Các phương án trả lời & Đánh dấu đáp án đúng:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        val optionsList = listOf(
                            Triple("A", manualOptionA) { v: String -> manualOptionA = v },
                            Triple("B", manualOptionB) { v: String -> manualOptionB = v },
                            Triple("C", manualOptionC) { v: String -> manualOptionC = v },
                            Triple("D", manualOptionD) { v: String -> manualOptionD = v }
                        )

                        optionsList.forEachIndexed { idx, (label, valText, setter) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = manualCorrectIndex == idx,
                                    onClick = { manualCorrectIndex = idx },
                                    modifier = Modifier.testTag("manual_radio_$label")
                                )
                                OutlinedTextField(
                                    value = valText,
                                    onValueChange = setter,
                                    label = { Text("Đáp án $label") },
                                    placeholder = { Text("Nhập phương án $label...") },
                                    modifier = Modifier.weight(1f).testTag("manual_option_$label"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // 3. Topic Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Chủ đề:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val topicList = listOf(
                                    "Giải tích" to (Icons.Default.ShowChart to Color(0xFF009688)),
                                    "Đại số" to (Icons.Default.Calculate to Color(0xFF3F51B5)),
                                    "Hình học phẳng" to (Icons.Default.Category to Color(0xFFFF9800)),
                                    "Hình học không gian" to (Icons.Default.Layers to Color(0xFF9C27B0)),
                                    "Tổ hợp & Xác suất" to (Icons.Default.Shuffle to Color(0xFFE91E63))
                                )
                                topicList.forEach { (topic, info) ->
                                    val (icon, baseColor) = info
                                    val isSelected = manualTopic == topic
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { manualTopic = topic },
                                        label = { Text(topic, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = baseColor.copy(alpha = 0.15f),
                                            selectedLabelColor = baseColor,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            selectedBorderColor = baseColor,
                                            borderWidth = if (isSelected) 1.5.dp else 1.dp
                                        ),
                                        modifier = Modifier.testTag("manual_topic_chip_$topic")
                                    )
                                }
                            }
                        }

                        // 4. Difficulty Selection
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Mức độ:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val diffList = listOf(
                                    "Dễ" to (Icons.Default.CheckCircle to Color(0xFF2E7D32)),
                                    "Trung bình" to (Icons.Default.Star to Color(0xFFE65100)),
                                    "Khó" to (Icons.Default.AutoAwesome to Color(0xFFC62828))
                                )
                                diffList.forEach { (diff, info) ->
                                    val (icon, baseColor) = info
                                    val isSelected = manualDifficulty == diff
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { manualDifficulty = diff },
                                        label = { Text(diff, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = baseColor.copy(alpha = 0.15f),
                                            selectedLabelColor = baseColor,
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            selectedBorderColor = baseColor,
                                            borderWidth = if (isSelected) 1.5.dp else 1.dp
                                        ),
                                        modifier = Modifier.weight(1f).testTag("manual_diff_chip_$diff")
                                    )
                                }
                            }
                        }

                        // 5. Explanation
                        OutlinedTextField(
                            value = manualExplanation,
                            onValueChange = { manualExplanation = it },
                            label = { Text("Lời giải chi tiết (không bắt buộc)") },
                            placeholder = { Text("Nhập lời giải hoặc giải thích chi tiết cho câu hỏi này...") },
                            modifier = Modifier.fillMaxWidth().testTag("manual_explanation"),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp)
                        )

                        // 6. Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    manualQuestionText = ""
                                    manualOptionA = ""
                                    manualOptionB = ""
                                    manualOptionC = ""
                                    manualOptionD = ""
                                    manualExplanation = ""
                                    manualCorrectIndex = 0
                                },
                                modifier = Modifier.weight(1f).testTag("manual_reset_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Nhập lại", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    if (manualQuestionText.isBlank()) {
                                        Toast.makeText(context, "Vui lòng nhập nội dung câu hỏi!", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    if (manualOptionA.isBlank() || manualOptionB.isBlank() || manualOptionC.isBlank() || manualOptionD.isBlank()) {
                                        Toast.makeText(context, "Vui lòng điền đầy đủ cả 4 phương án đáp án!", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }

                                    val newId = (System.currentTimeMillis() / 1000).toInt() + kotlin.random.Random.nextInt(1000)
                                    val newQuestion = Part1Question(
                                        id = newId,
                                        content = manualQuestionText.trim(),
                                        originalOptions = listOf(
                                            manualOptionA.trim(),
                                            manualOptionB.trim(),
                                            manualOptionC.trim(),
                                            manualOptionD.trim()
                                        ),
                                        correctIndex = manualCorrectIndex,
                                        explanation = manualExplanation.trim(),
                                        topic = manualTopic,
                                        difficulty = manualDifficulty
                                    )

                                    viewModel.addImportedQuestions(
                                        listOf(newQuestion),
                                        emptyList(),
                                        emptyList()
                                    )

                                    // Save to Room DB as well for persistent database management
                                    viewModel.saveMathQuestion(
                                        MathQuestionEntity(
                                            content = manualQuestionText.trim(),
                                            optionA = manualOptionA.trim(),
                                            optionB = manualOptionB.trim(),
                                            optionC = manualOptionC.trim(),
                                            optionD = manualOptionD.trim(),
                                            correctIndex = manualCorrectIndex,
                                            explanation = manualExplanation.trim(),
                                            topic = manualTopic,
                                            tagsJson = MathQuestionEntity.tagsListToJson(listOf("Algebra", "Calculus")),
                                            difficulty = manualDifficulty
                                        )
                                    )

                                    Toast.makeText(context, "Đã lưu câu hỏi vào ngân hàng cục bộ thành công!", Toast.LENGTH_LONG).show()

                                    // Clear fields
                                    manualQuestionText = ""
                                    manualOptionA = ""
                                    manualOptionB = ""
                                    manualOptionC = ""
                                    manualOptionD = ""
                                    manualExplanation = ""
                                    manualCorrectIndex = 0
                                },
                                modifier = Modifier.weight(1.2f).testTag("manual_submit_button"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Thêm câu hỏi", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (importMode == 4) {
            item {
                QuestionDatabaseManagerUI(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Loading or Status Area
        if (isLoading || statusMessage != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLoading) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        } else if (isErrorStatus) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = if (isErrorStatus) Icons.Default.Error else Icons.Default.CheckCircle,
                                contentDescription = "Status Icon",
                                tint = if (isErrorStatus) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(
                            text = statusMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isErrorStatus) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Local question database control dashboard and list
        val totalImported = customP1.size + customP2.size + customP3.size
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ngân hàng câu hỏi cục bộ",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tổng số: $totalImported câu hỏi tự chọn",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (totalImported > 0) {
                            IconButton(
                                onClick = {
                                    viewModel.clearImportedQuestions()
                                    statusMessage = "Đã dọn sạch ngân hàng câu hỏi tự chọn."
                                    isErrorStatus = false
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteSweep,
                                    contentDescription = "Xóa toàn bộ",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showP1List,
                            onClick = { showP1List = !showP1List },
                            label = { Text("Phần I (MCQ): ${customP1.size}") },
                            leadingIcon = { Icon(Icons.Default.List, null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showP2List,
                            onClick = { showP2List = !showP2List },
                            label = { Text("Phần II (Đ/S): ${customP2.size}") },
                            leadingIcon = { Icon(Icons.Default.Rule, null, modifier = Modifier.size(16.dp)) }
                        )
                        FilterChip(
                            selected = showP3List,
                            onClick = { showP3List = !showP3List },
                            label = { Text("Phần III (Ngắn): ${customP3.size}") },
                            leadingIcon = { Icon(Icons.Default.ShortText, null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Text(
                        text = "Các câu hỏi trong cơ sở dữ liệu cục bộ sẽ tự động được trộn vào khi tạo Đề thi hoặc Luyện tập mới.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Visual Database Explorer (customP1)
        if (showP1List && customP1.isNotEmpty()) {
            item {
                Text(
                    text = "CHI TIẾT CÂU HỎI PHẦN I (TRẮC NGHIỆM)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(customP1) { question ->
                DatabaseQuestionCard(
                    part = 1,
                    title = "Câu hỏi Phần I",
                    content = question.content,
                    topic = question.topic,
                    difficulty = question.difficulty,
                    explanation = question.explanation,
                    onDelete = { viewModel.deleteCustomQuestion(1, question.id) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        question.originalOptions.forEachIndexed { optIdx, option ->
                            val isCorrect = optIdx == question.correctIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isCorrect) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isCorrect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                                FormattedMathText(
                                    text = "${'A' + optIdx}. $option",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = if (isCorrect) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Visual Database Explorer (customP2)
        if (showP2List && customP2.isNotEmpty()) {
            item {
                Text(
                    text = "CHI TIẾT CÂU HỎI PHẦN II (ĐÚNG / SAI)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(customP2) { question ->
                DatabaseQuestionCard(
                    part = 2,
                    title = "Câu hỏi Phần II",
                    content = question.content,
                    topic = question.topic,
                    difficulty = question.difficulty,
                    explanation = question.explanation,
                    onDelete = { viewModel.deleteCustomQuestion(2, question.id) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        question.subQuestions.forEachIndexed { index, subQ ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (subQ.isTrue) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FormattedMathText(
                                    text = subQ.statement,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (subQ.isTrue) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (subQ.isTrue) "Đúng" else "Sai",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Visual Database Explorer (customP3)
        if (showP3List && customP3.isNotEmpty()) {
            item {
                Text(
                    text = "CHI TIẾT CÂU HỎI PHẦN III (TRẢ LỜI NGẮN)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(customP3) { question ->
                DatabaseQuestionCard(
                    part = 3,
                    title = "Câu hỏi Phần III",
                    content = question.content,
                    topic = question.topic,
                    difficulty = question.difficulty,
                    explanation = question.explanation,
                    onDelete = { viewModel.deleteCustomQuestion(3, question.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Đáp số chính xác:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (question.correctAnswer % 1.0 == 0.0) {
                                question.correctAnswer.toInt().toString()
                            } else {
                                question.correctAnswer.toString()
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // LaTeX formatting guidelines
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Hướng dẫn",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Hướng dẫn cú pháp LaTeX Toán:",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "Hệ thống hỗ trợ hoàn toàn cú pháp LaTeX tiêu chuẩn trong các tài liệu trích xuất từ Việt Nam:\n" +
                               "• Môi trường câu hỏi: \\begin{ex} ... \\end{ex} hoặc \\begin{question} ... \\end{question}\n" +
                               "• Các phương án trắc nghiệm: \\choice {Phương án 1} {\\True Phương án đúng} {Phương án 3} {Phương án 4}\n" +
                               "• Các phương án Đúng/Sai (Phần II): \\choiceTF {\\True Mệnh đề đúng} {Mệnh đề sai}\n" +
                               "• Đáp án trả lời ngắn (Phần III): Sử dụng lệnh \\gex{đáp_số} (Ví dụ: \\gex{15})\n" +
                               "• Lời giải chi tiết: \\loigiaichitiet{Nội dung lời giải}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DatabaseQuestionCard(
    part: Int,
    title: String,
    content: String,
    topic: String,
    difficulty: String,
    explanation: String,
    onDelete: () -> Unit,
    additionalContent: @Composable () -> Unit
) {
    var expandedExplanation by remember { mutableStateOf(false) }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                when (part) {
                                    1 -> MaterialTheme.colorScheme.primaryContainer
                                    2 -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.tertiaryContainer
                                },
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Phần $part",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = when (part) {
                                1 -> MaterialTheme.colorScheme.onPrimaryContainer
                                2 -> MaterialTheme.colorScheme.onSecondaryContainer
                                else -> MaterialTheme.colorScheme.onTertiaryContainer
                            }
                        )
                    }

                    AssistChip(
                        onClick = {},
                        label = { Text(topic, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )

                    AssistChip(
                        onClick = {},
                        label = { Text(difficulty, fontSize = 10.sp) },
                        modifier = Modifier.height(24.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Xóa câu hỏi",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Question Content
            FormattedMathText(
                text = content,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            // Options / Sub-questions
            additionalContent()

            // Explanation section
            if (explanation.isNotBlank()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedExplanation = !expandedExplanation }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Lời giải chi tiết",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (expandedExplanation) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                AnimatedVisibility(
                    visible = expandedExplanation,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        FormattedMathText(
                            text = explanation,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }
            }
        }
    }
}
