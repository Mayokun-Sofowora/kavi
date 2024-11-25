package com.mayor.kavi.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.milliseconds

// Data class to hold detection results
data class DiceDetection(
    val boundingBox: android.graphics.Rect,
    val value: String,
    val confidence: Float
)

// Sealed class for camera permission state
sealed class CameraPermissionState {
    object Granted : CameraPermissionState()
    object Denied : CameraPermissionState()
    object RequestNeeded : CameraPermissionState()
}

@Composable
fun VirtualMode(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // State management
    var cameraPermissionState by remember { mutableStateOf<CameraPermissionState>(CameraPermissionState.RequestNeeded) }
    var detectedObjects by remember { mutableStateOf<List<DiceDetection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionState = if (isGranted) {
            CameraPermissionState.Granted
        } else {
            CameraPermissionState.Denied
        }
    }

    // Check and request camera permission
    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) -> cameraPermissionState = CameraPermissionState.Granted
            else -> permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (cameraPermissionState) {
            CameraPermissionState.Granted -> {
                // Camera preview with object detection
                CameraPreview(
                    onObjectsDetected = { detectedObjects = it },
                    onLoadingChanged = { isLoading = it },
                    onError = { cameraError = it }
                )

                // Overlay for detected objects
                DetectionOverlay(
                    detections = detectedObjects,
                    modifier = Modifier.fillMaxSize()
                )
            }
            CameraPermissionState.Denied -> {
                PermissionDeniedMessage(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                )
            }
            CameraPermissionState.RequestNeeded -> {
                // Show nothing while waiting for permission request
            }
        }

        // UI overlays
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        cameraError?.let { error ->
            ErrorMessage(
                error = error,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        InstructionsOverlay(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun CameraPreview(
    onObjectsDetected: (List<DiceDetection>) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val objectDetector = ObjectDetection.getClient(
                ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableMultipleObjects()
                    .enableClassification()
                    .build()
            )

            startCameraPreview(
                context = context,
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                objectDetector = objectDetector,
                onObjectsDetected = onObjectsDetected,
                onError = onError
            )
            onLoadingChanged(false)
        }
    )
}

private fun startCameraPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    objectDetector: ObjectDetector,
    onObjectsDetected: (List<DiceDetection>) -> Unit,
    onError: (String) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
    val analysisExecutor = Executors.newSingleThreadExecutor()

    cameraProviderFuture.addListener({
        try {
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .apply {
                    surfaceProvider = previewView.surfaceProvider
                }

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(analysisExecutor) { imageProxy ->
                        processImageProxy(
                            imageProxy = imageProxy,
                            objectDetector = objectDetector,
                            onDetectionsReady = onObjectsDetected
                        )
                    }
                }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )

        } catch (e: Exception) {
            Log.e("VirtualMode", "Camera preview start failed", e)
            onError("Failed to start camera: ${e.localizedMessage}")
        }
    }, ContextCompat.getMainExecutor(context))
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    imageProxy: ImageProxy,
    objectDetector: ObjectDetector,
    onDetectionsReady: (List<DiceDetection>) -> Unit
) {
    imageProxy.image?.let { mediaImage ->
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val diceDetections = detectedObjects.mapNotNull { obj ->
                    extractDiceDetection(obj)
                }
                onDetectionsReady(diceDetections)
            }
            .addOnFailureListener { e ->
                Log.e("VirtualMode", "Object detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } ?: imageProxy.close()
}

private fun extractDiceDetection(obj: DetectedObject): DiceDetection? {
    val diceLabel = obj.labels.maxByOrNull { it.confidence }
        ?.takeIf { it.confidence > 0.7 && it.text.contains("dice", ignoreCase = true) }
        ?: return null

    return DiceDetection(
        boundingBox = obj.boundingBox,
        value = diceLabel.text.substringAfter("dice: "),
        confidence = diceLabel.confidence
    )
}

@Composable
private fun DetectionOverlay(
    detections: List<DiceDetection>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        detections.forEach { detection ->
            // Draw bounding box
            drawRect(
                color = Color.Green,
                topLeft = Offset(detection.boundingBox.left.toFloat(), detection.boundingBox.top.toFloat()),
                size = Size(
                    detection.boundingBox.width().toFloat(),
                    detection.boundingBox.height().toFloat()
                ),
                style = Stroke(width = 4f)
            )

            // Draw detection info
            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    "${detection.value} (${(detection.confidence * 100).toInt()}%)",
                    detection.boundingBox.left.toFloat(),
                    detection.boundingBox.top.toFloat() - 10,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GREEN
                        textSize = 40f
                        isFakeBoldText = true
                    }
                )
            }
        }
    }
}

