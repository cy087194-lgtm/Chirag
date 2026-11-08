package com.Lia.assistant.ui.screens.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Lia.assistant.data.NovaPreferences
import com.Lia.assistant.ui.components.NovaButton
import com.Lia.assistant.ui.components.NovaButtonType
import com.Lia.assistant.ui.fx.LiaOrb3D
import com.Lia.assistant.ui.fx.NovaOrbStyle
import com.Lia.assistant.ui.theme.LocalNovaColors
import com.Lia.assistant.ui.theme.SerifFontFamily
import com.Lia.assistant.ui.theme.nightSky
import com.Lia.assistant.voice.NovaOrbState
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val description: String,
    val orbStyle: NovaOrbStyle,
    val orbState: NovaOrbState
)

@Composable
fun OnboardingScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colors = LocalNovaColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val pages = listOf(
        OnboardingPageData(
            title = "A Pearl of Light in the Night",
            description = "Welcome to Lia AI. Real-time, full-duplex spoken intelligence that listens, understands, and accompanies you.",
            orbStyle = NovaOrbStyle.NOVA,
            orbState = NovaOrbState.IDLE
        ),
        OnboardingPageData(
            title = "Speak Hindi, Hinglish or English",
            description = "Naturally switch between languages. Lia automatically matches your tone, language, and nuance turn by turn.",
            orbStyle = NovaOrbStyle.AURORA,
            orbState = NovaOrbState.LISTENING
        ),
        OnboardingPageData(
            title = "Your Phone, Hands-Free",
            description = "Open apps, make calls, and send WhatsApp messages seamlessly with real-time on-device voice assistance.",
            orbStyle = NovaOrbStyle.ENERGY,
            orbState = NovaOrbState.SPEAKING
        ),
        OnboardingPageData(
            title = "Website Forge 3D",
            description = "Say 'Lia, ek cafe ki website banao' and watch a complete animated 3D website come alive in the Forge Reactor.",
            orbStyle = NovaOrbStyle.ARCHER,
            orbState = NovaOrbState.CONNECTING
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })

    fun completeOnboarding() {
        NovaPreferences.putBoolean(context, NovaPreferences.Keys.HAS_ONBOARDED, true)
        navController.navigate("home") {
            popUpTo("onboarding") { inclusive = true }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .nightSky()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            NovaButton(
                text = "Skip",
                onClick = { completeOnboarding() },
                type = NovaButtonType.TEXT
            )
        }

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(190.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LiaOrb3D(
                        state = page.orbState,
                        size = 190.dp,
                        amplitude = if (page.orbState == NovaOrbState.SPEAKING) 0.5f else 0.1f,
                        style = page.orbStyle
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = page.title,
                    fontFamily = SerifFontFamily,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = page.description,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }

        // Bottom Navigation: Indicator Dots + Next / Get Started
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in pages.indices) {
                    val isSelected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 6.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) colors.accent else colors.surfaceBorder)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val isLastPage = pagerState.currentPage == pages.size - 1
            NovaButton(
                text = if (isLastPage) "Get Started" else "Next",
                onClick = {
                    if (isLastPage) {
                        completeOnboarding()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
