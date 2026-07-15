package com.networth.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.networth.tracker.data.AssetCategory
import com.networth.tracker.ui.theme.LiabilityColor

@Composable
fun CategoryIcon(category: AssetCategory, size: Int = 40) {
    val icon = categoryIcon(category)
    val bgColor = if (category.isLiability) {
        LiabilityColor.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    }
    val tint = if (category.isLiability) LiabilityColor else MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = category.displayName,
            tint = tint,
            modifier = Modifier.size((size * 0.55f).dp)
        )
    }
}

private fun categoryIcon(category: AssetCategory): ImageVector = when (category) {
    AssetCategory.US_STOCKS -> Icons.AutoMirrored.Filled.ShowChart
    AssetCategory.MUTUAL_FUNDS -> Icons.Default.PieChart
    AssetCategory.INDIAN_STOCKS -> Icons.AutoMirrored.Filled.TrendingUp
    AssetCategory.EPF -> Icons.Default.AccountBalance
    AssetCategory.NPS -> Icons.Default.Savings
    AssetCategory.GOLD -> Icons.Default.Diamond
    AssetCategory.REAL_ESTATE -> Icons.Default.Home
    AssetCategory.HOME_LOAN -> Icons.Default.Home
    AssetCategory.VEHICLE_LOAN -> Icons.Default.DirectionsCar
    AssetCategory.PERSONAL_LOAN -> Icons.Default.Person
}
