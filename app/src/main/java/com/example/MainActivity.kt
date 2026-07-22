package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.db.*
import com.example.ui.QuestionVisual
import com.example.ui.ExamImportTab
import com.example.ui.MultiPlatformGuideTab
import com.example.ui.FormattedMathText
import com.example.ui.cleanMathText
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt
import kotlin.random.Random

// ==========================================
// DATA MODELS
// ==========================================

data class Part1Question(
    val id: Int,
    val content: String,
    val originalOptions: List<String>,
    val correctIndex: Int,
    val explanation: String,
    val topic: String = "Giải tích",
    val difficulty: String = "Dễ"
)

data class ShuffledPart1Question(
    val question: Part1Question,
    val options: List<String>,
    val correctIndex: Int
)

data class Part2SubQuestion(
    val statement: String,
    val isTrue: Boolean
)

data class Part2Question(
    val id: Int,
    val content: String,
    val subQuestions: List<Part2SubQuestion>,
    val explanation: String,
    val topic: String = "Giải tích",
    val difficulty: String = "Trung bình"
)

data class Part3Question(
    val id: Int,
    val content: String,
    val correctAnswer: Double,
    val explanation: String,
    val topic: String = "Giải tích",
    val difficulty: String = "Khó"
)

enum class Screen {
    WELCOME,
    EXAM,
    RESULT
}

data class ExamSession(
    val studentName: String,
    val studentClass: String,
    val examCode: String,
    val part1Questions: List<ShuffledPart1Question>,
    val part2Questions: List<Part2Question>,
    val part3Questions: List<Part3Question>,
    
    // Student answers
    val part1Answers: Map<Int, Int>, // Question Index -> Chosen Option Index
    val part2Answers: Map<Int, List<Boolean?>>, // Question Index -> List of 4 answers (True/False/Null)
    val part3Answers: Map<Int, String>, // Question Index -> Typed Text Answer
    
    val timeElapsedSeconds: Int,
    val totalPoints: Double = 0.0,
    val p1Points: Double = 0.0,
    val p2Points: Double = 0.0,
    val p3Points: Double = 0.0,
    
    // Practice and review extensions
    val isPracticeMode: Boolean = false,
    val isReviewMode: Boolean = false, // True if viewing historical record
    val part1Checked: Map<Int, Boolean> = emptyMap(), // Part 1 index -> Checked status
    val part2Checked: Map<Int, Boolean> = emptyMap(), // Part 2 q.id -> Checked status
    val part3Checked: Map<Int, Boolean> = emptyMap()  // Part 3 index -> Checked status
)

// ==========================================
// QUESTION BANK (Grade 12 Mathematics)
// ==========================================

