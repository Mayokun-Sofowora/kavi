package com.mayor.kavi.data.models.detection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a prediction made by a machine learning model.
 *
 * @property x The x-coordinate of the top-left corner of the bounding box
 * @property y The y-coordinate of the top-left corner of the bounding box
 * @property width The width of the bounding box
 * @property height The height of the bounding box
 * @property confidence The confidence score of the prediction
 * @property `class` The predicted class of the object
 * @property classId The ID of the predicted class
 * @property detectionId The ID of the detection that the prediction belongs to
 * @property imagePath The path to the image that the prediction was made on
 * @property predictionType The type of prediction
 */
@Serializable
data class Prediction(
    @SerialName("x")
    val x: Float,
    @SerialName("y")
    val y: Float,
    @SerialName("width")
    val width: Float,
    @SerialName("height")
    val height: Float,
    @SerialName("confidence")
    val confidence: Double,
    @SerialName("class")
    val `class`: String,
    @SerialName("class_id")
    val classId: Int,
    @SerialName("detection_id")
    val detectionId: String,
    @SerialName("image_path")
    val imagePath: String,
    @SerialName("prediction_type")
    val predictionType: String
)
