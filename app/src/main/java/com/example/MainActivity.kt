package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SearchRoute
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.AuthScreen
import com.example.ui.AccessibilityTutorialScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var isAuthenticated by remember { mutableStateOf(false) }
                val prefs = getSharedPreferences("blabla_settings", Context.MODE_PRIVATE)
                var hasCompletedTutorial by remember {
                    mutableStateOf(prefs.getBoolean("has_completed_tutorial", false))
                }

                if (!isAuthenticated) {
                    AuthScreen(onAuthSuccess = { isAuthenticated = true })
                } else if (!hasCompletedTutorial) {
                    AccessibilityTutorialScreen(
                        context = this,
                        onFinishTutorial = {
                            hasCompletedTutorial = true
                            prefs.edit().putBoolean("has_completed_tutorial", true).apply()
                        }
                    )
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { DashboardTopBar() }
                    ) { innerPadding ->
                        MainActivityContent(
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding),
                            onRequestPermission = { openOverlayPermissionSettings() }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermissionsAndStates(this)
    }

    private fun openOverlayPermissionSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Por favor, autorize BlaBla$ para desenhar sobre outras telas.", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar() {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00AFF5)) // BlaBla Blue
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Column {
                    Text(
                        text = "BlaBla$",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Inteligência de Mercado em Tempo Real",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
        )
    )
}

