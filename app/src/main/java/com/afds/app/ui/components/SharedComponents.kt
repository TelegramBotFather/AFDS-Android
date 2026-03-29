package com.afds.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.afds.app.AFDSApplication
import com.afds.app.data.model.FileCategory
import com.afds.app.data.model.FileDetails
import com.afds.app.data.model.FileItem
import com.afds.app.util.DownloadHelper
import com.afds.app.util.formatBytes
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AFDSTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@Composable
fun FileCard(
    file: FileItem,
    currentCategory: String,
    onDetailsClick: (String, String) -> Unit,
    showSaveButton: Boolean = true,
    onRemoveClick: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient = AFDSApplication.instance.apiClient
    val sessionManager = AFDSApplication.instance.sessionManager
    var isDownloading by remember { mutableStateOf(false) }
    var isCopying by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }
    var isSendingToChannel by remember { mutableStateOf(false) }
    val userChannelId by sessionManager.channelId.collectAsState(initial = null)
    val downloaderApp by sessionManager.downloaderApp.collectAsState(initial = "default")

    val fileId = file.effectiveId
    val category = file.effectiveCategory.ifEmpty { currentCategory }
    val catEnum = try { FileCategory.fromShortName(category) } catch (_: Exception) { FileCategory.MEDIA }
    // For saving: currentCategory is always reliable (comes from the screen context).
    // effectiveCategory from API response can be wrong (e.g. "files" for music results).
    val saveCatEnum = try { FileCategory.fromShortName(currentCategory) } catch (_: Exception) { catEnum }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // File name
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // File info row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = formatBytes(file.fileSizeLong),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SuggestionChip(
                    onClick = { },
                    label = {
                        Text(
                            text = catEnum.displayName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.height(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Save button
                if (showSaveButton) {
                    IconButton(
                        onClick = {
                            if (fileId.isEmpty()) {
                                Toast.makeText(context, "Invalid file ID", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            scope.launch {
                                isSaving = true
                                try {
                                    val token = sessionManager.getToken() ?: return@launch
                                    apiClient.saveFile(
                                        token = token,
                                        fileId = fileId,
                                        category = saveCatEnum.apiName,
                                        fileName = file.displayName,
                                        fileSize = file.fileSizeLong
                                    )
                                    isSaved = true
                                    Toast.makeText(context, "File saved!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Failed to save", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSaving = false
                                }
                            }
                        },
                        enabled = !isSaving && !isSaved
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (isSaved) Icons.Default.CheckCircle else Icons.Default.AddCircle,
                                contentDescription = "Save",
                                tint = if (isSaved) MaterialTheme.colorScheme.secondary
                                else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Telegram / Send to Channel button
                IconButton(
                    onClick = {
                        if (fileId.isEmpty()) {
                            Toast.makeText(context, "Invalid file ID", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        val channelIdVal = userChannelId
                        if (channelIdVal != null && channelIdVal.isNotEmpty()) {
                            // Send to channel
                            scope.launch {
                                isSendingToChannel = true
                                try {
                                    val token = sessionManager.getToken() ?: return@launch
                                    val uniqueId = catEnum.getTelegramPrefix(fileId)
                                    val response = apiClient.sendToChannel(token, uniqueId, channelIdVal)
                                    if (response.success) {
                                        Toast.makeText(context, "Sent to channel!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        throw Exception(response.error ?: response.message ?: "Failed to send")
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Failed to send to channel", Toast.LENGTH_SHORT).show()
                                } finally {
                                    isSendingToChannel = false
                                }
                            }
                        } else {
                            // Open bot URL
                            val url = catEnum.getTelegramBotUrl(fileId)
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        }
                    },
                    enabled = !isSendingToChannel
                ) {
                    if (isSendingToChannel) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            if (userChannelId != null) Icons.Default.Podcasts else Icons.AutoMirrored.Filled.Send,
                            contentDescription = if (userChannelId != null) "Send to Channel" else "Telegram",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                // Copy link button
                IconButton(
                    onClick = {
                        if (fileId.isEmpty()) {
                            Toast.makeText(context, "Invalid file ID", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        scope.launch {
                            isCopying = true
                            try {
                                val token = sessionManager.getToken() ?: return@launch
                                val response = apiClient.generateDownloadLink(
                                    token,
                                    catEnum.getTableName(),
                                    fileId
                                )
                                if (response.success && response.url != null) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Download URL", response.url))
                                    Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                                } else {
                                    throw Exception(response.error ?: "Failed to generate link")
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_SHORT).show()
                            } finally {
                                isCopying = false
                            }
                        }
                    },
                    enabled = !isCopying
                ) {
                    if (isCopying) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Download button (direct download)
                IconButton(
                    onClick = {
                        if (fileId.isEmpty()) {
                            Toast.makeText(context, "Invalid file ID", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        scope.launch {
                            isDownloading = true
                            try {
                                val token = sessionManager.getToken() ?: return@launch
                                val response = apiClient.generateDownloadLink(
                                    token,
                                    catEnum.getTableName(),
                                    fileId
                                )
                                if (response.success && response.url != null) {
                                    DownloadHelper.download(context, response.url, file.displayName, downloaderApp)
                                } else {
                                    throw Exception(response.error ?: "Failed to generate link")
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_SHORT).show()
                            } finally {
                                isDownloading = false
                            }
                        }
                    },
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Download",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Remove button (My Files only)
                if (onRemoveClick != null) {
                    IconButton(
                        onClick = { onRemoveClick(fileId, category) }
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                // Details button
                IconButton(
                    onClick = {
                        if (fileId.isEmpty()) {
                            Toast.makeText(context, "Invalid file ID", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        onDetailsClick(fileId, category)
                    }
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun FileDetailDialog(
    details: FileDetails?,
    category: String,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    if (details != null || isLoading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = details?.fileName ?: details?.caption ?: "File Details",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (details != null) {
                    val catEnum = try { FileCategory.fromShortName(category) } catch (_: Exception) { FileCategory.MEDIA }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        details.fileName?.let {
                            DetailRow("File Name", it)
                        }
                        if (details.fileSizeLong > 0) {
                            DetailRow("File Size", formatBytes(details.fileSizeLong))
                        }
                        details.effectiveMimeType?.let {
                            DetailRow("MIME Type", it)
                        }
                        details.caption?.let {
                            DetailRow("Caption", it)
                        }
                        DetailRow("Category", catEnum.displayName)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun FileListContent(
    files: List<FileItem>,
    currentCategory: String,
    isLoading: Boolean,
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    onDetailsClick: (String, String) -> Unit,
    showSaveButton: Boolean = true,
    onRemoveClick: ((String, String) -> Unit)? = null,
    emptyMessage: String = "No results found.",
    headerContent: @Composable () -> Unit = {}
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Scroll to top when page changes
    LaunchedEffect(currentPage) {
        listState.scrollToItem(0)
    }

    // Show scroll-to-top FAB when scrolled down
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Loading overlay - spinner with dimmed background
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!isLoading && files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (!isLoading) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item { headerContent() }

                // Top pagination
                if (totalPages > 1) {
                    item {
                        PaginationBar(
                            currentPage = currentPage,
                            totalPages = totalPages,
                            onPageChange = onPageChange
                        )
                    }
                }

                items(files.size) { index ->
                    val file = files[index]
                    FileCard(
                        file = file,
                        currentCategory = currentCategory,
                        onDetailsClick = onDetailsClick,
                        showSaveButton = showSaveButton,
                        onRemoveClick = onRemoveClick
                    )
                }

                // Bottom pagination
                if (totalPages > 1) {
                    item {
                        PaginationBar(
                            currentPage = currentPage,
                            totalPages = totalPages,
                            onPageChange = onPageChange
                        )
                    }
                }
            }
        }

        // Floating scroll-to-top button
        AnimatedVisibility(
            visible = showScrollToTop && !isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        listState.animateScrollToItem(0)
                    }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Scroll to top")
            }
        }
    }
}

@Composable
fun PaginationBar(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous
        FilledTonalIconButton(
            onClick = { onPageChange(currentPage - 1) },
            enabled = currentPage > 1
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Page $currentPage of $totalPages",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.width(16.dp))

        // Next
        FilledTonalIconButton(
            onClick = { onPageChange(currentPage + 1) },
            enabled = currentPage < totalPages
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
        }
    }
}