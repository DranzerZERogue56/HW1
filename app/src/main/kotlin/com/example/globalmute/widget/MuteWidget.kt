package com.example.globalmute.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.example.globalmute.R
import com.example.globalmute.data.MuteRepository

class MuteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = MuteRepository(context)
        provideContent {
            val isMuted by repo.isMuted.collectAsState(initial = false)
            Content(isMuted = isMuted)
        }
    }

    @Composable
    private fun Content(isMuted: Boolean) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color(0xFF1A1A2E)))
                .clickable(actionRunCallback<ToggleMuteAction>()),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(
                    if (isMuted) R.drawable.ic_muted else R.drawable.ic_unmuted,
                ),
                contentDescription = if (isMuted) "Muted – tap to unmute" else "Unmuted – tap to mute",
                modifier = GlanceModifier.size(48.dp),
            )
        }
    }
}
