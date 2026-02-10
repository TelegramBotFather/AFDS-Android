package com.afds.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    showSaveButton: Boolean = true
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient = AFDSApplication.instance.apiClient
    val sessionManager = AFDSApplication.instance.sessionManager
    var isDownloading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isSaved by remember { mutableStateOf(false) }

    val fileId = file.effectiveId
    val category = file.effectiveCategory.ifEmpty { currentCategory }
    val catEnum = try { FileCategory.fromShortName(category) } catch (_: Exception) { FileCategory.MEDIA }

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
                                    Log.d("AFDS_SAVE", "Saving file: id=$fileId, category=$category, name=${file.displayName}")
                                    apiClient.saveFile(
                                        token = token,
                                        fileId = fileId,
                                        category = category,
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

                // Telegram button
                IconButton(
                    onClick = {
                        if (fileId.isEmpty()) {
                            Toast.makeText(context, "Invalid file ID", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        val url = catEnum.getTelegramBotUrl(fileId)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Telegram",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                // Download button
                FilledTonalButton(
                    onClick = {
                        if (fileId.isEmpty()) {
                            Toast.makeText(context, "Invalid file ID", Toast.LENGTH_SHORT).show()
                            return@FilledTonalButton
                        }
                        scope.launch {
                            isDownloading = true
                            try {
                                Log.d("AFDS_DL", "Generating link: table=${catEnum.getTableName()}, id=$fileId")
                                val response = apiClient.generateDownloadLink(
                                    catEnum.getTableName(),
                                    fileId
                                )
                                if (response.success && response.url != null) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Download URL", response.url))
                                    Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
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
                    enabled = !isDownloading,
                    modifier = Modifier.padding(start = 4.dp)
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isDownloading) "..." else "Download", style = MaterialTheme.typography.labelMedium)
                }

                // Details button
                IconButton(
                    onClick = {
                        if (fileId.isEmpty()) {
                            Toast.makeText(context, "Invalid file ID", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        Log.d("AFDS_DETAILS", "Details click: id=$fileId, category=$category")
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
                        details.mimeType?.let {
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
    emptyMessage: String = "No results found.",
    headerContent: @Composable () -> Unit = {}
) {
    if (isLoading && files.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (files.isEmpty()) {
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
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item { headerContent() }

            items(files.size) { index ->
                val file = files[index]
                FileCard(
                    file = file,
                    currentCategory = currentCategory,
                    onDetailsClick = onDetailsClick,
                    showSaveButton = showSaveButton
                )
            }

            // Pagination
            if (totalPages > 1) {
                item {
                    PaginationBar(
                        currentPage = currentPage,
                        totalPages = totalPages,
                        onPageChange = onPageChange
                    )
                }
            }

            // Loading indicator at bottom
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
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
            .padding(16.dp),
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

@Composable
fun LoadingOverlay(isLoading: Boolean) {
    AnimatedVisibility(visible = isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}