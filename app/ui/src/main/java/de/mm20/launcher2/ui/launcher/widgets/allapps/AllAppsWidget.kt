package de.mm20.launcher2.ui.launcher.widgets.allapps

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.ui.launcher.search.common.grid.SearchResultGrid
import de.mm20.launcher2.widgets.AllAppsWidget

@Composable
fun AllAppsWidget(widget: AllAppsWidget) {
    val viewModel: AllAppsWidgetVM = viewModel(key = "all-apps-widget-${widget.id}")
    val apps by viewModel.apps.collectAsState(emptyList())

    SearchResultGrid(
        items = apps,
        modifier = Modifier.padding(vertical = 4.dp),
    )
}
