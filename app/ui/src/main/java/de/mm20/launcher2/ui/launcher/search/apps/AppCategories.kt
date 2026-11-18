package de.mm20.launcher2.ui.launcher.search.apps

// Versión personal: cajón de apps agrupado por categorías, con botones para filtrar.
//
// Categorías de cada app:
//  1. Si la app tiene etiquetas de Kvaesitso, sus etiquetas (puede estar en más de una).
//  2. Si no, la categoría que la app declara en Android (Juegos, Social, Música y audio...).
//  3. Si no declara ninguna, "Otras".

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.R
import java.text.Collator

class AppCategories(
    /** Apps que se están mostrando, en su orden original (alfabético). */
    private val apps: List<Application>,
    /** Categorías de cada app, por clave de app. */
    private val categoriesByKey: Map<String, List<String>>,
    /** Categorías presentes, ordenadas alfabéticamente y con "Otras" al final. */
    val categories: List<String>,
) {
    /** Apps de una categoría, o todas si [category] es null. */
    fun appsIn(category: String?): List<Application> {
        if (category == null) return apps
        return apps.filter { categoriesByKey[it.key]?.contains(category) == true }
    }

    companion object {
        fun from(
            context: Context,
            apps: List<Application>,
            tagsByKey: Map<String, List<String>>,
            otherLabel: String,
        ): AppCategories {
            val androidTitles = mutableMapOf<Int, String?>()
            val categoriesByKey = apps.associate { app ->
                val tags = tagsByKey[app.key]
                val categories = if (!tags.isNullOrEmpty()) {
                    tags
                } else {
                    val title = androidTitles.getOrPut(app.appCategory) {
                        if (app.appCategory == ApplicationInfo.CATEGORY_UNDEFINED) null
                        else ApplicationInfo.getCategoryTitle(context, app.appCategory)?.toString()
                    }
                    listOf(title ?: otherLabel)
                }
                app.key to categories
            }
            val collator = Collator.getInstance()
            val categories = categoriesByKey.values.flatten().distinct()
                .sortedWith { a, b ->
                    when {
                        a == otherLabel && b != otherLabel -> 1
                        b == otherLabel && a != otherLabel -> -1
                        else -> collator.compare(a, b)
                    }
                }
            return AppCategories(apps.toList(), categoriesByKey, categories)
        }
    }
}

@Composable
fun rememberAppCategories(
    apps: List<Application>,
    tagsByKey: Map<String, List<String>>,
): AppCategories {
    val context = LocalContext.current
    val otherLabel = stringResource(R.string.app_category_other)
    val currentTags by rememberUpdatedState(tagsByKey)
    // derivedStateOf vuelve a calcular cuando cambia la lista de apps o las etiquetas
    val state = remember(apps, otherLabel) {
        derivedStateOf { AppCategories.from(context, apps, currentTags, otherLabel) }
    }
    return state.value
}

/** Color oscuro para las líneas del cajón (bordes de botones y separador). */
@Composable
fun appCategoryLineColor(): Color = MaterialTheme.colorScheme.surfaceContainerHighest

@Composable
private fun darkChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = appCategoryLineColor(),
)

@Composable
fun AppCategoryChips(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selectedCategory == null,
            onClick = { onCategorySelected(null) },
            label = { Text(stringResource(R.string.app_category_all)) },
            border = darkChipBorder(selectedCategory == null),
        )
        for (category in categories) {
            FilterChip(
                selected = selectedCategory == category,
                onClick = {
                    onCategorySelected(if (selectedCategory == category) null else category)
                },
                label = { Text(category) },
                border = darkChipBorder(selectedCategory == category),
            )
        }
    }
}
