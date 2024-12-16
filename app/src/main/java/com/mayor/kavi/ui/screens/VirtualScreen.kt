//package com.mayor.kavi.ui.screens
//
//import android.content.Context
//import android.net.Uri
//import android.view.MotionEvent
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.runtime.*
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.viewinterop.AndroidView
//import com.google.ar.core.*
//import com.google.ar.sceneform.math.Vector3
//import com.google.ar.sceneform.rendering.ModelRenderable
//import com.google.ar.sceneform.ux.*
//import io.github.sceneview.ar.ArSceneView
//
//@Composable
//fun VirtualScreen() {
//    var diceModelRenderable by remember { mutableStateOf<ModelRenderable?>(null) }
//
//    AndroidView(
//        factory = { context ->
//            ArSceneView(context).apply {
//                setupSession(context)
//
//                // Load the 3D dice model
//                ModelRenderable.builder()
//                    .setSource(context, Uri.parse("dice.sfb")) // Replace with your dice model file
//                    .build()
//                    .thenAccept { renderable -> diceModelRenderable = renderable }
//
//                setOnTouchListener { _, event ->
//                    if (event.action == MotionEvent.ACTION_UP) {
//                        val frame = arFrame ?: return@setOnTouchListener false
//
//                        val hits = frame.hitTest(event)
//                        for (hit in hits) {
//                            val trackable = hit.trackable
//                            if (trackable is Plane && trackable.isPoseInPolygon(hit.hitPose)) {
//                                val anchor = hit.createAnchor()
//                                val diceNode = createDiceNode(anchor, diceModelRenderable)
//                                scene.addChild(diceNode)
//                                simulateDiceRoll(diceNode)
//                                break
//                            }
//                        }
//                    }
//                    true
//                }
//            }
//        },
//        modifier = Modifier.fillMaxSize()
//    )
//}
//
//fun ArSceneView.setupSession(context: Context) {
//    val session = Session(context).apply {
//        configure(
//            Config(this).apply {
//                planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
//            }
//        )
//    }
//    this.setupSession(session)
//}
//
//fun createDiceNode(anchor: Anchor, model: ModelRenderable?): TransformableNode {
//    val anchorNode = AnchorNode(anchor)
//    return TransformableNode(TransformationSystem()).apply {
//        setParent(anchorNode)
//        renderable = model
//        localPosition = Vector3(0f, 0f, 0f)
//    }
//}
//
//fun simulateDiceRoll(node: TransformableNode) {
//    // Example: Apply random rotation to simulate rolling
//    val randomRotation = Vector3(
//        (0..360).random().toFloat(),
//        (0..360).random().toFloat(),
//        (0..360).random().toFloat()
//    )
//    node.localRotation = Quaternion.eulerAngles(randomRotation)
//}