@Composable
private fun PermissionDeniedMessage(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Camera permission is required for dice detection",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

@Composable
private fun ErrorMessage(
    error: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = error,
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun InstructionsOverlay(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        shape = MaterialTheme.shapes.medium
    ) {
        Text(
            text = "Point your camera at dice to detect them",
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// TODO: figure out what to do with the object detection.
/*
// Data classes for game state
data class DiceDetection(
    val boundingBox: android.graphics.Rect,
    val value: Int,
    val confidence: Float
)

data class YahtzeeScore(
    val ones: Int? = null,
    val twos: Int? = null,
    val threes: Int? = null,
    val fours: Int? = null,
    val fives: Int? = null,
    val sixes: Int? = null,
    val threeOfAKind: Int? = null,
    val fourOfAKind: Int? = null,
    val fullHouse: Int? = null,
    val smallStraight: Int? = null,
    val largeStraight: Int? = null,
    val yahtzee: Int? = null,
    val chance: Int? = null
)

@Composable
fun VirtualMode(navController: NavController) {
    var gameState by remember { mutableStateOf(YahtzeeScore()) }
    var currentDice by remember { mutableStateOf<List<Int>>(emptyList()) }
    var rollsLeft by remember { mutableStateOf(3) }
    var selectedDice by remember { mutableStateOf(setOf<Int>()) }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        // Camera Preview (taking up top 60% of screen)
        Box(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
        ) {
            CameraWithDetection(
                onDiceDetected = { detections ->
                    // Convert detections to dice values
                    currentDice = detections.map { it.value }
                }
            )
        }

        // Game Controls and Score (bottom 40% of screen)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Current Roll Display
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                items(currentDice) { value ->
                    DieDisplay(
                        value = value,
                        isSelected = value in selectedDice,
                        onToggleSelection = {
                            selectedDice = if (value in selectedDice) {
                                selectedDice - value
                            } else {
                                selectedDice + value
                            }
                        }
                    )
                }
            }

            // Roll Button and Rolls Left
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Rolls left: $rollsLeft")
                Button(
                    onClick = {
                        if (rollsLeft > 0) {
                            scope.launch {
                                // Simulate roll animation
                                repeat(5) {
                                    currentDice = currentDice.map { (1..6).random() }
                                    delay(100)
                                }
                                rollsLeft--
                            }
                        }
                    },
                    enabled = rollsLeft > 0
                ) {
                    Text("Roll Dice")
                }
            }

            // Scoring Options
            ScoringOptions(
                currentDice = currentDice,
                gameState = gameState,
                onScore = { category, score ->
                    gameState = updateScore(gameState, category, score)
                    rollsLeft = 3
                    currentDice = emptyList()
                    selectedDice = emptySet()
                }
            )
        }
    }
}

@Composable
private fun DieDisplay(
    value: Int,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .size(40.dp)
            .background(if (isSelected) Color.LightGray else Color.White)
            .border(1.dp, Color.Black)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ScoringOptions(
    currentDice: List<Int>,
    gameState: YahtzeeScore,
    onScore: (String, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Scoring Options",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Upper Section
        ScoringRow("Ones", gameState.ones) {
            calculateUpperSection(currentDice, 1)?.let { onScore("ones", it) }
        }
        ScoringRow("Twos", gameState.twos) {
            calculateUpperSection(currentDice, 2)?.let { onScore("twos", it) }
        }
        // Add other scoring options...

        // Lower Section
        ScoringRow("Three of a Kind", gameState.threeOfAKind) {
            calculateNOfAKind(currentDice, 3)?.let { onScore("threeOfAKind", it) }
        }
        ScoringRow("Four of a Kind", gameState.fourOfAKind) {
            calculateNOfAKind(currentDice, 4)?.let { onScore("fourOfAKind", it) }
        }
        // Add other scoring options...
    }
}

@Composable
private fun ScoringRow(
    label: String,
    currentScore: Int?,
    onScore: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        if (currentScore == null) {
            Button(onClick = onScore) {
                Text("Score")
            }
        } else {
            Text(currentScore.toString())
        }
    }
}

@Composable
private fun CameraWithDetection(
    onDiceDetected: (List<DiceDetection>) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { previewView ->
            val objectDetector = ObjectDetection.getClient(
                ObjectDetectorOptions.Builder()
                    .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
                    .enableMultipleObjects()
                    .enableClassification()
                    .build()
            )

            startCameraWithDiceDetection(
                context,
                lifecycleOwner,
                previewView,
                objectDetector,
                onDiceDetected
            )
        }
    }
}

private fun startCameraWithDiceDetection(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    objectDetector: ObjectDetector,
    onDiceDetected: (List<DiceDetection>) -> Unit
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()

        val preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .build()
            .apply {
                surfaceProvider = previewView.surfaceProvider
            }

        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_4_3)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { imageProxy ->
            processImage(imageProxy, objectDetector, onDiceDetected)
        }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Binding failed", e)
        }
    }, ContextCompat.getMainExecutor(context))
}

@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun processImage(
    imageProxy: ImageProxy,
    objectDetector: ObjectDetector,
    onDiceDetected: (List<DiceDetection>) -> Unit
) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(
            image,
            imageProxy.imageInfo.rotationDegrees
        )

        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                // Process detected objects and extract dice values
                val diceDetections = detectedObjects.mapNotNull { obj ->
                    // Here you would need to implement custom logic to recognize dice values
                    // This is a simplified example
                    val value = extractDiceValue(obj)
                    value?.let {
                        DiceDetection(
                            boundingBox = obj.boundingBox,
                            value = it,
                            confidence = obj.labels.maxOfOrNull { it.confidence } ?: 0f
                        )
                    }
                }
                onDiceDetected(diceDetections)
            }
            .addOnFailureListener { e ->
                Log.e("DiceDetection", "Detection failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
}

// Helper functions for scoring
private fun calculateUpperSection(dice: List<Int>, number: Int): Int? {
    return dice.filter { it == number }.sum()
}

private fun calculateNOfAKind(dice: List<Int>, n: Int): Int? {
    return dice.groupBy { it }
        .filter { it.value.size >= n }
        .keys
        .maxOrNull()
        ?.let { dice.sum() }
}

private fun updateScore(currentScore: YahtzeeScore, category: String, score: Int): YahtzeeScore {
    return when (category) {
        "ones" -> currentScore.copy(ones = score)
        "twos" -> currentScore.copy(twos = score)
        "threeOfAKind" -> currentScore.copy(threeOfAKind = score)
        "fourOfAKind" -> currentScore.copy(fourOfAKind = score)
        // Add other categories...
        else -> currentScore
    }
}

// You'll need to implement this based on your ML model's output
private fun extractDiceValue(detectedObject: DetectedObject): Int? {
    // This needs to be implemented based on your specific ML model
    // Here's a placeholder implementation
    return detectedObject.labels
        .firstOrNull { it.text.contains("dice", ignoreCase = true) }
        ?.text
        ?.filter { it.isDigit() }
        ?.toIntOrNull()
        ?.takeIf { it in 1..6 }
}
* */