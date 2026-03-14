package com.facesearch.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.facesearch.app.util.ImageHasher
import com.facesearch.app.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    var targetImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFolderUri by remember { mutableStateOf<Uri?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0) }
    var scanTotal by remember { mutableStateOf(0) }
    var statusText by remember { mutableStateOf("请选择目标人物照片") }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var imageHashes by remember { mutableStateOf<List<ImageHash>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showLogs by remember { mutableStateOf(false) }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermission = permissions.values.all { it }
        if (!hasPermission) {
            Logger.logWarning("Permissions not granted")
        }
    }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        targetImageUri = uri
        if (uri != null) {
            statusText = "目标照片已选择"
            Logger.logInfo("Target image selected: $uri")
        }
    }
    
    // Folder picker launcher
    val folderPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        selectedFolderUri = uri
        if (uri != null) {
            try {
                val takeFlags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                statusText = "开始扫描..."
                isScanning = true
                imageHashes = emptyList()
                searchResults = emptyList()
                
                scope.launch {
                    try {
                        val hashes = scanFolderForImages(context, uri) { current, total ->
                            scanProgress = current
                            scanTotal = total
                        }
                        imageHashes = hashes
                        isScanning = false
                        statusText = "扫描完成，找到 ${hashes.size} 张图片"
                        Logger.logInfo("Folder scan complete: ${hashes.size} images")
                    } catch (e: Exception) {
                        isScanning = false
                        errorMessage = "扫描失败: ${e.message}"
                        Logger.logError("FolderScan", e.message ?: "Unknown error", e)
                    }
                }
            } catch (e: Exception) {
                errorMessage = "选择文件夹失败: ${e.message}"
                Logger.logError("FolderSelect", e.message ?: "Unknown error", e)
            }
        }
    }
    
    // Check permissions on launch
    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        } else {
            hasPermission = true
        }
    }
    
    // Error dialog
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("发生错误") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                TextButton(onClick = { errorMessage = null }) {
                    Text("关闭")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showLogs = true
                    errorMessage = null
                }) {
                    Text("查看日志")
                }
            }
        )
    }
    
    // Logs dialog
    if (showLogs) {
        val logContent = remember { Logger.getLogContent() }
        AlertDialog(
            onDismissRequest = { showLogs = false },
            title = { Text("应用日志") },
            text = {
                LazyColumn {
                    item {
                        Text(
                            logContent,
                            fontSize = 10.sp,
                            modifier = Modifier.heightIn(max = 400.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    Logger.clearLogs()
                    showLogs = false
                }) {
                    Text("清除日志")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogs = false }) {
                    Text("关闭")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🔍 人脸搜索", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { showLogs = true }) {
                            Icon(Icons.Default.BugReport, contentDescription = "日志", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6366F1),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAFC))
        ) {
            // Target image selection card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🎯 目标人物", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                            .border(3.dp, Color(0xFF6366F1), CircleShape)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (targetImageUri != null) {
                            AsyncImage(
                                model = targetImageUri,
                                contentDescription = "目标照片",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = Color(0xFF94A3B8)
                                )
                                Text("点击上传照片", color = Color(0xFF94A3B8), fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(Color(0xFF3B82F6)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("选择目标照片")
                    }
                }
            }
            
            // Folder selection card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.FolderOpen, 
                            contentDescription = null, 
                            tint = Color(0xFF10B981), 
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("📂 搜索目录", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isScanning
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (selectedFolderUri != null) "重新选择文件夹" else "选择文件夹")
                    }
                    
                    if (isScanning) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = if (scanTotal > 0) scanProgress.toFloat() / scanTotal else 0f,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF6366F1)
                        )
                        Text(
                            "扫描中: $scanProgress / $scanTotal", 
                            modifier = Modifier.padding(top = 8.dp), 
                            color = Color(0xFF64748B), 
                            fontSize = 12.sp
                        )
                    }
                    
                    if (imageHashes.isNotEmpty() && !isScanning) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "已加载 ${imageHashes.size} 张图片", 
                            color = Color(0xFF10B981), 
                            fontSize = 12.sp
                        )
                    }
                }
            }
            
            // Search button
            Button(
                onClick = {
                    if (targetImageUri != null && imageHashes.isNotEmpty()) {
                        isSearching = true
                        scope.launch {
                            try {
                                val results = searchSimilarImages(context, targetImageUri!!, imageHashes)
                                searchResults = results
                                statusText = "找到 ${results.size} 个相似图片"
                                Logger.logInfo("Search complete: ${results.size} results")
                            } catch (e: Exception) {
                                errorMessage = "搜索失败: ${e.message}"
                                Logger.logError("Search", e.message ?: "Unknown error", e)
                            }
                            isSearching = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(Color(0xFFEF4444)),
                enabled = targetImageUri != null && imageHashes.isNotEmpty() && !isScanning && !isSearching
            ) {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("开始搜索", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            // Status text
            Text(
                statusText, 
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp), 
                textAlign = TextAlign.Center, 
                color = Color(0xFF64748B)
            )
            
            // Search results
            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "📋 搜索结果 (${searchResults.size}个)", 
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 16.sp
                )
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { result ->
                        SearchResultItem(result = result)
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(result: SearchResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = result.uri,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    result.name, 
                    fontWeight = FontWeight.Medium, 
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "相似度: ${result.similarity}%", 
                    color = when {
                        result.similarity >= 80 -> Color(0xFF10B981)
                        result.similarity >= 60 -> Color(0xFFF59E0B)
                        else -> Color(0xFFEF4444)
                    }, 
                    fontSize = 12.sp
                )
            }
            
            Icon(
                Icons.Default.Image, 
                contentDescription = null, 
                tint = Color(0xFF6366F1)
            )
        }
    }
}

