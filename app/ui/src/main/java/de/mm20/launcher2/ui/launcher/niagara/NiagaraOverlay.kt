package de.mm20.launcher2.ui.launcher.niagara

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.mm20.launcher2.profiles.Profile
import de.mm20.launcher2.search.Application
import de.mm20.launcher2.ui.R
import de.mm20.launcher2.ui.component.ShapedLauncherIcon
import de.mm20.launcher2.ui.locals.LocalDarkTheme
import de.mm20.launcher2.ui.launcher.search.SearchVM
import de.mm20.launcher2.ui.launcher.search.common.SearchableItemVM
import de.mm20.launcher2.ui.launcher.search.listItemViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.Collator
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private sealed interface NiagaraEntry {
    data class Header(val letter: String) : NiagaraEntry
    data class App(val app: Application) : NiagaraEntry
}

private data class NiagaraSection(
    val letter: String,
    val apps: List<Application>,
)

private fun sectionLetter(label: String): String {
    val normalized = Normalizer.normalize(label, Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .trim()
    val first = normalized.firstOrNull()?.uppercaseChar()
    return if (first != null && first in 'A'..'Z') first.toString() else "#"
}

@Composable
fun NiagaraOverlay(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SearchVM = viewModel()
    val context = LocalContext.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }

    var query by rememberSaveable { mutableStateOf("") }
    var selectedProfileIndex by rememberSaveable { mutableIntStateOf(-1) }

    val apps = viewModel.appResults
    val workApps = viewModel.workAppResults
    val privateApps = viewModel.privateSpaceAppResults
    val profiles by viewModel.profiles.collectAsState(emptyList())
    val profileStates by viewModel.profileStates.collectAsState(emptyMap())

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        viewModel.search("", forceRestart = true)
    }

    val visibleProfiles = remember(profiles, profileStates) {
        profiles.filter { profileStates[it.type]?.hidden == false }
    }
    val selectedProfile = visibleProfiles.getOrNull(selectedProfileIndex)
        ?: visibleProfiles.firstOrNull()
    val selectedType = selectedProfile?.type
    val locked = selectedType?.let { profileStates[it]?.locked } == true

    val sourceApps: List<Application> = when {
        visibleProfiles.size > 1 && selectedType == Profile.Type.Work -> workApps
        visibleProfiles.size > 1 && selectedType == Profile.Type.Private -> privateApps
        else -> apps
    }

    val sortedApps by remember(query, sourceApps) {
        derivedStateOf {
            val collator = Collator.getInstance(Locale.getDefault())
            val normalizedQuery = query.trim()
            sourceApps
                .distinctBy { it.key }
                .filter {
                    normalizedQuery.isEmpty() ||
                            (it.labelOverride ?: it.label).contains(normalizedQuery, ignoreCase = true)
                }
                .sortedWith(
                    compareBy(collator) { it.labelOverride ?: it.label }
                )
        }
    }

    val sections = remember(sortedApps) {
        sortedApps
            .groupBy { sectionLetter(it.labelOverride ?: it.label) }
            .entries
            .sortedWith(
                compareBy<Map.Entry<String, List<Application>>> { if (it.key == "#") 1 else 0 }
                    .thenBy { it.key }
            )
            .map { NiagaraSection(it.key, it.value) }
    }

    val entries = remember(sections) {
        buildList {
            sections.forEach { section ->
                add(NiagaraEntry.Header(section.letter))
                section.apps.forEach { add(NiagaraEntry.App(it)) }
            }
        }
    }

    val displayEntries = remember(entries, query) {
        if (query.isBlank()) entries
        else sortedApps.map { NiagaraEntry.App(it) }
    }

    val sectionStart = remember(displayEntries) {
        buildMap {
            displayEntries.forEachIndexed { index, entry ->
                if (entry is NiagaraEntry.Header) {
                    put(entry.letter, index)
                }
            }
        }
    }

    val currentLetter by remember(displayEntries) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            displayEntries
                .take((index + 1).coerceAtMost(displayEntries.size))
                .lastOrNull { it is NiagaraEntry.Header }
                ?.let { (it as NiagaraEntry.Header).letter }
        }
    }

    LaunchedEffect(selectedType) {
        if (displayEntries.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    BackHandler {
        if (query.isNotEmpty()) {
            query = ""
            focusManager.clearFocus()
        } else {
            onClose()
        }
    }

    val time by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(1000)
        }
    }
    val dateText = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
            .replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
    }
    val darkTheme = LocalDarkTheme.current
    val view = LocalView.current

    DisposableEffect(view, darkTheme) {
        val activity = view.context.findActivity()
        val controller = activity?.let {
            WindowInsetsControllerCompat(it.window, view)
        }
        val previousStatusIcons = controller?.isAppearanceLightStatusBars
        val previousNavIcons = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = !darkTheme
        controller?.isAppearanceLightNavigationBars = !darkTheme
        onDispose {
            if (previousStatusIcons != null) {
                controller?.isAppearanceLightStatusBars = previousStatusIcons
            }
            if (previousNavIcons != null) {
                controller?.isAppearanceLightNavigationBars = previousNavIcons
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { total = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        total += dragAmount
                        if (total > 96.dp.toPx()) {
                            total = 0f
                            onClose()
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = dateText.uppercase(Locale.getDefault()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.4.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        fontSize = 64.sp,
                        lineHeight = 68.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-2).sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.close_24px),
                        contentDescription = stringResource(R.string.niagara_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .clickable { focusRequester.requestFocus() }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(R.drawable.search_24px),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = stringResource(R.string.search_bar_placeholder),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 17.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        ),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            query = ""
                            focusRequester.requestFocus()
                        },
                        modifier = Modifier.size(36.dp),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.close_24px),
                            contentDescription = stringResource(R.string.action_clear),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }

            if (visibleProfiles.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    visibleProfiles.forEachIndexed { index, profile ->
                        val label = when (profile.type) {
                            Profile.Type.Work -> stringResource(R.string.apps_profile_work)
                            Profile.Type.Private -> stringResource(R.string.apps_profile_private)
                            else -> stringResource(R.string.apps_profile_main)
                        }
                        val selected = profile.type == selectedType
                        Column(
                            modifier = Modifier.clickable {
                                selectedProfileIndex = index
                                query = ""
                                focusManager.clearFocus()
                            }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) MaterialTheme.colorScheme.onBackground
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 18.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(1.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.onBackground
                                        else androidx.compose.ui.graphics.Color.Transparent
                                    )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (locked) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.lock_24px),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(
                                if (selectedType == Profile.Type.Private) {
                                    R.string.profile_private_profile_state_locked
                                } else {
                                    R.string.profile_work_profile_state_locked
                                }
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (displayEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.niagara_no_apps),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 4.dp,
                            top = 4.dp,
                            end = 64.dp,
                            bottom = 32.dp,
                        ),
                    ) {
                        items(
                            items = displayEntries,
                            key = {
                                when (it) {
                                    is NiagaraEntry.Header -> "niagara-header-${it.letter}"
                                    is NiagaraEntry.App -> "niagara-app-${it.app.key}"
                                }
                            }
                        ) { entry ->
                            when (entry) {
                                is NiagaraEntry.Header -> {
                                    Text(
                                        text = entry.letter,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 1.4.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(
                                            start = 8.dp,
                                            top = if (entry.letter == displayEntries.firstOrNull()
                                                    ?.let { (it as? NiagaraEntry.Header)?.letter }) 2.dp else 16.dp,
                                            bottom = 4.dp,
                                        ),
                                    )
                                }
                                is NiagaraEntry.App -> {
                                    NiagaraAppRow(
                                        app = entry.app,
                                        onClick = {
                                            if (it) onClose()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (query.isBlank() && sectionStart.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 24.dp)
                                .width(32.dp)
                                .fillMaxHeight()
                                .padding(vertical = 8.dp)
                                .pointerInput(sectionStart) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        var lastTarget = -1

                                        fun jumpTo(y: Float) {
                                            val index = ((y / size.height) * 26)
                                                .toInt()
                                                .coerceIn(0, 25)
                                            val key = ('A' + index).toString()
                                            val target = sectionStart[key] ?: return
                                            if (target != lastTarget) {
                                                lastTarget = target
                                                scope.launch { listState.scrollToItem(target) }
                                            }
                                        }

                                        jumpTo(down.position.y)
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes
                                                .firstOrNull { it.id == down.id } ?: break
                                            if (!change.pressed) break
                                            jumpTo(change.position.y)
                                            change.consume()
                                        }
                                    }
                                },
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            for (letter in 'A'..'Z') {
                                val key = letter.toString()
                                val available = sectionStart.containsKey(key)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 10.sp,
                                        lineHeight = 10.sp,
                                        fontWeight = if (currentLetter == key) FontWeight.Bold
                                        else FontWeight.Normal,
                                        color = if (currentLetter == key) {
                                            MaterialTheme.colorScheme.onBackground
                                        } else if (available) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NiagaraAppRow(
    app: Application,
    onClick: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val iconSizePx = with(density) { 40.dp.roundToPx() }
    val viewModel: SearchableItemVM = listItemViewModel(key = "niagara-${app.key}")

    LaunchedEffect(app, iconSizePx) {
        viewModel.init(app, iconSizePx)
    }

    val icon by viewModel.icon.collectAsStateWithLifecycle()
    val badge by viewModel.badge.collectAsStateWithLifecycle(null)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable {
                onClick(viewModel.launch(context))
            }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShapedLauncherIcon(
            size = 40.dp,
            icon = { icon },
            badge = { badge },
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = app.labelOverride ?: app.label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun NiagaraEdgeHandle(
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val threshold = with(density) { 28.dp.toPx() }
    val description = stringResource(R.string.niagara_open)
    val view = LocalView.current

    DisposableEffect(view) {
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                view.systemGestureExclusionRects = emptyList()
            }
        }
    }

    Box(
        modifier = modifier
            .width(30.dp)
            .height(180.dp)
            .onGloballyPositioned { coordinates ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val bounds = coordinates.boundsInRoot()
                    view.systemGestureExclusionRects = listOf(
                        android.graphics.Rect(
                            bounds.left.roundToInt(),
                            bounds.top.roundToInt(),
                            bounds.right.roundToInt(),
                            bounds.bottom.roundToInt()
                        )
                    )
                }
            }
            .semantics { contentDescription = description }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    var total = 0f
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        val dx = change.positionChange().x
                        total += dx
                        if (abs(total) > viewConfiguration.touchSlop) {
                            moved = true
                        }
                        if (moved) {
                            change.consume()
                        }
                        if (total < -threshold) {
                            onOpen()
                            break
                        }
                    }
                    if (!moved) {
                        onOpen()
                    }
                }
            },
        contentAlignment = Alignment.CenterEnd,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.34f))
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
