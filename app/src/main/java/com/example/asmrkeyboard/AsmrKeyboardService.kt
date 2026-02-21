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

/** Defines the type of a key. / キーの種類を定義します。 */
enum class KeyType { CHARACTER, BACKSPACE, SPACE, SHIFT, ENTER, MODE_CHANGE }
/** Defines the current keyboard layout. / 現在のキーボードレイアウトを定義します。 */
enum class KeyboardLayout { ALPHABET, SYMBOLS }

/**
 * Data class for a single key.
 * 単一のキーのデータクラスです。
 * @param type The function of the key. / キーの機能。
 * @param text The character displayed on the key. / キーに表示される文字。
 * @param weight The layout weight of the key in a row. / 行内でのキーのレイアウト比重。
 */
data class KeyData(val type: KeyType, val text: String, val weight: Float = 1f)

/**
 * The main InputMethodService for the ASMR Keyboard.
 * This service manages the keyboard's lifecycle, sensor events for parallax effects, and the Compose UI.
 *
 * ASMRキーボードのメインとなるInputMethodServiceです。
 * このサービスは、キーボードのライフサイクル、視差効果のためのセンサーイベント、そしてCompose UIを管理します。
 */
@SuppressLint("ClickableViewAccessibility")
class AsmrKeyboardService : InputMethodService(), SensorEventListener {

    private lateinit var lifecycleOwner: ServiceLifecycleOwner
    private lateinit var sensorManager: SensorManager
    private var rotationSensor: Sensor? = null

    /** The horizontal tilt of the device (Roll), updated by the sensor. / デバイスの水平方向の傾き（ロール）。センサーによって更新されます。 */
    private var tiltX by mutableFloatStateOf(0f)
    /** The vertical tilt of the device (Pitch), updated by the sensor. / デバイスの垂直方向の傾き（ピッチ）。センサーによって更新されます。 */
    private var tiltY by mutableFloatStateOf(0f)

    /** The state of the Shift key. / Shiftキーの状態。 */
    var isShifted by mutableStateOf(false)
    /** The current keyboard layout (Alphabet or Symbols). / 現在のキーボードレイアウト（アルファベットまたは記号）。 */
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
        // Start listening to the sensor only when the keyboard is visible to save battery.
        // バッテリーを節約するため、キーボードが表示されている間のみセンサーの監視を開始します。
        rotationSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        // Stop listening to the sensor when the keyboard is hidden.
        // キーボードが非表示になったらセンサーの監視を停止します。
        sensorManager.unregisterListener(this)
    }

    /**
     * Called when sensor values have changed.
     * It uses the rotation vector sensor to get the device's orientation, which is more stable than using raw accelerometer data.
     *
     * センサーの値が変化したときに呼ばれます。
     * 加速度センサーの生データよりも安定している回転ベクトルセンサーを使い、デバイスの向きを取得します。
     */
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
            val rotationMatrix = FloatArray(9)
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            
            // getOrientation provides Pitch and Roll angles. Note: This can suffer from Gimbal Lock at extreme angles.
            // getOrientationはピッチとロールの角度を提供します。注意：極端な角度ではジンバルロック問題が発生する可能性があります。
            val orientation = FloatArray(3)
            SensorManager.getOrientation(rotationMatrix, orientation)
            tiltY = orientation[1] // Pitch (vertical tilt)
            tiltX = orientation[2] // Roll (horizontal tilt)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Creates the main view of the keyboard.
     * This sets up the multi-layered Compose UI with parallax effects.
     *
     * キーボードのメインビューを作成します。
     * ここで視差効果を持つ多層構造のCompose UIをセットアップします。
     */
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
                    Box(modifier = Modifier.fillMaxWidth().wrapContentHeight().background(Color.Black)) {
                        // L1: Background Layer - Moves significantly in the opposite direction of the tilt to create depth.
                        // L1: 背景レイヤー - 傾きと大きく逆方向に動き、奥行きを演出します。
                        Box(modifier = Modifier.matchParentSize().graphicsLayer {
                                translationX = tiltX * -40f
                                translationY = tiltY * -40f
                                scaleX = 1.2f
                                scaleY = 1.2f
                            }.blur(8.dp).background(ASMRKeyboardTheme.colors.keyboardBackgroundColor.copy(alpha = 0.7f)))

                        // The main keyboard content, which determines the overall size.
                        // キーボードの主コンテンツ。全体のサイズを決定します。
                        Column(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(bottom = 8.dp)) {
                            ThemeSelector(currentTheme = currentTheme, onThemeChange = { currentTheme = it })
                            Keyboard(isShifted, currentLayout, ::handleKeyPress, tiltX, tiltY)
                        }

                        // L6: Overlay Layer - Moves significantly with the tilt to create a foreground "glass" effect.
                        // L6: オーバーレイヤー - 傾きと同じ方向に大きく動き、最前面の「ガラス」のような効果を生み出します。
                        Box(modifier = Modifier.matchParentSize().graphicsLayer {
                                translationX = tiltX * 50f
                                translationY = tiltY * 50f
                                scaleX = 1.2f
                                scaleY = 1.2f
                            }.background(Brush.radialGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)), radius = 1500f)))
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

