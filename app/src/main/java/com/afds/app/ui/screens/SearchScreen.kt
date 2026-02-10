package com.afds.app.ui.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.afds.app.AFDSApplication
import com.afds.app.data.model.FileCategory
import com.afds.app.data.model.FileDetails
import com.afds.app.data.model.FileItem
import com.afds.app.data.remote.ApiException
import com.afds.app.ui.components.AFDSTopBar
import com.afds.app.ui.components.FileDetailDialog
import com.afds.app.ui.components.FileListContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    category: String,
    onBack: () -> Unit,
    onNewSearch: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val apiClient = AFDSApplication.instance.apiClient
    val sessionManager = AFDSApplication.instance.sessionManager

    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(1) }
    var totalPages by remember { mutableIntStateOf(1) }
    var totalFiles by remember { mutableIntStateOf(0) }
    var newSearchQuery by remember { mutableStateOf("") }

    // File details dialog state
    var showDetailsDialog by remember { mutableStateOf(false) }
    var detailsLoading by remember { mutableStateOf(false) }
    var fileDetails by remember { mutableStateOf<FileDetails?>(null) }
    var detailsCategory by remember { mutableStateOf("") }
    var detailsFileId by remember { mutableStateOf("") }

    val catEnum = try { FileCategory.fromApiName(category) } catch (_: Exception) { FileCategory.MEDIA }

    fun loadSearch(page: Int) {
        scope.launch {
            isLoading = true
            try {
                Log.d("AFDS_SEARCH", "Searching: query='$query', category='$category', page=$page")
                val response = apiClient.searchFiles(category, query, page)
                Log.d("AFDS_SEARCH", "Got ${response.files.size} files, page=${response.currentPageInt}/${response.totalPagesInt}")
                files = response.files
                currentPage = response.currentPageInt
                totalPages = response.totalPagesInt
                totalFiles = response.totalFilesInt
            } catch (e: ApiException) {
                Log.e("AFDS_SEARCH", "ApiException: ${e.message}, code=${e.statusCode}", e)
                if (e.statusCode == 401) {
                    sessionManager.clearSession()
                    onLogout()
                } else {
                    Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("AFDS_SEARCH", "Exception during search: ${e::class.simpleName}: ${e.message}", e)
                Toast.makeText(context, e.message ?: "Search failed", Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(query, category) {
        loadSearch(1)
    }

    Scaffold(
        topBar = {
            AFDSTopBar(
                title = "Search Results",
                onBack = onBack,
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            sessionManager.clearSession()
                            onLogout()
                        }
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
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
            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = newSearchQuery,
                    onValueChange = { newSearchQuery = it },
                    placeholder = { Text("New search...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                            if (newSearchQuery.isNotBlank()) {
                                onNewSearch(newSearchQuery.trim(), category)
                            }
                        }
                    ),
                    modifier = Modifier.weight(1f),
                    trailingIcon = {
                        IconButton(onClick = {
                            focusManager.clearFocus()
                            if (newSearchQuery.isNotBlank()) {
                                onNewSearch(newSearchQuery.trim(), category)
                            }
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }
                )
            }

            // Results info
            Text(
                text = "\"$query\" in ${catEnum.displayName}" +
                        if (totalFiles > 0) " ($totalFiles results)" else "",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // File list
            FileListContent(
                files = files,
                currentCategory = catEnum.shortName,
                isLoading = isLoading,
                currentPage = currentPage,
                totalPages = totalPages,
                onPageChange = { loadSearch(it) },
                onDetailsClick = { fileId: String, cat: String ->
                    scope.launch {
                        detailsLoading = true
                        showDetailsDialog = true
                        detailsCategory = cat
                        detailsFileId = fileId
                        fileDetails = null
                        try {
                            fileDetails = apiClient.getFileDetails(cat, fileId)
                        } catch (e: Exception) {
                            Log.e("AFDS_SEARCH", "Details error for $fileId/$cat: ${e.message}", e)
                            Toast.makeText(context, e.message ?: "Failed to load details", Toast.LENGTH_SHORT).show()
                            showDetailsDialog = false
                        } finally {
                            detailsLoading = false
                        }
                    }
                }
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