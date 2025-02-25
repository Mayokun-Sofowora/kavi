package com.mayor.kavi.ui.components

import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner

@Composable
fun CameraPreview(
    controller: LifecycleCameraController,
    modifier: Modifier = Modifier,
    onPreviewSizeChanged: (IntSize) -> Unit
) {
    val lifeCycleOwner: LifecycleOwner = LocalLifecycleOwner.current
    val previewSizeState = remember { mutableStateOf(IntSize(0, 0)) }

    AndroidView(
        factory = {
            PreviewView(it).apply {
                this.controller = controller
                controller.bindToLifecycle(lifeCycleOwner)
            }
        },
        modifier = modifier.onGloballyPositioned { coordinates ->
            // Retrieve and update the size of the preview when the layout is positioned globally.
            val size = coordinates.size
            previewSizeState.value = size
            // Invoke the callback with the new size.
            onPreviewSizeChanged(size)
        }
    )
}