/**
 * The main keyboard layout composable.
 * It switches between Alphabet and Symbols layouts based on the `currentLayout` state.
 *
 * メインのキーボードレイアウトコンポーザブルです。
 * `currentLayout`の状態に応じて、アルファベットと記号のレイアウトを切り替えます。
 */
@Composable
fun Keyboard(isShifted: Boolean, currentLayout: KeyboardLayout, onKeyPress: (KeyData) -> Unit, tiltX: Float, tiltY: Float) {
    val keyRows = when (currentLayout) {
        KeyboardLayout.ALPHABET -> {
            val row1 = "QWERTYUIOP".map { KeyData(KeyType.CHARACTER, it.toString()) }
            val row2 = "ASDFGHJKL".map { KeyData(KeyType.CHARACTER, it.toString()) }
            val row3 = listOf(KeyData(KeyType.SHIFT, "SFT", 1.2f)) + "ZXCVBNM".map { KeyData(KeyType.CHARACTER, it.toString()) } + listOf(KeyData(KeyType.BACKSPACE, "BS", 1.2f))
            val row4 = listOf(KeyData(KeyType.MODE_CHANGE, "?123", 1.5f), KeyData(KeyType.SPACE, "SPACE", 5f), KeyData(KeyType.ENTER, "ENTER", 2f))
            listOf(row1, row2, row3, row4)
        }
        KeyboardLayout.SYMBOLS -> {
            val row1 = "1234567890".map { KeyData(KeyType.CHARACTER, it.toString()) }
            val row2 = "@#\$%&*-+()".map { KeyData(KeyType.CHARACTER, it.toString()) }
            val row3 = listOf(KeyData(KeyType.CHARACTER, "!", 1.2f)) + "\"':;/?".map { KeyData(KeyType.CHARACTER, it.toString()) } + listOf(KeyData(KeyType.BACKSPACE, "BS", 1.2f))
            val row4 = listOf(KeyData(KeyType.MODE_CHANGE, "ABC", 1.5f), KeyData(KeyType.SPACE, "SPACE", 5f), KeyData(KeyType.ENTER, "ENTER", 2f))
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

/**
 * A composable for a single, multi-layered key with parallax and sinking effects.
 * A single key is composed of 3 layers (Base, Body, Surface) to create a 3D effect.
 *
 * 視差効果と沈み込み効果を持つ、多層構造の単一キーのコンポーザブルです。
 * 3D効果を出すために、一つのキーが3つのレイヤー（Base, Body, Surface）で構成されています。
 */
@Composable
fun RowScope.KeyButton(keyData: KeyData, isShifted: Boolean, onPress: (KeyData) -> Unit, tiltX: Float, tiltY: Float) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animation = ASMRKeyboardTheme.animation

    // Animation progress for sinking effect (0f = idle, 1f = fully pressed).
    // 沈み込み効果のためのアニメーション進捗（0f = 通常時, 1f = 完全な押下時）。
    val pressProgress by animateFloatAsState(if (isPressed) 1f else 0f, spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow))
    
    val elevation by animateDpAsState(if (isPressed) 0.dp else 6.dp)
    val scaleX by animateFloatAsState(if (isPressed) animation.pressScaleX else 1f, animation.scaleAnimationSpec)
    val scaleY by animateFloatAsState(if (isPressed) animation.pressScaleY else 1f, animation.scaleAnimationSpec)

    // --- Parallax Calculation --- //
    // Sensitivity values for each layer. The difference in these values creates the 3D effect.
    // 各レイヤーの感度。これらの値の差が3D効果を生み出します。
    val sSurface = 12f   // L4: Top layer, moves slightly with tilt.
    val sBody    = -4f   // L3: Middle layer, moves slightly opposite to tilt, creating thickness.
    val sBase    = -15f  // L2: Bottom layer, moves more opposite to tilt, acting as the "hole".

    // Calculate the idle (un-pressed) translation for each layer based on tilt.
    // 傾きに基づいて、各レイヤーの通常時の移動量を計算します。
    val idleSurfaceX = tiltX * sSurface
    val idleSurfaceY = tiltY * sSurface
    val idleBodyX    = tiltX * sBody
    val idleBodyY    = tiltY * sBody
    val targetBaseX  = tiltX * sBase
    val targetBaseY  = tiltY * sBase

    // Sinking Logic: When pressed, interpolate the position of Surface and Body towards the Base position.
    // 沈み込みロジック：押下時、SurfaceとBodyの位置をBaseの位置に向かって補間します。
    val currentSurfaceX = idleSurfaceX + (targetBaseX - idleSurfaceX) * pressProgress
    val currentSurfaceY = idleSurfaceY + (targetBaseY - idleSurfaceY) * pressProgress
    val currentBodyX    = idleBodyX + (targetBaseX - idleBodyX) * pressProgress
    val currentBodyY    = idleBodyY + (targetBaseY - idleBodyY) * pressProgress

    val surfaceColor by animateColorAsState(if (isPressed || (keyData.type == KeyType.SHIFT && isShifted)) ASMRKeyboardTheme.colors.keyBackgroundColorPressed else ASMRKeyboardTheme.colors.keyBackgroundColor)
    val bodyColor = ASMRKeyboardTheme.colors.keyBackgroundColorPressed.copy(alpha = 0.8f)
    val baseColor = Color.Black.copy(alpha = 0.5f)

    // Continuous Backspace deletion on long press.
    // Backspaceの長押しによる連続削除。
    if (isPressed && keyData.type == KeyType.BACKSPACE) {
        LaunchedEffect(keyData) {
            delay(500) // Initial delay
            while (true) {
                onPress(keyData)
                delay(100) // Repeat interval
            }
        }
    }

    // --- UI Structure (6-Layer Model) --- //
    Box(modifier = Modifier.weight(keyData.weight).height(54.dp), contentAlignment = Alignment.Center) {
        // L2: Key Base - The "hole" the key sinks into. Moves opposite to the tilt.
        // L2: キーの土台 - キーが沈み込む「穴」。傾きと逆方向に動きます。
        Box(modifier = Modifier.fillMaxSize().graphicsLayer { translationX = targetBaseX; translationY = targetBaseY }.shadow(elevation / 2, RoundedCornerShape(8.dp)).background(baseColor, RoundedCornerShape(8.dp)))

        // L3: Key Body - The side of the key, creates the thickness effect.
        // L3: キーの側面 - キーの厚みを表現する部分。
        Box(modifier = Modifier.fillMaxSize().padding(1.dp).graphicsLayer { translationX = currentBodyX; translationY = currentBodyY; this.scaleX = scaleX; this.scaleY = scaleY }.background(bodyColor, RoundedCornerShape(8.dp)))

        // L4: Key Surface - The top of the key that the user interacts with.
        // L4: キーの表面 - ユーザーが直接触れる部分。
        Box(modifier = Modifier.fillMaxSize().padding(1.dp).graphicsLayer { translationX = currentSurfaceX; translationY = currentSurfaceY; this.scaleX = scaleX; this.scaleY = scaleY }.shadow(elevation, RoundedCornerShape(8.dp)).clip(RoundedCornerShape(8.dp)).background(surfaceColor).clickable(interactionSource, null) { onPress(keyData) }, contentAlignment = Alignment.Center) {
            Text(text = if (keyData.type == KeyType.CHARACTER) (if (isShifted) keyData.text.uppercase() else keyData.text.lowercase()) else keyData.text, color = ASMRKeyboardTheme.colors.keyTextColor, fontSize = if (keyData.type == KeyType.CHARACTER) 20.sp else 14.sp)
        }
    }
}

/**
 * A simple UI element to cycle through available keyboard themes.
 * 利用可能なキーボードテーマを切り替えるためのシンプルなUI要素です。
 */
@Composable
fun ThemeSelector(currentTheme: KeyboardTheme, onThemeChange: (KeyboardTheme) -> Unit) {
    val themes = listOf(KeyboardTheme.Wood, KeyboardTheme.FrozenSerenity, KeyboardTheme.SlimePop, KeyboardTheme.CozyEmber)
    Box(modifier = Modifier.fillMaxWidth().padding(8.dp).clip(RoundedCornerShape(8.dp)).background(Color.DarkGray.copy(alpha = 0.5f)).clickable { onThemeChange(themes[(themes.indexOf(currentTheme) + 1) % themes.size]) }.padding(8.dp)) {
        Text("Next Theme: ${currentTheme.javaClass.simpleName}", color = Color.White, modifier = Modifier.align(Alignment.Center))
    }
}

/**
 * A custom LifecycleOwner for the InputMethodService.
 * This is crucial for making Jetpack Compose work correctly outside of a standard Activity.
 *
 * InputMethodService用のカスタムLifecycleOwnerです。
 * 標準のActivity外でJetpack Composeを正しく動作させるために不可欠です。
 */
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
