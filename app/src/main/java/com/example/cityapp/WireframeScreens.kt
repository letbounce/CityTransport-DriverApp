package com.example.cityapp

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WireframeBorderColor = Color.Gray
private val WireframeBackground = Color(0xFFF8F8F8)
private val WireframeTextColor = Color.Black

private val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 10.sp,
    color = Color.DarkGray
)

@Composable
private fun WireframeContainer(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        color = WireframeBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun LabeledBox(
    label: String,
    annotation: String? = null,
    modifier: Modifier = Modifier,
    height: Dp? = null
) {
    Column(
        modifier = modifier
            .then(if (height != null) Modifier.height(height) else Modifier)
            .fillMaxWidth()
            .border(1.dp, WireframeBorderColor),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = WireframeTextColor
            ),
            textAlign = TextAlign.Center
        )
        if (annotation != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = annotation,
                style = MonoStyle,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InputWireframe(label: String, annotation: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = WireframeTextColor
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(1.dp, WireframeBorderColor),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = " [ TextField Placeholder ] ",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = annotation,
            style = MonoStyle
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = WireframeTextColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}

@Composable
fun Wireframe_LoginScreen() {
    WireframeContainer {
        Box(
            modifier = Modifier
                .size(120.dp)
                .border(1.dp, WireframeBorderColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Logo\n[X]",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    color = WireframeTextColor
                ),
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Logo Area 120x120dp",
            style = MonoStyle
        )

        Spacer(modifier = Modifier.height(32.dp))

        InputWireframe(
            label = "Driver ID",
            annotation = "width: match_parent, height: 56dp"
        )
        Spacer(modifier = Modifier.height(16.dp))
        InputWireframe(
            label = "Password",
            annotation = "width: match_parent, height: 56dp"
        )

        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .border(2.dp, WireframeBorderColor, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "УВІЙТИ",
                    style = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = WireframeTextColor
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "height: 64dp, margin-top: 32dp",
                style = MonoStyle
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    name = "Wireframe_LoginScreen"
)
@Composable
fun Preview_Wireframe_LoginScreen() {
    MaterialTheme {
        Wireframe_LoginScreen()
    }
}

@Composable
fun Wireframe_RouteDashboard() {
    WireframeContainer {
        SectionTitle(text = "Route Dashboard (Wireframe)")

        Text(
            text = "User: DRV-1042 (Коксюк О.В.)",
            style = TextStyle(
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = WireframeTextColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WireframeBorderColor)
                .padding(8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, WireframeBorderColor))
                .padding(12.dp)
        ) {
            Text(
                text = "Route Card",
                style = MonoStyle
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Route #12: Центр - Вокзал",
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = WireframeTextColor
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "width: match_parent, height: wrap_content",
                style = MonoStyle
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WireframeBorderColor)
                .padding(8.dp)
        ) {
            Text(
                text = "Stops (Wireframe List)",
                style = MonoStyle
            )
            Spacer(modifier = Modifier.height(4.dp))

            val stops = listOf(
                "08:10  Центр площа",
                "08:18  Проспект Миру",
                "08:25  Ринок",
                "08:33  Вокзал"
            )
            stops.forEachIndexed { index, stop ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.LightGray)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Stop ${index + 1}",
                        style = TextStyle(fontSize = 12.sp, color = WireframeTextColor)
                    )
                    Text(
                        text = stop,
                        style = TextStyle(fontSize = 14.sp, color = WireframeTextColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "List item: height ~40dp, width: match_parent",
                style = MonoStyle
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        LabeledBox(
            label = "ОТРИМАТИ ДОРОЖНІЙ ЛИСТ",
            annotation = "Primary Action, height: 80dp",
            modifier = Modifier,
            height = 80.dp
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    name = "Wireframe_RouteDashboard"
)
@Composable
fun Preview_Wireframe_RouteDashboard() {
    MaterialTheme {
        Wireframe_RouteDashboard()
    }
}

@Composable
fun Wireframe_ActiveTripScreen() {
    WireframeContainer {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .border(1.dp, WireframeBorderColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Status Bar - 100% width",
                style = MonoStyle
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Рейс #12 В ДОРОЗІ",
            style = TextStyle(
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = WireframeTextColor
            ),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WireframeBorderColor)
                .padding(12.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WireframeBorderColor)
                .padding(8.dp)
        ) {
            Text(
                text = "Trip Info (Wireframe)",
                style = MonoStyle
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Поточна зупинка: Ринок",
                style = TextStyle(fontSize = 16.sp, color = WireframeTextColor)
            )
            Text(
                text = "Наступна зупинка: Вокзал",
                style = TextStyle(fontSize = 16.sp, color = WireframeTextColor)
            )
            Text(
                text = "Орієнтовний час прибуття: 08:33",
                style = TextStyle(fontSize = 16.sp, color = WireframeTextColor)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            LabeledBox(
                label = "ЗАВЕРШИТИ РЕЙС",
                annotation = "Primary Action, height: 80dp",
                modifier = Modifier,
                height = 80.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            LabeledBox(
                label = "ІНЦИДЕНТ",
                annotation = "Secondary Action, height: 80dp",
                modifier = Modifier,
                height = 80.dp
            )
        }
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    name = "Wireframe_ActiveTripScreen"
)
@Composable
fun Preview_Wireframe_ActiveTripScreen() {
    MaterialTheme {
        Wireframe_ActiveTripScreen()
    }
}

@Composable
fun Wireframe_IncidentReport() {
    WireframeContainer {
        SectionTitle(text = "Incident Report (Wireframe)")

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Тип інциденту",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WireframeTextColor
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(1.dp, WireframeBorderColor),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[ Dropdown Placeholder ]",
                        style = TextStyle(fontSize = 12.sp, color = Color.Gray)
                    )
                    Text(
                        text = "▼",
                        style = TextStyle(fontSize = 14.sp, color = Color.Gray)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "width: match_parent, height: 56dp",
                style = MonoStyle
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(
                text = "Опис інциденту",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WireframeTextColor
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .border(1.dp, WireframeBorderColor),
                contentAlignment = Alignment.TopStart
            ) {
                Text(
                    text = "[ Multiline Text Area Placeholder ]",
                    style = TextStyle(fontSize = 12.sp, color = Color.Gray),
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "min-height: 120dp, width: match_parent",
                style = MonoStyle
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .border(1.dp, WireframeBorderColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "X\nCamera\nPlaceholder",
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = WireframeTextColor
                    ),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Camera Intent Trigger 150x150dp",
                style = MonoStyle,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        LabeledBox(
            label = "ВІДПРАВИТИ ЗВІТ",
            annotation = "Primary Action, height: 72dp",
            modifier = Modifier,
            height = 72.dp
        )
    }
}

@Preview(
    showBackground = true,
    backgroundColor = 0xFFFFFFFF,
    name = "Wireframe_IncidentReport"
)
@Composable
fun Preview_Wireframe_IncidentReport() {
    MaterialTheme {
        Wireframe_IncidentReport()
    }
}

