package com.networth.tracker.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.networth.tracker.data.PinPreferences
import kotlinx.coroutines.delay

enum class PinLockMode {
    Unlock,
    Setup,
    Change
}

@Composable
fun PinLockScreen(
    pinPreferences: PinPreferences,
    mode: PinLockMode,
    onUnlocked: () -> Unit,
    onPinChanged: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null
) {
    BackHandler(enabled = mode == PinLockMode.Unlock) { /* block back while locked */ }
    if (mode != PinLockMode.Unlock) {
        BackHandler { onCancel?.invoke() }
    }

    val haptic = LocalHapticFeedback.current
    var step by remember(mode) {
        mutableStateOf(
            when (mode) {
                PinLockMode.Unlock -> PinStep.Enter
                PinLockMode.Setup -> PinStep.Create
                PinLockMode.Change -> PinStep.EnterCurrent
            }
        )
    }
    var enteredPin by remember(mode) { mutableStateOf("") }
    var pendingPin by remember(mode) { mutableStateOf("") }
    var errorMessage by remember(mode) { mutableStateOf<String?>(null) }
    var shakeTrigger by remember { mutableIntStateOf(0) }

    val title = when (step) {
        PinStep.Enter -> "Enter PIN"
        PinStep.Create -> "Create a PIN"
        PinStep.Confirm -> "Confirm PIN"
        PinStep.EnterCurrent -> "Enter current PIN"
        PinStep.EnterNew -> "Enter new PIN"
        PinStep.ConfirmNew -> "Confirm new PIN"
    }
    val subtitle = when (step) {
        PinStep.Enter -> "Enter your 4-digit PIN to unlock"
        PinStep.Create -> "Choose a 4-digit PIN to protect your data"
        PinStep.Confirm -> "Enter the same PIN again"
        PinStep.EnterCurrent -> "Verify your current PIN to continue"
        PinStep.EnterNew -> "Choose a new 4-digit PIN"
        PinStep.ConfirmNew -> "Enter the new PIN again"
    }

    fun fail(message: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        errorMessage = message
        enteredPin = ""
        shakeTrigger++
    }

    fun onDigit(digit: Char) {
        if (enteredPin.length >= 4) return
        errorMessage = null
        enteredPin += digit
    }

    fun onBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
            errorMessage = null
        }
    }

    LaunchedEffect(enteredPin, step) {
        if (enteredPin.length < 4) return@LaunchedEffect
        delay(80)

        when (step) {
            PinStep.Enter -> {
                if (pinPreferences.verifyPin(enteredPin)) {
                    onUnlocked()
                } else {
                    fail("Incorrect PIN. Try again.")
                }
            }
            PinStep.Create -> {
                pendingPin = enteredPin
                enteredPin = ""
                step = PinStep.Confirm
            }
            PinStep.Confirm -> {
                if (enteredPin == pendingPin) {
                    pinPreferences.setPin(enteredPin)
                    onUnlocked()
                } else {
                    fail("PINs do not match. Try again.")
                    pendingPin = ""
                    step = PinStep.Create
                }
            }
            PinStep.EnterCurrent -> {
                if (pinPreferences.verifyPin(enteredPin)) {
                    enteredPin = ""
                    step = PinStep.EnterNew
                } else {
                    fail("Incorrect PIN. Try again.")
                }
            }
            PinStep.EnterNew -> {
                pendingPin = enteredPin
                enteredPin = ""
                step = PinStep.ConfirmNew
            }
            PinStep.ConfirmNew -> {
                if (enteredPin == pendingPin) {
                    pinPreferences.setPin(enteredPin)
                    onPinChanged?.invoke()
                } else {
                    fail("PINs do not match. Try again.")
                    pendingPin = ""
                    step = PinStep.EnterNew
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        PinDots(
            filledCount = enteredPin.length,
            hasError = errorMessage != null,
            shakeTrigger = shakeTrigger
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.height(20.dp),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        PinKeypad(
            onDigit = ::onDigit,
            onBackspace = ::onBackspace
        )

        if (onCancel != null && mode != PinLockMode.Unlock) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        } else {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private enum class PinStep {
    Enter,
    Create,
    Confirm,
    EnterCurrent,
    EnterNew,
    ConfirmNew
}

@Composable
private fun PinDots(
    filledCount: Int,
    hasError: Boolean,
    shakeTrigger: Int
) {
    val offsetX = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger == 0) return@LaunchedEffect
        listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f).forEach { target ->
            offsetX.animateTo(target, animationSpec = tween(40))
        }
    }

    Row(
        modifier = Modifier.graphicsLayer { translationX = offsetX.value },
        horizontalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        repeat(4) { index ->
            val filled = index < filledCount
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            hasError -> MaterialTheme.colorScheme.error
                            filled -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        }
                    )
            )
        }
    }
}

@Composable
private fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "back")
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(modifier = Modifier.size(72.dp))
                        "back" -> KeypadButton(onClick = onBackspace) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        else -> KeypadButton(onClick = { onDigit(key[0]) }) {
                            Text(
                                text = key,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
