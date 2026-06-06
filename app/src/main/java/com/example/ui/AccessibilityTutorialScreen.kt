package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AccessibilityTutorialScreen(
    context: Context,
    onFinishTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var stepIndex by remember { mutableStateOf(0) }
    val primaryColor = Color(0xFF00AFF5) // BlaBla Blue

    val steps = listOf(
        Triple(
            "Seja Bem-vindo ao BlaBla$",
            "Nós te ajudamos a faturar mais calculando preços em tempo real com inteligência de mercado integrada sobre o BlaBlaCar.",
            Icons.Default.AutoAwesome
        ),
        Triple(
            "Como Funciona?",
            "Nosso robô inteligente analisa os preços das caronas concorrentes quando você acessa a busca de caronas e calcula instantaneamente o preço ideal competitivo.",
            Icons.Default.AccessibilityNew
        ),
        Triple(
            "Permissão Necessária",
            "Para ler e processar os preços automaticamente, você precisa ativar o Serviço de Acessibilidade 'BlaBla$ Assistente' nas suas Configurações.",
            Icons.Default.CheckCircle
        )
    )

    val currentStep = steps[stepIndex]

    // Action to open settings
    val openSettings = {
        try {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0B1015), Color(0xFF15222E))
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E2633).copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Step Indicator dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in steps.indices) {
                        Box(
                            modifier = Modifier
                                .size(width = if (i == stepIndex) 20.dp else 8.dp, height = 8.dp)
                                .background(
                                    color = if (i == stepIndex) primaryColor else Color.Gray.copy(alpha = 0.4f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                // Step Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentStep.third,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Header & Body
                Text(
                    text = currentStep.first,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = currentStep.second,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (stepIndex < steps.size - 1) {
                                stepIndex++
                            } else {
                                openSettings()
                                onFinishTutorial()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (stepIndex == steps.size - 1) "Entrar nas Configurações" else "Próximo",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (stepIndex < steps.size - 1) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }

                    // Pular Tutorial buttons (opens Settings immediately as requested in R2)
                    TextButton(
                        onClick = {
                            openSettings()
                            onFinishTutorial()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Pular Tutorial",
                            color = primaryColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
