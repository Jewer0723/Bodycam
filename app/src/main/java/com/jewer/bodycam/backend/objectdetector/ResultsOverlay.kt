/*
 * Copyright 2023 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jewer.bodycam.backend.objectdetector

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult
import com.jewer.bodycam.backend.functions.getVibrateStatus
import com.jewer.bodycam.backend.functions.vibrateOnce
import com.jewer.bodycam.ui.theme.DarkYellow

// 只在偵測到人時顯示
@Composable
fun ResultsOverlay(
    context: Context,
    results: ObjectDetectorResult,
    frameWidth: Int,
    frameHeight: Int,
) {
    // 辨識結果
    val detections = results.detections()

    // 震動狀態
    val vibrateApproved = getVibrateStatus(context)

    if (detections != null) {
        for (detection in detections) {
            if (detection.categories().first().categoryName() == "person") {

                // 檢查震動許可
                if (vibrateApproved) {
                    vibrateOnce(context, 250) // 如果許可震動，震動250ms
                }

                BoxWithConstraints(
                    Modifier
                        .fillMaxSize()
                ) {
                    // calculating the UI dimensions of the detection bounds
                    val resultBounds = detection.boundingBox()
                    val boxWidth = (resultBounds.width() / frameWidth) * this.maxWidth.value
                    val boxHeight = (resultBounds.height() / frameHeight) * this.maxHeight.value
                    val boxLeftOffset = (resultBounds.left / frameWidth) * this.maxWidth.value
                    val boxTopOffset = (resultBounds.top / frameHeight) * this.maxHeight.value

                    Box(
                        Modifier
                            .fillMaxSize()
                            .offset(
                                boxLeftOffset.dp,
                                boxTopOffset.dp,
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .border(3.dp, Color(DarkYellow.value))
                                .width(boxWidth.dp)
                                .height(boxHeight.dp)
                        )
                    }
                }
            }
        }
    }
}