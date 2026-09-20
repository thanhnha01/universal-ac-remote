package com.thanhnha.universalacremote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun CapabilityRow(
    title: String,
    detail: String,
    icon: ImageVector,
    color: Color,
    status: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconBubble(
            icon = icon,
            tint = color,
            background = if (color == AppColors.mint) AppColors.paleMint else AppColors.paleBlue,
            size = 42,
        )
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                detail,
                color = AppColors.navySoft,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        StatusChip(
            status,
            Icons.Filled.CheckCircle,
            color,
            if (color == AppColors.mint) AppColors.paleMint else AppColors.paleBlue,
        )
    }
}

@Composable
fun SourceInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    valueColor: Color = AppColors.navySoft,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.navySoft,
            modifier = Modifier.size(24.dp),
        )
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = AppColors.navy,
        )
        Text(
            value,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun OutlinedField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
    )
}
