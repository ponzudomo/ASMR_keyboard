package com.example.asmrkeyboard

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.asmrkeyboard.ui.theme.ASMRKeyboardTheme
import com.example.asmrkeyboard.ui.theme.KeyboardTheme

enum class KeyType {
    CHARACTER,
    BACKSPACE,
    SPACE,
    SHIFT,
    // ENTER will be added later.
}

data class KeyData(val type: KeyType, val text: String, val weight: Float = 1f)

@SuppressLint("ClickableViewAccessibility")
class AsmrKeyboardService : InputMethodService() {

    private lateinit var lifecycleOwner: ServiceLifecycleOwner

    override fun onCreate() {
        super.onCreate()
        lifecycleOwner = ServiceLifecycleOwner()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    override fun onInitializeInterface() {
        super.onInitializeInterface()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onFinishInput() {
        super.onFinishInput()
        if (isInputViewShown) {
            lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    }

    var isShifted by mutableStateOf(false)

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
                    Column {
                        ThemeSelector(currentTheme = currentTheme, onThemeChange = { currentTheme = it })
                        Keyboard(isShifted = isShifted, onKeyPress = ::handleKeyPress)
                    }
                }
            }
        }
    }

    private fun handleKeyPress(keyData: KeyData) {
        val inputConnection = currentInputConnection ?: return

        when (keyData.type) {
            KeyType.CHARACTER -> {
                val text = if (isShifted) keyData.text.uppercase() else keyData.text.lowercase()
                inputConnection.commitText(text, 1)
                isShifted = false
            }
            KeyType.BACKSPACE -> inputConnection.deleteSurroundingText(1, 0)
            KeyType.SPACE -> inputConnection.commitText(" ", 1)
            KeyType.SHIFT -> isShifted = !isShifted
        }
    }
}

private class ServiceLifecycleOwner() :
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController: SavedStateRegistryController

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performRestore(null)
    }

    fun handleLifecycleEvent(event: Lifecycle.Event) {
        lifecycleRegistry.handleLifecycleEvent(event)
    }
}

@Composable
fun ThemeSelector(currentTheme: KeyboardTheme, onThemeChange: (KeyboardTheme) -> Unit) {
    val themes = listOf(KeyboardTheme.Wood, KeyboardTheme.FrozenSerenity, KeyboardTheme.SlimePop, KeyboardTheme.CozyEmber)
    val currentIndex = themes.indexOf(currentTheme)
    val nextIndex = (currentIndex + 1) % themes.size
    val nextTheme = themes[nextIndex]

    Box(modifier = Modifier
        .fillMaxWidth()
        .padding(4.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(Color.DarkGray)
        .clickable { onThemeChange(nextTheme) }
        .padding(8.dp)
    ) {
        Text("Next Theme: ${nextTheme.javaClass.simpleName}", color = Color.White, modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun Keyboard(isShifted: Boolean, onKeyPress: (KeyData) -> Unit) {
    val keyRows = listOf(
        "QWERTYUIOP".map { KeyData(KeyType.CHARACTER, it.toString()) },
        "ASDFGHJKL".map { KeyData(KeyType.CHARACTER, it.toString()) },
        listOf(
            KeyData(KeyType.SHIFT, "SFT", 1.5f),
            KeyData(KeyType.CHARACTER, "Z"),
            KeyData(KeyType.CHARACTER, "X"),
            KeyData(KeyType.CHARACTER, "C"),
            KeyData(KeyType.CHARACTER, "V"),
            KeyData(KeyType.CHARACTER, "B"),
            KeyData(KeyType.CHARACTER, "N"),
            KeyData(KeyType.CHARACTER, "M"),
            KeyData(KeyType.BACKSPACE, "BS", 1.5f)
        ),
        listOf(
            KeyData(KeyType.SPACE, "", 8f)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ASMRKeyboardTheme.colors.keyboardBackgroundColor)
            .padding(2.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        keyRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { keyData ->
                    KeyButton(
                        keyData = keyData,
                        isShifted = isShifted,
                        onPress = onKeyPress
                    )
                }
            }
        }
    }
}

@Composable
fun RowScope.KeyButton(
    keyData: KeyData,
    isShifted: Boolean,
    onPress: (KeyData) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val animation = ASMRKeyboardTheme.animation

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 4.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
    )

    val scaleX by animateFloatAsState(
        targetValue = if (isPressed) animation.pressScaleX else 1f,
        animationSpec = animation.scaleAnimationSpec
    )
    val scaleY by animateFloatAsState(
        targetValue = if (isPressed) animation.pressScaleY else 1f,
        animationSpec = animation.scaleAnimationSpec
    )

    val color by animateColorAsState(
        targetValue = if (isPressed || (keyData.type == KeyType.SHIFT && isShifted)) {
            ASMRKeyboardTheme.colors.keyBackgroundColorPressed
        } else {
            ASMRKeyboardTheme.colors.keyBackgroundColor
        }
    )

    val text = if (keyData.type == KeyType.CHARACTER) {
        if (isShifted) keyData.text.uppercase() else keyData.text.lowercase()
    } else {
        keyData.text
    }

    Box(
        modifier = Modifier
            .weight(keyData.weight)
            .padding(2.dp)
            .graphicsLayer {
                this.scaleX = scaleX
                this.scaleY = scaleY
            }
            .shadow(elevation = elevation, shape = RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .clickable(interactionSource = interactionSource, indication = null) { onPress(keyData) }
            .padding(vertical = 16.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = ASMRKeyboardTheme.colors.keyTextColor)
    }
}
