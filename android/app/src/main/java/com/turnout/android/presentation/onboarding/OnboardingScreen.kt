package com.turnout.android.presentation.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.turnout.android.core.components.PulseLine
import com.turnout.android.core.components.TurnoutButton
import com.turnout.android.core.theme.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

private data class OnboardingPage(val title: String, val body: String)

private val pages = listOf(
    OnboardingPage(
        title = "Create your first event",
        body = "Set up an event in under 2 minutes — title, date, location, capacity."
    ),
    OnboardingPage(
        title = "Import your guest list",
        body = "Upload a CSV with full_name and email columns. Turnout handles the rest — tokens, links, everything."
    ),
    OnboardingPage(
        title = "Watch RSVPs arrive in real time",
        body = "Send invitations with one tap. Your dashboard updates live as guests respond."
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is OnboardingEvent.SkipToDashboard -> onComplete()
                is OnboardingEvent.NavigateToDashboard -> onComplete()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Canvas)) {
        Column(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                OnboardingPageContent(pageIndex = pageIndex, page = pages[pageIndex])
            }

            PageIndicatorDots(
                pageCount = pages.size,
                currentPage = pagerState.currentPage,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { viewModel.completeOnboarding() }) {
                    Text("Skip", color = TextOnCanvasSecondary)
                }

                val isLastPage = pagerState.currentPage == pages.lastIndex
                TurnoutButton(
                    text = if (isLastPage) "Get Started" else "Next",
                    onClick = {
                        if (isLastPage) {
                            viewModel.completeOnboarding()
                        } else {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PageIndicatorDots(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            val width by animateDpAsState(targetValue = if (isActive) 20.dp else 8.dp, label = "dot_width")

            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width)
                    .clip(if (isActive) RoundedCornerShape(4.dp) else CircleShape)
                    .background(if (isActive) AccentBlue else TextOnCanvasSecondary)
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(pageIndex: Int, page: OnboardingPage) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
            when (pageIndex) {
                0 -> CalendarSparkIllustration()
                1 -> CsvUploadIllustration()
                else -> PhonePulseIllustration()
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge.copy(fontFamily = SpaceGroteskFontFamily),
            color = TextOnCanvas,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = page.body,
            style = MaterialTheme.typography.bodyLarge,
            color = TextOnCanvasSecondary,
            textAlign = TextAlign.Center
        )
    }
}

/** Page 1 — a simple calendar grid with a spark/star accent, drawn entirely with Canvas. */
@Composable
private fun CalendarSparkIllustration() {
    Canvas(modifier = Modifier.size(140.dp)) {
        val cardWidth = size.width * 0.7f
        val cardHeight = size.height * 0.6f
        val left = (size.width - cardWidth) / 2f
        val top = (size.height - cardHeight) / 2f

        drawRoundRect(
            color = TextOnCanvasSecondary,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(cardWidth, cardHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
            style = Stroke(width = 3f)
        )
        // Header bar
        drawLine(
            color = AccentBlue,
            start = Offset(left, top + cardHeight * 0.25f),
            end = Offset(left + cardWidth, top + cardHeight * 0.25f),
            strokeWidth = 3f
        )
        // Spark/star accent — simple 4-point star made of two crossed lines
        val starCenter = Offset(left + cardWidth * 0.75f, top - 8f)
        drawLine(color = AccentBlue, start = starCenter.copy(y = starCenter.y - 10f), end = starCenter.copy(y = starCenter.y + 10f))
        drawLine(color = AccentBlue, start = starCenter.copy(x = starCenter.x - 10f), end = starCenter.copy(x = starCenter.x + 10f))
    }
}

/** Page 2 — a simple document outline with a folded corner and an upward arrow, SignalGreen. */
@Composable
private fun CsvUploadIllustration() {
    Canvas(modifier = Modifier.size(140.dp)) {
        val docWidth = size.width * 0.5f
        val docHeight = size.height * 0.65f
        val left = (size.width - docWidth) / 2f
        val top = (size.height - docHeight) / 2f + 10f

        drawRoundRect(
            color = SignalGreen,
            topLeft = Offset(left, top),
            size = androidx.compose.ui.geometry.Size(docWidth, docHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f),
            style = Stroke(width = 3f)
        )
        // Upload arrow above the document
        val arrowX = left + docWidth / 2f
        drawLine(SignalGreen, Offset(arrowX, top - 30f), Offset(arrowX, top - 5f), strokeWidth = 3f)
        drawLine(SignalGreen, Offset(arrowX - 8f, top - 22f), Offset(arrowX, top - 30f), strokeWidth = 3f)
        drawLine(SignalGreen, Offset(arrowX + 8f, top - 22f), Offset(arrowX, top - 30f), strokeWidth = 3f)
    }
}

/** Page 3 — a phone outline with an animated PulseLine wave across its "screen". */
@Composable
private fun PhonePulseIllustration() {
    Canvas(modifier = Modifier.size(100.dp, 140.dp)) {
        drawRoundRect(
            color = AccentBlue,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
            style = Stroke(width = 3f)
        )
    }
    Spacer(Modifier.height(8.dp))
    PulseLine(isActive = true, speedMultiplier = 1f)
}
