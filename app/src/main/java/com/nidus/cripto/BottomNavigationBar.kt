package com.nidus.cripto.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GoldAccent = Color(0xFFFFD700)
private val DarkBackground = Color(0xFF1A1A1A)

enum class BottomTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("Inicio", Icons.Default.Home),
    SEND("Enviar", Icons.Default.Send),
    MULTI_SEND("Multi-pago", Icons.Default.Payments),
    HISTORY("Historial", Icons.Default.History),
    OPTIONS("Opciones", Icons.Default.Settings)
}

@Composable
fun NidusBottomNavigationBar(
    currentTab: Screen,
    onTabSelected: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = DarkBackground,
        tonalElevation = 8.dp
    ) {
        BottomTab.entries.forEach { tab ->
            val isSelected = tab.ordinal == currentTab.ordinal

            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(Screen.entries[tab.ordinal]) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 10.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = GoldAccent,
                    indicatorColor = GoldAccent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}