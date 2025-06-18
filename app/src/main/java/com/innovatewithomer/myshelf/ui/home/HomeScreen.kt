package com.innovatewithomer.myshelf.ui.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.innovatewithomer.myshelf.data.local.entity.BookEntity
import com.innovatewithomer.myshelf.viewmodel.BookViewModel
import com.innovatewithomer.myshelf.viewmodel.UploadState
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen() {
    val bookViewModel: BookViewModel = hiltViewModel()
    val userId = FirebaseAuth.getInstance().currentUser?.uid?:return
    val books by bookViewModel.books.collectAsState()
    val isLoading by bookViewModel.isLoading.collectAsState()
    val uploadState by bookViewModel.uploadState.collectAsState()
    var isPdfOpening by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(books) {
        bookViewModel.getBooks(userId)
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) {uri: Uri? ->

        context.contentResolver.takePersistableUriPermission(
            uri?: return@rememberLauncherForActivityResult,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        uri.let {
            bookViewModel.addBook(userId, uri)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Shelf") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                launcher.launch(arrayOf("application/pdf"))
            }) {

                Icon(Icons.Default.Add, contentDescription = "Add Book")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Main content
            books?.let {
                if (it.isNotEmpty()) {
                    LazyColumn(
                        contentPadding = paddingValues,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(it) { book ->
                            BookItem(
                                book = book,
                                isPdfLoading = isPdfOpening,
                                onLoadingChange = { isPdfOpening = it }
                            )
                        }
                    }
                } else if (isLoading) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "No Books found",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Upload state overlay
            when (uploadState) {
                UploadState.Uploading -> {
//                    Box(
//                        contentAlignment = Alignment.Center,
//                        modifier = Modifier
//                            .fillMaxSize()
//                    ) {
//                        CircularProgressIndicator()
//                    }
                }

                is UploadState.Failure -> {
                    Toast.makeText(context, "Upload Failed", Toast.LENGTH_SHORT).show()
                }

                is UploadState.Success -> {
                    Toast.makeText(context, "Upload Successful", Toast.LENGTH_SHORT).show()
                    bookViewModel.clearUploadState()
                }

                UploadState.Idle -> {}
            }

            // PDF loading overlay (always on top)
            if (isPdfOpening) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.150f)),
                    contentAlignment = Alignment.Center
                ) {
                    ShowPdfLoading()
                }
            }
        }
    }
}

@Composable
fun BookItem(
    book: BookEntity,
    isPdfLoading: Boolean,
    onLoadingChange: (Boolean) -> Unit,
//    onDelete: (BookEntity)-> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(enabled = !isPdfLoading) {
                scope.launch {
                    onLoadingChange(true)
                    try {
                        if (book.isSynced) {
                            val fileName = "${book.title}.pdf"
                            val cachedFile = downloadAndCachePdf(context, book.fileUrl, fileName)
                            if (cachedFile != null) {
                                openPdf(context, cachedFile)
                            } else {
                                Toast.makeText(context, "Error opening Firebase PDF", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val localFile = File(book.fileUrl)
                            if (localFile.exists()) {
                                openPdf(context, localFile)
                            } else {
                                Toast.makeText(context, "Local file not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } finally {
                        onLoadingChange(false)
                    }
                }
            },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = book.title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = book.author, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text(text = book.author, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.width(2.dp))
            }
        }
    }
}


@Composable
fun ShowPdfLoading(modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .padding(32.dp)
    ) {
        Text(
            text = "Opening book PDF...",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(4.dp),
            color = Color.White
        )
    }
}


suspend fun downloadAndCachePdf(context: Context, fileUrl: String, fileName: String): File? {
    return try {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(fileUrl)
        val localFile = File(context.getExternalFilesDir("pdfs"), fileName)
        if (!localFile.exists()) {
            storageRef.getFile(localFile).await()
        }
        localFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}





fun openPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileProvider",
        file
    )

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri,"application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Open Pdf With...")
    context.startActivity(chooser)
}

fun openPdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(intent, "Open PDF With...")
    context.startActivity(chooser)
}
