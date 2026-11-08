package com.Lia.assistant.ui.screens.features

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.registry.FeatureEntry
import com.Lia.assistant.registry.FeatureRegistry
import com.Lia.assistant.registry.FeatureStatus
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonType
import com.Lia.assistant.ui.components.NovaGlassCard
import com.Lia.assistant.ui.components.NovaTextField
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.nightSky
import java.util.Locale

@Composable
fun FeaturesScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedFeature by remember { mutableStateOf<FeatureEntry?>(null) }
    var isSpeaking by remember { mutableStateOf(false) }

    var tts by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val ttsInstance = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    val filterChips = listOf(
        "All", "Routines", "Gaming", "Photo", "Study", "Finance", "Daily", "System", "Automation", "AI Brain", "Needs Key / Partial"
    )

    val allList = FeatureRegistry.getAllFeatures()
    val filteredList = remember(searchQuery, selectedFilter) {
        var result = if (searchQuery.isNotBlank()) {
            FeatureRegistry.searchFeatures(searchQuery)
        } else {
            allList
        }

        if (selectedFilter != "All") {
            result = when (selectedFilter) {
                "Routines" -> result.filter { it.part.equals("PART Z", ignoreCase = true) }
                "Needs Key / Partial" -> result.filter {
                    it.status == FeatureStatus.NEEDS_API_KEY ||
                    it.status == FeatureStatus.PARTIAL ||
                    it.status == FeatureStatus.BLOCKED_BY_ANDROID
                }
                else -> result.filter { it.category.equals(selectedFilter, ignoreCase = true) }
            }
        }
        result
    }

    val workingCount = allList.count { it.status == FeatureStatus.WORKING }
    val needsKeyCount = allList.count { it.status == FeatureStatus.NEEDS_API_KEY }
    val partialCount = allList.count { it.status == FeatureStatus.PARTIAL }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .padding(bottom = 16.dp)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Feature Registry (PART U)",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "A-to-Z Catalog of Everything Added to LiaAI",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            IconButton(
                onClick = {
                    if (isSpeaking) {
                        tts?.stop()
                        isSpeaking = false
                    } else {
                        val speechText = FeatureRegistry.getSummaryVoiceText(selectedFilter)
                        tts?.speak(speechText, TextToSpeech.QUEUE_FLUSH, null, "feat_summary")
                        isSpeaking = true
                        Toast.makeText(context, "Features summary speak ho rahi hai. 'Ruko' ke liye click karein.", Toast.LENGTH_SHORT).show()
                    }
                }
            ) {
                Icon(
                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Read aloud",
                    tint = if (isSpeaking) Color(0xFFFF5252) else colors.accent
                )
            }
        }

        // Stats Banner
        NovaGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            elevation = 4.dp,
            padding = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatColumn(label = "Total Features", value = "${allList.size}", color = colors.accent)
                StatColumn(label = "Working", value = "$workingCount", color = Color(0xFF00E676))
                StatColumn(label = "Needs Key", value = "$needsKeyCount", color = Color(0xFFFFB300))
                StatColumn(label = "Partial", value = "$partialCount", color = Color(0xFF40C4FF))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            NovaTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Search features, commands, parts..."
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterChips.forEach { chip ->
                val isSelected = selectedFilter == chip
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = chip },
                    label = { Text(chip, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accentGlow,
                        selectedLabelColor = colors.accent,
                        containerColor = colors.surfaceGlass,
                        labelColor = colors.textSecondary
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Feature List
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredList, key = { it.id }) { item ->
                FeatureCardRow(item = item, onClick = { selectedFeature = item })
            }
        }
    }

    // Details Dialog
    selectedFeature?.let { feature ->
        AlertDialog(
            onDismissRequest = { selectedFeature = null },
            title = {
                Column {
                    Text(text = feature.name, color = colors.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "${feature.part} • ${feature.category}", color = colors.accent, fontSize = 12.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = feature.shortDescription, color = colors.textSecondary, fontSize = 13.sp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceGlass)
                            .padding(10.dp)
                    ) {
                        Column {
                            Text(text = "HOW TO ACTIVATE:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                            Text(text = feature.activation, fontSize = 12.sp, color = colors.textPrimary)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "HOW TO STOP:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                            Text(text = feature.stopping, fontSize = 12.sp, color = colors.textPrimary)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "STATUS: ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary)
                        StatusBadge(status = feature.status)
                    }

                    if (feature.statusDetails.isNotBlank()) {
                        Text(text = "Note: ${feature.statusDetails}", fontSize = 11.sp, color = colors.textTertiary)
                    }
                }
            },
            confirmButton = {
                NovaButton(
                    text = "Close",
                    onClick = { selectedFeature = null },
                    type = NovaButtonType.PRIMARY
                )
            },
            containerColor = colors.surfaceRaised
        )
    }
}

@Composable
private fun StatColumn(label: String, value: String, color: Color) {
    val colors = LocalNovaColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, fontSize = 10.sp, color = colors.textSecondary)
    }
}

@Composable
private fun FeatureCardRow(
    item: FeatureEntry,
    onClick: () -> Unit
) {
    val colors = LocalNovaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceGlass)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                StatusBadge(status = item.status)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.shortDescription,
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${item.part} • ${item.category}",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent
            )
        }
    }
}

@Composable
private fun StatusBadge(status: FeatureStatus) {
    val (color, text) = when (status) {
        FeatureStatus.WORKING -> Color(0xFF00E676) to "WORKING"
        FeatureStatus.NEEDS_API_KEY -> Color(0xFFFFB300) to "NEEDS KEY"
        FeatureStatus.PARTIAL -> Color(0xFF40C4FF) to "PARTIAL"
        FeatureStatus.BLOCKED_BY_ANDROID -> Color(0xFFFF5252) to "BLOCKED"
        FeatureStatus.UI_ONLY -> Color(0xFFB0BEC5) to "UI ONLY"
        FeatureStatus.BROKEN -> Color(0xFFFF1744) to "BROKEN"
        FeatureStatus.NOT_VERIFIED -> Color(0xFFAB47BC) to "UNVERIFIED"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}
