package net.chasmine.oneline.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
        AlertType.SUCCESS -> Icons.Default.CheckCircle
        AlertType.ERROR -> Icons.Default.Error
        AlertType.WARNING -> Icons.Default.Warning
        AlertType.INFO -> Icons.Default.Info
    }

    val iconTint = when (alertType) {
        AlertType.SUCCESS -> Color(0xFF4CAF50)
        AlertType.ERROR -> MaterialTheme.colorScheme.error
        AlertType.WARNING -> Color(0xFFFF9800)
        AlertType.INFO -> MaterialTheme.colorScheme.primary
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
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
                style = MaterialTheme.typography.headlineSmall
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

@Composable
fun InfoCard(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Info,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
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
        icon = Icons.Default.CheckCircle,
        containerColor = Color(0xFFE8F5E9),
        contentColor = Color(0xFF2E7D32)
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
        icon = Icons.Default.Error,
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
        icon = Icons.Default.Warning,
        containerColor = Color(0xFFFFF3E0),
        contentColor = Color(0xFFE65100)
    )
}
