package de.mm20.launcher2.ui.launcher.search.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LeadingIconTab
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.launcher.search.common.grid.GridItem
import de.mm20.launcher2.ui.launcher.search.common.grid.GridResults
import de.mm20.launcher2.ui.launcher.search.common.list.ListItem
import de.mm20.launcher2.ui.launcher.search.common.list.ListResults
import de.mm20.launcher2.ui.layout.BottomReversed
import de.mm20.launcher2.ui.locals.LocalGridSettings

fun LazyListScope.AppResults(
    onProfileSelected: (Profile) -> Unit = {},
    profiles: List<Profile> = emptyList(),
    selectedProfile: Profile? = null,
    profileStates: Map<Profile.Type, Profile.State> = emptyMap(),
    showProfileLockControls: Boolean = false,
    onProfileLockChange: ((Profile, Boolean) -> Unit)? = null,
    apps: List<Application>,
    // Versión personal: grupos del cajón (null = lista plana, por ejemplo al buscar).
    groups: List<AppGroup>? = null,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    highlightedItem: Application? = null,
    columns: Int,
    reverse: Boolean,
    showList: Boolean,
) {

    val profileIndex by derivedStateOf {
        profiles.indexOf(selectedProfile).takeIf { it >= 0 } ?: 0
    }
    val selectedProfileType by derivedStateOf {
        selectedProfile?.type ?: Profile.Type.Personal
    }

    val isProfileLocked by derivedStateOf {
        profileStates[selectedProfileType]?.locked == true
    }

    val before = if (profiles.size > 1) {
         @Composable {
            Column(
                verticalArrangement = if (reverse) Arrangement.BottomReversed else Arrangement.Top,
            ) {
                if (profiles.size > 1) PrimaryScrollableTabRow(
                    selectedTabIndex = profileIndex,
                    containerColor = Color.Transparent,
                    edgePadding = 16.dp,
                    divider = {}
                ) {
                    for (profile in profiles) {
                        val selected = profile.type == selectedProfileType
                        LeadingIconTab(
                            selected = selected,
                            text = {
                                Text(
                                    when (profile.type) {
                                        Profile.Type.Personal -> stringResource(R.string.apps_profile_main)
                                        Profile.Type.Work -> stringResource(R.string.apps_profile_work)
                                        Profile.Type.Private -> stringResource(R.string.apps_profile_private)
                                    }
                                )
                            },
                            icon = {
                                when (profile.type) {
                                    Profile.Type.Personal -> Icon(
                                        painterResource(if (selected) R.drawable.person_24px_filled else R.drawable.person_24px),
                                        contentDescription = null
                                    )

                                    Profile.Type.Work -> Icon(
                                        painterResource(if (selected) R.drawable.enterprise_24px_filled else R.drawable.enterprise_24px),
                                        contentDescription = null
                                    )

                                    Profile.Type.Private -> Icon(
                                        painterResource(if (selected) R.drawable.encrypted_24px_filled else R.drawable.encrypted_24px),
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                onProfileSelected(profile)
                            }
                        )
                    }
                }

                if (!showList || isProfileLocked) {
                    HorizontalDivider()
                }

                if (selectedProfile != null && selectedProfile?.type != Profile.Type.Personal) {
                    if (isProfileLocked) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant,
                                    MaterialTheme.shapes.small
                                )
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainer,
                                    MaterialTheme.shapes.small
                                )
                                .padding(vertical = 64.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                painterResource(if (selectedProfileType == Profile.Type.Work) R.drawable.enterprise_off_48px else R.drawable.lock_48px),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.secondary,
                            )
                            Text(
                                stringResource(
                                    if (selectedProfileType == Profile.Type.Work) R.string.profile_work_profile_state_locked
                                    else R.string.profile_private_profile_state_locked
                                ),
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                style = MaterialTheme.typography.titleSmall,
                            )
                            if (showProfileLockControls) {
                                Button(
                                    modifier = Modifier.padding(top = 32.dp),
                                    onClick = {
                                        onProfileLockChange?.invoke(
                                            selectedProfile!!,
                                            false
                                        )
                                    },
                                    contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                                ) {
                                    Icon(
                                        painterResource(if (selectedProfileType == Profile.Type.Work) R.drawable.enterprise_20px else R.drawable.lock_open_20px),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .padding(end = ButtonDefaults.IconSpacing)
                                            .size(ButtonDefaults.IconSize)
                                    )
                                    Text(
                                        stringResource(
                                            if (selectedProfileType == Profile.Type.Work) R.string.profile_work_profile_action_unlock
                                            else R.string.profile_private_profile_action_unlock
                                        )
                                    )
                                }
                            }
                        }
                    } else if (showProfileLockControls) {
                        FilledTonalButton(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            onClick = {
                                onProfileLockChange?.invoke(
                                    selectedProfile,
                                    true
                                )
                            },
                            contentPadding = ButtonDefaults.TextButtonWithIconContentPadding,
                        ) {
                            Icon(
                                painterResource(if (selectedProfileType == Profile.Type.Work) R.drawable.enterprise_off_20px else R.drawable.lock_20px),
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(end = ButtonDefaults.IconSpacing)
                                    .size(ButtonDefaults.IconSize)
                            )
                            Text(
                                stringResource(
                                    if (selectedProfileType == Profile.Type.Work) R.string.profile_work_profile_action_lock
                                    else R.string.profile_private_profile_action_lock
                                )
                            )
                        }
                    }
                }
            }
        }
    } else null
    val locked = isProfileLocked
    val groupedApps = if (locked) emptyList() else groups.orEmpty()
    if (showList) {
        if (groupedApps.isNotEmpty()) {
            // Versión personal: la cabecera (perfiles, separador, candado) va una sola vez.
            ListResults(
                key = "apps-before",
                items = emptyList<Application>(),
                before = before?.let { { it() } },
                itemContent = { _, _, _ -> },
                reverse = reverse,
            )
            var offset = 0
            groupedApps.forEachIndexed { groupIndex, group ->
                item(
                    key = "apps-group-header-$groupIndex",
                    contentType = { "apps-group-header" },
                ) {
                    AppGroupHeader(title = group.title, reverse = reverse)
                }
                val groupOffset = offset
                val localSelectedIndex = selectedIndex - groupOffset
                ListResults(
                    key = "apps-group-$groupIndex",
                    items = group.apps,
                    selectedIndex = if (localSelectedIndex in group.apps.indices) localSelectedIndex else -1,
                    itemContent = { app, showDetails, index ->
                        ListItem(
                            modifier = Modifier
                                .fillMaxWidth(),
                            item = app,
                            showDetails = showDetails,
                            onShowDetails = { onSelect(if (it) index + groupOffset else -1) },
                            highlight = highlightedItem?.key == app.key
                        )
                    },
                    reverse = reverse,
                )
                offset += group.apps.size
            }
        } else {
            ListResults(
                key = "apps",
                items = if (locked) emptyList() else apps,
                before = before?.let { { it() } },
                selectedIndex = selectedIndex,
                itemContent = { app, showDetails, index ->
                    ListItem(
                        modifier = Modifier
                            .fillMaxWidth(),
                        item = app,
                        showDetails = showDetails,
                        onShowDetails = { onSelect(if (it) index else -1) },
                        highlight = highlightedItem?.key == app.key
                    )
                },
                reverse = reverse,
            )
        }
    } else {
        if (groupedApps.isNotEmpty()) {
            GridResults(
                key = "apps-before",
                items = emptyList<Application>(),
                before = before,
                itemContent = { },
                columns = columns,
                reverse = reverse,
            )
            groupedApps.forEachIndexed { groupIndex, group ->
                item(
                    key = "apps-group-header-$groupIndex",
                    contentType = { "apps-group-header" },
                ) {
                    AppGroupHeader(title = group.title, reverse = reverse)
                }
                GridResults(
                    key = "apps-group-$groupIndex",
                    items = group.apps,
                    itemContent = {
                        GridItem(
                            item = it,
                            showLabels = LocalGridSettings.current.showLabels,
                            highlight = it.key == highlightedItem?.key
                        )
                    },
                    reverse = reverse,
                    columns = columns,
                )
            }
        } else {
            GridResults(
                key = "apps",
                items = if (locked) emptyList() else apps,
                before = before,
                itemContent = {
                    GridItem(
                        item = it,
                        showLabels = LocalGridSettings.current.showLabels,
                        highlight = it.key == highlightedItem?.key
                    )
                },
                reverse = reverse,
                columns = columns,
            )
        }
    }

}

/** Título de cada grupo del cajón. */
@Composable
private fun AppGroupHeader(title: String, reverse: Boolean) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = if (reverse) 4.dp else 12.dp,
                bottom = if (reverse) 12.dp else 4.dp,
            ),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
