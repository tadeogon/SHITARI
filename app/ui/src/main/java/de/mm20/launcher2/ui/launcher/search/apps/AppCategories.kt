package de.mm20.launcher2.ui.launcher.search.apps

// Versión personal: cajón de apps agrupado por categorías, con un título por grupo.
//
// Cada app va a un solo grupo (así no se repite en el cajón):
//  1. Si tiene etiquetas de Kvaesitso, la primera alfabéticamente.
//  2. Si no, la categoría que declara en Android (Juegos, Social, Música y audio...).
//  3. Si no declara ninguna, "Otras".
//
// Los grupos se ordenan alfabéticamente y "Otras" queda al final. Dentro de cada
// grupo las apps se ordenan alfabéticamente. No se muestran grupos vacíos.

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.stringResource
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.R
import java.text.Collator

/** Un grupo del cajón: título visible y sus apps, ya ordenadas. */
data class AppGroup(
    val title: String,
    val apps: List<Application>,
)

class AppCategories(
    /** Grupos con apps, ordenados. Nunca hay grupos vacíos. */
    val groups: List<AppGroup>,
) {
    companion object {
        fun from(
            apps: List<Application>,
            tagsByKey: Map<String, List<String>>,
            categoryTitles: Map<Int, String>,
            otherLabel: String,
        ): AppCategories {
            val collator = Collator.getInstance()
            val tagComparator = Comparator<String> { a, b -> collator.compare(a, b) }
            val appsByTitle = linkedMapOf<String, MutableList<Application>>()

            for (app in apps.distinctBy { it.key }) {
                val tags = tagsByKey[app.key]
                    .orEmpty()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                val title = when {
                    tags.isNotEmpty() -> tags.minWithOrNull(tagComparator) ?: otherLabel
                    else -> categoryTitles[app.appCategory] ?: otherLabel
                }
                appsByTitle.getOrPut(title) { mutableListOf() }.add(app)
            }

            val groups = appsByTitle.map { (title, groupApps) ->
                AppGroup(
                    title = title,
                    apps = groupApps.sortedWith { a, b ->
                        collator.compare(a.labelOverride ?: a.label, b.labelOverride ?: b.label)
                    },
                )
            }.sortedWith { a, b ->
                when {
                    a.title == otherLabel && b.title != otherLabel -> 1
                    b.title == otherLabel && a.title != otherLabel -> -1
                    else -> collator.compare(a.title, b.title)
                }
            }

            return AppCategories(groups)
        }
    }
}

@Composable
fun rememberAppCategories(
    apps: List<Application>,
    tagsByKey: Map<String, List<String>>,
): AppCategories {
    val otherLabel = stringResource(R.string.app_category_other)
    val categoryTitles = mapOf(
        ApplicationInfo.CATEGORY_GAME to stringResource(R.string.app_category_game),
        ApplicationInfo.CATEGORY_AUDIO to stringResource(R.string.app_category_audio),
        ApplicationInfo.CATEGORY_VIDEO to stringResource(R.string.app_category_video),
        ApplicationInfo.CATEGORY_IMAGE to stringResource(R.string.app_category_image),
        ApplicationInfo.CATEGORY_SOCIAL to stringResource(R.string.app_category_social),
        ApplicationInfo.CATEGORY_NEWS to stringResource(R.string.app_category_news),
        ApplicationInfo.CATEGORY_MAPS to stringResource(R.string.app_category_maps),
        ApplicationInfo.CATEGORY_PRODUCTIVITY to stringResource(R.string.app_category_productivity),
        ApplicationInfo.CATEGORY_ACCESSIBILITY to stringResource(R.string.app_category_accessibility),
    )
    val currentTags by rememberUpdatedState(tagsByKey)
    val currentCategoryTitles by rememberUpdatedState(categoryTitles)
    // Se vuelve a calcular cuando cambian las apps, las etiquetas o los textos.
    val state = remember(apps, otherLabel) {
        derivedStateOf {
            AppCategories.from(apps, currentTags, currentCategoryTitles, otherLabel)
        }
    }
    return state.value
}
