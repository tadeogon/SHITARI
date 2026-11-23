package de.mm20.launcher2.widgets

import android.content.Context
import de.mm20.launcher2.database.entities.PartialWidgetEntity
import java.util.UUID

data class AllAppsWidget(
    override val id: UUID,
) : Widget() {

    override fun toDatabaseEntity(): PartialWidgetEntity {
        return PartialWidgetEntity(
            id = id,
            type = Type,
            config = null,
        )
    }

    override fun getLabel(context: Context): String {
        return context.getString(R.string.widget_name_apps)
    }

    companion object {
        const val Type = "all_apps"
    }
}
