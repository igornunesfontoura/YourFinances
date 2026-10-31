package io.github.igornunesfontoura.yourfinances.ui.help

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AddCircle
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.igornunesfontoura.yourfinances.R
import io.github.igornunesfontoura.yourfinances.ui.components.PrimaryButton
import kotlinx.coroutines.launch

private class WelcomePage(val icon: ImageVector, @StringRes val title: Int, @StringRes val body: Int)

private val pages = listOf(
    WelcomePage(Icons.Outlined.Shield, R.string.welcome_1_title, R.string.welcome_1_body),
    WelcomePage(Icons.Outlined.AccountBalanceWallet, R.string.welcome_2_title, R.string.welcome_2_body),
    WelcomePage(Icons.Outlined.AddCircle, R.string.welcome_3_title, R.string.welcome_3_body),
    WelcomePage(Icons.Outlined.GridView, R.string.welcome_4_title, R.string.welcome_4_body),
    WelcomePage(Icons.Outlined.Autorenew, R.string.welcome_5_title, R.string.welcome_5_body),
)

/** Swipeable tour of what the app does and where things are; [onFinish] runs on skip or start. */
@Composable
fun WelcomeScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = pagerState.currentPage == pages.lastIndex
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().systemBarsPadding().padding(16.dp)) {
            Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.CenterEnd) {
                if (!last) TextButton(onClick = onFinish) { Text(stringResource(R.string.welcome_skip)) }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index -> Page(pages[index]) }
            PageDots(current = pagerState.currentPage, count = pages.size)
            Spacer(Modifier.height(24.dp))
            PrimaryButton(
                text = stringResource(if (last) R.string.welcome_start else R.string.welcome_next),
                onClick = {
                    if (last) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                enabled = true,
            )
        }
    }
}

@Composable
private fun Page(page: WelcomePage) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(120.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(page.icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(stringResource(page.title), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            stringResource(page.body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** The current page is a wider pill; read out as "Página 2 de 5". */
@Composable
private fun PageDots(current: Int, count: Int) {
    val description = stringResource(R.string.welcome_page_indicator, current + 1, count)
    Row(
        Modifier.fillMaxWidth().semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        repeat(count) { index ->
            val selected = index == current
            val width by animateDpAsState(if (selected) 24.dp else 8.dp, label = "dot width")
            val color by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                label = "dot color",
            )
            Box(Modifier.height(8.dp).width(width).background(color, CircleShape))
        }
    }
}
