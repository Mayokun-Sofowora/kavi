package com.mayor.kavi.data.models.detection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a prediction made by the model.
 *
 * @property predictions The predicted label.
 * @property image Additional information about the image.
 */
@Serializable
data class DetectionResponse(
    @SerialName("predictions")
    val predictions: List<Prediction> = emptyList(),
    @SerialName("image")
    val image: ImageInfo? = null
)