data class SearchResult(
    val uri: Uri,
    val name: String,
    val similarity: Int
)

data class ImageHash(
    val uri: Uri,
    val hash: String,
    val name: String
)

/**
 * Scan folder for images and calculate their hashes
 */
suspend fun scanFolderForImages(
    context: android.content.Context, 
    folderUri: Uri, 
    onProgress: (Int, Int) -> Unit
): List<ImageHash> = withContext(Dispatchers.IO) {
    val hashes = mutableListOf<ImageHash>()
    
    try {
        val documentFile = androidx.documentfile.provider.DocumentFile.fromTreeUri(context, folderUri)
        val files = documentFile?.listFiles()?.filter { 
            it.isFile && it.type?.startsWith("image") == true 
        } ?: emptyList()
        
        val total = files.size
        Logger.logInfo("Found $total image files")
        
        files.forEachIndexed { index, file ->
            try {
                val uri = file.uri
                val hash = ImageHasher.calculateHash(context, uri)
                
                if (hash != null) {
                    val name = file.name ?: "未知_${index}"
                    hashes.add(ImageHash(uri, hash, name))
                }
            } catch (e: Exception) {
                Logger.logWarning("Error processing file: ${e.message}")
            }
            
            if ((index + 1) % 10 == 0) {
                onProgress(index + 1, total)
            }
        }
        
        onProgress(total, total)
        
    } catch (e: Exception) {
        Logger.logError("ScanFolder", "Error scanning folder", e)
        throw e
    }
    
    hashes
}

/**
 * Search for similar images using local hashing
 */
suspend fun searchSimilarImages(
    context: android.content.Context,
    targetUri: Uri,
    imageHashes: List<ImageHash>
): List<SearchResult> = withContext(Dispatchers.IO) {
    val results = mutableListOf<SearchResult>()
    
    try {
        val targetHash = ImageHasher.calculateHash(context, targetUri)
        
        if (targetHash == null) {
            Logger.logError("Search", "Failed to calculate target hash")
            return@withContext emptyList()
        }
        
        Logger.logInfo("Target hash: ${targetHash.take(16)}...")
        
        // Compare with all known images
        imageHashes.forEach { imageHash ->
            val similarity = ImageHasher.calculateSimilarity(targetHash, imageHash.hash)
            
            // Only include results with similarity > 50%
            if (similarity > 50) {
                results.add(SearchResult(imageHash.uri, imageHash.name, similarity))
            }
        }
        
    } catch (e: Exception) {
        Logger.logError("Search", "Error during search", e)
    }
    
    // Sort by similarity descending
    results.sortedByDescending { it.similarity }
}
