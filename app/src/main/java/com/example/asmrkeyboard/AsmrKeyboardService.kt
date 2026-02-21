package com.example.asmrkeyboard

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.*
import androidx.savedstate.*
import com.example.asmrkeyboard.ui.theme.ASMRKeyboardTheme
import com.example.asmrkeyboard.ui.theme.KeyboardTheme
import kotlinx.coroutines.delay

enum class KeyType { CHARACTER, BACKSPACE, SPACE, SHIFT, ENTER, MODE_CHANGE }
enum class KeyboardLayout { ALPHABET, SYMBOLS }

data class KeyData(val type: KeyType, val text: String, val weight: Float = 1f)

@SuppressLint("ClickableViewAccessibility")
class AsmrKeyboardService : InputMethodService(), SensorEventListener {

    private lateinit var lifecycleOwner: ServiceLifecycleOwner
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    private var tiltX by mutableFloatStateOf(0f)
    private var tiltY by mutableFloatStateOf(0f)

    var isShifted by mutableStateOf(false)
    var currentLayout by mutableStateOf(KeyboardLayout.ALPHABET)

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            tiltY = orientation[1] // Pitch
            tiltX = orientation[2] // Roll
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onCreateInputView(): View {
        val decorView = window.window!!.decorView
        decorView.setViewTreeLifecycleOwner(lifecycleOwner)
        decorView.setViewTreeViewModelStoreOwner(lifecycleOwner)
        decorView.setViewTreeSavedStateRegistryOwner(lifecycleOwner)

        return ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                var currentTheme by remember { mutableStateOf<KeyboardTheme>(KeyboardTheme.Wood) }
                
                ASMRKeyboardTheme(theme = currentTheme) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .background(Color.Black)
                    ) {
                        Box(modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                translationX = tiltX * -40f
                                translationY = tiltY * -40f
                                scaleX = 1.2f
                                scaleY = 1.2f
                            }
                            .blur(8.dp)
                            .background(ASMRKeyboardTheme.colors.keyboardBackgroundColor.copy(alpha = 0.7f))
                        )

                        Column(modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(bottom = 8.dp)
                        ) {
                            ThemeSelector(currentTheme = currentTheme, onThemeChange = { currentTheme = it })
                            Keyboard(
                                isShifted = isShifted,
                                currentLayout = currentLayout,
                                onKeyPress = ::handleKeyPress,
                                tiltX = tiltX,
                                tiltY = tiltY
                            )
                        }

                        Box(modifier = Modifier
                            .matchParentSize()
                            .graphicsLayer {
                                translationX = tiltX * 50f
                                translationY = tiltY * 50f
                                scaleX = 1.2f
                                scaleY = 1.2f
                            }
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                                    radius = 1500f
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    private fun handleKeyPress(keyData: KeyData) {
        val ic = currentInputConnection ?: return
        when (keyData.type) {
            KeyType.CHARACTER -> {
                ic.commitText(if (isShifted) keyData.text.uppercase() else keyData.text.lowercase(), 1)
                isShifted = false
            }
            KeyType.BACKSPACE -> ic.deleteSurroundingText(1, 0)
            KeyType.SPACE -> ic.commitText(" ", 1)
            KeyType.SHIFT -> isShifted = !isShifted
            KeyType.ENTER -> {
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            KeyType.MODE_CHANGE -> {
                currentLayout = if (currentLayout == KeyboardLayout.ALPHABET) KeyboardLayout.SYMBOLS else KeyboardLayout.ALPHABET
            }
        }
    }

    override fun onDestroy() { super.onDestroy(); sensorManager.unregisterListener(this) }
}

@Composable
fun Keyboard(isShifted: Boolean, currentLayout: KeyboardLayout, onKeyPress: (KeyData) -> Unit, tiltX: Float, tiltY: Float) {
    val keyRows = when (currentLayout) {
        KeyboardLayout.ALPHABET -> {
            val row1 = "QWERTYUIOP".map { KeyData(KeyType.CHARACTER, it.toString()) }
            val row2 = "ASDFGHJKL".map { KeyData(KeyType.CHARACTER, it.toString()) }
            val row3 = listOf(KeyData(KeyType.SHIFT, "SFT", 1.2f)) +
                    "ZXCVBNM".map { KeyData(KeyType.CHARACTER, it.toString()) } +
                    listOf(KeyData(KeyType.BACKSPACE, "BS", 1.2f))
            val row4 = listOf(
                KeyData(KeyType.MODE_CHANGE, "?123", 1.5f),
                KeyData(KeyType.SPACE, "SPACE", 5f),
                KeyData(KeyType.ENTER, "ENTER", 2f)
            )
            listOf(row1, row2, row3, row4)
        }
        KeyboardLayout.SYMBOLS -> {
            val row1 = "1234567890".map { KeyData(KeyType.CHARACTER, it.toString()) }
            val row2 = "@#\$%&*-+()".map { KeyData(KeyType.CHARACTER, it.toString()) }
            val row3 = listOf(KeyData(KeyType.CHARACTER, "!", 1.2f)) +
                    "\"':;/?".map { KeyData(KeyType.CHARACTER, it.toString()) } +
                    listOf(KeyData(KeyType.BACKSPACE, "BS", 1.2f))
            val row4 = listOf(
                KeyData(KeyType.MODE_CHANGE, "ABC", 1.5f),
                KeyData(KeyType.SPACE, "SPACE", 5f),
                KeyData(KeyType.ENTER, "ENTER", 2f)
            )
            listOf(row1, row2, row3, row4)
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        keyRows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                row.forEach { keyData ->
                    KeyButton(keyData, isShifted, onKeyPress, tiltX, tiltY)
                }
            }
        }
    }
}

@Composable
fun RowScope.KeyButton(keyData: KeyData, isShifted: Boolean, onPress: (KeyData) -> Unit, tiltX: Float, tiltY: Float) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animation = ASMRKeyboardTheme.animation

