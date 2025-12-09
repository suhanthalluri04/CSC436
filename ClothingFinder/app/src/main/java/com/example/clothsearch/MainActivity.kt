package com.example.clothsearch

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val vm: ResultsViewModel by viewModels()
    private val savedVm: SavedResultsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                vm.onCameraPermission(granted)
            }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            vm.onCameraPermission(true)
        }

        setContent {
            val colors = lightColorScheme(
                primary = Color(0xFF4A4A4A),
                onPrimary = Color.White,
                secondary = Color(0xFF6A6A6A),
                onSecondary = Color.White,
                background = Color(0xFFF6F6F7),
                onBackground = Color(0xFF1F1F1F),
                surface = Color(0xFFF6F6F7),
                onSurface = Color(0xFF1F1F1F),
                surfaceVariant = Color(0xFFE8E8EB),
                onSurfaceVariant = Color(0xFF3A3A3A)
            )
            MaterialTheme(colorScheme = colors) {
                AppScaffold(vm, savedVm)
            }
        }
        ResultsViewModel.appResolver = applicationContext.contentResolver
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(vm: ResultsViewModel, savedVm: SavedResultsViewModel) {
    var showCamera by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { vm.onPhotoCaptured(it) }
    }

    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val saved by savedVm.saved.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    "Saved searches",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()
                if (saved.isEmpty()) {
                    Text(
                        "Nothing saved yet",
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                } else {
                    LazyColumn {
                        items(saved) { item ->
                            SavedRow(
                                saved = item,
                                onClick = {
                                    vm.loadSavedSearch(item)
                                    scope.launch { drawerState.close() }
                                },
                                onDelete = { savedVm.delete(item) }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ClothingSearch") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = "Open saved")
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showPicker = true }) {
                    Icon(Icons.Filled.Search, contentDescription = "New search")
                }
            }
        ) { padding ->
            ResultsScreen(vm, savedVm, Modifier.padding(padding))

            if (showCamera) {
                CameraDialog(onDismiss = { showCamera = false }) { uri ->
                    vm.onPhotoCaptured(uri)
                }
            }

            if (showPicker) {
                AlertDialog(
                    onDismissRequest = { showPicker = false },
                    title = { Text("Add photo") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    showPicker = false
                                    showCamera = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Take new photo")
                            }
                            OutlinedButton(
                                onClick = {
                                    showPicker = false
                                    galleryLauncher.launch("image/*")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Choose from gallery")
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {}
                )
            }
        }
    }
}

@Composable
fun ResultsScreen(vm: ResultsViewModel, savedVm: SavedResultsViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    val photo by vm.photoBitmap.collectAsState()
    val label by vm.selectedCategory.collectAsState()
    val colorName by vm.dominantColorName.collectAsState()
    val isProcessing by vm.isProcessing.collectAsState()
    val query by vm.queryText.collectAsState()
    var toast by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
    ) {
        if (photo != null) {
            Image(
                bitmap = photo!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(Color(0xFFF0F0F4), RoundedCornerShape(16.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(Color(0xFFE8E8EB), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("Tap search to get started")
            }
        }

        if (isProcessing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Analyzing…")
        } else {
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                AssistChip("Category", label.ifBlank { "—" })
                AssistChip("Color", colorName.ifBlank { "—" })
            }

            if (query.isNotBlank()) {
                Text("Query: $query", maxLines = 1, overflow = TextOverflow.Ellipsis)

                MarketplaceButtons(query = query) { url ->
                    CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
                }

                Button(
                    onClick = {
                        val item = vm.currentSavedSearch()
                        if (item != null) {
                            savedVm.save(item)
                            toast = "Saved search"
                        } else {
                            toast = "Nothing to save yet"
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save search") }
            }
        }
    }

    toast?.let { msg ->
        LaunchedEffect(msg) {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            toast = null
        }
    }
}

@Composable
private fun AssistChip(title: String, value: String) {
    Surface(shape = RoundedCornerShape(12.dp), tonalElevation = 2.dp) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$title:")
            Spacer(Modifier.width(6.dp))
            Text(value, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun MarketplaceButtons(query: String, open: (String) -> Unit) {
    val encoded = Uri.encode(query)
    val grailed = "https://www.grailed.com/shop?query=$encoded"
    val depop = "https://www.depop.com/search/?q=$encoded"
    val posh = "https://poshmark.com/search?query=$encoded"
    val ebay = "https://www.ebay.com/sch/i.html?_nkw=$encoded"

    val buttons = listOf(
        Triple("eBay", ebay, painterResource(id = R.drawable.logo_ebay)),
        Triple("Depop", depop, painterResource(id = R.drawable.logo_depop)),
        Triple("Poshmark", posh, painterResource(id = R.drawable.logo_poshmark)),
        Triple("Grailed", grailed, painterResource(id = R.drawable.logo_grailed))
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SquareLink(
                painter = buttons[0].third,
                contentDescription = buttons[0].first,
                modifier = Modifier.weight(1f)
            ) { open(buttons[0].second) }
            SquareLink(
                painter = buttons[1].third,
                contentDescription = buttons[1].first,
                modifier = Modifier.weight(1f)
            ) { open(buttons[1].second) }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SquareLink(
                painter = buttons[2].third,
                contentDescription = buttons[2].first,
                modifier = Modifier.weight(1f)
            ) { open(buttons[2].second) }
            SquareLink(
                painter = buttons[3].third,
                contentDescription = buttons[3].first,
                modifier = Modifier.weight(1f)
            ) { open(buttons[3].second) }
        }
    }
}

@Composable
private fun SavedRow(saved: SavedSearch, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val painter: Painter? = saved.imageUri?.let { rememberAsyncImagePainter(it) }
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(end = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (painter != null) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent, RoundedCornerShape(8.dp))
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(saved.queryText, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${saved.colorName} • ${saved.category}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete saved search")
            }
        }
    }
}

@Composable
private fun SquareLink(
    painter: Painter,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(90.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color(0xFFE7E7EC),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp)
    ) {
        Image(
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        )
    }
}

@Composable
fun CameraDialog(onDismiss: () -> Unit, onPhoto: (Uri) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp)) {
            CameraCapture(onDismiss, onPhoto)
        }
    }
}

@Composable
fun CameraCapture(onClose: () -> Unit, onPhoto: (Uri) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var preview by remember { mutableStateOf<Preview?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    Column(
        Modifier.padding(12.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                val cameraProvider = cameraProviderFuture.get()

                preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()

                val selector = CameraSelector.DEFAULT_BACK_CAMERA
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                previewView
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        )

        Row(
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = {
                    val file = createImageFile(context)
                    val uri = FileProvider.getUriForFile(
                        context, "com.example.clothsearch.fileprovider", file
                    )
                    val output = ImageCapture.OutputFileOptions.Builder(file).build()
                    imageCapture?.takePicture(
                        output,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onError(exc: ImageCaptureException) {
                                onClose()
                            }

                            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                                onPhoto(uri)
                                onClose()
                            }
                        })
                },
                modifier = Modifier.weight(1f)
            ) { Text("Capture") }
        }
    }
}

private fun createImageFile(context: Context): File {
    val timeStamp =
        SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val dir = File(context.cacheDir, "images").also { it.mkdirs() }
    return File(dir, "IMG_$timeStamp.jpg")
}
