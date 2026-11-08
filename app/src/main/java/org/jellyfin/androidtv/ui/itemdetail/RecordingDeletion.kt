package org.jellyfin.androidtv.ui.itemdetail

import org.jellyfin.sdk.model.api.BaseItemDto
import org.jellyfin.sdk.model.api.BaseItemKind
import java.util.UUID

/**
 * Ephyra: only live TV recordings can be deleted from the TV, never films, episodes or other library media. An item is a recording
 * when the server says so or when it sits in one of the server's recording folders; the server must also allow the deletion.
 */
fun canDeleteFromTv(item: BaseItemDto, ancestorIds: Collection<UUID>, recordingFolderIds: Collection<UUID>): Boolean {
	if (item.canDelete != true) return false
	return item.type == BaseItemKind.RECORDING || ancestorIds.any { it in recordingFolderIds }
}
