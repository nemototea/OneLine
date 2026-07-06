package net.chasmine.oneline.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class AlertType {
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

@Composable
fun MaterialAlertDialog(
    onDismissRequest: () -> Unit,
    title: String,
    message: String,
    alertType: AlertType = AlertType.INFO,
    confirmText: String = "OK",
    onConfirmClick: () -> Unit = onDismissRequest,
    dismissText: String? = null,
    onDismissClick: (() -> Unit)? = null
) {
    val icon = when (alertType) {
        AlertType.SUCCESS -> Icons.Outlined.CheckCircle
        AlertType.ERROR -> Icons.Outlined.ErrorOutline
        AlertType.WARNING -> Icons.Outlined.WarningAmber
        AlertType.INFO -> Icons.Outlined.Info
    }

    // 種別ごとの色はトークンに固定（新しい色を作らない）
    val iconTint = when (alertType) {
        AlertType.SUCCESS -> MaterialTheme.colorScheme.tertiary   // 抹茶
        AlertType.ERROR -> MaterialTheme.colorScheme.error
        AlertType.WARNING -> MaterialTheme.colorScheme.error
        AlertType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(confirmText)
            }
        },
        dismissButton = if (dismissText != null && onDismissClick != null) {
            {
                TextButton(onClick = onDismissClick) {
                    Text(dismissText)
                }
            }
        } else null
    )
}

/**
 * メッセージカード（DESIGN.md: カード地の使い分け / セマンティックの意味づけ）
 *
 * 地は中立の濃い和紙（surfaceVariant）。種別はアイコンの色だけで静かに伝え、
 * 地全体を塗らない。影は使わず、必要なら折り目線で締める。
 */
@Composable
fun InfoCard(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SuccessCard(
    message: String,
    modifier: Modifier = Modifier
) {
    InfoCard(
        message = message,
        modifier = modifier,
        icon = Icons.Outlined.CheckCircle,
        iconTint = MaterialTheme.colorScheme.tertiary   // 抹茶
    )
}

@Composable
fun ErrorCard(
    message: String,
    modifier: Modifier = Modifier
) {
    InfoCard(
        message = message,
        modifier = modifier,
        icon = Icons.Outlined.ErrorOutline,
        iconTint = MaterialTheme.colorScheme.error,
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    )
}

@Composable
fun WarningCard(
    message: String,
    modifier: Modifier = Modifier
) {
    InfoCard(
        message = message,
        modifier = modifier,
        icon = Icons.Outlined.WarningAmber,
        iconTint = MaterialTheme.colorScheme.error   // 新しい橙色は作らない
    )
}
