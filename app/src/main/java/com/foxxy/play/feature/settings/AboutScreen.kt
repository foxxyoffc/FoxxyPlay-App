package com.foxxy.play.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foxxy.play.BuildConfig
import com.foxxy.play.R

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showQris by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("← Back")
            }

            Spacer(Modifier.width(8.dp))

            Text(
                "About",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(28.dp))

        Image(
            painter = painterResource(R.drawable.foxxyplay_logo),
            contentDescription = "FoxxyPlay Logo",
            modifier = Modifier.size(104.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(14.dp))

        Text(
            "FoxxyPlay",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Your music, your moment.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(28.dp))

        AboutCard(title = "APPLICATION") {
            AboutInfoRow("Application", "FoxxyPlay")
            HorizontalDivider()
            AboutInfoRow("Version", BuildConfig.VERSION_NAME)
            HorizontalDivider()
            AboutInfoRow("Build", BuildConfig.VERSION_CODE.toString())
            HorizontalDivider()
            AboutInfoRow("Platform", "Android")
        }

        Spacer(Modifier.height(18.dp))

        AboutCard(title = "WHAT'S NEW") {
            AboutInfoRow("Update", "Version update")
            HorizontalDivider()
            AboutInfoRow("Improvement", "Update checker")
            HorizontalDivider()
            AboutInfoRow("Stability", "Performance improvements")
        }

        Spacer(Modifier.height(18.dp))
        AboutCard(title = "DEVELOPER") {
            Image(
                painter = painterResource(R.drawable.developer_profile),
                contentDescription = "Developer Profile",
                modifier = Modifier
                    .size(112.dp)
                    .align(Alignment.CenterHorizontally),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.height(12.dp))

            Text(
                "FoxxyOffc",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Developer of FoxxyPlay",
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(18.dp))

        AboutCard(title = "GET IN TOUCH") {
            Text(
                "Menemukan bug, mengalami kendala, atau punya ide fitur baru? Hubungi developer melalui WhatsApp.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/6281997149736")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Contact Us  ·  WhatsApp")
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = { showQris = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SupportCupIcon()
                    Text("Support Developer")
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "FoxxyPlay By FoxxyOffc",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            "© 2026 FoxxyOffc. All rights reserved.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))
    }

    if (showQris) {
        AlertDialog(
            onDismissRequest = { showQris = false },
            title = {
                Text("Support Developer")
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Terima kasih atas dukunganmu untuk pengembangan FoxxyPlay.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(Modifier.height(16.dp))

                    Image(
                        painter = painterResource(R.drawable.foxxy_qris),
                        contentDescription = "QRIS Support Developer",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQris = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}


@Composable
private fun SupportCupIcon() {
    Canvas(modifier = Modifier.size(24.dp)) {
        val gold = Color(0xFFD9A441)
        val line = Stroke(
            width = 1.8.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )

        scale(size.width / 24f, size.height / 24f, pivot = Offset.Zero) {
            val cup = Path().apply {
                moveTo(4f, 8f)
                lineTo(19f, 8f)
                lineTo(17f, 17f)
                quadraticTo(11.5f, 21f, 6f, 17f)
                close()
            }

            val handle = Path().apply {
                moveTo(19f, 10f)
                cubicTo(25f, 8f, 25f, 16f, 18f, 16f)
            }

            val steam = Path().apply {
                moveTo(8f, 5f)
                quadraticTo(5f, 3f, 8f, 1f)
                moveTo(13f, 5f)
                quadraticTo(10f, 3f, 13f, 1f)
            }

            drawPath(cup, gold, style = line)
            drawPath(handle, gold, style = line)
            drawPath(steam, gold, style = line)
            drawLine(
                gold,
                Offset(5f, 21f),
                Offset(19f, 21f),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun AboutCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            title,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

@Composable
private fun AboutInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            value,
            fontWeight = FontWeight.SemiBold
        )
    }
}