@Composable
fun MainActivityContent(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit
) {
    val context = LocalContext.current
    val allRoutes by viewModel.allRoutes.collectAsStateWithLifecycle()
    val isServiceRunning by viewModel.isServiceRunning.collectAsStateWithLifecycle()
    val hasPermission by viewModel.hasOverlayPermission.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    // Forms fields
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var averagePriceInput by remember { mutableStateOf("") }
    var discountInput by remember { mutableStateOf("7.0") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // PERMISSION & FLOATING SERVICE CARD CONTROL
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Controle do Assistant (Overlay)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ative o widget flutuante horizontal compacto para monitorar preços em tempo real por cima do aplicativo BlaBlaCar.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (!hasPermission) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Alerta",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Permissão Necessária",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "É necessário habilitar 'Escrever sobre outros apps' para o Widget BlaBla$ funcionar.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                                    )
                                }
                                Button(
                                    onClick = onRequestPermission,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Ativar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Overlay is ready, show nicely styled toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (isServiceRunning) Color(0xFF39FF14) else Color.Red)
                                )
                                Text(
                                    text = if (isServiceRunning) "OVERLAY ATIVO" else "OVERLAY DESATIVADO",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    letterSpacing = 1.sp,
                                    color = if (isServiceRunning) Color(0xFF00C853) else Color.Red
                                )
                            }

                            Button(
                                onClick = { viewModel.toggleService(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isServiceRunning) MaterialTheme.colorScheme.error else Color(0xFF00AFF5)
                                )
                            ) {
                                Icon(
                                    imageVector = if (isServiceRunning) Icons.Default.Close else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isServiceRunning) "Parar Widget" else "Iniciar Widget",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // MODULE CONFIGURATION CARD (MOTORISTA vs PASSAGEIRO)
        item {
            val prefs = LocalContext.current.getSharedPreferences("blabla_settings", Context.MODE_PRIVATE)
            var currentModeIsDriver by remember { 
                mutableStateOf(prefs.getBoolean("is_driver_mode", true)) 
            }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Módulo de Funcionamento Ativo",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Alterne o escopo do Widget Flutuante (Balão) em tempo real conforme a sua necessidade atual.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Driver Option Card
                        Card(
                            modifier = Modifier
                                .weight(1.5f)
                                .clickable {
                                    currentModeIsDriver = true
                                    prefs.edit().putBoolean("is_driver_mode", true).apply()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (currentModeIsDriver) {
                                    Color(0xFF00AFF5).copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (currentModeIsDriver) 2.dp else 1.dp,
                                color = if (currentModeIsDriver) Color(0xFF00AFF5) else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Motorista",
                                    tint = if (currentModeIsDriver) Color(0xFF00AFF5) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Motorista",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (currentModeIsDriver) Color(0xFF00AFF5) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Preços Sugeridos",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Passenger Option Card
                        Card(
                            modifier = Modifier
                                .weight(1.5f)
                                .clickable {
                                    currentModeIsDriver = false
                                    prefs.edit().putBoolean("is_driver_mode", false).apply()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (!currentModeIsDriver) {
                                    Color(0xFF00C853).copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (!currentModeIsDriver) 2.dp else 1.dp,
                                color = if (!currentModeIsDriver) Color(0xFF00C853) else Color.Transparent
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Luggage,
                                    contentDescription = "Passageiro",
                                    tint = if (!currentModeIsDriver) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Passageiro",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = if (!currentModeIsDriver) Color(0xFF00C853) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Metas de Tarifas",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // MONITORED ROUTES TITLE & ADD ACTION
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Rotas Monitoradas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Simule e controle os trechos ativos do BlaBlaCar",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }

                FilledTonalButton(
                    onClick = { showAddDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Nova Rota", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // MONITORED TARGETS LIST OR EMPTY STATE
        if (allRoutes.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(27.dp))
                            .background(Color(0x1100AFF5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF00AFF5),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Nenhuma rota cadastrada",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Cadastre uma rota para ver a sugestão de preço e inteligência do BlaBla$",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.addNewRoute("São Paulo", "Campinas", 45.0, 7.0)
                            viewModel.addNewRoute("Curitiba", "Joinville", 33.0, 7.0)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("Inserir Rotas Exemplo")
                    }
                }
            }
        } else {
            items(allRoutes) { route ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (route.isTracking) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = if (route.isTracking) {
                        BorderStroke(1.dp, Color(0xFF00AFF5).copy(alpha = 0.3f))
                    } else null
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF00AFF5).copy(alpha = 0.15f))
                                        .padding(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF00AFF5),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "${route.origin} → ${route.destination}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column {
                                    Text("MÉDIA", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                                    Text("R$ %.2f".format(route.averagePrice), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Column {
                                    Text("DIFERENCIAL (-${route.requestedDiscountPercentage}%)", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                                    Text("R$ %.2f".format(route.calculateSuggestedPrice()), fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF00C853))
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Switch(
                                checked = route.isTracking,
                                onCheckedChange = { viewModel.toggleRouteTracking(route) },
                                thumbContent = {
                                    Icon(
                                        imageVector = if (route.isTracking) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            )

                            IconButton(
                                onClick = { viewModel.deleteRoute(route) },
                                colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Deletar Rota", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }

        // INTELLIGENCE & SCROLL PAGINATION METRICS EDUCATION CORNER
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF00AFF5),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Por que fazer o 'Back to Top' automático?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Evita viés de amostragem na leitura do BlaBlaCar\n" +
                                "Ao clicar em Atualizar, o Widget rola a tela de fundo totalmente para o topo para garantir que nenhuma oferta com preço competitivo à frente seja ignorada na paginação.\n\n" +
                                "2. Como funciona tecnicamente na AccessibilityService?\n" +
                                "• O serviço localiza a lista recicladora da tela ativa.\n" +
                                "• Executa ações do tipo ACTION_SCROLL_BACKWARD até o topo ser alcançado de forma controlada.\n" +
                                "• Em seguida, inicia o escaneamento OCR/leitura de nós das ofertas sequenciadas.",
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                    )
                }
            }
        }
    }

    // NEW ROUTE POPUP MODAL DIALOG
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Cadastrar Rota de Monitoramento", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = origin,
                        onValueChange = { origin = it },
                        label = { Text("Cidade Origem") },
                        placeholder = { Text("Ex: São Paulo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Cidade Destino") },
                        placeholder = { Text("Ex: Campinas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = averagePriceInput,
                        onValueChange = { averagePriceInput = it },
                        label = { Text("Valor Médio Corrente (R$)") },
                        placeholder = { Text("Ex: 45.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        value = discountInput,
                        onValueChange = { discountInput = it },
                        label = { Text("Diferencial de Preço (%)") },
                        placeholder = { Text("Ex: 7.0% mais barato") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "* BlaBla$ calcula o preço sugerido arredondado automaticamente.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val avg = averagePriceInput.toDoubleOrNull() ?: 0.0
                        val disc = discountInput.toDoubleOrNull() ?: 7.0
                        if (origin.isNotBlank() && destination.isNotBlank() && avg > 0) {
                            viewModel.addNewRoute(origin, destination, avg, disc)
                            showAddDialog = false
                            // clear fields
                            origin = ""
                            destination = ""
                            averagePriceInput = ""
                            discountInput = "7.0"
                        } else {
                            Toast.makeText(context, "Por favor, insira todos os dados válidos.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Salvar Rota")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}