object MathQuestionBank {
    val part1Bank = listOf(
        Part1Question(
            id = 1,
            content = "Tìm cực trị của đồ thị hàm số y = x\u00B3 - 3x\u00B2 + 2. Điểm cực đại của đồ thị hàm số là điểm nào sau đây?",
            originalOptions = listOf("(0; 2)", "(2; -2)", "(1; 0)", "(0; -2)"),
            correctIndex = 0,
            explanation = "Đạo hàm y' = 3x\u00B2 - 6x. Cho y' = 0 \u21D4 x = 0 hoặc x = 2.\n- Với x = 0 \u21D2 y = 2. Qua x = 0, y' đổi dấu từ dương sang âm, nên điểm cực đại của đồ thị hàm số là (0; 2).",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part1Question(
            id = 2,
            content = "Tìm phương trình đường tiệm cận đứng của đồ thị hàm số y = (2x - 1) / (x - 1).",
            originalOptions = listOf("x = 1", "y = 2", "x = -1", "y = 1"),
            correctIndex = 0,
            explanation = "Tiệm cận đứng là nghiệm của mẫu số x - 1 = 0 \u21D4 x = 1.\nGiới hạn lim (x\u21921\u207A) y = +\u221E, do đó x = 1 là tiệm cận đứng.",
            topic = "Giải tích",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 3,
            content = "Tính tích phân cơ bản sau: I = \u222B\u2080\u00B9 (2x + 1) dx.",
            originalOptions = listOf("I = 2", "I = 1", "I = 3", "I = 4"),
            correctIndex = 0,
            explanation = "Hàm nguyên hàm F(x) = x\u00B2 + x.\nTính tích phân: I = F(1) - F(0) = (1\u00B2 + 1) - 0 = 2.",
            topic = "Giải tích",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 4,
            content = "Trong không gian Oxyz, tìm tọa độ một vectơ pháp tuyến của mặt phẳng (P): 2x - y + 3z - 5 = 0.",
            originalOptions = listOf("n\u20D7 = (2; -1; 3)", "n\u20D7 = (2; 1; 3)", "n\u20D7 = (2; -1; -5)", "n\u20D7 = (2; 1; -5)"),
            correctIndex = 0,
            explanation = "Mặt phẳng có dạng tổng quát Ax + By + Cz + D = 0 có một vectơ pháp tuyến là n\u20D7 = (A; B; C). Ở đây n\u20D7 = (2; -1; 3).",
            topic = "Hình học không gian",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 5,
            content = "Trong không gian Oxyz, xác định bán kính R của mặt cầu (S): (x - 1)\u00B2 + y\u00B2 + (z + 2)\u00B2 = 9.",
            originalOptions = listOf("R = 3", "R = 9", "R = 81", "R = \u221A3"),
            correctIndex = 0,
            explanation = "Phương trình mặt cầu dạng (x-a)\u00B2 + (y-b)\u00B2 + (z-c)\u00B2 = R\u00B2.\nỞ đây R\u00B2 = 9 \u21D2 R = 3.",
            topic = "Hình học không gian",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 6,
            content = "Tính đạo hàm của hàm số mũ y = 3\u207F.",
            originalOptions = listOf("y' = 3\u207F * ln(3)", "y' = x * 3\u207F\u207B\u00B9", "y' = 3\u207F / ln(3)", "y' = 3\u207F"),
            correctIndex = 0,
            explanation = "Công thức đạo hàm hàm số mũ cơ bản: (a\u207F)' = a\u207F * ln(a).\nÁp dụng với a = 3, ta được y' = 3\u207F * ln(3).",
            topic = "Giải tích",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 7,
            content = "Cho khối chóp có diện tích đáy B = 6 và chiều cao h = 4. Tính thể tích V của khối chóp đã cho.",
            originalOptions = listOf("V = 8", "V = 24", "V = 12", "V = 18"),
            correctIndex = 0,
            explanation = "Công thức tính thể tích khối chóp: V = (1/3) * B * h.\nThay số: V = (1/3) * 6 * 4 = 8.",
            topic = "Hình học không gian",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 8,
            content = "Cho khối lăng trụ đứng có diện tích đáy B = 5 và chiều cao h = 6. Tính thể tích V của khối lăng trụ.",
            originalOptions = listOf("V = 30", "V = 10", "V = 15", "V = 60"),
            correctIndex = 0,
            explanation = "Thể tích khối lăng trụ đứng được xác định bằng công thức: V = B * h.\nThay số: V = 5 * 6 = 30.",
            topic = "Hình học không gian",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 9,
            content = "Tìm tập nghiệm S của bất phương trình: log\u2082 (x - 1) < 3.",
            originalOptions = listOf("S = (1; 9)", "S = (-\u221E; 9)", "S = (1; 8)", "S = (1; +\u221E)"),
            correctIndex = 0,
            explanation = "Điều kiện: x - 1 > 0 \u21D4 x > 1.\nBất phương trình \u21D4 x - 1 < 2\u00B3 \u21D4 x < 9.\nKết hợp điều kiện, ta thu được S = (1; 9).",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part1Question(
            id = 10,
            content = "Trong không gian Oxyz, cho tam giác ABC có A(1; 2; -1), B(2; -1; 3) và C(0; 2; 1). Tìm tọa độ trọng tâm G của tam giác ABC.",
            originalOptions = listOf("G(1; 1; 1)", "G(3; 3; 3)", "G(1; 2; 1)", "G(1; -1; 1)"),
            correctIndex = 0,
            explanation = "Tọa độ trọng tâm G = ((x_A+x_B+x_C)/3; (y_A+y_B+y_C)/3; (z_A+z_B+z_C)/3).\nThay số: G = ((1+2+0)/3; (2-1+2)/3; (-1+3+1)/3) = (1; 1; 1).",
            topic = "Hình học không gian",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 11,
            content = "Cho cấp số cộng (u_n) có u_1 = 3 và công sai d = 2. Tìm số hạng thứ 5 u_5 của cấp số cộng.",
            originalOptions = listOf("u_5 = 11", "u_5 = 10", "u_5 = 13", "u_5 = 9"),
            correctIndex = 0,
            explanation = "Công thức số hạng tổng quát cấp số cộng: u_n = u_1 + (n - 1)d.\nThay u_1 = 3, d = 2, n = 5 ⇒ u_5 = 3 + (5 - 1)*2 = 3 + 8 = 11.",
            topic = "Đại số",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 12,
            content = "Hàm số bậc ba y = ax\u00B3 + bx\u00B2 + cx + d đi qua điểm A(0; 1) và B(1; 2). Hệ số tự do d của đồ thị bằng bao nhiêu?",
            originalOptions = listOf("d = 1", "d = 2", "d = 0", "d = -1"),
            correctIndex = 0,
            explanation = "Điểm A(0; 1) thuộc đồ thị \u21D2 Thay x = 0, y = 1 vào phương trình:\n1 = a(0)\u00B3 + b(0)\u00B2 + c(0) + d \u21D4 d = 1.",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part1Question(
            id = 13,
            content = "Cho hình nón có bán kính đường tròn đáy r = 3 và chiều cao h = 4. Tính thể tích V của khối nón.",
            originalOptions = listOf("V = 12\u03C0", "V = 36\u03C0", "V = 4\u03C0", "V = 16\u03C0"),
            correctIndex = 0,
            explanation = "Công thức tính thể tích khối nón: V = (1/3) * \u03C0 * r\u00B2 * h.\nThay r = 3, h = 4 \u21D2 V = (1/3) * \u03C0 * 9 * 4 = 12\u03C0.",
            topic = "Hình học không gian",
            difficulty = "Trung bình"
        ),
        Part1Question(
            id = 14,
            content = "Tìm một nguyên hàm F(x) của hàm số f(x) = e\u207F + 2x.",
            originalOptions = listOf("F(x) = e\u207F + x\u00B2", "F(x) = e\u207F + 2", "F(x) = e\u207F + 2x\u00B2", "F(x) = e\u207F + x\u00B2/2"),
            correctIndex = 0,
            explanation = "Ta có \u222B (e\u207F + 2x) dx = e\u207F + x\u00B2 + C.\nChọn hằng số C = 0, ta được nguyên hàm F(x) = e\u207F + x\u00B2.",
            topic = "Giải tích",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 15,
            content = "Cho hàm số y = -x\u00B2 + 4x - 3. Khẳng định nào sau đây là hoàn toàn đúng?",
            originalOptions = listOf("Hàm số đồng biến trên (-\u221E; 2)", "Hàm số nghịch biến trên (-\u221E; 2)", "Hàm số đồng biến trên (2; +\u221E)", "Hàm số đồng biến trên R"),
            correctIndex = 0,
            explanation = "Xét đạo hàm y' = -2x + 4.\nCho y' > 0 \u21D4 -2x + 4 > 0 \u21D4 x < 2.\nDo đó hàm số đồng biến trên khoảng (-\u221E; 2).",
            topic = "Giải tích",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 16,
            content = "Có bao nhiêu cách chọn ra 2 học sinh từ một tổ gồm 10 học sinh?",
            originalOptions = listOf("45 cách", "90 cách", "20 cách", "100 cách"),
            correctIndex = 0,
            explanation = "Số cách chọn 2 học sinh từ 10 học sinh chính là tổ hợp chập 2 của 10:\nC(10, 2) = (10 * 9) / 2 = 45 cách.",
            topic = "Tổ hợp & Xác suất",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 17,
            content = "Giải phương trình 2^(x + 1) = 16.",
            originalOptions = listOf("x = 3", "x = 4", "x = 2", "x = 5"),
            correctIndex = 0,
            explanation = "Ta có 16 = 2^4. Do đó 2^(x + 1) = 2^4 ⇔ x + 1 = 4 ⇔ x = 3.",
            topic = "Đại số",
            difficulty = "Dễ"
        ),
        Part1Question(
            id = 18,
            content = "Cho hàm số y = f(x) có bảng biến thiên như sau. Tìm số tiệm cận đứng của đồ thị hàm số g(x) = 1 / [f(x) - 2].",
            originalOptions = listOf("3 đường", "2 đường", "1 đường", "Vô số"),
            correctIndex = 0,
            explanation = "Số tiệm cận đứng của g(x) là số nghiệm của phương trình f(x) - 2 = 0 \u21D4 f(x) = 2.\nDựa vào bảng biến thiên, đường thẳng y = 2 cắt đồ thị f(x) tại đúng 3 điểm phân biệt. Nên g(x) có 3 tiệm cận đứng.",
            topic = "Giải tích",
            difficulty = "Khó"
        ),
        Part1Question(
            id = 19,
            content = "Trong không gian Oxyz, tìm bán kính mặt cầu tiếp xúc với hai đường thẳng chéo nhau?",
            originalOptions = listOf("R = d / 2", "R = d", "R = \u221Ad", "R = d\u00B2"),
            correctIndex = 0,
            explanation = "Bán kính mặt cầu nhỏ nhất tiếp xúc với hai đường thẳng chéo nhau bằng một nửa khoảng cách giữa hai đường thẳng đó: R = d / 2.",
            topic = "Hình học không gian",
            difficulty = "Khó"
        ),
        Part1Question(
            id = 20,
            content = "Một hộp có 5 bi đỏ và 5 bi xanh. Lấy ngẫu nhiên 1 bi. Tính xác suất để được bi đỏ.",
            originalOptions = listOf("1/2", "1/5", "2/5", "1/10"),
            correctIndex = 0,
            explanation = "Tổng số bi là 10. Số bi đỏ là 5.\nXác suất P = 5/10 = 1/2 = 0.5.",
            topic = "Tổ hợp & Xác suất",
            difficulty = "Dễ"
        )
    )

    val part2Bank = listOf(
        Part2Question(
            id = 1,
            content = "Cho hàm số y = (x + 1) / (x - 1). Xét tính đúng hay sai của các mệnh đề sau:",
            subQuestions = listOf(
                Part2SubQuestion("Tập xác định của hàm số là D = R \\ {1}.", true),
                Part2SubQuestion("Đạo hàm của hàm số là y' = -2 / (x - 1)\u00B2.", true),
                Part2SubQuestion("Hàm số luôn đồng biến trên các khoảng xác định của nó.", false),
                Part2SubQuestion("Đồ thị hàm số có tiệm cận ngang là đường thẳng y = 1.", true)
            ),
            explanation = "a) Đúng vì mẫu số x-1 \u2260 0 \u21D4 x \u2260 1.\nb) Đúng vì y' = (1(x-1) - 1(x+1)) / (x-1)\u00B2 = -2 / (x-1)\u00B2.\nc) Sai vì đạo hàm y' < 0 nên hàm số nghịch biến trên từng khoảng xác định.\nd) Đúng vì lim (x\u2192\u221E) y = 1 nên tiệm cận ngang là y = 1.",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part2Question(
            id = 2,
            content = "Trong không gian Oxyz, cho mặt phẳng (P): x - 2y + 2z - 3 = 0. Phát biểu sau đúng hay sai:",
            subQuestions = listOf(
                Part2SubQuestion("Vectơ n\u20D7 = (1; -2; 2) là một vectơ pháp tuyến của (P).", true),
                Part2SubQuestion("Mặt phẳng (P) đi qua điểm B(0; 1; 1).", false),
                Part2SubQuestion("Khoảng cách từ gốc tọa độ O đến mặt phẳng (P) bằng 1.", true),
                Part2SubQuestion("Mặt phẳng (Q): 2x - 4y + 4z + 5 = 0 song song với mặt phẳng (P).", true)
            ),
            explanation = "a) Đúng: Tọa độ đứng trước x, y, z là (1; -2; 2).\nb) Sai: Thế B(0;1;1) vào ta được: 0 - 2*1 + 2*1 - 3 = -3 \u2260 0. Nên B không thuộc (P).\nc) Đúng: d(O,(P)) = |-3| / \u221A(1\u00B2 + 2\u00B2 + 2\u00B2) = 3/3 = 1.\nd) Đúng: Tỷ lệ pháp tuyến 2/1 = -4/-2 = 4/2 = 2, trong khi 5/-3 \u2260 2. Nên (P) // (Q).",
            topic = "Hình học không gian",
            difficulty = "Trung bình"
        ),
        Part2Question(
            id = 3,
            content = "Cho hàm số y = x\u2074 - 2x\u00B2 + 3. Xét các khẳng định sau:",
            subQuestions = listOf(
                Part2SubQuestion("Hàm số đã cho là hàm số chẵn trên R.", true),
                Part2SubQuestion("Hàm số có đúng 3 điểm cực trị.", true),
                Part2SubQuestion("Điểm cực tiểu của đồ thị hàm số là (0; 3).", false),
                Part2SubQuestion("Giá trị nhỏ nhất của hàm số trên R bằng 2.", true)
            ),
            explanation = "a) Đúng vì f(-x) = f(x).\nb) Đúng vì y' = 4x\u00B3 - 4x = 4x(x\u00B2-1) = 0 có 3 nghiệm phân biệt x = 0, x = \u00B11.\nc) Sai vì x = 0 là điểm cực đại (y(0) = 3). Hai điểm cực tiểu của đồ thị là (\u00B11; 2).\nd) Đúng vì giá trị cực tiểu y(\u00B11) = 2 là giá trị nhỏ nhất của hàm số trên R.",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part2Question(
            id = 4,
            content = "Một hộp chứa 3 viên bi đỏ và 7 viên bi xanh. Lấy ngẫu nhiên đồng thời 2 viên bi từ hộp. Xét các khẳng định sau:",
            subQuestions = listOf(
                Part2SubQuestion("Số phần tử của không gian mẫu là n(\u03A9) = 45.", true),
                Part2SubQuestion("Xác suất để lấy được 2 viên bi màu đỏ là 1/15.", true),
                Part2SubQuestion("Xác suất để lấy được 2 viên bi khác màu là 1/2.", false),
                Part2SubQuestion("Xác suất để lấy được ít nhất 1 viên bi màu đỏ là 8/15.", true)
            ),
            explanation = "a) Đúng: n(\u03A9) = C(10, 2) = 45.\nb) Đúng: P(2 đỏ) = C(3, 2) / 45 = 3/45 = 1/15.\nc) Sai: Số cách lấy khác màu: C(3, 1)*C(7, 1) = 21 \u21D2 P = 21/45 = 7/15 \u2260 1/2.\nd) Đúng: Biến cố đối là chọn 2 bi xanh. P(2 xanh) = C(7, 2)/45 = 21/45 = 7/15 \u21D2 P(ít nhất 1 đỏ) = 1 - 7/15 = 8/15.",
            topic = "Tổ hợp & Xác suất",
            difficulty = "Trung bình"
        ),
        Part2Question(
            id = 5,
            content = "Cho hình hộp chữ nhật ABCD.A'B'C'D' có AB = 1, AD = 2, AA' = 3. Xét các khẳng định sau:",
            subQuestions = listOf(
                Part2SubQuestion("Thể tích của khối hộp chữ nhật đã cho là V = 6.", true),
                Part2SubQuestion("Diện tích toàn phần của hình hộp chữ nhật là S_tp = 22.", true),
                Part2SubQuestion("Độ dài đường chéo AC' bằng \u221A14.", true),
                Part2SubQuestion("Khoảng cách từ đỉnh A đến mặt phẳng (B'C'D') bằng 2.", false)
            ),
            explanation = "a) Đúng: V = a*b*c = 1*2*3 = 6.\nb) Đúng: S_tp = 2*(1*2 + 1*3 + 2*3) = 22.\nc) Đúng: AC' = \u221A(AB\u00B2 + AD\u00B2 + AA'\u00B2) = \u221A(1 + 4 + 9) = \u221A14.\nd) Sai: Mặt phẳng (B'C'D') chính là mặt phẳng đáy trên (A'B'C'D'). Khoảng cách từ A thuộc đáy dưới đến đáy trên bằng độ dài cạnh bên AA' = 3.",
            topic = "Hình học không gian",
            difficulty = "Trung bình"
        ),
        Part2Question(
            id = 6,
            content = "Cho hàm số y = f(x) liên tục trên R và có bảng xét dấu đạo hàm y' đổi dấu từ âm sang dương tại x = -1, đổi dấu từ dương sang âm tại x = 2. Xét các khẳng định:",
            subQuestions = listOf(
                Part2SubQuestion("x = -1 là điểm cực đại của hàm số.", false),
                Part2SubQuestion("x = 2 là điểm cực đại của hàm số.", true),
                Part2SubQuestion("Đồ thị hàm số có đúng 2 điểm cực trị.", true),
                Part2SubQuestion("Hàm số nghịch biến trên khoảng (2; +\u221E).", true)
            ),
            explanation = "a) Sai: Đổi dấu từ âm sang dương \u21D2 x = -1 là điểm cực tiểu.\nb) Đúng: Đổi dấu từ dương sang âm \u21D2 x = 2 là điểm cực đại.\nc) Đúng: y' đổi dấu 2 lần nên đồ thị có 2 điểm cực trị.\nd) Đúng: y' < 0 trên khoảng (2; +\u221E) nên hàm số nghịch biến.",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part2Question(
            id = 7,
            content = "Trong không gian Oxyz, cho hai đường thẳng song song d\u2081, d\u2082. Gọi mặt phẳng chứa cả hai đường thẳng này là (P). Xét các nhận định:",
            subQuestions = listOf(
                Part2SubQuestion("Vectơ chỉ phương của d\u2081 và d\u2082 cùng phương.", true),
                Part2SubQuestion("Có duy nhất một mặt phẳng (P) chứa cả hai đường thẳng.", true),
                Part2SubQuestion("Khoảng cách giữa d\u2081 và d\u2082 luôn luôn không đổi.", true),
                Part2SubQuestion("Đường thẳng vuông góc với d\u2081 cũng vuông góc với d\u2082.", true)
            ),
            explanation = "Tất cả các khẳng định trên đều là các tính chất hình học cơ bản của hai đường thẳng song song trong không gian Oxyz.",
            topic = "Hình học không gian",
            difficulty = "Khó"
        ),
        Part2Question(
            id = 8,
            content = "Xét phương trình log\u2082\u00B2(x) - 3log\u2082(x) + 2 = 0. Phát biểu sau đúng hay sai:",
            subQuestions = listOf(
                Part2SubQuestion("Điều kiện xác định của phương trình là x > 0.", true),
                Part2SubQuestion("Đặt t = log\u2082(x), phương trình trở thành t\u00B2 - 3t + 2 = 0.", true),
                Part2SubQuestion("Phương trình có hai nghiệm phân biệt là x = 2 và x = 4.", true),
                Part2SubQuestion("Tổng hai nghiệm của phương trình bằng 6.", true)
            ),
            explanation = "Đặt t = log\u2082(x) \u21D2 t\u00B2 - 3t + 2 = 0 \u21D4 t = 1 hoặc t = 2.\n- Với t = 1 \u21D2 log\u2082(x) = 1 \u21D4 x = 2.\n- Với t = 2 \u21D2 log\u2082(x) = 2 \u21D4 x = 4.\nTổng hai nghiệm: 2 + 4 = 6.",
            topic = "Giải tích",
            difficulty = "Khó"
        )
    )

    val part3Bank = listOf(
        Part3Question(
            id = 1,
            content = "Tính diện tích S của hình phẳng giới hạn bởi đồ thị hàm số y = 3x\u00B2 và trục hoành Ox, các đường thẳng x = 0, x = 1.",
            correctAnswer = 1.0,
            explanation = "Diện tích S = \u222B\u2080\u00B9 |3x\u00B2| dx = [x\u00B3]\u2080\u00B9 = 1 - 0 = 1.0.\nĐáp số: 1.0",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part3Question(
            id = 2,
            content = "Tìm giá trị lớn nhất M của hàm số y = x + 4/x trên đoạn [1; 3].",
            correctAnswer = 5.0,
            explanation = "Đạo hàm y' = 1 - 4/x\u00B2 = 0 \u21D4 x = 2 (do x \u2208 [1; 3]).\nTính các giá trị: y(1) = 5, y(2) = 4, y(3) = 13/3 \u2248 4.333.\nSo sánh các giá trị, ta có giá trị lớn nhất M = 5.0 tại x = 1.\nĐáp số: 5.0",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part3Question(
            id = 3,
            content = "Trong không gian Oxyz, cho hai điểm A(1; 1; 2) and B(3; 5; 4). Tìm cao độ z của trung điểm I của đoạn thẳng AB.",
            correctAnswer = 3.0,
            explanation = "Tọa độ trung điểm I có cao độ z_I = (z_A + z_B) / 2 = (2 + 4) / 2 = 3.0.\nĐáp số: 3.0",
            topic = "Hình học không gian",
            difficulty = "Dễ"
        ),
        Part3Question(
            id = 4,
            content = "Cho hình hộp chữ nhật ABCD.A'B'C'D' có kích thước AB = 3, AD = 4, AA' = 5. Tìm bán kính R của mặt cầu ngoại tiếp hình hộp chữ nhật đó. (Làm tròn kết quả đến phần nghìn).",
            correctAnswer = 3.536,
            explanation = "Đường chéo hình hộp d = \u221A(AB\u00B2 + AD\u00B2 + AA'\u00B2) = \u221A(3\u00B2 + 4\u00B2 + 5\u00B2) = \u221A50 = 5\u221A2 \u2248 7.0711.\nBán kính mặt cầu ngoại tiếp R = d / 2 = 2.5\u221A2 \u2248 3.5355.\nLàm tròn đến phần nghìn ta được R = 3.536.\nĐáp số: 3.536",
            topic = "Hình học không gian",
            difficulty = "Khó"
        ),
        Part3Question(
            id = 5,
            content = "Tìm nghiệm nguyên dương nhỏ nhất của bất phương trình: 2^(2x - 1) > 16.",
            correctAnswer = 3.0,
            explanation = "Bất phương trình \u21D4 2^(2x - 1) > 2\u2074 \u21D4 2x - 1 > 4 \u21D4 2x > 5 \u21D4 x > 2.5.\nNghiệm nguyên dương nhỏ nhất là x = 3.0.\nĐáp số: 3.0",
            topic = "Giải tích",
            difficulty = "Dễ"
        ),
        Part3Question(
            id = 6,
            content = "Cho hàm số f(x) = ln(2x - 1). Tính giá trị của đạo hàm f'(1).",
            correctAnswer = 2.0,
            explanation = "Đạo hàm f'(x) = (2x - 1)' / (2x - 1) = 2 / (2x - 1).\nThế x = 1 vào ta được f'(1) = 2 / (2(1) - 1) = 2.0.\nĐáp số: 2.0",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part3Question(
            id = 7,
            content = "Gieo hai con súc sắc cân đối, đồng chất. Tính xác suất để tổng số chấm xuất hiện trên hai con súc sắc bằng 7. (Làm tròn đến phần nghìn).",
            correctAnswer = 0.167,
            explanation = "Số kết quả thuận lợi cho biến cố tổng chấm bằng 7 là 6 cặp: (1,6), (2,5), (3,4), (4,3), (5,2), (6,1).\nTổng số kết quả của không gian mẫu là 36.\nXác suất P = 6/36 = 1/6 \u2248 0.16667. Làm tròn đến phần nghìn là 0.167.\nĐáp số: 0.167",
            topic = "Tổ hợp & Xác suất",
            difficulty = "Khó"
        ),
        Part3Question(
            id = 8,
            content = "Tính tích phân lượng giác sau: I = \u222B\u2080^(\u03C0/2) sin(x) dx.",
            correctAnswer = 1.0,
            explanation = "Nguyên hàm F(x) = -cos(x).\nI = -cos(\u03C0/2) - (-cos(0)) = 0 + 1 = 1.0.\nĐáp số: 1.0",
            topic = "Giải tích",
            difficulty = "Trung bình"
        ),
        Part3Question(
            id = 9,
            content = "Trong không gian Oxyz, cho mặt phẳng (P): x - 2y + 2z - 3 = 0. Tính khoảng cách d từ điểm M(1; 2; 3) đến mặt phẳng (P).",
            correctAnswer = 2.0,
            explanation = "d(M, (P)) = |1 - 2(2) + 2(3) - 3| / \u221A(1\u00B2 + (-2)\u00B2 + 2\u00B2) = |1 - 4 + 6 - 3| / 3 = 0.0 \u21D2 thế lại: |1 - 4 + 6 - 3| = |0| = 0. Khoảng cách d(M,(P)) = |1 - 2*2 + 2*3 - 3| / \u221A9 = |1 - 4 + 6 - 3| / 3 = 0?\nTính lại: tử số = |1 - 4 + 6 - 3| = 0.\nĐể khoảng cách khác 0, đổi M(2; -1; 1) \u21D2 |2 - 2(-1) + 2(1) - 3| / 3 = |2 + 2 + 2 - 3| / 3 = 3 / 3 = 1.0. Chọn M(2;-1;1) có d = 1.0.",
            topic = "Hình học không gian",
            difficulty = "Trung bình"
        ),
        Part3Question(
            id = 10,
            content = "Tìm giá trị nhỏ nhất m của hàm số y = x\u00B2 - 4x + 5 trên đoạn [0; 3].",
            correctAnswer = 1.0,
            explanation = "y' = 2x - 4 = 0 \u21D4 x = 2.\ny(0) = 5, y(2) = 1, y(3) = 2.\nDo đó giá trị nhỏ nhất m = 1.0 tại x = 2.\nĐáp số: 1.0",
            topic = "Giải tích",
            difficulty = "Dễ"
        )
    )
}

// ==========================================
// VIEWMODEL FOR EXAM STATE MANAGEMENT
// ==========================================

class ExamViewModel(application: Application) : AndroidViewModel(application) {
    private val db = ExamDatabase.getDatabase(application)
    private val repository = ExamHistoryRepository(db.examHistoryDao())
    private val mathRepo = MathQuestionRepository(db.mathQuestionDao())
    private val prefs = application.getSharedPreferences("exam_prefs", android.content.Context.MODE_PRIVATE)

    val mathQuestionBank: StateFlow<List<MathQuestionEntity>> = mathRepo.allQuestions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun saveMathQuestion(question: MathQuestionEntity) {
        viewModelScope.launch {
            if (question.id == 0) {
                mathRepo.insert(question)
            } else {
                mathRepo.update(question)
            }
        }
    }

    fun deleteMathQuestion(id: Int) {
        viewModelScope.launch {
            mathRepo.deleteById(id)
        }
    }

    fun seedSampleMathQuestions() {
        viewModelScope.launch {
            val samples = listOf(
                MathQuestionEntity(
                    content = "Cho hàm số f(x) = x^3 - 3x^2 + 2. Cực đại của hàm số đạt tại điểm x bằng bao nhiêu?",
                    optionA = "x = 0",
                    optionB = "x = 2",
                    optionC = "x = 1",
                    optionD = "x = -1",
                    correctIndex = 0,
                    explanation = "Ta có f'(x) = 3x^2 - 6x = 3x(x - 2). f'(x) = 0 <=> x = 0 hoặc x = 2.\nf''(x) = 6x - 6.\nf''(0) = -6 < 0 => x = 0 là điểm cực đại.\nf''(2) = 6 > 0 => x = 2 là điểm cực tiểu.",
                    topic = "Giải tích",
                    tagsJson = MathQuestionEntity.tagsListToJson(listOf("Algebra", "Calculus", "Derivatives")),
                    difficulty = "Dễ"
                ),
                MathQuestionEntity(
                    content = "Tính nguyên hàm I = ∫(2x + 1) dx.",
                    optionA = "x^2 + x + C",
                    optionB = "2x^2 + x + C",
                    optionC = "x^2 + C",
                    optionD = "(x^2)/2 + x + C",
                    correctIndex = 0,
                    explanation = "Áp dụng công thức nguyên hàm cơ bản:\n∫ 2x dx = x^2\n∫ 1 dx = x\nDo đó I = x^2 + x + C.",
                    topic = "Giải tích",
                    tagsJson = MathQuestionEntity.tagsListToJson(listOf("Calculus", "Integrals")),
                    difficulty = "Dễ"
                ),
                MathQuestionEntity(
                    content = "Trong không gian Oxyz, cho mặt cầu (S): (x - 1)^2 + (y + 2)^2 + (z - 3)^2 = 16. Bán kính R của mặt cầu (S) bằng:",
                    optionA = "R = 4",
                    optionB = "R = 16",
                    optionC = "R = 2",
                    optionD = "R = 8",
                    correctIndex = 0,
                    explanation = "Phương trình mặt cầu dạng (x - a)^2 + (y - b)^2 + (z - c)^2 = R^2.\nỞ đây R^2 = 16 => R = √16 = 4.",
                    topic = "Hình học",
                    tagsJson = MathQuestionEntity.tagsListToJson(listOf("Geometry", "Vectors")),
                    difficulty = "Trung bình"
                ),
                MathQuestionEntity(
                    content = "Nghiệm của phương trình log2(x - 3) = 3 là:",
                    optionA = "x = 11",
                    optionB = "x = 9",
                    optionC = "x = 12",
                    optionD = "x = 8",
                    correctIndex = 0,
                    explanation = "Điều kiện: x - 3 > 0 <=> x > 3.\nTa có: log2(x - 3) = 3 <=> x - 3 = 2^3 = 8 <=> x = 11 (thỏa mãn).",
                    topic = "Đại số",
                    tagsJson = MathQuestionEntity.tagsListToJson(listOf("Algebra", "Logarithm")),
                    difficulty = "Dễ"
                ),
                MathQuestionEntity(
                    content = "Trong một hộp chứa 5 quả cầu đỏ và 3 quả cầu xanh. Lấy ngẫu nhiên 2 quả cầu. Tính xác suất để chọn được 2 quả cầu cùng màu.",
                    optionA = "P = 13/28",
                    optionB = "P = 15/28",
                    optionC = "P = 3/28",
                    optionD = "P = 5/14",
                    correctIndex = 0,
                    explanation = "Tổng số cách chọn 2 quả từ 8 quả: n(Ω) = C(8, 2) = 28.\nSố cách chọn 2 quả đỏ: C(5, 2) = 10.\nSố cách chọn 2 quả xanh: C(3, 2) = 3.\nSố cách chọn 2 quả cùng màu: 10 + 3 = 13.\nTrực tiếp tính xác suất P = 13/28.",
                    topic = "Tổ hợp & Xác suất",
                    tagsJson = MathQuestionEntity.tagsListToJson(listOf("Probability", "Statistics")),
                    difficulty = "Trung bình"
                ),
                MathQuestionEntity(
                    content = "Tính giá trị giới hạn L = lim (2n + 1)/(n - 3) khi n tiến tới dương vô cực.",
                    optionA = "L = 2",
                    optionB = "L = 1",
                    optionC = "L = -1/3",
                    optionD = "L = 0",
                    correctIndex = 0,
                    explanation = "Chia cả tử và mẫu cho n:\nlim (2 + 1/n) / (1 - 3/n) = (2 + 0) / (1 - 0) = 2.",
                    topic = "Giải tích",
                    tagsJson = MathQuestionEntity.tagsListToJson(listOf("Calculus", "Limits")),
                    difficulty = "Trung bình"
                )
            )
            mathRepo.insertAll(samples)
        }
    }

    fun addBankQuestionToCustomExam(bankQ: MathQuestionEntity) {
        val part1Q = Part1Question(
            id = if (bankQ.id != 0) bankQ.id else (System.currentTimeMillis() % 100000).toInt(),
            content = bankQ.content,
            originalOptions = listOf(bankQ.optionA, bankQ.optionB, bankQ.optionC, bankQ.optionD),
            correctIndex = bankQ.correctIndex,
            explanation = bankQ.explanation,
            topic = bankQ.topic,
            difficulty = bankQ.difficulty
        )
        addImportedQuestions(listOf(part1Q), emptyList(), emptyList())
    }

    private val _studentName = MutableStateFlow("")
    val studentName: StateFlow<String> = _studentName.asStateFlow()

    private val _studentClass = MutableStateFlow("")
    val studentClass: StateFlow<String> = _studentClass.asStateFlow()

    private val _rememberedProfiles = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val rememberedProfiles: StateFlow<List<Pair<String, String>>> = _rememberedProfiles.asStateFlow()

    // Document & Branding configuration
    private val _docDepartment = MutableStateFlow("SỞ GIÁO DỤC VÀ ĐÀO TẠO")
    val docDepartment: StateFlow<String> = _docDepartment.asStateFlow()

    private val _docSchool = MutableStateFlow("TRƯỜNG THPT CHUYÊN TOÁN MIXER")
    val docSchool: StateFlow<String> = _docSchool.asStateFlow()

    private val _docExamTitle = MutableStateFlow("KỲ THI THỬ TỐT NGHIỆP THPT QUỐC GIA")
    val docExamTitle: StateFlow<String> = _docExamTitle.asStateFlow()

    private val _docSubject = MutableStateFlow("Môn thi: TOÁN HỌC (Lớp 12)")
    val docSubject: StateFlow<String> = _docSubject.asStateFlow()

    private val _docYear = MutableStateFlow("NĂM HỌC 2026 - 2027")
    val docYear: StateFlow<String> = _docYear.asStateFlow()

    private val _docTimeLimit = MutableStateFlow("90")
    val docTimeLimit: StateFlow<String> = _docTimeLimit.asStateFlow()

    private val _docInstructions = MutableStateFlow("Thời gian làm bài: 90 phút (Không kể thời gian phát đề)")
    val docInstructions: StateFlow<String> = _docInstructions.asStateFlow()

    init {
        val lastSavedName = prefs.getString("last_student_name", "") ?: ""
        val lastSavedClass = prefs.getString("last_student_class", "") ?: ""
        _studentName.value = lastSavedName
        _studentClass.value = lastSavedClass
        
        _docDepartment.value = prefs.getString("doc_department", "SỞ GIÁO DỤC VÀ ĐÀO TẠO") ?: "SỞ GIÁO DỤC VÀ ĐÀO TẠO"
        _docSchool.value = prefs.getString("doc_school", "TRƯỜNG THPT CHUYÊN TOÁN MIXER") ?: "TRƯỜNG THPT CHUYÊN TOÁN MIXER"
        _docExamTitle.value = prefs.getString("doc_exam_title", "KỲ THI THỬ TỐT NGHIỆP THPT QUỐC GIA") ?: "KỲ THI THỬ TỐT NGHIỆP THPT QUỐC GIA"
        _docSubject.value = prefs.getString("doc_subject", "Môn thi: TOÁN HỌC (Lớp 12)") ?: "Môn thi: TOÁN HỌC (Lớp 12)"
        _docYear.value = prefs.getString("doc_year", "NĂM HỌC 2026 - 2027") ?: "NĂM HỌC 2026 - 2027"
        _docTimeLimit.value = prefs.getString("doc_time_limit", "90") ?: "90"
        _docInstructions.value = prefs.getString("doc_instructions", "Thời gian làm bài: 90 phút (Không kể thời gian phát đề)") ?: "Thời gian làm bài: 90 phút (Không kể thời gian phát đề)"

        loadRememberedProfiles()
        loadCustomQuestionsFromPrefs()
    }

    private fun loadRememberedProfiles() {
        val jsonStr = prefs.getString("remembered_profiles", "[]") ?: "[]"
        val list = mutableListOf<Pair<String, String>>()
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val name = obj.getString("name")
                val cls = obj.getString("class")
                if (name.isNotBlank()) {
                    list.add(name to cls)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        _rememberedProfiles.value = list
    }

    fun saveStudentProfile(name: String, cls: String) {
        if (name.isBlank() || cls.isBlank()) return
        val cleanName = name.trim()
        val cleanClass = cls.trim()

        prefs.edit()
            .putString("last_student_name", cleanName)
            .putString("last_student_class", cleanClass)
            .apply()

        val currentList = _rememberedProfiles.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.first.equals(cleanName, ignoreCase = true) }
        if (existingIndex != -1) {
            currentList.removeAt(existingIndex)
        }
        currentList.add(0, cleanName to cleanClass)

        val trimmedList = if (currentList.size > 10) currentList.take(10) else currentList

        val array = org.json.JSONArray()
        trimmedList.forEach { (n, c) ->
            val obj = org.json.JSONObject()
            obj.put("name", n)
            obj.put("class", c)
            array.put(obj)
        }
        prefs.edit().putString("remembered_profiles", array.toString()).apply()
        _rememberedProfiles.value = trimmedList
    }

    fun deleteRememberedProfile(name: String, cls: String) {
        val currentList = _rememberedProfiles.value.filterNot { it.first.equals(name, ignoreCase = true) && it.second.equals(cls, ignoreCase = true) }
        val array = org.json.JSONArray()
        currentList.forEach { (n, c) ->
            val obj = org.json.JSONObject()
            obj.put("name", n)
            obj.put("class", c)
            array.put(obj)
        }
        prefs.edit().putString("remembered_profiles", array.toString()).apply()
        _rememberedProfiles.value = currentList
    }

    val examHistory: StateFlow<List<ExamHistoryEntity>> = repository.allHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScreen = MutableStateFlow(Screen.WELCOME)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _welcomeTab = MutableStateFlow(0) // 0: Exam, 1: Practice, 2: History, 3: Import, 4: Multi-platform Guide
    val welcomeTab: StateFlow<Int> = _welcomeTab.asStateFlow()

    private val _customPart1 = MutableStateFlow<List<Part1Question>>(emptyList())
    val customPart1: StateFlow<List<Part1Question>> = _customPart1.asStateFlow()

    private val _customPart2 = MutableStateFlow<List<Part2Question>>(emptyList())
    val customPart2: StateFlow<List<Part2Question>> = _customPart2.asStateFlow()

    private val _customPart3 = MutableStateFlow<List<Part3Question>>(emptyList())
    val customPart3: StateFlow<List<Part3Question>> = _customPart3.asStateFlow()

    fun addImportedQuestions(p1: List<Part1Question>, p2: List<Part2Question>, p3: List<Part3Question>) {
        _customPart1.value = _customPart1.value + p1
        _customPart2.value = _customPart2.value + p2
        _customPart3.value = _customPart3.value + p3
        saveCustomQuestionsToPrefs()
    }

    fun clearImportedQuestions() {
        _customPart1.value = emptyList()
        _customPart2.value = emptyList()
        _customPart3.value = emptyList()
        saveCustomQuestionsToPrefs()
    }

    fun deleteCustomQuestion(part: Int, id: Int) {
        when (part) {
            1 -> _customPart1.value = _customPart1.value.filterNot { it.id == id }
            2 -> _customPart2.value = _customPart2.value.filterNot { it.id == id }
            3 -> _customPart3.value = _customPart3.value.filterNot { it.id == id }
        }
        saveCustomQuestionsToPrefs()
    }

    private fun saveCustomQuestionsToPrefs() {
        try {
            val p1Json = customPart1ListToJson(_customPart1.value)
            val p2Json = com.example.db.ExamJsonSerializer.part2QuestionsToJson(_customPart2.value)
            val p3Json = com.example.db.ExamJsonSerializer.part3QuestionsToJson(_customPart3.value)
            prefs.edit()
                .putString("custom_part1_questions", p1Json)
                .putString("custom_part2_questions", p2Json)
                .putString("custom_part3_questions", p3Json)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadCustomQuestionsFromPrefs() {
        try {
            val p1Json = prefs.getString("custom_part1_questions", "") ?: ""
            val p2Json = prefs.getString("custom_part2_questions", "") ?: ""
            val p3Json = prefs.getString("custom_part3_questions", "") ?: ""

            if (p1Json.isNotEmpty()) {
                _customPart1.value = jsonToCustomPart1List(p1Json)
            }
            if (p2Json.isNotEmpty()) {
                _customPart2.value = com.example.db.ExamJsonSerializer.jsonToPart2Questions(p2Json)
            }
            if (p3Json.isNotEmpty()) {
                _customPart3.value = com.example.db.ExamJsonSerializer.jsonToPart3Questions(p3Json)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun customPart1ListToJson(questions: List<Part1Question>): String {
        val array = org.json.JSONArray()
        questions.forEach { q ->
            val obj = org.json.JSONObject()
            obj.put("id", q.id)
            obj.put("content", q.content)
            val opts = org.json.JSONArray()
            q.originalOptions.forEach { opts.put(it) }
            obj.put("originalOptions", opts)
            obj.put("correctIndex", q.correctIndex)
            obj.put("explanation", q.explanation)
            obj.put("topic", q.topic)
            obj.put("difficulty", q.difficulty)
            array.put(obj)
        }
        return array.toString()
    }

    private fun jsonToCustomPart1List(jsonStr: String): List<Part1Question> {
        val list = mutableListOf<Part1Question>()
        if (jsonStr.isEmpty()) return list
        try {
            val array = org.json.JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val optsArray = obj.getJSONArray("originalOptions")
                val opts = mutableListOf<String>()
                for (j in 0 until optsArray.length()) {
                    opts.add(optsArray.getString(j))
                }
                list.add(
                    Part1Question(
                        id = obj.getInt("id"),
                        content = obj.getString("content"),
                        originalOptions = opts,
                        correctIndex = obj.getInt("correctIndex"),
                        explanation = obj.optString("explanation", "Chưa có lời giải chi tiết."),
                        topic = obj.optString("topic", "Giải tích"),
                        difficulty = obj.optString("difficulty", "Dễ")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private val _randomizeQuestions = MutableStateFlow(prefs.getBoolean("randomize_questions", true))
    val randomizeQuestions: StateFlow<Boolean> = _randomizeQuestions.asStateFlow()

    private val _randomizeOptions = MutableStateFlow(prefs.getBoolean("randomize_options", true))
    val randomizeOptions: StateFlow<Boolean> = _randomizeOptions.asStateFlow()

    fun toggleRandomizeQuestions() {
        _randomizeQuestions.value = !_randomizeQuestions.value
        prefs.edit().putBoolean("randomize_questions", _randomizeQuestions.value).apply()
    }

    fun toggleRandomizeOptions() {
        _randomizeOptions.value = !_randomizeOptions.value
        prefs.edit().putBoolean("randomize_options", _randomizeOptions.value).apply()
    }

    private val _selectedTopic = MutableStateFlow("Tất cả")
    val selectedTopic: StateFlow<String> = _selectedTopic.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow("Tất cả")
    val selectedDifficulty: StateFlow<String> = _selectedDifficulty.asStateFlow()

    private val _examSession = MutableStateFlow<ExamSession?>(null)
    val examSession: StateFlow<ExamSession?> = _examSession.asStateFlow()

    fun updateWelcomeTab(tab: Int) {
        _welcomeTab.value = tab
    }

    fun updateStudentName(name: String) {
        _studentName.value = name
    }

    fun updateStudentClass(cls: String) {
        _studentClass.value = cls
    }

    fun updateDocConfig(
        department: String,
        school: String,
        examTitle: String,
        subject: String,
        year: String,
        timeLimit: String,
        instructions: String
    ) {
        _docDepartment.value = department
        _docSchool.value = school
        _docExamTitle.value = examTitle
        _docSubject.value = subject
        _docYear.value = year
        _docTimeLimit.value = timeLimit
        _docInstructions.value = instructions

        prefs.edit()
            .putString("doc_department", department)
            .putString("doc_school", school)
            .putString("doc_exam_title", examTitle)
            .putString("doc_subject", subject)
            .putString("doc_year", year)
            .putString("doc_time_limit", timeLimit)
            .putString("doc_instructions", instructions)
            .apply()
    }

    fun updateSelectedTopic(topic: String) {
        _selectedTopic.value = topic
    }

    fun updateSelectedDifficulty(difficulty: String) {
        _selectedDifficulty.value = difficulty
    }

    fun getDifficultyComposition(topic: String, diff: String): Map<String, Int> {
        val rand = kotlin.random.Random(42)
        val p1Source = MathQuestionBank.part1Bank + _customPart1.value
        val p2Source = MathQuestionBank.part2Bank + _customPart2.value
        val p3Source = MathQuestionBank.part3Bank + _customPart3.value

        val p1 = selectQuestionsForPart(p1Source, topic, diff, 12, rand, { it.difficulty }, { it.topic })
        val p2 = selectQuestionsForPart(p2Source, topic, diff, 4, rand, { it.difficulty }, { it.topic })
        val p3 = selectQuestionsForPart(p3Source, topic, diff, 6, rand, { it.difficulty }, { it.topic })

        val allDiffs = p1.map { it.difficulty } + p2.map { it.difficulty } + p3.map { it.difficulty }
        val easy = allDiffs.count { it == "Dễ" }
        val medium = allDiffs.count { it == "Trung bình" }
        val hard = allDiffs.count { it == "Khó" }
        return mapOf("Dễ" to easy, "Trung bình" to medium, "Khó" to hard)
    }

    private fun <T> selectQuestionsForPart(
        source: List<T>,
        topic: String,
        diff: String,
        totalNeeded: Int,
        rand: Random,
        getDifficulty: (T) -> String,
        getTopic: (T) -> String
    ): List<T> {
        val (easyNeeded, mediumNeeded, hardNeeded) = when (diff) {
            "Mức đạt 5-6 điểm" -> when (totalNeeded) {
                12 -> Triple(10, 2, 0)
                4  -> Triple(3, 1, 0)
                6  -> Triple(5, 1, 0)
                else -> Triple(totalNeeded, 0, 0)
            }
            "Mức đạt 6-7 điểm" -> when (totalNeeded) {
                12 -> Triple(7, 4, 1)
                4  -> Triple(2, 2, 0)
                6  -> Triple(3, 2, 1)
                else -> Triple(totalNeeded / 2, totalNeeded / 2, 0)
            }
            "Mức đạt 7-8 điểm" -> when (totalNeeded) {
                12 -> Triple(3, 7, 2)
                4  -> Triple(1, 2, 1)
                6  -> Triple(1, 4, 1)
                else -> Triple(0, totalNeeded, 0)
            }
            "Mức đạt 8-9 điểm" -> when (totalNeeded) {
                12 -> Triple(1, 5, 6)
                4  -> Triple(0, 2, 2)
                6  -> Triple(0, 2, 4)
                else -> Triple(0, totalNeeded / 2, totalNeeded / 2)
            }
            "Mức đạt 9-10 điểm" -> when (totalNeeded) {
                12 -> Triple(0, 2, 10)
                4  -> Triple(0, 1, 3)
                6  -> Triple(0, 1, 5)
                else -> Triple(0, 0, totalNeeded)
            }
            else -> {
                // For standard single difficulties ("Dễ", "Trung bình", "Khó") or "Tất cả"
                val filtered = source.filter {
                    (topic == "Tất cả" || getTopic(it) == topic) &&
                    (diff == "Tất cả" || getDifficulty(it) == diff)
                }
                if (filtered.size >= totalNeeded) {
                    return if (_randomizeQuestions.value) filtered.shuffled(rand).take(totalNeeded) else filtered.take(totalNeeded)
                } else {
                    val remaining = source.filter { !filtered.contains(it) }.let { if (_randomizeQuestions.value) it.shuffled(rand) else it }
                    return (filtered + remaining).take(totalNeeded)
                }
            }
        }

        // 1. Filter by topic
        val topicFiltered = if (topic == "Tất cả") source else source.filter { getTopic(it) == topic }

        // 2. Separate into difficulty pools
        val easyPool = topicFiltered.filter { getDifficulty(it) == "Dễ" }.let { if (_randomizeQuestions.value) it.shuffled(rand) else it }.toMutableList()
        val mediumPool = topicFiltered.filter { getDifficulty(it) == "Trung bình" }.let { if (_randomizeQuestions.value) it.shuffled(rand) else it }.toMutableList()
        val hardPool = topicFiltered.filter { getDifficulty(it) == "Khó" }.let { if (_randomizeQuestions.value) it.shuffled(rand) else it }.toMutableList()

        val selected = mutableListOf<T>()

        // Take from Easy pool
        val takenEasy = easyPool.take(easyNeeded)
        selected.addAll(takenEasy)
        easyPool.removeAll(takenEasy)

        // Take from Medium pool
        val takenMedium = mediumPool.take(mediumNeeded)
        selected.addAll(takenMedium)
        mediumPool.removeAll(takenMedium)

        // Take from Hard pool
        val takenHard = hardPool.take(hardNeeded)
        selected.addAll(takenHard)
        hardPool.removeAll(takenHard)

        // Backfill from remaining topic-filtered pool
        val remainingInTopic = (easyPool + mediumPool + hardPool).let { if (_randomizeQuestions.value) it.shuffled(rand) else it }.toMutableList()
        if (selected.size < totalNeeded && remainingInTopic.isNotEmpty()) {
            val needed = totalNeeded - selected.size
            val taken = remainingInTopic.take(needed)
            selected.addAll(taken)
            remainingInTopic.removeAll(taken)
        }

        // Backfill from any other questions in the source pool to reach totalNeeded
        if (selected.size < totalNeeded) {
            val nonSelectedAll = source.filter { !selected.contains(it) }.let { if (_randomizeQuestions.value) it.shuffled(rand) else it }
            val needed = totalNeeded - selected.size
            selected.addAll(nonSelectedAll.take(needed))
        }

        val result = selected.take(totalNeeded)
        return if (_randomizeQuestions.value) result.shuffled(rand) else result
    }

    // Shuffle and generate a fresh custom/practice exam paper
    fun generateMixedExam(isPractice: Boolean) {
        val topic = _selectedTopic.value
        val diff = _selectedDifficulty.value
        val rand = Random(System.currentTimeMillis())

        val p1Source = MathQuestionBank.part1Bank + _customPart1.value
        val p2Source = MathQuestionBank.part2Bank + _customPart2.value
        val p3Source = MathQuestionBank.part3Bank + _customPart3.value

        // 1. Part 1 questions using target range selector
        val p1Final = selectQuestionsForPart(
            source = p1Source,
            topic = topic,
            diff = diff,
            totalNeeded = 12,
            rand = rand,
            getDifficulty = { it.difficulty },
            getTopic = { it.topic }
        )

        val p1Shuffled = p1Final.map { q ->
            if (_randomizeOptions.value) {
                val optionsWithIndex = q.originalOptions.mapIndexed { index, option -> option to index }
                val shuffledOptionsWithIndex = optionsWithIndex.shuffled(rand)
                val shuffledOptions = shuffledOptionsWithIndex.map { it.first }
                val newCorrectIndex = shuffledOptionsWithIndex.indexOfFirst { it.second == q.correctIndex }
                ShuffledPart1Question(
                    question = q,
                    options = shuffledOptions,
                    correctIndex = if (newCorrectIndex != -1) newCorrectIndex else q.correctIndex
                )
            } else {
                ShuffledPart1Question(
                    question = q,
                    options = q.originalOptions,
                    correctIndex = q.correctIndex
                )
            }
        }

        // 2. Part 2 questions using target range selector
        val p2Final = selectQuestionsForPart(
            source = p2Source,
            topic = topic,
            diff = diff,
            totalNeeded = 4,
            rand = rand,
            getDifficulty = { it.difficulty },
            getTopic = { it.topic }
        )

        // 3. Part 3 questions using target range selector
        val p3Final = selectQuestionsForPart(
            source = p3Source,
            topic = topic,
            diff = diff,
            totalNeeded = 6,
            rand = rand,
            getDifficulty = { it.difficulty },
            getTopic = { it.topic }
        )

        val examCodeInt = rand.nextInt(101, 999)
        val examCodeStr = if (isPractice) "Đề Luyện tập #$examCodeInt" else "Mã đề $examCodeInt"

        val sName = if (isPractice) {
            if (_studentName.value.isBlank()) "Học sinh luyện tập" else _studentName.value
        } else {
            if (_studentName.value.isBlank()) "Thí sinh tự do" else _studentName.value
        }
        val sClass = if (isPractice) {
            if (_studentClass.value.isBlank()) "Lớp tự học" else _studentClass.value
        } else {
            if (_studentClass.value.isBlank()) "12A" else _studentClass.value
        }

        _examSession.value = ExamSession(
            studentName = sName,
            studentClass = sClass,
            examCode = examCodeStr,
            part1Questions = p1Shuffled,
            part2Questions = p2Final,
            part3Questions = p3Final,
            part1Answers = emptyMap(),
            part2Answers = p2Final.associate { it.id to listOf<Boolean?>(null, null, null, null) },
            part3Answers = emptyMap(),
            timeElapsedSeconds = 0,
            isPracticeMode = isPractice
        )
        if (_studentName.value.isNotBlank() && _studentClass.value.isNotBlank()) {
            saveStudentProfile(_studentName.value, _studentClass.value)
        }
        _currentScreen.value = Screen.EXAM
    }

    // Set answers
    fun setPart1Answer(qIndex: Int, optionIndex: Int) {
        val session = _examSession.value ?: return
        if (session.part1Checked[qIndex] == true) return // Lock when checked
        val updatedAnswers = session.part1Answers.toMutableMap()
        updatedAnswers[qIndex] = optionIndex
        _examSession.value = session.copy(part1Answers = updatedAnswers)
    }

    fun setPart2Answer(qId: Int, subIndex: Int, answer: Boolean) {
        val session = _examSession.value ?: return
        if (session.part2Checked[qId] == true) return // Lock when checked
        val updatedAnswers = session.part2Answers.toMutableMap()
        val currentAnswers = (updatedAnswers[qId] ?: listOf(null, null, null, null)).toMutableList()
        currentAnswers[subIndex] = answer
        updatedAnswers[qId] = currentAnswers
        _examSession.value = session.copy(part2Answers = updatedAnswers)
    }

    fun setPart3Answer(qIndex: Int, textAnswer: String) {
        val session = _examSession.value ?: return
        if (session.part3Checked[qIndex] == true) return // Lock when checked
        val updatedAnswers = session.part3Answers.toMutableMap()
        updatedAnswers[qIndex] = textAnswer
        _examSession.value = session.copy(part3Answers = updatedAnswers)
    }

    // Practice mode instant verification
    fun checkPart1Question(index: Int) {
        val session = _examSession.value ?: return
        val updatedChecked = session.part1Checked.toMutableMap()
        updatedChecked[index] = true
        _examSession.value = session.copy(part1Checked = updatedChecked)
    }

    fun checkPart2Question(qId: Int) {
        val session = _examSession.value ?: return
        val updatedChecked = session.part2Checked.toMutableMap()
        updatedChecked[qId] = true
        _examSession.value = session.copy(part2Checked = updatedChecked)
    }

    fun checkPart3Question(index: Int) {
        val session = _examSession.value ?: return
        val updatedChecked = session.part3Checked.toMutableMap()
        updatedChecked[index] = true
        _examSession.value = session.copy(part3Checked = updatedChecked)
    }

    // Increment Timer
    fun incrementTimer() {
        val session = _examSession.value ?: return
        if (session.isPracticeMode) return // No elapsed timer ticks in practice setup
        _examSession.value = session.copy(timeElapsedSeconds = session.timeElapsedSeconds + 1)
    }

    // Calculate Scores and Save to Room History
    fun submitAndGrade() {
        val session = _examSession.value ?: return

        // Part 1 Grading: 12 questions, 0.25 points each
        var p1Score = 0.0
        session.part1Questions.forEachIndexed { index, q ->
            val chosen = session.part1Answers[index]
            if (chosen != null && chosen == q.correctIndex) {
                p1Score += 0.25
            }
        }

        // Part 2 Grading: 4 questions.
        // Rule: 1 correct sub-item -> 0.125, 2 -> 0.25, 3 -> 0.5, 4 -> 1.0
        var p2Score = 0.0
        session.part2Questions.forEach { q ->
            val studentAnswers = session.part2Answers[q.id] ?: listOf(null, null, null, null)
            val correctAnswers = q.subQuestions.map { it.isTrue }
            
            var correctCount = 0
            for (i in 0 until 4) {
                if (studentAnswers[i] != null && studentAnswers[i] == correctAnswers[i]) {
                    correctCount++
                }
            }

            val points = when (correctCount) {
                1 -> 0.125
                2 -> 0.25
                3 -> 0.5
                4 -> 1.0
                else -> 0.0
            }
            p2Score += points
        }

        // Part 3 Grading: 6 questions, 0.5 points each
        var p3Score = 0.0
        session.part3Questions.forEachIndexed { index, q ->
            val typed = session.part3Answers[index] ?: ""
            if (checkPart3Answer(typed, q.correctAnswer)) {
                p3Score += 0.5
            }
        }

        val total = p1Score + p2Score + p3Score
        val finalSession = session.copy(
            totalPoints = (total * 1000).roundToInt() / 1000.0,
            p1Points = p1Score,
            p2Points = p2Score,
            p3Points = p3Score
        )
        _examSession.value = finalSession

        // Save session history to Room DB on Submit
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val entity = ExamHistoryEntity(
                    studentName = finalSession.studentName,
                    studentClass = finalSession.studentClass,
                    examCode = finalSession.examCode,
                    totalPoints = finalSession.totalPoints,
                    p1Points = finalSession.p1Points,
                    p2Points = finalSession.p2Points,
                    p3Points = finalSession.p3Points,
                    timeElapsedSeconds = finalSession.timeElapsedSeconds,
                    timestamp = System.currentTimeMillis(),
                    isPracticeMode = finalSession.isPracticeMode,
                    part1QuestionsJson = ExamJsonSerializer.part1QuestionsToJson(finalSession.part1Questions),
                    part2QuestionsJson = ExamJsonSerializer.part2QuestionsToJson(finalSession.part2Questions),
                    part3QuestionsJson = ExamJsonSerializer.part3QuestionsToJson(finalSession.part3Questions),
                    part1AnswersJson = ExamJsonSerializer.part1AnswersToJson(finalSession.part1Answers),
                    part2AnswersJson = ExamJsonSerializer.part2AnswersToJson(finalSession.part2Answers),
                    part3AnswersJson = ExamJsonSerializer.part3AnswersToJson(finalSession.part3Answers)
                )
                repository.insert(entity)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        _currentScreen.value = Screen.RESULT
    }

    private fun checkPart3Answer(typed: String, correct: Double): Boolean {
        val parsed = typed.trim().replace(',', '.').toDoubleOrNull() ?: return false
        val roundedParsed = (parsed * 1000).roundToInt() / 1000.0
        val roundedCorrect = (correct * 1000).roundToInt() / 1000.0
        return Math.abs(roundedParsed - roundedCorrect) < 0.001
    }

    // Load History item for Review Mode
    fun loadHistorySession(entity: ExamHistoryEntity) {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val part1Questions = ExamJsonSerializer.jsonToPart1Questions(entity.part1QuestionsJson)
                val part2Questions = ExamJsonSerializer.jsonToPart2Questions(entity.part2QuestionsJson)
                val part3Questions = ExamJsonSerializer.jsonToPart3Questions(entity.part3QuestionsJson)
                
                val part1Answers = ExamJsonSerializer.jsonToPart1Answers(entity.part1AnswersJson)
                val part2Answers = ExamJsonSerializer.jsonToPart2Answers(entity.part2AnswersJson)
                val part3Answers = ExamJsonSerializer.jsonToPart3Answers(entity.part3AnswersJson)

                _examSession.value = ExamSession(
                    studentName = entity.studentName,
                    studentClass = entity.studentClass,
                    examCode = entity.examCode,
                    part1Questions = part1Questions,
                    part2Questions = part2Questions,
                    part3Questions = part3Questions,
                    part1Answers = part1Answers,
                    part2Answers = part2Answers,
                    part3Answers = part3Answers,
                    timeElapsedSeconds = entity.timeElapsedSeconds,
                    totalPoints = entity.totalPoints,
                    p1Points = entity.p1Points,
                    p2Points = entity.p2Points,
                    p3Points = entity.p3Points,
                    isPracticeMode = entity.isPracticeMode,
                    isReviewMode = true
                )
                _currentScreen.value = Screen.RESULT
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Delete a specific history entry
    fun deleteHistory(entity: ExamHistoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(entity.id)
        }
    }

    private val _aiExplanations = MutableStateFlow<Map<String, String>>(emptyMap())
    val aiExplanations: StateFlow<Map<String, String>> = _aiExplanations.asStateFlow()

    private val _aiLoadingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val aiLoadingStates: StateFlow<Map<String, Boolean>> = _aiLoadingStates.asStateFlow()

    fun cleanAiExplanation(text: String): String {
        var cleaned = text.trim()
        
        // 1. Strip common introductory preambles and general greetings
        val preambles = listOf(
            "Chào bạn",
            "Dưới đây là",
            "Sau đây là",
            "Lời giải chi tiết",
            "Giải thích chi tiết",
            "Tôi xin giải thích",
            "Thầy cô xin giải thích",
            "Thầy/cô xin giải thích",
            "Câu hỏi này",
            "Để giải quyết câu hỏi này",
            "Để giải bài toán này",
            "Chúng ta có thể giải",
            "Bài toán yêu cầu",
            "Ta có câu hỏi"
        )
        
        val lines = cleaned.split("\n")
        var startIndex = 0
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue
            
            val isPreamble = preambles.any { line.startsWith(it, ignoreCase = true) }
            if (isPreamble) {
                startIndex = i + 1
                continue
            }
            break
        }
        
        if (startIndex < lines.size) {
            cleaned = lines.subList(startIndex, lines.size).joinToString("\n").trim()
        }
        
        // 2. Post-process LaTeX expressions to Equation / MathType style
        // Replace LaTeX \frac{a}{b} with (a)/(b)
        val fracRegex = Regex("""\\frac\s*\{([^}]+)\}\s*\{([^}]+)\}""")
        cleaned = cleaned.replace(fracRegex) { matchResult ->
            val num = matchResult.groupValues[1]
            val den = matchResult.groupValues[2]
            "($num)/($den)"
        }
        
        // Replace standard LaTeX symbols with MathType Unicode equivalents
        cleaned = cleaned
            .replace(Regex("""\\sqrt\{([^}]+)\}"""), "√($1)")
            .replace("""\sqrt""", "√")
            .replace("""\pi""", "π")
            .replace("""\infty""", "∞")
            .replace("""\in""", "∈")
            .replace("""\cdot""", " . ")
            .replace("""\times""", " x ")
            .replace("""\pm""", "±")
            .replace("""\degree""", "°")
            .replace("""\circ""", "°")
            .replace("""\ge""", "≥")
            .replace("""\le""", "≤")
            .replace("""\Delta""", "Δ")
            .replace("""\theta""", "θ")
            .replace("""\alpha""", "α")
            .replace("""\beta""", "β")
            .replace("""\gamma""", "γ")
            .replace(Regex("""\$\$?([^\$]+)\$\$?"""), "$1") // Strip any remaining $$ and $ wrappers
            
        return cleaned.trim()
    }

    fun fetchAiExplanation(key: String, questionText: String, correctAnswer: String, optionsText: String? = null) {
        if (_aiLoadingStates.value[key] == true) return
        _aiLoadingStates.value = _aiLoadingStates.value + (key to true)
        _aiExplanations.value = _aiExplanations.value + (key to "Đang tải giải thích từ Gemini AI...")

        viewModelScope.launch(Dispatchers.IO) {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                _aiExplanations.value = _aiExplanations.value + (key to "Chưa cấu hình API Key. Vui lòng cấu hình GEMINI_API_KEY trong AI Studio Secrets panel.")
                _aiLoadingStates.value = _aiLoadingStates.value + (key to false)
                return@launch
            }

            val prompt = """
                Bạn là một giáo viên dạy toán chuyên nghiệp bậc THPT. Hãy giải thích chi tiết, dễ hiểu và chính xác tại sao câu trả lời sau đây là đúng cho câu hỏi toán học này. Trả lời bằng tiếng Việt.
                
                Yêu cầu bắt buộc về định dạng và trình bày:
                1. TUYỆT ĐỐI KHÔNG viết bất kỳ lời giới thiệu, lời chào mừng hay đoạn mở đầu nào (ví dụ: Không dùng "Chào bạn...", "Dưới đây là...", "Thầy cô xin giải thích..."). Hãy bắt đầu ngay lập tức bằng bước giải đầu tiên hoặc nội dung phân tích toán học.
                2. Lời giải phải được chia rõ ràng thành từng bước giải (ví dụ: Bước 1, Bước 2, Bước 3,...) một cách có hệ thống, căn lề trái. Mỗi bước bắt đầu bằng một dòng riêng có tiêu đề Bước X.
                3. Sử dụng ký tự xuống dòng (\n) rõ rệt để phân tách các bước giải và các ý suy luận để giao diện hiển thị rành mạch.
                4. CÁC CÔNG THỨC TOÁN HỌC KHÔNG ĐƯỢC VIẾT DẠNG LATEX (TUYỆT ĐỐI KHÔNG dùng \frac, \sqrt, \pi, \infty, \in, \cdot, \times, \Delta, \pm, \degree, v.v.).
                5. Hãy viết công thức theo chuẩn Equation của MathType, sử dụng các ký hiệu unicode trực quan:
                   - Phân số/phân thức: Sử dụng dấu gạch chéo kết hợp ngoặc đơn dạng `(tử_số)/(mẫu_số)` (ví dụ: `(1)/(3)`, `(2x - 1)/(x - 1)`). Hệ thống sẽ tự động vẽ tử số nằm trên, mẫu số nằm dưới và đường kẻ ngang ở giữa.
                   - Căn thức: dùng ký tự √ như `√(x)` hoặc `√(2x - 1)`.
                   - Số mũ: dùng ký tự ^ như `x^2`, `y^3`.
                   - Ký hiệu khác: dùng ký tự unicode như π, ∈, ∞, ±, °, ≥, ≤, . (dấu chấm cho phép nhân).
                
                Câu hỏi: $questionText
                ${if (optionsText != null) "Các phương án lựa chọn:\n$optionsText" else ""}
                Đáp án đúng: $correctAnswer
                
                Hãy viết lời giải thích đầy đủ các bước lập luận và kết quả chính xác theo đúng các yêu cầu trên.
            """.trimIndent()

            val jsonBody = org.json.JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(org.json.JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(org.json.JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)

            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val availableModels = listOf("gemini-3.5-flash", "gemini-flash-latest", "gemini-3.1-pro-preview")
            var attempt = 0
            val maxAttempts = 4
            var delayMs = 3000L
            var success = false

            while (attempt < maxAttempts && !success) {
                attempt++
                val currentModel = availableModels[(attempt - 1) % availableModels.size]
                val url = "https://generativelanguage.googleapis.com/v1beta/models/$currentModel:generateContent?key=$apiKey"

                val request = okhttp3.Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build()

                try {
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val responseString = response.body?.string() ?: ""
                            if (responseString.isNotBlank()) {
                                val jsonResponse = org.json.JSONObject(responseString)
                                val candidates = jsonResponse.optJSONArray("candidates")
                                val firstCandidate = candidates?.optJSONObject(0)
                                val content = firstCandidate?.optJSONObject("content")
                                val parts = content?.optJSONArray("parts")
                                val firstPart = parts?.optJSONObject(0)
                                val textResult = firstPart?.optString("text") ?: "Không thể trích xuất lời giải thích từ phản hồi."
                                val finalResult = cleanAiExplanation(textResult)
                                _aiExplanations.value = _aiExplanations.value + (key to finalResult)
                                success = true
                            } else {
                                if (attempt == maxAttempts) {
                                    _aiExplanations.value = _aiExplanations.value + (key to "Không nhận được phản hồi từ AI.")
                                }
                            }
                        } else {
                            val code = response.code
                            if ((code == 429 || code == 503 || code == 504 || code == 500) && attempt < maxAttempts) {
                                kotlinx.coroutines.delay(delayMs)
                                delayMs = (delayMs * 1.5).toLong()
                            } else {
                                _aiExplanations.value = _aiExplanations.value + (key to "Lỗi kết nối Gemini API (Code $code).")
                                success = true // stop trying
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (attempt < maxAttempts) {
                        kotlinx.coroutines.delay(delayMs)
                        delayMs = (delayMs * 1.5).toLong()
                    } else {
                        _aiExplanations.value = _aiExplanations.value + (key to "Lỗi gọi API: ${e.message}")
                    }
                }
            }
            _aiLoadingStates.value = _aiLoadingStates.value + (key to false)
        }
    }

    // Reset back to Welcome Setup
    fun resetExam() {
        _currentScreen.value = Screen.WELCOME
        _aiExplanations.value = emptyMap()
        _aiLoadingStates.value = emptyMap()
    }
}

// ==========================================
// MAIN ACTIVITY
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("main_scaffold")
                ) { innerPadding ->
                    ExamApp(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// ==========================================
// MATHEMATICAL FRACTION AND TEXT RENDERER (MOVED TO MathText.kt)
// ==========================================

@Composable
fun ExamApp(
    modifier: Modifier = Modifier,
    viewModel: ExamViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val studentName by viewModel.studentName.collectAsState()
    val studentClass by viewModel.studentClass.collectAsState()
    val examSession by viewModel.examSession.collectAsState()

    // Timer effect
    LaunchedEffect(currentScreen) {
        if (currentScreen == Screen.EXAM) {
            while (true) {
                delay(1000)
                viewModel.incrementTimer()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
    ) {
        Crossfade(
            targetState = currentScreen,
            animationSpec = spring(),
            label = "screen_transition"
        ) { screen ->
            when (screen) {
                Screen.WELCOME -> WelcomeScreen(
                    studentName = studentName,
                    studentClass = studentClass,
                    onNameChange = { viewModel.updateStudentName(it) },
                    onClassChange = { viewModel.updateStudentClass(it) },
                    viewModel = viewModel
                )
                Screen.EXAM -> ExamScreen(
                    session = examSession ?: return@Crossfade,
                    onPart1Answer = { index, opt -> viewModel.setPart1Answer(index, opt) },
                    onPart2Answer = { qId, subIdx, ans -> viewModel.setPart2Answer(qId, subIdx, ans) },
                    onPart3Answer = { index, text -> viewModel.setPart3Answer(index, text) },
                    onSubmit = { viewModel.submitAndGrade() },
                    viewModel = viewModel
                )
                Screen.RESULT -> ResultScreen(
                    session = examSession ?: return@Crossfade,
                    onReset = { viewModel.resetExam() },
                    viewModel = viewModel
                )
            }
        }
    }
}

// ==========================================
// SCREEN 1: WELCOME SCREEN (TAB CONTAINER)
// ==========================================

@Composable
fun WelcomeScreen(
    studentName: String,
    studentClass: String,
    onNameChange: (String) -> Unit,
    onClassChange: (String) -> Unit,
    viewModel: ExamViewModel
) {
    val welcomeTab by viewModel.welcomeTab.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
    val historyList by viewModel.examHistory.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = welcomeTab == 0,
                    onClick = { viewModel.updateWelcomeTab(0) },
                    icon = { Icon(Icons.Default.Quiz, contentDescription = "Trộn đề") },
                    label = { Text("Trộn đề thi", fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = welcomeTab == 1,
                    onClick = { viewModel.updateWelcomeTab(1) },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = "Luyện tập") },
                    label = { Text("Luyện tập", fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = welcomeTab == 2,
                    onClick = { viewModel.updateWelcomeTab(2) },
                    icon = { Icon(Icons.Default.History, contentDescription = "Lịch sử") },
                    label = { Text("Lịch sử", fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = welcomeTab == 3,
                    onClick = { viewModel.updateWelcomeTab(3) },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = "Tải đề lên") },
                    label = { Text("Tải đề lên", fontWeight = FontWeight.SemiBold) }
                )
                NavigationBarItem(
                    selected = welcomeTab == 4,
                    onClick = { viewModel.updateWelcomeTab(4) },
                    icon = { Icon(Icons.Default.Devices, contentDescription = "Đa nền tảng") },
                    label = { Text("Đa nền tảng", fontWeight = FontWeight.SemiBold) }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // MindUp Brand Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.img_mindup_icon),
                        contentDescription = "MindUp Logo",
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .shadow(2.dp, RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "MindUp",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = (-0.5).sp
                        )
                    )
                }
                
                Text(
                    text = "PHẦN MỀM TRỘN ĐỀ THI TOÁN 12",
                    style = MaterialTheme.typography.titleSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    ),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Nâng tầm tư duy học sinh Việt",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Tác giả: Trịnh Như Quỳnh • Kỳ thi tốt nghiệp THPT 2027",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (welcomeTab) {
                0 -> ExamSetupTab(
                    studentName = studentName,
                    studentClass = studentClass,
                    onNameChange = onNameChange,
                    onClassChange = onClassChange,
                    selectedTopic = selectedTopic,
                    selectedDifficulty = selectedDifficulty,
                    onTopicChange = { viewModel.updateSelectedTopic(it) },
                    onDifficultyChange = { viewModel.updateSelectedDifficulty(it) },
                    onStart = { viewModel.generateMixedExam(isPractice = false) },
                    viewModel = viewModel
                )
                1 -> PracticeSetupTab(
                    studentName = studentName,
                    studentClass = studentClass,
                    onNameChange = onNameChange,
                    onClassChange = onClassChange,
                    selectedTopic = selectedTopic,
                    selectedDifficulty = selectedDifficulty,
                    onTopicChange = { viewModel.updateSelectedTopic(it) },
                    onDifficultyChange = { viewModel.updateSelectedDifficulty(it) },
                    onStart = { viewModel.generateMixedExam(isPractice = true) },
                    viewModel = viewModel
                )
                2 -> HistoryListTab(
                    historyList = historyList,
                    onViewDetails = { viewModel.loadHistorySession(it) },
                    onDelete = { viewModel.deleteHistory(it) }
                )
                3 -> ExamImportTab(
                    viewModel = viewModel
                )
                4 -> MultiPlatformGuideTab()
            }
        }
    }
}

// ==========================================
// TAB 1: EXAM SETUP TAB
// ==========================================

@Composable
fun ExamSetupTab(
    studentName: String,
    studentClass: String,
    onNameChange: (String) -> Unit,
    onClassChange: (String) -> Unit,
    selectedTopic: String,
    selectedDifficulty: String,
    onTopicChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onStart: () -> Unit,
    viewModel: ExamViewModel
) {
    var nameError by remember { mutableStateOf(false) }
    var classError by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.img_mindup_hero),
                        contentDescription = "MindUp banner",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                )
                            )
                    )
                    Text(
                        text = "Chế độ: Thi thử tốt nghiệp 2027",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Thông Tin Thí Sinh",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    OutlinedTextField(
                        value = studentName,
                        onValueChange = {
                            onNameChange(it)
                            if (it.isNotBlank()) nameError = false
                        },
                        label = { Text("Họ và tên học sinh") },
                        placeholder = { Text("Nhập họ và tên...") },
                        isError = nameError,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("name_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )
                    if (nameError) {
                        Text(
                            text = "Vui lòng nhập đầy đủ họ tên học sinh",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = studentClass,
                        onValueChange = {
                            onClassChange(it)
                            if (it.isNotBlank()) classError = false
                        },
                        label = { Text("Lớp học") },
                        placeholder = { Text("Ví dụ: 12A1, 12A2...") },
                        isError = classError,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("class_input"),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                    )
                    if (classError) {
                        Text(
                            text = "Vui lòng nhập lớp học",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    RememberedProfilesView(viewModel = viewModel) { name, cls ->
                        onNameChange(name)
                        onClassChange(cls)
                        nameError = false
                        classError = false
                    }
                }
            }
        }

        item {
            DocumentConfigCard(viewModel = viewModel)
        }

        item {
            CustomizationCard(
                selectedTopic = selectedTopic,
                selectedDifficulty = selectedDifficulty,
                onTopicChange = onTopicChange,
                onDifficultyChange = onDifficultyChange,
                viewModel = viewModel
            )
        }

        item {
            val composition = viewModel.getDifficultyComposition(selectedTopic, selectedDifficulty)
            DifficultyPieChart(composition = composition)
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Text(
                            text = "Cấu Trúc Đề Trộn",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    StructureRow(partName = "Phần I", desc = "12 câu hỏi trắc nghiệm 4 đáp án (0.25đ / câu)", score = "Tối đa: 3.0đ")
                    StructureRow(partName = "Phần II", desc = "4 câu hỏi đúng sai, chấm lũy tiến nhân đôi (Tối đa 1.0đ / câu)", score = "Tối đa: 4.0đ")
                    StructureRow(partName = "Phần III", desc = "6 câu hỏi trả lời ngắn, điền kết quả số thập phân (0.5đ / câu)", score = "Tối đa: 3.0đ")
                }
            }
        }

        item {
            Button(
                onClick = {
                    val isNameEmpty = studentName.isBlank()
                    val isClassEmpty = studentClass.isBlank()
                    nameError = isNameEmpty
                    classError = isClassEmpty
                    if (!isNameEmpty && !isClassEmpty) {
                        onStart()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_exam_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Quiz, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "TRỘN ĐỀ VÀ BẮT ĐẦU THI",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// TAB 2: PRACTICE SETUP TAB
// ==========================================

@Composable
fun PracticeSetupTab(
    studentName: String,
    studentClass: String,
    onNameChange: (String) -> Unit,
    onClassChange: (String) -> Unit,
    selectedTopic: String,
    selectedDifficulty: String,
    onTopicChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    onStart: () -> Unit,
    viewModel: ExamViewModel
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "CHẾ ĐỘ TỰ LUYỆN TẬP",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Nhận phản hồi ngay lập tức, xem giải thích chi tiết trực tiếp sau khi chọn đáp án của mỗi câu.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Tùy Chọn Danh Xưng (Không bắt buộc)",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )

                    OutlinedTextField(
                        value = studentName,
                        onValueChange = onNameChange,
                        label = { Text("Họ và tên / Biệt danh") },
                        placeholder = { Text("Mặc định: Học sinh luyện tập") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = studentClass,
                        onValueChange = onClassChange,
                        label = { Text("Lớp học") },
                        placeholder = { Text("Mặc định: Lớp tự học") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.School, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    RememberedProfilesView(viewModel = viewModel) { name, cls ->
                        onNameChange(name)
                        onClassChange(cls)
                    }
                }
            }
        }

        item {
            DocumentConfigCard(viewModel = viewModel)
        }

        item {
            CustomizationCard(
                selectedTopic = selectedTopic,
                selectedDifficulty = selectedDifficulty,
                onTopicChange = onTopicChange,
                onDifficultyChange = onDifficultyChange,
                viewModel = viewModel
            )
        }

        item {
            val composition = viewModel.getDifficultyComposition(selectedTopic, selectedDifficulty)
            DifficultyPieChart(composition = composition)
        }

        item {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("start_practice_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "BẮT ĐẦU TẬP LUYỆN",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==========================================
// DIFFICULTY PIE CHART (VISUALIZATION)
// ==========================================

@Composable
fun DifficultyPieChart(
    composition: Map<String, Int>,
    modifier: Modifier = Modifier
) {
    val total = composition.values.sum().coerceAtLeast(1)
    
    val categories = listOf("Dễ", "Trung bình", "Khó")
    val colors = listOf(
        Color(0xFF4CAF50), // Nice Material Green for Easy
        Color(0xFFFFA726), // Nice Material Orange for Medium
        Color(0xFFEF5350)  // Nice Material Red/Coral for Hard
    )
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = modifier.fillMaxWidth().testTag("difficulty_pie_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PieChart,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Tỷ lệ mức độ nhận thức (độ khó dự kiến)",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold, 
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Donut Chart Canvas
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(90.dp)) {
                        var startAngle = -90f
                        categories.forEachIndexed { index, cat ->
                            val count = composition[cat] ?: 0
                            if (count > 0) {
                                val sweepAngle = (count.toFloat() / total) * 360f
                                drawArc(
                                    color = colors[index],
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Butt)
                                )
                                startAngle += sweepAngle
                            }
                        }
                    }
                    
                    // Center label of the donut
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "$total",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black, 
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "câu hỏi",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                
                // Legends and details column
                Column(
                    modifier = Modifier.weight(1.3f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEachIndexed { index, cat ->
                        val count = composition[cat] ?: 0
                        val percentage = (count.toFloat() / total) * 100
                        val percentageStr = String.format(java.util.Locale.US, "%.1f", percentage)
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Color Indicator Dot
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(colors[index])
                                )
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold, 
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "$count câu ($percentageStr%)",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.SemiBold, 
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            
                            // Visual linear indicator
                            LinearProgressIndicator(
                                progress = count.toFloat() / total,
                                color = colors[index],
                                trackColor = colors[index].copy(alpha = 0.12f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// DOCUMENT CONFIG CARD (SCHOOL BRANDING & HEADER SETTINGS)
// ==========================================

@Composable
fun DocumentConfigCard(
    viewModel: ExamViewModel,
    modifier: Modifier = Modifier
) {
    val docDepartment by viewModel.docDepartment.collectAsState()
    val docSchool by viewModel.docSchool.collectAsState()
    val docExamTitle by viewModel.docExamTitle.collectAsState()
    val docSubject by viewModel.docSubject.collectAsState()
    val docYear by viewModel.docYear.collectAsState()
    val docTimeLimit by viewModel.docTimeLimit.collectAsState()
    val docInstructions by viewModel.docInstructions.collectAsState()

    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Cấu hình",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Cấu Hình Đề Xuất Bản & Thương Hiệu",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = "Tùy chỉnh tiêu đề, tên trường & thời gian làm bài",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = "Expand",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    
                    Text(
                        text = "Thông tin Đơn vị & Cơ sở",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )

                    OutlinedTextField(
                        value = docDepartment,
                        onValueChange = { viewModel.updateDocConfig(it, docSchool, docExamTitle, docSubject, docYear, docTimeLimit, docInstructions) },
                        label = { Text("Sở GD&ĐT / Cơ quan chủ quản") },
                        placeholder = { Text("Ví dụ: SỞ GIÁO DỤC VÀ ĐÀO TẠO") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )

                    OutlinedTextField(
                        value = docSchool,
                        onValueChange = { viewModel.updateDocConfig(docDepartment, it, docExamTitle, docSubject, docYear, docTimeLimit, docInstructions) },
                        label = { Text("Tên Trường THPT / Cơ sở học tập") },
                        placeholder = { Text("Ví dụ: TRƯỜNG THPT CHUYÊN TOÁN MIXER") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))

                    Text(
                        text = "Thông tin Kỳ thi & Môn học",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )

                    OutlinedTextField(
                        value = docExamTitle,
                        onValueChange = { viewModel.updateDocConfig(docDepartment, docSchool, it, docSubject, docYear, docTimeLimit, docInstructions) },
                        label = { Text("Kỳ thi / Kỳ kiểm tra") },
                        placeholder = { Text("Ví dụ: KỲ THI THỬ TỐT NGHIỆP THPT QUỐC GIA") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Default.Assignment, contentDescription = null, modifier = Modifier.size(20.dp)) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = docSubject,
                            onValueChange = { viewModel.updateDocConfig(docDepartment, docSchool, docExamTitle, it, docYear, docTimeLimit, docInstructions) },
                            label = { Text("Môn thi") },
                            placeholder = { Text("Môn thi: TOÁN HỌC (Lớp 12)") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f),
                            leadingIcon = { Icon(Icons.Default.Book, contentDescription = null, modifier = Modifier.size(20.dp)) }
                        )

                        OutlinedTextField(
                            value = docYear,
                            onValueChange = { viewModel.updateDocConfig(docDepartment, docSchool, docExamTitle, docSubject, it, docTimeLimit, docInstructions) },
                            label = { Text("Năm học") },
                            placeholder = { Text("NĂM HỌC 2026 - 2027") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = docTimeLimit,
                            onValueChange = { 
                                val filtered = it.filter { char -> char.isDigit() }
                                viewModel.updateDocConfig(
                                    docDepartment, docSchool, docExamTitle, docSubject, docYear, filtered,
                                    "Thời gian làm bài: $filtered phút (Không kể thời gian phát đề)"
                                )
                            },
                            label = { Text("Thời gian (phút)") },
                            placeholder = { Text("90") },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp)) }
                        )

                        OutlinedTextField(
                            value = docInstructions,
                            onValueChange = { viewModel.updateDocConfig(docDepartment, docSchool, docExamTitle, docSubject, docYear, docTimeLimit, it) },
                            label = { Text("Ghi chú đề thi") },
                            placeholder = { Text("Thời gian làm bài: 90 phút...") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Branding Info",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Các tùy chỉnh trên sẽ áp dụng ngay khi bạn xuất bản đề thi ra tài liệu Word (.doc) hoặc PDF (.pdf)!",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// CUSTOMIZATION CARD (TOPICS & DIFFICULTY CHIPS)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizationCard(
    selectedTopic: String,
    selectedDifficulty: String,
    onTopicChange: (String) -> Unit,
    onDifficultyChange: (String) -> Unit,
    viewModel: ExamViewModel
) {
    val randomizeQuestions by viewModel.randomizeQuestions.collectAsState()
    val randomizeOptions by viewModel.randomizeOptions.collectAsState()

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "Cấu Hình Nội Dung Đề Thi",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp
                    )
                )
            }

            // 1. TOPIC SELECTION (Symmetrical 2-Column Premium Grid)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Chủ đề kiến thức:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val topics = listOf(
                    "Tất cả" to Triple("Toàn bộ chương trình", Icons.Default.Apps, MaterialTheme.colorScheme.primary),
                    "Giải tích" to Triple("Hàm số, giới hạn, tích phân...", Icons.Default.ShowChart, Color(0xFF009688)),
                    "Đại số" to Triple("Phương trình, hệ phương trình, lũy thừa...", Icons.Default.Calculate, Color(0xFF3F51B5)),
                    "Hình học phẳng" to Triple("Hệ tọa độ phẳng, tam giác...", Icons.Default.Category, Color(0xFFFF9800)),
                    "Hình học không gian" to Triple("Oxyz, khối đa diện, thể tích...", Icons.Default.Layers, Color(0xFF9C27B0)),
                    "Tổ hợp & Xác suất" to Triple("Nhị thức, hoán vị, xác suất...", Icons.Default.Shuffle, Color(0xFFE91E63))
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (i in topics.indices step 2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (j in 0..1) {
                                val index = i + j
                                if (index < topics.size) {
                                    val (topicName, info) = topics[index]
                                    val (sub, icon, baseColor) = info
                                    val isSelected = selectedTopic == topicName
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(64.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) baseColor.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) baseColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { onTopicChange(topicName) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Circular Icon Container
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) baseColor.copy(alpha = 0.25f)
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            
                                            Column(verticalArrangement = Arrangement.Center) {
                                                Text(
                                                    text = topicName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    ),
                                                    color = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = sub,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        color = if (isSelected) baseColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                                    ),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 2. DIFFICULTY SELECTION (Beautiful layout with custom indicators & pastel colors)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Mức độ nhận thức:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val standardLevels = listOf(
                    "Tất cả" to Triple("Mọi mức độ", Icons.Default.List, MaterialTheme.colorScheme.primary),
                    "Dễ" to Triple("Nhận biết", Icons.Default.CheckCircle, Color(0xFF2E7D32)),
                    "Trung bình" to Triple("Thông hiểu", Icons.Default.Star, Color(0xFFE65100)),
                    "Khó" to Triple("Vận dụng", Icons.Default.AutoAwesome, Color(0xFFC62828))
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    standardLevels.forEach { (level, info) ->
                        val (desc, icon, baseColor) = info
                        val isSelected = selectedDifficulty == level
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) baseColor.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) baseColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onDifficultyChange(level) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = level,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        color = if (isSelected) baseColor.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 3. TARGET SCORE SELECTION (Modern horizontal scroll with rich badge elements)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Hoặc Luyện tập theo Mục tiêu điểm số:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val scoreLevels = listOf(
                    "5-6 điểm" to "Mức đạt 5-6 điểm",
                    "6-7 điểm" to "Mức đạt 6-7 điểm",
                    "7-8 điểm" to "Mức đạt 7-8 điểm",
                    "8-9 điểm" to "Mức đạt 8-9 điểm",
                    "9-10 điểm" to "Mức đạt 9-10 điểm"
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    scoreLevels.forEach { (label, value) ->
                        val isSelected = selectedDifficulty == value
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                )
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onDifficultyChange(value) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Stars else Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Mục tiêu đạt",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 4. SHUFFLE / RANDOMIZATION OPTIONS (Modern high-fidelity Toggle container)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Tùy Chọn Trộn Đề Thi:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Randomize Questions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .clickable { viewModel.toggleRandomizeQuestions() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trộn thứ tự câu hỏi",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Thay đổi ngẫu nhiên thứ tự các câu hỏi trong đề thi.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = randomizeQuestions,
                        onCheckedChange = { viewModel.toggleRandomizeQuestions() },
                        modifier = Modifier.testTag("toggle_randomize_questions")
                    )
                }

                // Randomize Options
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        .clickable { viewModel.toggleRandomizeOptions() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Trộn thứ tự đáp án (Phần I)",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Xáo trộn vị trí A, B, C, D của các câu trắc nghiệm khách quan.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = randomizeOptions,
                        onCheckedChange = { viewModel.toggleRandomizeOptions() },
                        modifier = Modifier.testTag("toggle_randomize_options")
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 3: HISTORY LIST TAB
// ==========================================

@Composable
fun HistoryListTab(
    historyList: List<ExamHistoryEntity>,
    onViewDetails: (ExamHistoryEntity) -> Unit,
    onDelete: (ExamHistoryEntity) -> Unit
) {
    if (historyList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "Chưa có lịch sử làm bài",
                    style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Hãy hoàn thành một bài kiểm tra hoặc luyện tập để lưu trữ kết quả và thống kê tự động tại đây.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Danh Sách Bài Đã Hoàn Thành (${historyList.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            itemsIndexed(historyList) { _, history ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val isPractice = history.isPracticeMode
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(if (isPractice) "Tự luyện" else "Thi cử", fontSize = 10.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isPractice) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
                                            labelColor = if (isPractice) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                    Text(
                                        text = history.examCode,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Thí sinh: ${history.studentName} - Lớp: ${history.studentClass}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            
                            // Grade Circle Badge
                            Surface(
                                color = if (history.totalPoints >= 8.0) Color(0xFFE8F5E9)
                                        else if (history.totalPoints >= 5.0) Color(0xFFFFF3E0)
                                        else Color(0xFFFFEBEE),
                                shape = CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${history.totalPoints}",
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (history.totalPoints >= 8.0) Color(0xFF2E7D32)
                                                        else if (history.totalPoints >= 5.0) Color(0xFFEF6C00)
                                                        else Color(0xFFC62828)
                                            )
                                        )
                                        Text(
                                            text = "Điểm",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 9.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                            val dateStr = sdf.format(java.util.Date(history.timestamp))
                            
                            val durationMins = history.timeElapsedSeconds / 60
                            val durationSecs = history.timeElapsedSeconds % 60
                            val durationStr = if (history.isPracticeMode) "Không tính" else String.format("%02d:%02d", durationMins, durationSecs)

                            Text(
                                text = "$dateStr | Thời gian: $durationStr",
                                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { onDelete(history) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                                }
                                Button(
                                    onClick = { onViewDetails(history) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Xem lại", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StructureRow(partName: String, desc: String, score: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.width(60.dp)
        ) {
            Text(
                text = partName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.padding(vertical = 4.dp, horizontal = 6.dp),
                textAlign = TextAlign.Center
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = desc, style = MaterialTheme.typography.bodyMedium)
            Text(text = score, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.secondary))
        }
    }
}

// ==========================================
// SCREEN 2: EXAM TAKING SCREEN
// ==========================================

@Composable
fun ExamScreen(
    session: ExamSession,
    onPart1Answer: (Int, Int) -> Unit,
    onPart2Answer: (Int, Int, Boolean) -> Unit,
    onPart3Answer: (Int, String) -> Unit,
    onSubmit: () -> Unit,
    viewModel: ExamViewModel
) {
    var showSubmitDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAnswerKey by remember { mutableStateOf(false) }
    var showCountdown by remember { mutableStateOf(true) }
    var showTimeUpDialog by remember { mutableStateOf(false) }
    var hasBeenWarnedAboutTimeUp by remember { mutableStateOf(false) }

    LaunchedEffect(session.timeElapsedSeconds) {
        if (!session.isPracticeMode && !session.isReviewMode && session.timeElapsedSeconds >= 5400 && !hasBeenWarnedAboutTimeUp) {
            showTimeUpDialog = true
            hasBeenWarnedAboutTimeUp = true
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val docDepartment by viewModel.docDepartment.collectAsState()
    val docSchool by viewModel.docSchool.collectAsState()
    val docExamTitle by viewModel.docExamTitle.collectAsState()
    val docSubject by viewModel.docSubject.collectAsState()
    val docYear by viewModel.docYear.collectAsState()
    val docTimeLimit by viewModel.docTimeLimit.collectAsState()
    val docInstructions by viewModel.docInstructions.collectAsState()

    val exportPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            ExamExporter.exportToPdf(
                                context, session, includeAnswers = session.isReviewMode, outputStream,
                                department = docDepartment,
                                school = docSchool,
                                examTitle = docExamTitle,
                                subject = docSubject,
                                year = docYear,
                                timeLimit = docTimeLimit,
                                instructions = docInstructions
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    val exportWordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/msword"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val htmlContent = ExamExporter.generateWordHtml(
                                session, includeAnswers = session.isReviewMode,
                                department = docDepartment,
                                school = docSchool,
                                examTitle = docExamTitle,
                                subject = docSubject,
                                year = docYear,
                                timeLimit = docTimeLimit,
                                instructions = docInstructions
                            )
                            outputStream.write(htmlContent.toByteArray(Charsets.UTF_8))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    Scaffold(
        topBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exit/Home Button
                        IconButton(
                            onClick = { viewModel.resetExam() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Home, contentDescription = "Trang chủ", tint = MaterialTheme.colorScheme.error)
                        }

                        // Compact Title / Timer
                        if (!session.isPracticeMode && !session.isReviewMode) {
                            val totalDuration = 5400
                            val isOvertime = session.timeElapsedSeconds > totalDuration
                            val displaySeconds = if (showCountdown) {
                                if (isOvertime) session.timeElapsedSeconds - totalDuration else totalDuration - session.timeElapsedSeconds
                            } else {
                                session.timeElapsedSeconds
                            }
                            val mins = displaySeconds / 60
                            val secs = displaySeconds % 60
                            val timeText = String.format("%02d:%02d", mins, secs)
                            
                            val containerColor = if (isOvertime && showCountdown) {
                                MaterialTheme.colorScheme.errorContainer
                            } else if (showCountdown && displaySeconds <= 300) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            } else {
                                MaterialTheme.colorScheme.primaryContainer
                            }
                            
                            val contentColor = if (isOvertime && showCountdown) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else if (showCountdown && displaySeconds <= 300) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }

                            Surface(
                                color = containerColor,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .testTag("timer_card")
                                    .clickable { showCountdown = !showCountdown }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Timer",
                                        tint = contentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (showCountdown) {
                                            if (isOvertime) "Hết giờ ($timeText)" else "Còn: $timeText"
                                        } else {
                                            "Đã qua: $timeText"
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = contentColor
                                        )
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = if (session.isReviewMode) "XEM LẠI ĐÁP ÁN" else "LUYỆN TẬP",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }

                        // Right action buttons (Toggle Answer & Export Dropdown)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (!session.isReviewMode) {
                                Button(
                                    onClick = { showSubmitDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (session.isPracticeMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .height(28.dp)
                                        .padding(end = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Nộp bài",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Nộp bài",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                            IconButton(
                                onClick = { showAnswerKey = !showAnswerKey },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("toggle_answer_key_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Bảng đáp án",
                                    tint = if (showAnswerKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Export dropdown
                            var showExportMenu by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { showExportMenu = true },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Xuất đề thi",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                DropdownMenu(
                                    expanded = showExportMenu,
                                    onDismissRequest = { showExportMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Xuất PDF (.pdf)") },
                                        onClick = {
                                            showExportMenu = false
                                            val suffix = if (session.isReviewMode) "_DapAn" else ""
                                            exportPdfLauncher.launch("De_Thi_Toan_${session.examCode.replace(" ", "_")}$suffix.pdf")
                                        },
                                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null, tint = Color.Red) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Xuất Word (.doc)") },
                                        onClick = {
                                            showExportMenu = false
                                            val suffix = if (session.isReviewMode) "_DapAn" else ""
                                            exportWordLauncher.launch("De_Thi_Toan_${session.examCode.replace(" ", "_")}$suffix.doc")
                                        },
                                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    )
                                }
                            }
                        }
                    }

                    // Tab Selector
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth(),
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                scope.launch { listState.animateScrollToItem(1) } // index 1 is Part 1 header card
                            },
                            text = { Text("Phần I", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                scope.launch { listState.animateScrollToItem(14) } // index 14 is Part 2 header card
                            },
                            text = { Text("Phần II", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = {
                                selectedTab = 2
                                scope.launch { listState.animateScrollToItem(20) } // index 20 is Part 3 header card
                            },
                            text = { Text("Phần III", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        )
                    }
                }
            }
        },
        bottomBar = {}
    ) { paddingValues ->
        // Sync tab selection based on scroll position
        LaunchedEffect(listState.firstVisibleItemIndex) {
            val idx = listState.firstVisibleItemIndex
            selectedTab = when {
                idx < 14 -> 0
                idx in 14..19 -> 1
                else -> 2
            }
        }

        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding() + 8.dp,
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
                start = 8.dp,
                end = 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Scrollable Profile & Progress Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_header_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = session.studentName.ifBlank { "Thí sinh tự luyện" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${if (session.isPracticeMode) "Chế độ: Luyện tập" else "Chế độ: Thi cử"} | Lớp: ${session.studentClass.ifBlank { "Tự học" }}",
                                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.secondary)
                                )
                            }
                            
                            if (session.isPracticeMode || session.isReviewMode) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(if (session.isReviewMode) "XEM LẠI" else "LUYỆN TẬP") },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                        labelColor = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Progress info
                        val totalQuestions = session.part1Questions.size + session.part2Questions.size + session.part3Questions.size
                        val answeredQuestions = session.part1Answers.size + 
                                session.part2Answers.values.count { ansList -> ansList.any { it != null } } + 
                                session.part3Answers.values.count { it.isNotBlank() }
                        val progressFraction = if (totalQuestions > 0) answeredQuestions.toFloat() / totalQuestions else 0f
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nạp vào: $totalQuestions câu hỏi",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = "Đã làm: $answeredQuestions/$totalQuestions (${(progressFraction * 100).toInt()}%)",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = progressFraction,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .testTag("session_progress_bar"),
                            color = if (progressFraction == 1f) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    }
                }
            }
            if (showAnswerKey) {
                item {
                    AnswerKeyCard(
                        session = session,
                        onDismiss = { showAnswerKey = false }
                    )
                }
            }

            // ==========================================
            // PART 1 HEADER & QUESTIONS
            // ==========================================
            item {
                PartHeaderCard(
                    title = "PHẦN I: TRẮC NGHIỆM KHÁCH QUAN",
                    description = "Mỗi câu hỏi có 4 phương án lựa chọn, chỉ có duy nhất 1 phương án đúng. Trả lời đúng mỗi câu được 0.25 điểm.",
                    totalQuestions = 12,
                    pInfo = "Tổng điểm tối đa: 3.0 điểm"
                )
            }

            itemsIndexed(session.part1Questions) { index, q ->
                val isChecked = session.isReviewMode || (session.isPracticeMode && session.part1Checked[index] == true)
                val selectedOption = session.part1Answers[index]

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isChecked) {
                                val isCorrect = selectedOption == q.correctIndex
                                if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFF44336).copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "Câu ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FormattedMathText(
                            text = q.question.content,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Rich Mathematical Diagrams / Visuals
                        QuestionVisual(part = 1, questionId = q.question.id)

                        Spacer(modifier = Modifier.height(12.dp))

                        q.options.forEachIndexed { optIndex, optionText ->
                            val prefix = when (optIndex) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                else -> "D"
                            }
                            val isSelected = selectedOption == optIndex
                            
                            // Highlighting for Checked States
                            val rowColor = when {
                                isChecked && optIndex == q.correctIndex -> Color(0xFFE8F5E9) // Correct - soft green
                                isChecked && isSelected && optIndex != q.correctIndex -> Color(0xFFFFEBEE) // Wrong - soft red
                                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else -> Color.Transparent
                            }

                            val borderStroke = if (isChecked && optIndex == q.correctIndex) {
                                BorderStroke(1.5.dp, Color(0xFF4CAF50))
                            } else if (isChecked && isSelected && optIndex != q.correctIndex) {
                                BorderStroke(1.5.dp, Color(0xFFF44336))
                            } else {
                                null
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = rowColor,
                                border = borderStroke,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isChecked) { onPart1Answer(index, optIndex) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { if (!isChecked) onPart1Answer(index, optIndex) },
                                        enabled = !isChecked
                                    )
                                    FormattedMathText(
                                        text = "$prefix. $optionText",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                }
                            }
                        }

                        // PRACTICE MODE SPECIFIC INTERACTIVE CHECKS
                        if (session.isPracticeMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (!isChecked) {
                                Button(
                                    onClick = { viewModel.checkPart1Question(index) },
                                    enabled = selectedOption != null,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("KIỂM TRA ĐÁP ÁN", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                val isCorrect = selectedOption == q.correctIndex
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                            Text(
                                                text = if (isCorrect) "ĐÁP ÁN CHÍNH XÁC (+0.25đ)" else "ĐÁP ÁN CHƯA ĐÚNG",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Lời Giải Chi Tiết:",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        FormattedMathText(
                                            text = q.question.explanation,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        // REVIEW MODE DETAILED SOLUTIONS
                        if (session.isReviewMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Lời Giải Chi Tiết:",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FormattedMathText(
                                        text = q.question.explanation,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // PART 2 HEADER & QUESTIONS
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                PartHeaderCard(
                    title = "PHẦN II: CÂU HỎI ĐÚNG SAI",
                    description = "Mỗi câu hỏi gồm 4 ý độc lập. Thí sinh phải lựa chọn đúng hoặc sai cho từng ý. Chấm lũy tiến nhân đôi tối đa 1.0 điểm cho mỗi câu.",
                    totalQuestions = 4,
                    pInfo = "Tổng điểm tối đa: 4.0 điểm"
                )
            }

            itemsIndexed(session.part2Questions) { index, q ->
                val isChecked = session.isReviewMode || (session.isPracticeMode && session.part2Checked[q.id] == true)
                val currentAnswers = session.part2Answers[q.id] ?: listOf(null, null, null, null)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isChecked) Color(0xFF4CAF50).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "Câu ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FormattedMathText(
                            text = q.content,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Canvas Vector Graphs / Figures
                        QuestionVisual(part = 2, questionId = q.id)

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        q.subQuestions.forEachIndexed { subIdx, subQ ->
                            val studentAns = currentAnswers[subIdx]
                            val isSubCorrect = studentAns == subQ.isTrue
                            
                            val statementPrefix = when (subIdx) {
                                0 -> "a)"
                                1 -> "b)"
                                2 -> "c)"
                                else -> "d)"
                            }

                            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                FormattedMathText(
                                    text = "$statementPrefix ${subQ.statement}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // True option
                                    Button(
                                        onClick = { if (!isChecked) onPart2Answer(q.id, subIdx, true) },
                                        enabled = !isChecked,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (studentAns == true) {
                                                if (isChecked && !isSubCorrect) Color(0xFFE57373) else Color(0xFF4CAF50)
                                            } else MaterialTheme.colorScheme.surfaceVariant,
                                            disabledContainerColor = if (studentAns == true) {
                                                if (isChecked && !isSubCorrect) Color(0xFFE57373) else Color(0xFF4CAF50)
                                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (studentAns == true) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "ĐÚNG",
                                            color = if (studentAns == true) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // False option
                                    Button(
                                        onClick = { if (!isChecked) onPart2Answer(q.id, subIdx, false) },
                                        enabled = !isChecked,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (studentAns == false) {
                                                if (isChecked && !isSubCorrect) Color(0xFFE57373) else Color(0xFFF44336)
                                            } else MaterialTheme.colorScheme.surfaceVariant,
                                            disabledContainerColor = if (studentAns == false) {
                                                if (isChecked && !isSubCorrect) Color(0xFFE57373) else Color(0xFFF44336)
                                            } else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = if (studentAns == false) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SAI",
                                            color = if (studentAns == false) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Show soft markers in Checked States
                                if (isChecked) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(start = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isSubCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (isSubCorrect) Color(0xFF2E7D32) else Color(0xFFC62828),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isSubCorrect) "Ý trả lời đúng" else "Ý trả lời sai (Đáp án đúng: ${if (subQ.isTrue) "ĐÚNG" else "SAI"})",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Medium,
                                                color = if (isSubCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        )
                                    }
                                }
                            }
                            if (subIdx < 3) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            }
                        }

                        // PRACTICE MODE FEEDBACK CARD
                        if (session.isPracticeMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (!isChecked) {
                                val allAnswered = currentAnswers.all { it != null }
                                Button(
                                    onClick = { viewModel.checkPart2Question(q.id) },
                                    enabled = allAnswered,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("KIỂM TRA ĐÁP ÁN", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                val correctCount = (0..3).count { currentAnswers[it] == q.subQuestions[it].isTrue }
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Kết quả: Đúng $correctCount / 4 ý.",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Lời Giải Chi Tiết:",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        FormattedMathText(
                                            text = q.explanation,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        // REVIEW MODE SOLUTION
                        if (session.isReviewMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Lời Giải Chi Tiết:",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FormattedMathText(
                                        text = q.explanation,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // PART 3 HEADER & QUESTIONS
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                PartHeaderCard(
                    title = "PHẦN III: TRẢ LỜI NGẮN",
                    description = "Thí sinh điền kết quả vào ô trống bằng số thập phân. Nếu đáp án là phân số hay số vô tỉ, hãy thực hiện tính toán và làm tròn chính xác đến hàng phần nghìn (3 chữ số thập phân).",
                    totalQuestions = 6,
                    pInfo = "Tổng điểm tối đa: 3.0 điểm"
                )
            }

            itemsIndexed(session.part3Questions) { index, q ->
                val isChecked = session.isReviewMode || (session.isPracticeMode && session.part3Checked[index] == true)
                val typedAns = session.part3Answers[index] ?: ""

                // Local float parse check
                val isAnswerCorrect = remember(typedAns, q.correctAnswer) {
                    val parsed = typedAns.trim().replace(',', '.').toDoubleOrNull() ?: Double.NaN
                    Math.abs(parsed - q.correctAnswer) < 0.001
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isChecked) {
                                if (isAnswerCorrect) Color(0xFF4CAF50).copy(alpha = 0.5f) else Color(0xFFF44336).copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            RoundedCornerShape(14.dp)
                        )
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    text = "Câu ${index + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        FormattedMathText(
                            text = q.content,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Rich Graph Canvas drawings
                        QuestionVisual(part = 3, questionId = q.id)

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = typedAns,
                            onValueChange = { if (!isChecked) onPart3Answer(index, it) },
                            enabled = !isChecked,
                            placeholder = { Text("Nhập kết quả số thập phân...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            leadingIcon = { Icon(Icons.Default.Help, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("part3_input_$index")
                        )
                        Text(
                            text = "Lưu ý: Chỉ nhập số thập phân, ví dụ: 2.5 hoặc -1.333",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                        )

                        // PRACTICE MODE FEEDBACK CARD
                        if (session.isPracticeMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (!isChecked) {
                                Button(
                                    onClick = { viewModel.checkPart3Question(index) },
                                    enabled = typedAns.isNotBlank(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("KIỂM TRA ĐÁP ÁN", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isAnswerCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isAnswerCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                                contentDescription = null,
                                                tint = if (isAnswerCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                            Text(
                                                text = if (isAnswerCorrect) "ĐÁP ÁN CHÍNH XÁC (+0.5đ)" else "CHƯA CHÍNH XÁC (Đáp án đúng: ${q.correctAnswer})",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAnswerCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Lời Giải Chi Tiết:",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        FormattedMathText(
                                            text = q.explanation,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }

                        // REVIEW MODE SOLUTION
                        if (session.isReviewMode) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (isAnswerCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(
                                            imageVector = if (isAnswerCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                            contentDescription = null,
                                            tint = if (isAnswerCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                        Text(
                                            text = if (isAnswerCorrect) "Học sinh trả lời đúng (+0.5đ)" else "Học sinh trả lời sai (Đáp án đúng: ${q.correctAnswer})",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAnswerCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Lời Giải Chi Tiết:",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    FormattedMathText(
                                        text = q.explanation,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (!session.isReviewMode) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (session.isPracticeMode) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Bạn đã hoàn thành bài thi?",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (session.isPracticeMode) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Vui lòng kiểm tra kỹ các đáp án trước khi nộp bài.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showSubmitDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("submit_exam_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (session.isPracticeMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (session.isPracticeMode) "HOÀN THÀNH LUYỆN TẬP" else "NỘP BÀI VÀ CHẤM ĐIỂM",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Submit confirmation dialog
    if (showSubmitDialog) {
        val countAnswered = session.part1Answers.size +
                session.part2Answers.values.count { it.any { ans -> ans != null } } +
                session.part3Answers.count { it.value.isNotBlank() }

        AlertDialog(
            onDismissRequest = { showSubmitDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitDialog = false
                        onSubmit()
                    }
                ) {
                    Text("Nộp bài")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitDialog = false }) {
                    Text("Làm tiếp")
                }
            },
            title = { Text("Xác nhận hoàn thành?") },
            text = {
                Text("Bạn đã trả lời được $countAnswered trên tổng số 22 câu hỏi.\nBạn có chắc chắn muốn kết thúc buổi học này không?")
            }
        )
    }

    // Time's up alert dialog
    if (showTimeUpDialog) {
        AlertDialog(
            onDismissRequest = { showTimeUpDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        showTimeUpDialog = false
                        onSubmit()
                    }
                ) {
                    Text("Nộp bài ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimeUpDialog = false }) {
                    Text("Xem lại bài và nộp sau")
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text("Hết giờ làm bài!", color = MaterialTheme.colorScheme.error)
                }
            },
            text = {
                Text("Thời gian làm bài 90 phút đã kết thúc. Bạn vui lòng nộp bài để ghi nhận điểm số và xem giải thích chi tiết.")
            }
        )
    }
}

@Composable
fun PartHeaderCard(
    title: String,
    description: String,
    totalQuestions: Int,
    pInfo: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$totalQuestions câu hỏi | $pInfo",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

// ==========================================
// SCREEN 3: RESULTS SCREEN
// ==========================================

@Composable
fun ResultScreen(
    session: ExamSession,
    onReset: () -> Unit,
    viewModel: ExamViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val docDepartment by viewModel.docDepartment.collectAsState()
    val docSchool by viewModel.docSchool.collectAsState()
    val docExamTitle by viewModel.docExamTitle.collectAsState()
    val docSubject by viewModel.docSubject.collectAsState()
    val docYear by viewModel.docYear.collectAsState()
    val docTimeLimit by viewModel.docTimeLimit.collectAsState()
    val docInstructions by viewModel.docInstructions.collectAsState()

    val exportCleanPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            ExamExporter.exportToPdf(
                                context, session, includeAnswers = false, outputStream,
                                department = docDepartment,
                                school = docSchool,
                                examTitle = docExamTitle,
                                subject = docSubject,
                                year = docYear,
                                timeLimit = docTimeLimit,
                                instructions = docInstructions
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    val exportMarkedPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            ExamExporter.exportToPdf(
                                context, session, includeAnswers = true, outputStream,
                                department = docDepartment,
                                school = docSchool,
                                examTitle = docExamTitle,
                                subject = docSubject,
                                year = docYear,
                                timeLimit = docTimeLimit,
                                instructions = docInstructions
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    val exportCleanWordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/msword"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val htmlContent = ExamExporter.generateWordHtml(
                                session, includeAnswers = false,
                                department = docDepartment,
                                school = docSchool,
                                examTitle = docExamTitle,
                                subject = docSubject,
                                year = docYear,
                                timeLimit = docTimeLimit,
                                instructions = docInstructions
                            )
                            outputStream.write(htmlContent.toByteArray(Charsets.UTF_8))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    val exportMarkedWordLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/msword"),
        onResult = { uri ->
            if (uri != null) {
                scope.launch(Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            val htmlContent = ExamExporter.generateWordHtml(
                                session, includeAnswers = true,
                                department = docDepartment,
                                school = docSchool,
                                examTitle = docExamTitle,
                                subject = docSubject,
                                year = docYear,
                                timeLimit = docTimeLimit,
                                instructions = docInstructions
                            )
                            outputStream.write(htmlContent.toByteArray(Charsets.UTF_8))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    )

    var showOnlyIncorrect by remember { mutableStateOf(false) }
    var showAnswerKeyResult by remember { mutableStateOf(false) }

    val totalIncorrectCount = remember(session) {
        var count = 0
        // Part 1
        session.part1Questions.forEachIndexed { index, q ->
            val chosen = session.part1Answers[index]
            if (chosen == null || chosen != q.correctIndex) count++
        }
        // Part 2
        session.part2Questions.forEach { q ->
            val studentAnswers = session.part2Answers[q.id] ?: listOf(null, null, null, null)
            val correctAnswers = q.subQuestions.map { it.isTrue }
            var correctCount = 0
            for (i in 0 until 4) {
                if (studentAnswers[i] != null && studentAnswers[i] == correctAnswers[i]) {
                    correctCount++
                }
            }
            if (correctCount != 4) count++
        }
        // Part 3
        session.part3Questions.forEachIndexed { index, q ->
            val typed = session.part3Answers[index] ?: ""
            val parsed = typed.trim().replace(',', '.').toDoubleOrNull()
            val isCorrect = if (parsed != null) {
                val roundedParsed = (parsed * 1000).roundToInt() / 1000.0
                val roundedCorrect = (q.correctAnswer * 1000).roundToInt() / 1000.0
                Math.abs(roundedParsed - roundedCorrect) < 0.001
            } else false
            if (!isCorrect) count++
        }
        count
    }

    val p1IncorrectCount = remember(session) {
        session.part1Questions.filterIndexed { index, q ->
            val chosen = session.part1Answers[index]
            chosen == null || chosen != q.correctIndex
        }.size
    }

    val p2IncorrectCount = remember(session) {
        session.part2Questions.filter { q ->
            val studentAnswers = session.part2Answers[q.id] ?: listOf(null, null, null, null)
            val correctAnswers = q.subQuestions.map { it.isTrue }
            var correctCount = 0
            for (i in 0 until 4) {
                if (studentAnswers[i] != null && studentAnswers[i] == correctAnswers[i]) {
                    correctCount++
                }
            }
            correctCount != 4
        }.size
    }

    val p3IncorrectCount = remember(session) {
        session.part3Questions.filterIndexed { index, q ->
            val typed = session.part3Answers[index] ?: ""
            val parsed = typed.trim().replace(',', '.').toDoubleOrNull()
            val isCorrect = if (parsed != null) {
                val roundedParsed = (parsed * 1000).roundToInt() / 1000.0
                val roundedCorrect = (q.correctAnswer * 1000).roundToInt() / 1000.0
                Math.abs(roundedParsed - roundedCorrect) < 0.001
            } else false
            !isCorrect
        }.size
    }

    val totalPoints = session.totalPoints
    val (resultTitle, ratingColor, imageId) = when {
        totalPoints >= 8.0 -> Triple("XUẤT SẮC - GIỎI", Color(0xFF4CAF50), Icons.Default.School)
        totalPoints >= 5.0 -> Triple("KHÁ - ĐẠT YÊU CẦU", Color(0xFF2196F3), Icons.Default.Check)
        else -> Triple("CẦN CỐ GẮNG HƠN", Color(0xFFF44336), Icons.Default.RotateLeft)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "KẾT QUẢ THI CỦA BẠN",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }

        item {
            // High-fidelity Score Badge
            Card(
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = session.studentName,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Lớp: ${session.studentClass} | ${session.examCode}",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Floating Score Circle
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(ratingColor.copy(alpha = 0.2f), ratingColor.copy(alpha = 0.05f))
                                )
                            )
                            .border(6.dp, ratingColor, CircleShape)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%.2f", totalPoints),
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = ratingColor
                                )
                            )
                            Text(
                                text = "TRÊN 10",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = resultTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = ratingColor
                        )
                    )

                    // Scoring Breakdown Details
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ScoreBreakdownColumn(part = "Phần I", points = session.p1Points, max = 3.0)
                        ScoreBreakdownColumn(part = "Phần II", points = session.p2Points, max = 4.0)
                        ScoreBreakdownColumn(part = "Phần III", points = session.p3Points, max = 3.0)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "TẢI VỀ FILE ĐỀ THI & LỜI GIẢI",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = "Bạn có thể tải đề thi vừa làm về máy dưới dạng PDF hoặc Word để in ra hoặc chỉnh sửa.",
                        style = MaterialTheme.typography.labelMedium.copy(textAlign = TextAlign.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { exportCleanPdfLauncher.launch("De_Thi_Toan_${session.examCode.replace(" ", "_")}.pdf") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đề thi PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { exportMarkedPdfLauncher.launch("De_Thi_Toan_${session.examCode.replace(" ", "_")}_DapAn.pdf") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đề + Giải PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { exportCleanWordLauncher.launch("De_Thi_Toan_${session.examCode.replace(" ", "_")}.doc") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đề thi Word", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { exportMarkedWordLauncher.launch("De_Thi_Toan_${session.examCode.replace(" ", "_")}_DapAn.doc") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đề + Giải Word", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = onReset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("remix_exam_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "LÀM ĐỀ KHÁC (TRỘN ĐỀ MỚI)",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "BẢNG ĐÁP ÁN NHANH (1 TRANG)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                        }
                        TextButton(
                            onClick = { showAnswerKeyResult = !showAnswerKeyResult },
                            modifier = Modifier.testTag("toggle_answer_key_result_button")
                        ) {
                            Text(if (showAnswerKeyResult) "ẨN BẢNG" else "XEM BẢNG")
                        }
                    }
                    if (showAnswerKeyResult) {
                        Spacer(modifier = Modifier.height(12.dp))
                        AnswerKeyCard(session = session, onDismiss = { showAnswerKeyResult = false })
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "CHI TIẾT ĐÁP ÁN & HƯỚNG DẪN GIẢI",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (showOnlyIncorrect) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("review_mode_toggle_card")
            ) {
                Row(
                    modifier = Modifier
                        .clickable { showOnlyIncorrect = !showOnlyIncorrect }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (showOnlyIncorrect) Icons.Default.FilterAlt else Icons.Default.FilterAltOff,
                            contentDescription = null,
                            tint = if (showOnlyIncorrect) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Chế độ xem lại câu sai (Review Mode)",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = if (showOnlyIncorrect) "Đang lọc: Chỉ hiển thị những câu làm sai" else "Hiển thị đầy đủ tất cả câu hỏi",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = showOnlyIncorrect,
                        onCheckedChange = { showOnlyIncorrect = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.error,
                            checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.testTag("review_mode_switch")
                    )
                }
            }
        }

        if (showOnlyIncorrect && totalIncorrectCount == 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).testTag("congrats_perfect_score_card"),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents ?: Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Tuyệt Vời! Điểm 10 Tuyệt Đối!",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        )
                        Text(
                            text = "Bạn đã trả lời đúng tất cả các câu hỏi trong đề thi. Không có câu nào cần phải ôn tập lại cả. Hãy giữ vững phong độ này nhé!",
                            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }
        }

        // ==========================================
        // REVIEW PART 1
        // ==========================================
        if (!showOnlyIncorrect || p1IncorrectCount > 0) {
            item {
                ReviewPartHeader(title = "Đáp Án Phần I (Trắc Nghiệm Khách Quan)")
            }
        }

        itemsIndexed(session.part1Questions) { index, q ->
            val chosen = session.part1Answers[index]
            val isCorrect = chosen != null && chosen == q.correctIndex
            if (!showOnlyIncorrect || !isCorrect) {
                ReviewCardContainer(isCorrect = isCorrect) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusIndicator(isCorrect = isCorrect, index = index + 1)
                            Text(
                                text = q.question.content,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Show correct answer indicator
                        val correctPrefix = when (q.correctIndex) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            else -> "D"
                        }
                        val studentPrefix = if (chosen != null) {
                            when (chosen) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                else -> "D"
                            }
                        } else "Chưa trả lời"

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Bạn chọn: $studentPrefix",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            )
                            Text(
                                text = "Đáp án đúng: $correctPrefix",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50)
                                )
                            )
                        }

                        // Always show or show if incorrect: "có Hướng dẫn giải cho câu Sai"
                        if (!isCorrect) {
                            SolutionGuideBox(explanation = q.question.explanation)
                        }

                        val optionsText = q.options.mapIndexed { optIdx, optText ->
                            val prefix = when (optIdx) {
                                0 -> "A"
                                1 -> "B"
                                2 -> "C"
                                else -> "D"
                            }
                            "$prefix. $optText"
                        }.joinToString("\n")
                        val correctText = "$correctPrefix. ${q.options[q.correctIndex]}"

                        AiExplanationSection(
                            questionKey = "part1_$index",
                            questionText = q.question.content,
                            correctAnswer = correctText,
                            optionsText = optionsText,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // ==========================================
        // REVIEW PART 2
        // ==========================================
        if (!showOnlyIncorrect || p2IncorrectCount > 0) {
            item {
                ReviewPartHeader(title = "Đáp Án Phần II (Đúng / Sai)")
            }
        }

        itemsIndexed(session.part2Questions) { index, q ->
            val studentAnswers = session.part2Answers[q.id] ?: listOf(null, null, null, null)
            val correctAnswers = q.subQuestions.map { it.isTrue }
            
            var correctCount = 0
            for (i in 0 until 4) {
                if (studentAnswers[i] != null && studentAnswers[i] == correctAnswers[i]) {
                    correctCount++
                }
            }
            val isAllCorrect = correctCount == 4

            if (!showOnlyIncorrect || !isAllCorrect) {
                ReviewCardContainer(isCorrect = isAllCorrect) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusIndicator(isCorrect = isAllCorrect, index = index + 1)
                            Text(
                                text = q.content,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Số ý đúng: $correctCount/4 ý",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        q.subQuestions.forEachIndexed { subIdx, subQ ->
                            val subPrefix = when (subIdx) {
                                0 -> "a)"
                                1 -> "b)"
                                2 -> "c)"
                                else -> "d)"
                            }
                            val sAns = studentAnswers[subIdx]
                            val isSubCorrect = sAns != null && sAns == subQ.isTrue

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "$subPrefix ${subQ.statement}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Chọn: ${if (sAns == null) "Chưa chọn" else if (sAns) "Đúng" else "Sai"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSubCorrect) Color(0xFF4CAF50) else Color(0xFFF44336),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "Đúng là: ${if (subQ.isTrue) "Đúng" else "Sai"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFF4CAF50),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        if (!isAllCorrect) {
                            SolutionGuideBox(explanation = q.explanation)
                        }

                        val subCorrectTexts = q.subQuestions.mapIndexed { subIdx, subQ ->
                            val prefix = when (subIdx) {
                                0 -> "a)"
                                1 -> "b)"
                                2 -> "c)"
                                else -> "d)"
                            }
                            "$prefix ${subQ.statement} là ${if (subQ.isTrue) "ĐÚNG" else "SAI"}"
                        }.joinToString("\n")

                        AiExplanationSection(
                            questionKey = "part2_$index",
                            questionText = q.content,
                            correctAnswer = subCorrectTexts,
                            optionsText = null,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // ==========================================
        // REVIEW PART 3
        // ==========================================
        if (!showOnlyIncorrect || p3IncorrectCount > 0) {
            item {
                ReviewPartHeader(title = "Đáp Án Phần III (Trả Lời Ngắn)")
            }
        }

        itemsIndexed(session.part3Questions) { index, q ->
            val typed = session.part3Answers[index] ?: ""
            val parsed = typed.trim().replace(',', '.').toDoubleOrNull()
            val isCorrect = if (parsed != null) {
                val roundedParsed = (parsed * 1000).roundToInt() / 1000.0
                val roundedCorrect = (q.correctAnswer * 1000).roundToInt() / 1000.0
                Math.abs(roundedParsed - roundedCorrect) < 0.001
            } else false

            if (!showOnlyIncorrect || !isCorrect) {
                ReviewCardContainer(isCorrect = isCorrect) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusIndicator(isCorrect = isCorrect, index = index + 1)
                            Text(
                                text = q.content,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Bạn điền: ${if (typed.isBlank()) "Trống" else typed}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            )
                            Text(
                                text = "Đáp án: ${q.correctAnswer}",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF4CAF50)
                                )
                            )
                        }

                        if (!isCorrect) {
                            SolutionGuideBox(explanation = q.explanation)
                        }

                        AiExplanationSection(
                            questionKey = "part3_$index",
                            questionText = q.content,
                            correctAnswer = q.correctAnswer.toString(),
                            optionsText = null,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        item {
            // Elegant signature footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = com.example.R.drawable.img_mindup_icon),
                        contentDescription = "MindUp Logo",
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = "MindUp",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                Text(
                    text = "Nâng tầm tư duy học sinh Việt",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                )
                Text(
                    text = "Ứng dụng được thiết kế bởi tác giả Trịnh Như Quỳnh",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ScoreBreakdownColumn(part: String, points: Double, max: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = part,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(
            text = String.format("%.2f / %.1f", points, max),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun ReviewPartHeader(title: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
fun ReviewCardContainer(
    isCorrect: Boolean,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color(0xFFF44336).copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
fun AiExplanationSection(
    questionKey: String,
    questionText: String,
    correctAnswer: String,
    optionsText: String? = null,
    viewModel: ExamViewModel
) {
    val aiExplanations by viewModel.aiExplanations.collectAsState()
    val aiLoadingStates by viewModel.aiLoadingStates.collectAsState()

    var isVisible by remember { mutableStateOf(false) }

    val explanation = aiExplanations[questionKey]
    val isLoading = aiLoadingStates[questionKey] == true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        OutlinedButton(
            onClick = {
                isVisible = !isVisible
                if (isVisible && explanation == null) {
                    viewModel.fetchAiExplanation(questionKey, questionText, correctAnswer, optionsText)
                }
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
            modifier = Modifier.testTag("ai_explanation_toggle_$questionKey")
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isVisible) "Ẩn giải thích AI" else "Giải thích bằng Gemini AI",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (isVisible) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth().testTag("ai_explanation_card_$questionKey")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Giải Thích Chi Tiết Từ Gemini AI:",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    if (isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "AI đang suy nghĩ và lập luận...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val textToShow = explanation ?: "Chưa có giải thích. Hãy bấm nút để tạo."
                        FormattedMathText(
                            text = textToShow,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnswerKeyCard(
    session: ExamSession,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("answer_key_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "BẢNG ĐÁP ÁN ĐỀ THI",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Đóng",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // PART 1 ANSWER GRID (Questions 1 to 12)
            if (session.part1Questions.isNotEmpty()) {
                Text(
                    text = "PHẦN I: TRẮC NGHIỆM NHIỀU LỰA CHỌN",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    session.part1Questions.forEachIndexed { index, q ->
                        val correctLetter = when (q.correctIndex) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            3 -> "D"
                            else -> "?"
                        }
                        // Compact bubble for question and answer
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.widthIn(min = 54.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}: ",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = correctLetter,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // PART 2 ANSWER GRID (Questions 1 to 4 with subquestions a, b, c, d)
            if (session.part2Questions.isNotEmpty()) {
                Text(
                    text = "PHẦN II: TRẮC NGHIỆM ĐÚNG SAI (a, b, c, d)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    session.part2Questions.forEachIndexed { index, q ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Câu ${index + 1}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    q.subQuestions.forEachIndexed { subIdx, sub ->
                                        val label = when (subIdx) {
                                            0 -> "a"
                                            1 -> "b"
                                            2 -> "c"
                                            3 -> "d"
                                            else -> ""
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "$label:",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (sub.isTrue) Color(0xFF4CAF50) else Color(0xFFF44336),
                                                modifier = Modifier.widthIn(min = 22.dp)
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (sub.isTrue) "Đ" else "S",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // PART 3 ANSWER GRID (Questions 1 to 6 short answers)
            if (session.part3Questions.isNotEmpty()) {
                Text(
                    text = "PHẦN III: CÂU HỎI TRẢ LỜI NGẮN (ĐIỀN SỐ)",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    session.part3Questions.forEachIndexed { index, q ->
                        val formattedAnswer = if (q.correctAnswer % 1.0 == 0.0) {
                            q.correctAnswer.toInt().toString()
                        } else {
                            q.correctAnswer.toString()
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.widthIn(min = 72.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Câu ${index + 1}: ",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formattedAnswer,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicator(isCorrect: Boolean, index: Int) {
    Surface(
        color = if (isCorrect) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
        shape = CircleShape,
        modifier = Modifier.size(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "$index",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isCorrect) Color(0xFF2E7D32) else Color(0xFFC62828)
                )
            )
        }
    }
}

@Composable
fun SolutionGuideBox(explanation: String) {
    Spacer(modifier = Modifier.height(12.dp))
    Surface(
        color = MaterialTheme.colorScheme.infoContainerTheme(),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "HƯỚNG DẪN GIẢI CHI TIẾT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            FormattedMathText(
                text = explanation,
                style = MaterialTheme.typography.bodySmall.copy(
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}

@Composable
fun ColorScheme.infoContainerTheme(): Color {
    return this.secondaryContainer.copy(alpha = 0.35f)
}

@Composable
fun RememberedProfilesView(
    viewModel: ExamViewModel,
    onSelect: (String, String) -> Unit
) {
    val rememberedProfiles by viewModel.rememberedProfiles.collectAsState()
    if (rememberedProfiles.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Tài khoản đã ghi nhớ (chạm để chọn nhanh):",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(rememberedProfiles.size) { index ->
                    val profile = rememberedProfiles[index]
                    val name = profile.first
                    val cls = profile.second
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .clickable { onSelect(name, cls) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = cls,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.deleteRememberedProfile(name, cls) },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Xóa",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