    // Animation Specs
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
    )
    
    val elevation by animateDpAsState(targetValue = if (isPressed) 0.dp else 6.dp)
    val scaleX by animateFloatAsState(targetValue = if (isPressed) animation.pressScaleX else 1f, animationSpec = animation.scaleAnimationSpec)
    val scaleY by animateFloatAsState(targetValue = if (isPressed) animation.pressScaleY else 1f, animationSpec = animation.scaleAnimationSpec)

    // Parallax Sensitivity
    val sSurface = 12f   // L4: Top
    val sBody    = -4f   // L3: Side
    val sBase    = -15f  // L2: Hole (Target for sinking)

    // Calculate base (idle) positions
    val idleSurfaceX = tiltX * sSurface
    val idleSurfaceY = tiltY * sSurface
    val idleBodyX    = tiltX * sBody
    val idleBodyY    = tiltY * sBody
    val targetBaseX  = tiltX * sBase
    val targetBaseY  = tiltY * sBase

    // Sinking logic: Move towards the base (shadow direction) when pressed
    val currentSurfaceX = idleSurfaceX + (targetBaseX - idleSurfaceX) * pressProgress
    val currentSurfaceY = idleSurfaceY + (targetBaseY - idleSurfaceY) * pressProgress
    val currentBodyX    = idleBodyX + (targetBaseX - idleBodyX) * pressProgress
    val currentBodyY    = idleBodyY + (targetBaseY - idleBodyY) * pressProgress

    val surfaceColor by animateColorAsState(if (isPressed || (keyData.type == KeyType.SHIFT && isShifted)) ASMRKeyboardTheme.colors.keyBackgroundColorPressed else ASMRKeyboardTheme.colors.keyBackgroundColor)
    val bodyColor = ASMRKeyboardTheme.colors.keyBackgroundColorPressed.copy(alpha = 0.8f)
    val baseColor = Color.Black.copy(alpha = 0.5f)

    if (isPressed && keyData.type == KeyType.BACKSPACE) {
        LaunchedEffect(keyData) {
            delay(500)
            while (true) {
                onPress(keyData)
                delay(100)
            }
        }
    }

    Box(modifier = Modifier.weight(keyData.weight).height(54.dp), contentAlignment = Alignment.Center) {
        // L2: Key Base (The "Hole" - Fixed depth)
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationX = targetBaseX; translationY = targetBaseY }.shadow(elevation / 2, RoundedCornerShape(8.dp)).background(baseColor, RoundedCornerShape(8.dp)))

        // L3: Key Body (The "Side" - Sinks and hides)
        Box(modifier = Modifier.fillMaxSize().padding(1.dp).graphicsLayer { translationX = currentBodyX; translationY = currentBodyY; this.scaleX = scaleX; this.scaleY = scaleY }.background(bodyColor, RoundedCornerShape(8.dp)))

        // L4: Key Surface (The "Top" - Sinks to the bottom)
        Box(modifier = Modifier.fillMaxSize().padding(1.dp).graphicsLayer { translationX = currentSurfaceX; translationY = currentSurfaceY; this.scaleX = scaleX; this.scaleY = scaleY }.shadow(elevation, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(surfaceColor).clickable(interactionSource, null) { onPress(keyData) }, contentAlignment = Alignment.Center) {
            Text(text = if (keyData.type == KeyType.CHARACTER) (if (isShifted) keyData.text.uppercase() else keyData.text.lowercase()) else keyData.text, color = ASMRKeyboardTheme.colors.keyTextColor, fontSize = if (keyData.type == KeyType.CHARACTER) 20.sp else 14.sp)
        }
    }
}

@Composable
fun ThemeSelector(currentTheme: KeyboardTheme, onThemeChange: (KeyboardTheme) -> Unit) {
    val themes = listOf(KeyboardTheme.Wood, KeyboardTheme.FrozenSerenity, KeyboardTheme.SlimePop, KeyboardTheme.CozyEmber)
    Box(modifier = Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray.copy(alpha = 0.5f)).clickable { onThemeChange(themes[(themes.indexOf(currentTheme) + 1) % themes.size]) }.padding(8.dp)) {
        Text("Next Theme: ${currentTheme.javaClass.simpleName}", color = Color.White, modifier = Modifier.align(Alignment.Center))
    }
}

private class ServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    init { savedStateRegistryController.performRestore(null) }
    fun handleLifecycleEvent(event: Lifecycle.Event) = lifecycleRegistry.handleLifecycleEvent(event)
}
