package de.mm20.launcher2.ui.launcher.widgets.allapps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.mm20.launcher2.search.SearchService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AllAppsWidgetVM : ViewModel(), KoinComponent {

    private val searchService: SearchService by inject()

    val apps = searchService.getAllApps()
        .map { it.standardProfileApps }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())
}
