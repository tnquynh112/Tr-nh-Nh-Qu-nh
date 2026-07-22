package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiPlatformGuideTab() {
    var selectedTab by remember { mutableStateOf(0) } // 0: iOS, 1: Samsung (S25 Ultra), 2: Desktop & Laptop
    var selectedDesktopOS by remember { mutableStateOf(0) } // 0: Windows, 1: macOS
    val context = LocalContext.current
    
    // Checklists state
    val iosSteps = remember {
        mutableStateListOf(
            "Truy cập Shared App URL của MindUp trên trình duyệt Safari của iPhone/iPad.",
            "Bấm vào biểu tượng 'Chia sẻ' (Share - hình vuông có mũi tên chỉ lên) ở thanh công cụ phía dưới.",
            "Kéo xuống dưới danh sách tùy chọn và chọn 'Thêm vào MH chính' (Add to Home Screen).",
            "Đặt tên hiển thị là 'MindUp Toán 12' và bấm 'Thêm' (Add) ở góc trên bên phải.",
            "Quay lại màn hình chính của iPhone, mở biểu tượng MindUp để trải nghiệm chế độ Độc lập (Standalone) không thanh địa chỉ!"
        )
    }
    val iosChecked = remember { mutableStateListOf(false, false, false, false, false) }

    val samsungSteps = remember {
        mutableStateListOf(
            "Tải file APK từ AI Studio Build hoặc xuất file nén dự án để lấy file APK cài đặt.",
            "Truy cập ứng dụng 'File của bạn' (My Files) trên S25 Ultra, tìm đến thư mục 'Tải về' (Downloads) và chọn file APK.",
            "Nếu xuất hiện cảnh báo của Trình chặn tự động (Auto Blocker - Tính năng bảo mật One UI 8.5/8): Truy cập 'Cài đặt' -> 'Bảo mật và riêng tư' -> Tắt tạm thời 'Trình chặn tự động' để cấp phép tải bên ngoài.",
            "Sau khi cài đặt thành công, nếu Google Play Protect cảnh báo 'Ứng dụng chưa được xác minh', bấm 'Xem chi tiết' (More details) -> 'Vẫn cài đặt' (Install anyway).",
            "Mở app từ danh sách ứng dụng One UI, cấp quyền cần thiết và bắt đầu ôn luyện Toán 12!"
        )
    }
    val samsungChecked = remember { mutableStateListOf(false, false, false, false, false) }

    val windowsSteps = remember {
        mutableStateListOf(
            "Mở Google Chrome, Microsoft Edge hoặc Cốc Cốc trên máy tính Windows (PC/Laptop) và truy cập trang web ứng dụng.",
            "Nhìn lên thanh địa chỉ (Address Bar) ở góc phải, bấm vào biểu tượng hình màn hình có mũi tên tải xuống hoặc dấu '+' (Cài đặt ứng dụng). Hoặc mở Menu (dấu 3 chấm) -> Chọn 'Cài đặt MindUp...'.",
            "Bấm nút 'Cài đặt' (Install) trong hộp thoại xác nhận hiện ra.",
            "Ứng dụng sẽ được khởi tạo độc lập thành một app Desktop chạy mượt mà, khởi động cực nhanh và tạo biểu tượng tắt (Shortcut) trực tiếp trên Desktop của bạn.",
            "Bấm chuột phải vào biểu tượng ứng dụng trên thanh Taskbar và chọn 'Ghim vào thanh tác vụ' (Pin to Taskbar) để mở nhanh bất cứ lúc nào!"
        )
    }
    val windowsChecked = remember { mutableStateListOf(false, false, false, false, false) }

    val macSteps = remember {
        mutableStateListOf(
            "Sử dụng trình duyệt Safari hoặc Google Chrome trên MacBook / iMac và truy cập trang web ứng dụng.",
            "Nếu dùng Safari: Bấm vào menu 'Tệp' (File) trên thanh hệ thống phía trên cùng -> Chọn 'Thêm vào Dock...' (Add to Dock...).",
            "Nếu dùng Google Chrome: Nhấp vào dấu 3 chấm ở góc trên bên phải -> Chọn 'Lưu và chia sẻ' -> Chọn 'Cài đặt trang này làm ứng dụng...'.",
            "Đặt tên hiển thị là 'MindUp Toán 12' và bấm nút 'Thêm' (Add) hoặc 'Cài đặt' (Install).",
            "Ứng dụng sẽ tự động xuất hiện trên thanh Dock và trong Launchpad của macOS dưới dạng một ứng dụng độc lập, hoạt động mượt mà và tối ưu hóa pin cho chip Apple Silicon!"
        )
    }
    val macChecked = remember { mutableStateListOf(false, false, false, false, false) }

    // Dynamic progress calculation
    val currentStepsSize = when (selectedTab) {
        0 -> iosSteps.size
        1 -> samsungSteps.size
        else -> {
            if (selectedDesktopOS == 0) windowsSteps.size else macSteps.size
        }
    }
    val currentCheckedCount = when (selectedTab) {
        0 -> iosChecked.count { it }
        1 -> samsungChecked.count { it }
        else -> {
            if (selectedDesktopOS == 0) windowsChecked.count { it } else macChecked.count { it }
        }
    }
    val progress = if (currentStepsSize > 0) currentCheckedCount.toFloat() / currentStepsSize else 0f

    // Success dialog state
    var showSuccessDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("multi_platform_guide_tab")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Text(
                        text = "Trung Tâm Đa Nền Tảng",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = "Hướng dẫn cài đặt phần mềm MindUp lên các thiết bị Android mới nhất (S25 Ultra, One UI 8.5), iOS (iPhone/iPad) và máy tính Windows.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Platform Tab selector
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.PhoneIphone, contentDescription = "iOS Icon", modifier = Modifier.size(16.dp))
                            Text("iPhone & iOS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.Android, contentDescription = "Android Icon", modifier = Modifier.size(16.dp))
                            Text("S25 & OneUI", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(imageVector = Icons.Default.Laptop, contentDescription = "Desktop Laptop Icon", modifier = Modifier.size(16.dp))
                            Text("Desktop & Laptop", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                )
            }
        }

        // Sub-selector for Desktop/Laptop OS
        if (selectedTab == 2) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Windows button
                    val isWindows = selectedDesktopOS == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isWindows) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                            .border(
                                width = if (isWindows) 2.dp else 1.dp,
                                color = if (isWindows) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDesktopOS = 0 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Window,
                                contentDescription = "Windows OS",
                                tint = if (isWindows) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Windows (PC/Laptop)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWindows) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // macOS button
                    val isMac = selectedDesktopOS == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isMac) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                            .border(
                                width = if (isMac) 2.dp else 1.dp,
                                color = if (isMac) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedDesktopOS = 1 },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Laptop,
                                contentDescription = "macOS Apple",
                                tint = if (isMac) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "macOS (MacBook/iMac)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMac) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            }
        }

        // Dynamic installation instructions based on tab
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header of selected platform
                val platformTitle = when (selectedTab) {
                    0 -> "Hướng dẫn cài đặt trên iOS (iPhone/iPad)"
                    1 -> "Hướng dẫn Sideload APK trên Samsung (One UI 8.5 / S25)"
                    else -> {
                        if (selectedDesktopOS == 0) "Cài đặt app trên Máy tính Windows (PC/Laptop)"
                        else "Cài đặt app trên Máy tính Apple (MacBook/iMac)"
                    }
                }
                
                val platformDesc = when (selectedTab) {
                    0 -> "Chạy độc lập mượt mà không cần thông qua App Store bằng công nghệ Progressive Web App (PWA) hiện đại."
                    1 -> "Khắc phục cơ chế kiểm duyệt nghiêm ngặt trên One UI 8.5 Auto Blocker của Samsung S25 Ultra để cài file APK."
                    else -> {
                        if (selectedDesktopOS == 0) "Biến MindUp thành một ứng dụng Windows độc lập, mượt mà và khởi chạy trực tiếp từ Desktop/Taskbar cực nhẹ."
                        else "Ghim MindUp trực tiếp lên Dock và Launchpad trên macOS, được tối ưu hóa năng lượng tuyệt đối cho chip Apple Silicon."
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = platformTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            text = platformDesc,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Interactive progress bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = progress,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "${(progress * 100).toInt()}% Hoàn thành",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Checklist Steps Items
        val stepsList = when (selectedTab) {
            0 -> iosSteps
            1 -> samsungSteps
            else -> {
                if (selectedDesktopOS == 0) windowsSteps else macSteps
            }
        }
        val checkedState = when (selectedTab) {
            0 -> iosChecked
            1 -> samsungChecked
            else -> {
                if (selectedDesktopOS == 0) windowsChecked else macChecked
            }
        }

        items(stepsList.size) { index ->
            val stepText = stepsList[index]
            val isChecked = checkedState[index]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { checkedState[index] = !isChecked },
                colors = CardDefaults.cardColors(
                    containerColor = if (isChecked) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                border = BorderStroke(
                    1.dp,
                    if (isChecked) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Checkbox
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { checkedState[index] = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = MaterialTheme.colorScheme.primary,
                            checkmarkColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    // Step Number + Content
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bước ${index + 1}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stepText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Verify / Celebration Action button
        item {
            Button(
                onClick = {
                    val allDone = checkedState.all { it }
                    if (allDone) {
                        showSuccessDialog = true
                    } else {
                        Toast.makeText(context, "Vui lòng tích hoàn thành các bước hướng dẫn!", Toast.LENGTH_SHORT).show()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .height(50.dp)
                    .testTag("verify_install_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Text("Xác nhận Đã Cài Đặt", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }

        // Kotlin Multiplatform Technical Note Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Góc Kỹ Thuật (KMP & Compose Multiplatform)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                    Text(
                        text = "Về mặt phát triển, MindUp được viết hoàn toàn bằng Jetpack Compose. Sử dụng Kotlin Multiplatform (KMP), toàn bộ phần logic (ExamViewModel, QuestionBank) và giao diện Compose UI này có thể dễ dàng biên dịch chéo trực tiếp sang nền tảng iOS mà không cần viết lại mã Swift! Điều này đảm bảo hiệu năng gốc tuyệt đối và trải nghiệm đồng nhất 100% giữa S25 Ultra (Android) và iPhone.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Success Celebration Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("Tuyệt vời!", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.SentimentSatisfiedAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "Cài đặt thành công!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Text(
                    text = "Chúc mừng bạn! Ứng dụng MindUp Toán 12 đã sẵn sàng hoạt động độc lập trên thiết bị của bạn. Chúc bạn có những giờ ôn thi thật hiệu quả và đạt điểm số tối đa trong kỳ thi THPT quốc gia!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}
