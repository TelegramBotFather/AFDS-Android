package com.afds.app.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.afds.app.AFDSApplication
import com.afds.app.data.model.FileDetails
import com.afds.app.data.model.FileItem
import com.afds.app.data.remote.ApiException
import com.afds.app.ui.components.AFDSTopBar
import com.afds.app.ui.components.FileDetailDialog
import com.afds.app.ui.components.FileListContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFilesScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiClient = AFDSApplication.instance.apiClient
    val sessionManager = AFDSApplication.instance.sessionManager

    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }

    // File details dialog state
    var showDetailsDialog by remember { mutableStateOf(false) }
    var detailsLoading by remember { mutableStateOf(false) }
    var fileDetails by remember { mutableStateOf<FileDetails?>(null) }
    var detailsCategory by remember { mutableStateOf("") }

    fun loadMyFiles(page: Int) {
        scope.launch {
            isLoading = true
            try {
                val token = sessionManager.getToken() ?: run {
                    onLogout()
                    return@launch
                }
                val response = apiClient.getMyFiles(token, page)
                if (response.files.isNotEmpty()) {
                    val f = response.files[0]
                    Log.d("AFDS_MYFILES", "First file: id=${f.id}, fileId=${f.fileId}, effectiveId=${f.effectiveId}, category=${f.category}, effectiveCategory=${f.effectiveCategory}, fileName=${f.fileName}")
                }
                files = response.files
                currentPage = response.currentPageInt
                totalPages = response.totalPagesInt
            } catch (e: ApiException) {
                if (e.statusCode == 401) {
                    sessionManager.clearSession()
                    onLogout()
                } else {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Failed to load files", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadMyFiles(1)
    }

    Scaffold(
        topBar = {
            AFDSTopBar(
                title = "My Files",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            sessionManager.clearSession()
                            onLogout()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            FileListContent(
                files = files,
                currentCategory = "files",
                isLoading = isLoading,
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = { loadMyFiles(it) },
                onDetailsClick = { fileId: String, cat: String ->
                    scope.launch {
                        detailsLoading = true
                        showDetailsDialog = true
                        detailsCategory = cat
                        fileDetails = null
                        try {
                            fileDetails = apiClient.getFileDetails(cat, fileId)
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Failed to load details", Toast.LENGTH_SHORT).show()
                            showDetailsDialog = false
                        } finally {
                            detailsLoading = false
                        }
                    }
                },
                showSaveButton = false,
                emptyMessage = "No saved files yet."
            )
        }

        // Details dialog
        if (showDetailsDialog) {
            FileDetailDialog(
                details = fileDetails,
                category = detailsCategory,
                isLoading = detailsLoading,
                onDismiss = {
                    showDetailsDialog = false
                    fileDetails = null
                }
            )
        }
    }
}