package org.jellyfin.androidtv.ui.settings.screen.authentication

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.repository.AuthenticationRepository
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.model.User
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.base.ProfilePicture
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.button.IconButtonDefaults
import org.jellyfin.androidtv.ui.base.form.RadioButton
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.koin.compose.koinInject

@Composable
fun SettingsAuthenticationAutoSignInScreen() {
	val router = LocalRouter.current
	val serverRepository = koinInject<ServerRepository>()
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationPreferences = koinInject<AuthenticationPreferences>()
	var autoLoginUserBehavior by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginUserBehavior)
	var autoLoginServerId by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginServerId)
	var autoLoginUserId by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginUserId)

	LaunchedEffect(serverRepository) {
		serverRepository.loadStoredServers()
	}

	val storedServers by serverRepository.storedServers.collectAsState()

	SettingsColumn {
		item { AutoSignInHeader() }

		for (option in behaviorOptions) {
			item {
				AutoSignInOption(
					heading = option.heading,
					caption = option.caption,
					key = option.key,
					selected = autoLoginUserBehavior == option.behavior,
					onClick = {
						autoLoginUserBehavior = option.behavior
						router.back()
					},
				)
			}
		}

		for (server in storedServers) {
			item { ListSection(headingContent = { Text(server.name) }) }

			val users = serverUserRepository.getStoredServerUsers(server)
			val serverId = server.id.toString()
			items(users) { user ->
				val userId = user.id.toString()
				val selected = autoLoginUserBehavior == UserSelectBehavior.SPECIFIC_USER &&
					autoLoginServerId == serverId &&
					autoLoginUserId == userId

				AutoSignInUserButton(
					server = server,
					user = user,
					selected = selected,
					onClick = {
						autoLoginUserBehavior = UserSelectBehavior.SPECIFIC_USER
						autoLoginServerId = serverId
						autoLoginUserId = userId

						router.back()
					},
				)
			}
		}
	}
}

@Composable
private fun AutoSignInHeader() {
	ListSection(
		overlineContent = { Text(stringResource(R.string.pref_login).uppercase()) },
		headingContent = { Text(stringResource(R.string.auto_sign_in)) },
	)
}

/** An automatic sign in behavior that is not a specific user. */
private data class AutoSignInBehaviorOption(
	val behavior: UserSelectBehavior,
	@StringRes val heading: Int,
	@StringRes val caption: Int,
	val key: String,
)

private val behaviorOptions = listOf(
	AutoSignInBehaviorOption(
		behavior = UserSelectBehavior.DISABLED,
		heading = R.string.user_picker_disable_title,
		caption = R.string.user_picker_disable_summary,
		key = "disable",
	),
	AutoSignInBehaviorOption(
		behavior = UserSelectBehavior.LAST_USER,
		heading = R.string.user_picker_last_user_title,
		caption = R.string.user_picker_last_user_summary,
		key = "last_user",
	),
)

@Composable
private fun AutoSignInUserButton(
	server: Server,
	user: User,
	selected: Boolean,
	onClick: () -> Unit,
) {
	val authenticationRepository = koinInject<AuthenticationRepository>()

	ListButton(
		headingContent = { Text(user.name) },
		slots = ListItemSlots(
			leadingContent = {
				ProfilePicture(
					url = authenticationRepository.getUserImageUrl(server, user),
					modifier = Modifier
						.size(24.dp)
						.clip(IconButtonDefaults.Shape)
				)
			},
			trailingContent = { RadioButton(checked = selected) },
		),
		onClick = onClick,
		modifier = Modifier.focusKey(key = "user_${user.id}", initialFocus = selected)
	)
}

@Composable
private fun AutoSignInOption(
	@StringRes heading: Int,
	@StringRes caption: Int,
	key: String,
	selected: Boolean,
	onClick: () -> Unit,
) {
	ListButton(
		headingContent = { Text(stringResource(heading)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(caption)) },
			trailingContent = { RadioButton(checked = selected) },
		),
		onClick = onClick,
		modifier = Modifier.focusKey(key, initialFocus = selected)
	)
}
