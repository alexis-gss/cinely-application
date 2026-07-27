package com.cinely.app.ui.detail.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cinely.app.ui.components.SectionTitle

@Composable
fun SynopsisSection(
    tagline: String?,
    overview: String?,
) {
    val cleanTagline = tagline?.takeIf { it.isNotBlank() }
    val cleanOverview = overview?.takeIf { it.isNotBlank() }

    if (cleanTagline == null && cleanOverview == null) return

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SectionTitle(title = "Synopsis")

        cleanTagline?.let {
            Text(
                text = "\"$it\"",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic
            )
        }

        cleanOverview?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Justify,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = if (cleanTagline != null) 8.dp else 0.dp)
            )
        }
    }
}
