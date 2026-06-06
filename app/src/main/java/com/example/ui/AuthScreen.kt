package com.example.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    
    var selectedTab by remember { mutableStateOf(0) } // 0 = Entrar, 1 = Cadastrar-se
    
    // Shared State fields
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    // Register specific state fields
    var fullName by remember { mutableStateOf("") }
    var phoneRaw by remember { mutableStateOf("") } // Only digits
    
    // SMS validation step state
    var verificationId by remember { mutableStateOf<String?>(null) }
    var smsCode by remember { mutableStateOf("") }
    var isSendingCode by remember { mutableStateOf(false) }

    // Phone parsing/mask constraints
    val cleanPhone = phoneRaw.filter { it.isDigit() }
    val isPhoneValid = cleanPhone.length == 11

    // Colors
    val primaryColor = Color(0xFF00AFF5) // BlaBla Blue
    val darkBlueGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(darkBlueGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xF21A1A1A) // Glassy Dark Obsidian
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Logo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Shield",
                        tint = primaryColor,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "BlaBla$",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Text(
                    text = "Acesse o painel do motorista de cobrança esperta",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Light
                )

                // TabRow
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = primaryColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = primaryColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Entrar", fontWeight = FontWeight.Bold, color = if (selectedTab == 0) primaryColor else Color.White.copy(alpha = 0.6f)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Cadastrar-se", fontWeight = FontWeight.Bold, color = if (selectedTab == 1) primaryColor else Color.White.copy(alpha = 0.6f)) }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (selectedTab == 0) {
                    // LOGIN FORM
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            AuthManager.loginWithEmailAndPassword(
                                context = context,
                                email = email,
                                password = password,
                                onSuccess = onAuthSuccess,
                                onFailure = {
                                    // Handle errors or print warnings
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Acessar Conta", fontWeight = FontWeight.Black, color = Color.White)
                    }

                } else {
                    // REGISTER FORM
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Nome Completo", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail com verificação", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = primaryColor) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha (Criação)", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // BRAZIL CELLPHONE INPUT (MASKS AUTOMATICALLY)
                    OutlinedTextField(
                        value = phoneRaw,
                        onValueChange = { input -> 
                            // Only allow numbers
                            val cleaned = input.filter { it.isDigit() }
                            if (cleaned.length <= 11) {
                                phoneRaw = cleaned
                            }
                        },
                        label = { Text("Telefone com DDD (11 dígitos)", color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = primaryColor) },
                        singleLine = true,
                        visualTransformation = PhoneMaskVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = if (isPhoneValid) Color(0xFF39FF14) else primaryColor,
                            unfocusedBorderColor = if (isPhoneValid) Color(0xFF39FF14).copy(alpha = 0.5f) else Color.Gray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Registration state visual hint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = if (isPhoneValid) "Telefone Pronto!" else "${cleanPhone.length}/11 dígitos",
                            color = if (isPhoneValid) Color(0xFF39FF14) else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Bottom Register action - starts disabled
                    Button(
                        onClick = {
                            if (isPhoneValid) {
                                AuthManager.signUpWithEmailAndPassword(
                                    context = context,
                                    email = email,
                                    password = password,
                                    onSuccess = {
                                        // Once password account is set, proceed to SMS setup if activity not nul
                                        if (activity != null) {
                                            isSendingCode = true
                                            AuthManager.sendSmsVerificationCode(
                                                activity = activity,
                                                phoneNumber = "+55$cleanPhone", // Brazilian phone format E.164
                                                onCodeSent = { id ->
                                                    verificationId = id
                                                    isSendingCode = false
                                                },
                                                onFailure = { err ->
                                                    isSendingCode = false
                                                }
                                            )
                                        }
                                        onAuthSuccess()
                                    },
                                    onFailure = {}
                                )
                            }
                        },
                        // Disabled states explicitly colored gray
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPhoneValid) primaryColor else Color.DarkGray,
                            contentColor = if (isPhoneValid) Color.White else Color.Gray
                        ),
                        enabled = isPhoneValid,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isSendingCode) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        } else {
                            Text("Cadastrar", fontWeight = FontWeight.Black)
                        }
                    }
                }

                // SMS Verification Code display (Conditional layout step)
                AnimatedVisibility(visible = verificationId != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Divider(color = Color.Gray.copy(alpha = 0.3f))
                        Text(
                            text = "Código de confirmação de 6 dígitos enviado por SMS:",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = smsCode,
                            onValueChange = { if (it.length <= 6) smsCode = it },
                            label = { Text("Código de 6 dígitos", color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = primaryColor) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color.Gray
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (smsCode.length == 6) {
                                    // Simulated or actual confirmation flow
                                    onAuthSuccess()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF39FF14)), // Bright green
                            enabled = smsCode.length == 6,
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Text("Confirmar SMS e Ativar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
