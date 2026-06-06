package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Luggage
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.model.SearchRoute
import com.example.data.repository.RouteRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Service BlaBlaFloatingService handles the overlay view (system alert window)
 * and active intelligence collection of the BlaBlaCar market pricing on background.
 * 
 * Re-elaborado com foco em uma UI leve, limpa e com excelente UX (design translúcido e minimalista).
 */
class BlaBlaFloatingService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    // Lifecycle, SavedState, and ViewModel store owners required to render Compose natively inside custom window
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val prefs by lazy { getSharedPreferences("blabla_settings", Context.MODE_PRIVATE) }
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "is_driver_mode") {
            isDriverMode = prefs.getBoolean("is_driver_mode", true)
        }
    }

    // Database states
    private lateinit var repository: RouteRepository
    private var activeRouteState = mutableStateOf<SearchRoute?>(null)

    // Current calculation and simulation metrics
    private var mockAverageFactor by mutableStateOf(1.0)
    private var isUpdatingState by mutableStateOf(false)
    private var statusMessage by mutableStateOf("Pronto")
    private var isDriverMode by mutableStateOf(true)
    private var isExpanded by mutableStateOf(false)
    private var showFullList by mutableStateOf(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // Load persisted driver/passenger module state and register changes
        isDriverMode = prefs.getBoolean("is_driver_mode", true)
        prefs.registerOnSharedPreferenceChangeListener(prefListener)

        // Initialize repository and fetch the active target route being monitored
        val database = AppDatabase.getDatabase(this)
        repository = RouteRepository(database.routeDao())

        serviceScope.launch {
            repository.activeTrackingRoutes.collectLatest { activeRoutes ->
                // Look for the first actively tracked stretch, or fall back to default
                activeRouteState.value = activeRoutes.firstOrNull() ?: SearchRoute(
                    origin = "São Paulo",
                    destination = "Campinas",
                    averagePrice = 45.00,
                    requestedDiscountPercentage = 7.0,
                    isTracking = true
                )
            }
        }

        startAsForeground()
        showFloatingOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        return START_STICKY
    }

    private fun startAsForeground() {
        val channelId = "blabla_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "BlaBla$ Monitoramento"
            val descriptionText = "Executando monitoramento inteligente em tempo real"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("BlaBla$ Ativo")
            .setContentText("Balão flutuante inteligente de preços ativo.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1337, notification)
    }

    private fun showFloatingOverlay() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.e("BlaBlaService", "SYSTEM_ALERT_WINDOW permission not granted. Stopping overlay service.")
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            x = 80 // Start padding
            y = 350 // Clear default top info
        }

        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            
            // Link Tree Owners
            setViewTreeLifecycleOwner(this@BlaBlaFloatingService)
            setViewTreeViewModelStoreOwner(this@BlaBlaFloatingService)
            setViewTreeSavedStateRegistryOwner(this@BlaBlaFloatingService)

            setContent {
                val currentRoute = activeRouteState.value
                val baseAvg = currentRoute?.averagePrice ?: 45.00
                val activeAvg = baseAvg * mockAverageFactor
                val activeRouteWithFactor = currentRoute?.copy(averagePrice = activeAvg) ?: SearchRoute(
                    origin = "São Paulo",
                    destination = "Campinas",
                    averagePrice = activeAvg,
                    requestedDiscountPercentage = 7.0
                )

                if (isExpanded) {
                    FloatingPanelContent(
                        route = activeRouteWithFactor,
                        statusText = statusMessage,
                        isUpdating = isUpdatingState,
                        isDriverMode = isDriverMode,
                        showFullList = showFullList,
                        onToggleShowFullList = { showFullList = !showFullList },
                        onToggleMode = {
                            isDriverMode = !isDriverMode
                            prefs.edit().putBoolean("is_driver_mode", isDriverMode).apply()
                        },
                        onCollapse = { 
                            isExpanded = false
                            showFullList = false // Default back to compact on close
                        },
                        onUpdateClick = {
                            triggerIntelligenceScan()
                        },
                        onDrag = { dx, dy ->
                            layoutParams.x += dx.toInt()
                            layoutParams.y += dy.toInt()
                            windowManager?.updateViewLayout(this, layoutParams)
                        }
                    )
                } else {
                    FloatingBubbleContent(
                        route = activeRouteWithFactor,
                        isDriverMode = isDriverMode,
                        isWorking = isUpdatingState,
                        onExpand = { isExpanded = true },
                        onUpdate = { triggerIntelligenceScan() },
                        onDrag = { dx, dy ->
                            layoutParams.x += dx.toInt()
                            layoutParams.y += dy.toInt()
                            windowManager?.updateViewLayout(this, layoutParams)
                        }
                    )
                }
            }
        }

        windowManager?.addView(composeView, layoutParams)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    /**
     * Simula a varredura inteligente do mercado:
     * 1. Executa auto-scroll para o topo absoluto para posicionar as ofertas corretamente.
     * 2. Trata a paginação navegando sequencialmente.
     * 
     * COMO IMPLEMENTAR O SCROLL E LEITURA DE PÁGINAS SEM PERDER NENHUMA OFERTA (PRODUCION READY):
     * - Em um ambiente de produção usando um AccessibilityService oficial do Android:
     *   a) Busque o nó da lista (Ex: RecyclerView do BlaBlaCar) usando findAccessibilityNodeInfosByViewId.
     *   b) Execute performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) iterativamente em um loop
     *      até que o topo seja validado (por exemplo, quando o primeiro item visível index=0 está visível
     *      e suas coordenadas no topo da View não oscilam mais, indicando o fim da ancoragem).
     *   c) Uma vez posicionado no topo ("Back to Top"), execute a varredura de OCR e árvore de nós dos itens visíveis.
     *   d) Para paginar sem perder dados ou ler itens duplicados: Guarde a assinatura única (ID ou timestamp + motorista + preço)
     *      de cada oferta em um Set em memória.
     *   e) Execute performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD). Compare os novos elementos lidos
     *      com o Set em memória. Caso haja elementos novos, compute e continue. Se a quantidade de itens duplicados
     *      for igual ao tamanho da página visível, ou se o scroll-forward não alterar mais a árvore do layout, o fim da lista foi atingido.
     */
    private fun triggerIntelligenceScan() {
        if (isUpdatingState) return
        isUpdatingState = true
        statusMessage = "Rolando..."

        serviceScope.launch {
            // PASSO 1: FORCE AUTOMATIC SCROLL TO THE TOP (Ancoragem de início do trecho)
            delay(1000)

            // PASSO 2: SCANNIG PAGINATION PAGES (Navegação estruturada por páginas via Scroll)
            statusMessage = "Varrer Págs..."
            delay(1200)

            // PASSO 3: COMPUTAÇÃO EM TEMPO REAL DOS PREÇOS ENCONTRADOS
            mockAverageFactor = 0.94 + (Math.random() * 0.13) // Variação realista do mercado (0.94 a 1.07)
            statusMessage = "Pronto!"
            isUpdatingState = false
            delay(800)
            statusMessage = "Pronto"
        }
    }

    /**
     * UI do Balão flutuante compacto e discreto.
     * Implementa um visual leve com efeito Glassmorphic translúcido moderno,
     * bordas finas com brilho neon e feedback tátil de arrasto super suave.
     * 
     * Apresenta um ciclo de animação infinita que exibe:
     * - Ícone do Modo Ativo (Carona/Passageiro)
     * - Valor Médio da rota em tempo real
     * - Valor Sugerido (competitivo) ou Alvo
     * - Toque para Atualizar / Sincronizar em ambos os módulos
     * - Dica de Expansão ("TOQUE ABRIR")
     */
    @Composable
    private fun FloatingBubbleContent(
        route: SearchRoute?,
        isDriverMode: Boolean,
        isWorking: Boolean,
        onExpand: () -> Unit,
        onUpdate: () -> Unit,
        onDrag: (Float, Float) -> Unit
    ) {
        val bubbleColor = if (isDriverMode) Color(0xFF00AFF5) else Color(0xFF00C853) // BlaBla Blue vs Success Green
        val glowColor = if (isDriverMode) Color(0xFF39FF14) else Color(0xFF00E5FF) // Neon Pulse Accent
        
        var isDragging by remember { mutableStateOf(false) }
        val bubbleScale by animateFloatAsState(
            targetValue = if (isDragging) 0.90f else 1.0f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
            label = "bubbleScale"
        )

        // Carrossel de Informações compacto no próprio balão redondo (5 estados!)
        var infoIndex by remember { mutableStateOf(0) }
        LaunchedEffect(Unit) {
            while (true) {
                delay(3000)
                infoIndex = (infoIndex + 1) % 5
            }
        }

        // Pulsing animation for the neon status dot
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1500
                    0.3f at 0
                    1.0f at 750
                    0.3f at 1500
                },
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )

        // Rotate the outer ring when updating
        val updateAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "updateAngle"
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .wrapContentSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    )
                }
        ) {
            // Corpo Circular ultra-leve, limpo e translúcido (Glassmorphism)
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(bubbleScale)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xEC14181F), // Glassy dark obsidian
                                Color(0xF70B0D11)
                            )
                        )
                    )
                    .border(
                        BorderStroke(
                            width = 1.5.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(bubbleColor, glowColor.copy(alpha = 0.6f))
                            )
                        ),
                        shape = CircleShape
                    )
                    .clickable { 
                        if (infoIndex == 3) {
                            onUpdate()
                        } else {
                            onExpand()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // Anel indicador interno que rotaciona quando o scan está ativo
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(3.dp)
                        .graphicsLayer(rotationZ = if (isWorking) updateAngle else 0f)
                        .border(
                            width = 1.dp,
                            color = if (isWorking) glowColor.copy(alpha = 0.5f) else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(
                        targetState = infoIndex,
                        animationSpec = tween(500, easing = FastOutSlowInEasing),
                        label = "bubbleCarousel"
                    ) { index ->
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            when (index) {
                                0 -> {
                                    Icon(
                                        imageVector = if (isDriverMode) Icons.Default.DirectionsCar else Icons.Default.Luggage,
                                        contentDescription = "BlaBla$ Widget",
                                        tint = bubbleColor,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                                1 -> {
                                    val avgPrice = route?.averagePrice ?: 45.0
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = "MÉDIA",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 0.2.sp
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "R$%.0f".format(avgPrice),
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                2 -> {
                                    val suggPrice = route?.calculateSuggestedPrice() ?: 41.85
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = if (isDriverMode) "SUGERIDO" else "ALVO",
                                            color = if (isDriverMode) Color(0xFF39FF14) else Color(0xFF00E5FF),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.1.sp
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "R$%.1f".format(suggPrice),
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                                3 -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = "ATUALIZAR",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 0.2.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Atualizar Ofertas",
                                            tint = if (isDriverMode) Color(0xFF39FF14) else Color(0xFF00E5FF),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                4 -> {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(2.dp)
                                    ) {
                                        Text(
                                            text = "TOQUE",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "ABRIR",
                                            color = bubbleColor,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pequeno ponto de status inteligente (Smart Neon Badge) no canto superior direito
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-1).dp, y = 1.dp)
                        .clip(CircleShape)
                        .background(
                            if (isWorking) {
                                Color(0xFF00E5FF)
                            } else {
                                Color(0xFF39FF14).copy(alpha = glowAlpha)
                            }
                        )
                        .border(1.dp, Color.Black, CircleShape)
                )
            }

            // Indicador sutil apontado para baixo (Speech Bubble tail), compacto e com gradiente
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = (-3).dp)
                    .graphicsLayer(rotationZ = 45f)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(bubbleColor, bubbleColor.copy(alpha = 0.8f))
                        )
                    )
            )
        }
    }

    data class MarketOffer(
        val id: Int,
        val driverName: String,
        val rating: Double,
        val departureTime: String,
        val seatsAvailable: Int,
        val price: Double
    )

    private fun generateMarketOffers(routePrice: Double): List<MarketOffer> {
        val drivers = listOf(
            Pair("Claudio F.", 4.9),
            Pair("Rodrigo Lima", 4.8),
            Pair("Patricia S.", 4.7),
            Pair("Carla Santos", 4.9),
            Pair("Julio Cesar", 4.6),
            Pair("Marcia Toledo", 4.8),
            Pair("Eduardo L.", 4.5),
            Pair("Mariana Pinheiro", 5.0),
            Pair("Felipe Rocha", 4.7),
            Pair("Gabriel Silva", 4.9)
        )
        val times = listOf(
            "Em 10 min", "Saída 19:30", "Saída 19:45", "Saída 19:55", 
            "Saída 20:15", "Saída 20:30", "Saída 21:00", "Saída 21:30", 
            "Amanhã 07:15", "Amanhã 08:30"
        )
        val seats = listOf(3, 1, 2, 4, 1, 3, 2, 4, 3, 2)
        val priceFactors = listOf(0.78, 0.84, 0.89, 0.94, 1.00, 1.05, 1.12, 1.18, 1.25, 1.35)

        return drivers.indices.map { index ->
            val driver = drivers[index]
            val factor = priceFactors[index]
            val rawPrice = routePrice * factor
            val roundedPrice = Math.round(rawPrice * 2.0) / 2.0
            MarketOffer(
                id = index + 1,
                driverName = driver.first,
                rating = driver.second,
                departureTime = times[index],
                seatsAvailable = seats[index],
                price = roundedPrice
            )
        }
    }

    /**
     * Card de oferta individual adaptado para renderizar perfeitamente no mini painel flutuante
     */
    @Composable
    private fun MarketOfferCard(
        offer: MarketOffer,
        themeColor: Color,
        isDriverMode: Boolean,
        suggestedPrice: Double
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0x13FFFFFF)
            ),
            border = BorderStroke(
                width = 0.5.dp,
                color = themeColor.copy(alpha = 0.15f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = offer.driverName,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(8.dp)
                        )
                        Text(
                            text = "%.1f".format(offer.rating),
                            color = Color(0xFFFFD700),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${offer.departureTime} • ${offer.seatsAvailable} ${if (offer.seatsAvailable == 1) "vaga" else "vagas"}",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "R$ %.2f".format(offer.price),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )

                    val isMoreExpensiveThanSuggested = offer.price >= suggestedPrice
                    val withinTarget = offer.price <= suggestedPrice

                    val badgeText = if (isDriverMode) {
                        if (isMoreExpensiveThanSuggested) "Competir!" else "Abaixo"
                    } else {
                        if (withinTarget) "META" else "Alto"
                    }

                    val badgeBg = if (isDriverMode) {
                        if (isMoreExpensiveThanSuggested) Color(0x2239FF14) else Color(0x22FF3D00)
                    } else {
                        if (withinTarget) Color(0x2200E5FF) else Color(0x19FFFFFF)
                    }

                    val badgeColor = if (isDriverMode) {
                        if (isMoreExpensiveThanSuggested) Color(0xFF39FF14) else Color(0xFFFF5252)
                    } else {
                        if (withinTarget) Color(0xFF00E5FF) else Color(0xFFB0B0B0)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeBg)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    /**
     * Painel expandido atualizado: Horizontal, mais estreito, clean e leve.
     * Oferece contraste excelente para motoristas em trânsito com visual Glassmorphic premium.
     */
    @Composable
    private fun FloatingPanelContent(
        route: SearchRoute,
        statusText: String,
        isUpdating: Boolean,
        isDriverMode: Boolean,
        showFullList: Boolean,
        onToggleShowFullList: () -> Unit,
        onToggleMode: () -> Unit,
        onCollapse: () -> Unit,
        onUpdateClick: () -> Unit,
        onDrag: (Float, Float) -> Unit
    ) {
        val themeColor = if (isDriverMode) Color(0xFF00AFF5) else Color(0xFF00C853)
        val modeLabel = if (isDriverMode) "Motorista" else "Passageiro"
        val activeIcon = if (isDriverMode) Icons.Default.DirectionsCar else Icons.Default.Luggage
        val priceLabel = if (isDriverMode) "Sugerido" else "Alvo"
        val offers = remember(route.averagePrice) { generateMarketOffers(route.averagePrice) }

        Box(
            modifier = Modifier
                .width(310.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xF912161E), // Obsidian translúcido limpo
                            Color(0xF90A0D14)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (isUpdating) Color(0xFF39FF14) else themeColor.copy(alpha = 0.45f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // ROW 1: HEADER & ACTIONS
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Seletor de Modo Rápido (Motorista/Passageiro) com feedback visual
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(themeColor.copy(alpha = 0.15f))
                                .border(1.dp, themeColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                                .clickable { onToggleMode() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeIcon,
                                contentDescription = "Mudar Módulo",
                                tint = themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(8.dp)
                                    .background(Color.Black, RoundedCornerShape(2.dp))
                            )
                        }

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "BlaBla$ • $modeLabel",
                                color = themeColor,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1
                            )
                            Text(
                                text = "${route.origin} → ${route.destination}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Ações Compactas e Flutuantes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Badge de status
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (isUpdating) Color(0x3339FF14) else Color(0x19FFFFFF)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = statusText,
                                color = if (isUpdating) Color(0xFF39FF14) else Color(0xFFB0B0B0),
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Botão Atualizar (Gera efeito de rotação sutil)
                        IconButton(
                            onClick = onUpdateClick,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = themeColor,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (isUpdating) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 1.2.dp,
                                    modifier = Modifier.size(10.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sincronizar",
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }

                        // Recolher para Balão
                        IconButton(
                            onClick = onCollapse,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Minimizar",
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // BOX DE PRECIFICAÇÃO (VALORES RESUMIDOS EM LINHA ÚNICA)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x0EFFFFFF))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Média: R$ %.2f".format(route.averagePrice),
                        color = Color(0xFFC0C0C5),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Preço sugerido destacado e ultra limpo para visão rápida
                    Text(
                        text = "$priceLabel: R$ %.2f".format(route.calculateSuggestedPrice()),
                        color = if (isDriverMode) Color(0xFF39FF14) else Color(0xFF00E5FF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Divisor de layout translúcido
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(themeColor.copy(alpha = 0.2f))
                )

                // Sub-cabeçalho de Listagem
                val listHeader = if (showFullList) {
                    if (isDriverMode) "CONCORRÊNCIA DO TRECHO" else "MELHORES CARONAS DO TRECHO"
                } else {
                    if (isDriverMode) "DICA DE CONCORRENTE DIRETO" else "MELHOR CARONA ENCONTRADA"
                }
                Text(
                    text = listHeader,
                    color = themeColor.copy(alpha = 0.8f),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp
                )

                if (showFullList) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(offers) { offer ->
                            MarketOfferCard(
                                offer = offer,
                                themeColor = themeColor,
                                isDriverMode = isDriverMode,
                                suggestedPrice = route.calculateSuggestedPrice()
                            )
                        }
                    }
                } else {
                    val bestOffer = offers.firstOrNull()
                    if (bestOffer != null) {
                        MarketOfferCard(
                            offer = bestOffer,
                            themeColor = themeColor,
                            isDriverMode = isDriverMode,
                            suggestedPrice = route.calculateSuggestedPrice()
                        )
                    }
                }

                // Botão de Toggle para ver outras ofertas (Leve e Elegante)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onToggleShowFullList() }
                        .background(themeColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (showFullList) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Configuração de Listagem",
                        tint = themeColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showFullList) "Ver Apenas o Melhor" else "Ver Outras 9 Ofertas Encontradas",
                        color = themeColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Drag instruction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Arraste para reposicionar • Toque no ícone para mudar de módulo",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 7.5.sp
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        serviceJob.cancel()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        
        composeView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e("BlaBlaService", "Error removing layout floating view: ${e.message}")
            }
        }
    }
}
