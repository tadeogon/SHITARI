package de.mm20.launcher2.ui.launcher.niagara

/**
 * Where the Niagara overlay was opened from. The launcher uses this to choose
 * an entry animation that matches the gesture instead of always sliding in
 * from the side.
 */
enum class NiagaraTrigger {
    Handle,
    SearchBar,
    SwipeDown,
}
