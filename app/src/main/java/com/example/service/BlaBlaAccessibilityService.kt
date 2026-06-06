package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.local.AppDatabase
import com.example.data.model.OfertaCaronaEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Requirement 4: ROBOT LOGIC (AccessibilityService)
 * Automates scrolling, scans rides cards, enforces precise city filtering,
 * operates in either Quick Restrictive or Deep Detailed modes, and saves offers.
 */
class BlaBlaAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isBotRunning = false
    private var lastNodeTreeHash = 0
    private var sameTreeCount = 0

    // Filters specified by user search (origin matching)
    private var userOriginCity = "Tramandaí" // Or loaded dynamically from route persistence
    private var userDestCity = "Porto Alegre"

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        
        // Listen to window state updates or list scrolls on BlaBlaCar package
        if (event.packageName == "com.comuto.blablacar") {
            val rootNode = rootInActiveWindow ?: return
            
            // Auto start intelligence scan if we enter a list screen
            if (!isBotRunning && (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) {
                checkAndStartHarvesting(rootNode)
            }
        }
    }

    private fun checkAndStartHarvesting(rootNode: AccessibilityNodeInfo) {
        val pageText = findAnyTextInTree(rootNode)
        // Detect if we are on BlaBlaCar search results page
        if (pageText.contains("carona") || pageText.contains("R$") || pageText.contains("Filtro")) {
            startLoop(rootNode)
        }
    }

    private fun findAnyTextInTree(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val sb = java.lang.StringBuilder()
        if (node.text != null) {
            sb.append(node.text.toString()).append(" ")
        }
        for (i in 0 until node.childCount) {
            sb.append(findAnyTextInTree(node.getChild(i)))
        }
        return sb.toString()
    }

    /**
     * Entry point to execute scrolling, top return gestures, and mode parsing
     */
    private fun startLoop(rootNode: AccessibilityNodeInfo) {
        isBotRunning = true
        Log.d("BlaBlaBot", "Iniciando robô inteligente de coleta BlaBla$...")
        
        // Read configuration mode
        val state = GerenciadorEstado.estado.value
        val mode = if (state.moduloAtivo == "MOTORISTA") "RESTRITO RÁPIDO" else "PROFUNDO DETALHADO"
        GerenciadorEstado.atualizarEstado(state.moduloAtivo, "$mode | Iniciando...")

        // Step 1: Return to Top
        executeScrollToTop {
            // Step 2: Loop scroll and extract
            runDelayedScroller(0)
        }
    }

    /**
     * Requirement 4: Scroll up gestured execution
     */
    private fun executeScrollToTop(onComplete: () -> Unit) {
        Log.d("BlaBlaBot", "Executando gesto de rolagem para o Topo")
        val topPath = Path().apply {
            moveTo(500f, 300f)
            lineTo(500f, 1600f) // Long drag down to scroll up
        }
        val stroke = GestureDescription.StrokeDescription(topPath, 0, 400)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                mainHandler.postDelayed({ onComplete() }, 800)
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                onComplete()
            }
        }, null)
    }

    /**
     * Requirement 4: Loop scroll down until tree nodes remain unaltered
     */
    private fun runDelayedScroller(scrollAttempt: Int) {
        if (!isBotRunning) return

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            stopBot("Lista finalizada ou tela perdida")
            return
        }

        // Calculate a simple structural hash of the node tree to verify if list ended
        val currentHash = calculateTreeHash(rootNode)
        if (currentHash == lastNodeTreeHash) {
            sameTreeCount++
            if (sameTreeCount >= 3) {
                stopBot("Fim da lista detectado (Árvore estável)")
                return
            }
        } else {
            sameTreeCount = 0
            lastNodeTreeHash = currentHash
        }

        // Harvest nodes
        executeDataExtraction(rootNode)

        // Drag down gesture upwards (scrolling down the list)
        val downPath = Path().apply {
            moveTo(500f, 1500f)
            lineTo(500f, 500f)
        }
        val stroke = GestureDescription.StrokeDescription(downPath, 0, 500)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                mainHandler.postDelayed({
                    runDelayedScroller(scrollAttempt + 1)
                }, 1200) // Pause dynamically for content loads
            }
            override fun onCancelled(gestureDescription: GestureDescription?) {
                stopBot("Gesto cancelado")
            }
        }, null)
    }

    private fun calculateTreeHash(node: AccessibilityNodeInfo?): Int {
        if (node == null) return 0
        var hash = (node.text?.hashCode() ?: 0) + (node.className?.hashCode() ?: 0)
        for (i in 0 until node.childCount) {
            hash += calculateTreeHash(node.getChild(i))
        }
        return hash
    }

    /**
     * Requirement 4: Modos de Operação (RESTRITO RÁPIDO & PROFUNDO DETALHADO)
     * Extract coordinates, checks precision filters (Exact City checks) and saves.
     */
    private fun executeDataExtraction(rootNode: AccessibilityNodeInfo) {
        val state = GerenciadorEstado.estado.value
        val isDeepMode = state.moduloAtivo == "PASSAGEIRO" // If passageiro, triggers PROFUNDO DETALHADO

        val cards = mutableListOf<AccessibilityNodeInfo>()
        findCardNodes(rootNode, cards)

        Log.d("BlaBlaBot", "Contagem de cartões de caronas na viewport: ${cards.size}")
        
        for (card in cards) {
            val texts = mutableListOf<String>()
            extractAllTextFromNode(card, texts)
            
            // Check exact Origin City validation filter
            var hasExactOrigin = false
            for (txt in texts) {
                if (txt.equals(userOriginCity, ignoreCase = true)) {
                    hasExactOrigin = true
                    break
                }
            }

            if (!hasExactOrigin) {
                Log.d("BlaBlaBot", "Descartando cartão - Cidade de Origem difere do filtro exato '$userOriginCity'")
                continue
            }

            // Exceed parsing variables
            var preco = 0.0
            var nota = 4.8
            var avaliacoes: Int? = null
            var modeloCarro: String? = null

            for (txt in texts) {
                if (txt.contains("R$")) {
                    val rawPreco = txt.replace("R$", "").replace(",", ".").trim()
                    preco = rawPreco.toDoubleOrNull() ?: 0.0
                }
                if (txt.contains("★") || txt.matches(Regex("\\d[.,]\\d"))) {
                    val rawNota = txt.replace("★", "").replace(",", ".").trim()
                    nota = rawNota.toDoubleOrNull() ?: 4.8
                }
            }

            if (isDeepMode) {
                // PROFUNDO DETALHADO: Click through to details
                GerenciadorEstado.atualizarEstado("PASSAGEIRO", "PROFUNDO | Analisando detalhes...")
                if (card.isClickable) {
                    card.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    mainHandler.postDelayed({
                        val detailedRoot = rootInActiveWindow
                        if (detailedRoot != null) {
                            val detailedTexts = mutableListOf<String>()
                            extractAllTextFromNode(detailedRoot, detailedTexts)
                            
                            // Extract detailed statistics
                            for (dt in detailedTexts) {
                                if (dt.contains("avaliações") || dt.contains("avaliação")) {
                                    avaliacoes = dt.filter { it.isDigit() }.toIntOrNull()
                                }
                                if (dt.contains("Ford") || dt.contains("Chevrolet") || dt.contains("Fiat") || dt.contains("VW") || dt.contains("Volkswagen") || dt.contains("Renault") || dt.contains("Hyundai")) {
                                    modeloCarro = dt
                                }
                            }
                        }
                        // Press system back button to exit detailed screen
                        performGlobalAction(GLOBAL_ACTION_BACK)
                    }, 1000)
                }
            } else {
                GerenciadorEstado.atualizarEstado("MOTORISTA", "RESTRITO RÁPIDO | Analisando...")
            }

            // Save harvested result to local Room Db
            saveHarvestedRide(preco, nota, avaliacoes, modeloCarro)
        }
    }

    private fun findCardNodes(node: AccessibilityNodeInfo?, cards: MutableList<AccessibilityNodeInfo>) {
        if (node == null) return
        
        // BlaBlaCar typical ride cards have layout containers or actionable elements
        if (node.isClickable && (node.className == "android.view.ViewGroup" || node.className == "android.widget.FrameLayout")) {
            cards.add(node)
            return
        }
        for (i in 0 until node.childCount) {
            findCardNodes(node.getChild(i), cards)
        }
    }

    private fun extractAllTextFromNode(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        node.text?.let { list.add(it.toString()) }
        for (i in 0 until node.childCount) {
            extractAllTextFromNode(node.getChild(i), list)
        }
    }

    private fun saveHarvestedRide(preco: Double, nota: Double, avaliacoes: Int?, modeloCarro: String?) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        
        val record = OfertaCaronaEntity(
            trechoOrigem = userOriginCity,
            trechoDestino = userDestCity,
            preco = preco,
            notaMotorista = nota,
            avaliacoesQtde = avaliacoes,
            dataViagem = todayStr,
            horarioViagem = timeStr,
            modeloCarro = modeloCarro ?: "Não Informado"
        )

        scope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            db.ofertaCaronaDao().insert(record)
            Log.d("BlaBlaBot", "Oferta de carona persistida no Room com Sucesso: R$ $preco, Origem: $userOriginCity")
        }
    }

    private fun stopBot(reason: String) {
        isBotRunning = false
        Log.d("BlaBlaBot", "Robô finalizado: $reason")
        val state = GerenciadorEstado.estado.value
        GerenciadorEstado.atualizarEstado(state.moduloAtivo, "Inativo | Coleta Concluída")
    }

    override fun onInterrupt() {
        stopBot("Serviço de Acessibilidade interrompido")
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
