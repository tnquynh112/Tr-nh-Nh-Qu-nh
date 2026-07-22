package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ExamViewModel
import com.example.db.MathQuestionEntity

// Standard Topic Tags for Math
val PREDEFINED_MATH_TAGS = listOf(
    "Algebra",
    "Geometry",
    "Calculus",
    "Trigonometry",
    "Statistics",
    "Vectors",
    "Integrals",
    "Derivatives",
    "Limits",
    "Logarithm",
    "Probability",
    "Exponents"
)

val STANDARD_TOPICS = listOf(
    "Giải tích",
    "Đại số",
    "Hình học",
    "Lượng giác",
    "Tổ hợp & Xác suất"
)

val DIFFICULTY_LEVELS = listOf(
    "Dễ",
    "Trung bình",
    "Khó",
    "Vận dụng cao"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuestionDatabaseManagerUI(
    viewModel: ExamViewModel,
    modifier: Modifier = Modifier
) {
    val questions by viewModel.mathQuestionBank.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("Tất cả") }
    var selectedTopicFilter by remember { mutableStateOf("Tất cả") }
    
    var showFormDialog by remember { mutableStateOf(false) }
    var editingQuestion by remember { mutableStateOf<MathQuestionEntity?>(null) }
    var questionToDelete by remember { mutableStateOf<MathQuestionEntity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Filter questions based on search query, topic, and tags
    val filteredQuestions = remember(questions, searchQuery, selectedTagFilter, selectedTopicFilter) {
        questions.filter { q ->
            val matchesSearch = searchQuery.isBlank() || 
                q.content.contains(searchQuery, ignoreCase = true) ||
                q.explanation.contains(searchQuery, ignoreCase = true) ||
                q.optionA.contains(searchQuery, ignoreCase = true) ||
                q.optionB.contains(searchQuery, ignoreCase = true) ||
                q.optionC.contains(searchQuery, ignoreCase = true) ||
                q.optionD.contains(searchQuery, ignoreCase = true)
            
            val matchesTopic = selectedTopicFilter == "Tất cả" || q.topic == selectedTopicFilter
            
            val matchesTag = if (selectedTagFilter == "Tất cả") {
                true
            } else {
                q.getTagsList().any { tag -> tag.equals(selectedTagFilter, ignoreCase = true) }
            }

            matchesSearch && matchesTopic && matchesTag
        }
    }

    // Collect all available tags across questions for filter chips
    val allTagsInDb = remember(questions) {
        val set = mutableSetOf<String>()
        set.addAll(PREDEFINED_MATH_TAGS)
        questions.forEach { q ->
            set.addAll(q.getTagsList())
        }
        set.toList().sorted()
    }

    // Counts by topic
    val algebraCount = questions.count { it.topic == "Đại số" || it.getTagsList().contains("Algebra") }
    val geometryCount = questions.count { it.topic == "Hình học" || it.getTagsList().contains("Geometry") }
    val calculusCount = questions.count { it.topic == "Giải tích" || it.getTagsList().contains("Calculus") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = "Database",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ngân hàng câu hỏi Room DB",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "${questions.size} câu hỏi",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Text(
                    text = "Quản lý dữ liệu trắc nghiệm Toán học, phân loại thẻ Tag (Algebra, Geometry, Calculus,...) và tìm kiếm thông minh.",
                    style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )

                // Topic Badge Summary Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TopicBadge("Đại số (Algebra)", algebraCount, Color(0xFF1E88E5))
                    TopicBadge("Hình học (Geometry)", geometryCount, Color(0xFF43A047))
                    TopicBadge("Giải tích (Calculus)", calculusCount, Color(0xFF8E24AA))
                }

                // Quick Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            editingQuestion = null
                            showFormDialog = true
                        },
                        modifier = Modifier.weight(1f).testTag("add_question_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm câu hỏi", fontWeight = FontWeight.Bold)
                    }

                    if (questions.isEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.seedSampleMathQuestions() },
                            modifier = Modifier.weight(1f).testTag("seed_sample_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nạp 6 câu mẫu", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Search and Tag Filter Bar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_question_input"),
                    placeholder = { Text("Tìm kiếm theo nội dung, công thức, lời giải...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Filter Tag Chips Row
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Lọc theo thẻ chủ đề (Tags):",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    )
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = selectedTagFilter == "Tất cả",
                                onClick = { selectedTagFilter = "Tất cả" },
                                label = { Text("Tất cả tags") },
                                leadingIcon = if (selectedTagFilter == "Tất cả") {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }

                        items(allTagsInDb) { tag ->
                            val isSelected = selectedTagFilter == tag
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedTagFilter = if (isSelected) "Tất cả" else tag
                                },
                                label = { Text("#$tag") },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // List Header & Items Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Danh sách câu hỏi (${filteredQuestions.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )

            if (selectedTagFilter != "Tất cả" || searchQuery.isNotEmpty()) {
                TextButton(onClick = {
                    searchQuery = ""
                    selectedTagFilter = "Tất cả"
                    selectedTopicFilter = "Tất cả"
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Xóa bộ lọc", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Questions LazyColumn
        if (filteredQuestions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (questions.isEmpty()) "Chưa có câu hỏi nào trong ngân hàng dữ liệu" else "Không tìm thấy câu hỏi phù hợp với bộ lọc",
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                    )
                    if (questions.isEmpty()) {
                        Button(
                            onClick = { viewModel.seedSampleMathQuestions() },
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Thêm dữ liệu câu hỏi mẫu")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredQuestions, key = { it.id }) { q ->
                    MathQuestionCard(
                        question = q,
                        onEdit = {
                            editingQuestion = q
                            showFormDialog = true
                        },
                        onDelete = {
                            questionToDelete = q
                            showDeleteConfirmDialog = true
                        },
                        onAddToExam = {
                            viewModel.addBankQuestionToCustomExam(q)
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Form Dialog
    if (showFormDialog) {
        QuestionFormDialog(
            questionToEdit = editingQuestion,
            onDismiss = {
                showFormDialog = false
                editingQuestion = null
            },
            onSave = { newQuestion ->
                viewModel.saveMathQuestion(newQuestion)
                showFormDialog = false
                editingQuestion = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && questionToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteConfirmDialog = false
                questionToDelete = null
            },
            title = { Text("Xác nhận xóa câu hỏi") },
            text = { Text("Bạn có chắc chắn muốn xóa câu hỏi này khỏi ngân hàng dữ liệu Room DB?") },
            confirmButton = {
                Button(
                    onClick = {
                        questionToDelete?.let { viewModel.deleteMathQuestion(it.id) }
                        showDeleteConfirmDialog = false
                        questionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Xóa")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteConfirmDialog = false
                    questionToDelete = null
                }) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun TopicBadge(label: String, count: Int, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
        }
    }
}

@Composable
fun MathQuestionCard(
    question: MathQuestionEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAddToExam: () -> Unit
) {
    var expandedExplanation by remember { mutableStateOf(false) }
    val tags = question.getTagsList()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("math_question_card_${question.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Topic Badge, Difficulty, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = question.topic,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }

                    val (diffColor, diffBg) = when (question.difficulty) {
                        "Dễ" -> Pair(Color(0xFF2E7D32), Color(0xFFE8F5E9))
                        "Trung bình" -> Pair(Color(0xFFE65100), Color(0xFFFFF3E0))
                        "Khó" -> Pair(Color(0xFFC62828), Color(0xFFFFEBEE))
                        else -> Pair(Color(0xFF6A1B9A), Color(0xFFF3E5F5))
                    }

                    Surface(
                        color = diffBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = question.difficulty,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = diffColor
                            )
                        )
                    }
                }

                // Action Buttons
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onAddToExam, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Thêm vào đề thi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Chỉnh sửa",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Tags Chips Row
            if (tags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    tags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            // Question Content (Rendered Math)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(10.dp)
            ) {
                FormattedMathText(
                    text = question.content,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium)
                )
            }

            // Options Grid / List
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val options = listOf(question.optionA, question.optionB, question.optionC, question.optionD)
                val labels = listOf("A", "B", "C", "D")

                options.forEachIndexed { index, optionText ->
                    val isCorrect = index == question.correctIndex
                    Surface(
                        color = if (isCorrect) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isCorrect) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = if (isCorrect) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = labels[index],
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCorrect) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                FormattedMathText(text = optionText, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp))
                            }

                            if (isCorrect) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Đáp án đúng",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Solution / Explanation
            if (question.explanation.isNotBlank()) {
                Column {
                    TextButton(
                        onClick = { expandedExplanation = !expandedExplanation },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (expandedExplanation) "Ẩn lời giải chi tiết" else "Xem lời giải chi tiết",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Icon(
                                imageVector = if (expandedExplanation) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    AnimatedVisibility(visible = expandedExplanation) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Lời giải chi tiết:",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                )
                                FormattedMathText(
                                    text = question.explanation,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Dialog form for creating / editing a math question
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuestionFormDialog(
    questionToEdit: MathQuestionEntity?,
    onDismiss: () -> Unit,
    onSave: (MathQuestionEntity) -> Unit
) {
    var content by remember { mutableStateOf(questionToEdit?.content ?: "") }
    var optionA by remember { mutableStateOf(questionToEdit?.optionA ?: "") }
    var optionB by remember { mutableStateOf(questionToEdit?.optionB ?: "") }
    var optionC by remember { mutableStateOf(questionToEdit?.optionC ?: "") }
    var optionD by remember { mutableStateOf(questionToEdit?.optionD ?: "") }
    var correctIndex by remember { mutableStateOf(questionToEdit?.correctIndex ?: 0) }
    var topic by remember { mutableStateOf(questionToEdit?.topic ?: "Giải tích") }
    var difficulty by remember { mutableStateOf(questionToEdit?.difficulty ?: "Dễ") }
    var explanation by remember { mutableStateOf(questionToEdit?.explanation ?: "") }
    
    // Tags selection
    var selectedTags by remember {
        mutableStateOf(questionToEdit?.getTagsList()?.toSet() ?: setOf("Algebra", "Calculus"))
    }
    var customTagInput by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Form Editor, 1: Live Preview

    val mathSymbols = listOf("√", "²", "³", "π", "∫", "∞", "±", "∈", "≤", "≥", "÷", "×", "^", "/", "α", "β", "Δ")

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dialog Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (questionToEdit == null) "Thêm câu hỏi trắc nghiệm mới" else "Chỉnh sửa câu hỏi #${questionToEdit.id}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Editor vs Live Preview Tabs
                TabRow(selectedTabIndex = activeTab) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Biểu mẫu nhập liệu", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = null) }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Xem trước công thức", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                    )
                }

                if (activeTab == 0) {
                    // Form Content
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Topic & Difficulty Selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Topic Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Chủ đề chính:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(STANDARD_TOPICS) { t ->
                                        FilterChip(
                                            selected = topic == t,
                                            onClick = { topic = t },
                                            label = { Text(t, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }

                            // Difficulty Selector
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mức độ:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    items(DIFFICULTY_LEVELS) { d ->
                                        FilterChip(
                                            selected = difficulty == d,
                                            onClick = { difficulty = d },
                                            label = { Text(d, fontSize = 11.sp) }
                                        )
                                    }
                                }
                            }
                        }

                        // Math Topic Tags Selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Thẻ phân loại topic (Tags):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(PREDEFINED_MATH_TAGS) { tag ->
                                    val isSelected = selectedTags.contains(tag)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTags = if (isSelected) {
                                                selectedTags - tag
                                            } else {
                                                selectedTags + tag
                                            }
                                        },
                                        label = { Text("#$tag", fontSize = 11.sp) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                        } else null
                                    )
                                }
                            }

                            // Add Custom Tag Input
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customTagInput,
                                    onValueChange = { customTagInput = it },
                                    modifier = Modifier.weight(1f),
                                    placeholder = { Text("Thêm tag tùy chỉnh...", fontSize = 12.sp) },
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        val clean = customTagInput.trim().replace("#", "")
                                        if (clean.isNotEmpty()) {
                                            selectedTags = selectedTags + clean
                                            customTagInput = ""
                                        }
                                    },
                                    enabled = customTagInput.isNotBlank()
                                ) {
                                    Text("Thêm tag")
                                }
                            }
                        }

                        // Question Content Input
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Nội dung câu hỏi:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )

                            // Quick Math Toolbar
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(mathSymbols) { symbol ->
                                    SuggestionChip(
                                        onClick = { content += symbol },
                                        label = { Text(symbol, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = content,
                                onValueChange = { content = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .testTag("form_question_content"),
                                placeholder = { Text("Nhập đề bài câu hỏi (Ví dụ: Cho hàm số f(x) = x^2 - 4x + 3...)") }
                            )
                        }

                        // Options A, B, C, D Inputs with Correct Radio Button
                        Text(
                            text = "Các lựa chọn đáp án (Chọn đáp án đúng):",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        val optionStates = listOf(
                            Triple("A", optionA) { str: String -> optionA = str },
                            Triple("B", optionB) { str: String -> optionB = str },
                            Triple("C", optionC) { str: String -> optionC = str },
                            Triple("D", optionD) { str: String -> optionD = str }
                        )

                        optionStates.forEachIndexed { index, (label, valStr, onChange) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = correctIndex == index,
                                    onClick = { correctIndex = index }
                                )
                                OutlinedTextField(
                                    value = valStr,
                                    onValueChange = onChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("form_option_$label"),
                                    label = { Text("Lựa chọn $label") },
                                    singleLine = true
                                )
                            }
                        }

                        // Explanation Input
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Lời giải chi tiết:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            OutlinedTextField(
                                value = explanation,
                                onValueChange = { explanation = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .testTag("form_explanation"),
                                placeholder = { Text("Nhập các bước giải chi tiết để học sinh tham khảo...") }
                            )
                        }
                    }
                } else {
                    // Live Math Formula Preview Tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Xem trước hiển thị thực tế:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )

                        MathQuestionCard(
                            question = MathQuestionEntity(
                                id = questionToEdit?.id ?: 0,
                                content = content.ifBlank { "(Chưa có nội dung đề bài)" },
                                optionA = optionA.ifBlank { "Lựa chọn A" },
                                optionB = optionB.ifBlank { "Lựa chọn B" },
                                optionC = optionC.ifBlank { "Lựa chọn C" },
                                optionD = optionD.ifBlank { "Lựa chọn D" },
                                correctIndex = correctIndex,
                                explanation = explanation.ifBlank { "(Chưa có lời giải)" },
                                topic = topic,
                                tagsJson = MathQuestionEntity.tagsListToJson(selectedTags.toList()),
                                difficulty = difficulty
                            ),
                            onEdit = {},
                            onDelete = {},
                            onAddToExam = {}
                        )
                    }
                }

                // Save & Cancel Action Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Hủy")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val newEntity = MathQuestionEntity(
                                id = questionToEdit?.id ?: 0,
                                content = content,
                                optionA = optionA,
                                optionB = optionB,
                                optionC = optionC,
                                optionD = optionD,
                                correctIndex = correctIndex,
                                explanation = explanation,
                                topic = topic,
                                tagsJson = MathQuestionEntity.tagsListToJson(selectedTags.toList()),
                                difficulty = difficulty
                            )
                            onSave(newEntity)
                        },
                        enabled = content.isNotBlank() && optionA.isNotBlank() && optionB.isNotBlank(),
                        modifier = Modifier.testTag("save_question_button")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lưu câu hỏi")
                    }
                }
            }
        }
    }
}
