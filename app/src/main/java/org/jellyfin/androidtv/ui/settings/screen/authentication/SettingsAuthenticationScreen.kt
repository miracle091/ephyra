package org.jellyfin.androidtv.ui.settings.screen.authentication

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.jellyfin.androidtv.R
import org.jellyfin.androidtv.auth.model.Server
import org.jellyfin.androidtv.auth.repository.ServerRepository
import org.jellyfin.androidtv.auth.repository.ServerUserRepository
import org.jellyfin.androidtv.auth.store.AuthenticationPreferences
import org.jellyfin.androidtv.preference.constant.UserSelectBehavior
import org.jellyfin.androidtv.ui.base.Icon
import org.jellyfin.androidtv.ui.base.Text
import org.jellyfin.androidtv.ui.base.form.Checkbox
import org.jellyfin.androidtv.ui.base.list.ListButton
import org.jellyfin.androidtv.ui.base.list.ListItemSlots
import org.jellyfin.androidtv.ui.base.list.ListSection
import org.jellyfin.androidtv.ui.navigation.LocalRouter
import org.jellyfin.androidtv.ui.navigation.focus.focusKey
import org.jellyfin.androidtv.ui.settings.Routes
import org.jellyfin.androidtv.ui.settings.compat.rememberPreference
import org.jellyfin.androidtv.ui.settings.composable.SettingsColumn
import org.jellyfin.sdk.model.serializer.toUUIDOrNull
import org.koin.compose.koinInject

@Composable
fun SettingsAuthenticationScreen(launchedFromLogin: Boolean = false) {
	val router = LocalRouter.current
	val serverRepository = koinInject<ServerRepository>()

	LaunchedEffect(serverRepository) { serverRepository.loadStoredServers() }

	val storedServers by serverRepository.storedServers.collectAsState()

	SettingsColumn {
		item {
			ListSection(
				overlineContent = {
					Text(stringResource(if (launchedFromLogin) R.string.app_name else R.string.settings).uppercase())
				},
				headingContent = { Text(stringResource(R.string.pref_login)) },
			)
		}

		item {
			AutoSignInButton(
				storedServers = storedServers,
				onClick = { router.push(Routes.AUTHENTICATION_AUTO_SIGN_IN) },
			)
		}

		item { SortAccountsButton(onClick = { router.push(Routes.AUTHENTICATION_SORT_BY) }) }

		if (storedServers.isNotEmpty()) {
			item { ListSection(headingContent = { Text(stringResource(R.string.lbl_manage_servers)) }) }

			items(storedServers) { server ->
				ManagedServerButton(
					server = server,
					onClick = {
						router.push(
							route = Routes.AUTHENTICATION_SERVER,
							parameters = mapOf(
								"serverId" to server.id.toString(),
							),
						)
					},
				)
			}
		}

		// Disallow changing the "always authenticate" option from the login screen
		// because that could allow a kid to disable the function to access a parent's account
		if (!launchedFromLogin) {
			item { ListSection(headingContent = { Text(stringResource(R.string.advanced_settings)) }) }

			item { AlwaysAuthenticateButton() }
		}

		if (launchedFromLogin) item {
			ListButton(
				slots = ListItemSlots(
					leadingContent = { Icon(painterResource(R.drawable.ic_jellyfin), contentDescription = null) },
				),
				headingContent = { Text(stringResource(R.string.pref_about_title)) },
				onClick = { router.push(Routes.ABOUT, mapOf("fromLogin" to "true")) },
				modifier = Modifier.focusKey(Routes.ABOUT)
			)
		}
	}
}

@Composable
private fun SortAccountsButton(onClick: () -> Unit) {
	val authenticationPreferences = koinInject<AuthenticationPreferences>()
	val sortBy by rememberPreference(authenticationPreferences, AuthenticationPreferences.sortBy)

	ListButton(
		headingContent = { Text(stringResource(R.string.sort_accounts_by)) },
		slots = ListItemSlots(
			captionContent = { Text(stringResource(sortBy.nameRes)) },
		),
		onClick = onClick,
		modifier = Modifier.focusKey(Routes.AUTHENTICATION_SORT_BY)
	)
}

@Composable
private fun ManagedServerButton(server: Server, onClick: () -> Unit) {
	ListButton(
		headingContent = { Text(server.name) },
		slots = ListItemSlots(
			leadingContent = { Icon(painterResource(R.drawable.ic_house), contentDescription = null) },
			captionContent = { Text(server.address) },
		),
		onClick = onClick,
		modifier = Modifier.focusKey("server_${server.id}")
	)
}

@Composable
private fun AlwaysAuthenticateButton() {
	val authenticationPreferences = koinInject<AuthenticationPreferences>()
	var alwaysAuthenticate by rememberPreference(authenticationPreferences, AuthenticationPreferences.alwaysAuthenticate)

	ListButton(
		headingContent = { Text(stringResource(R.string.always_authenticate)) },
		slots = ListItemSlots(
			trailingContent = { Checkbox(checked = alwaysAuthenticate) },
			captionContent = { Text(stringResource(R.string.always_authenticate_description)) },
		),
		onClick = { alwaysAuthenticate = !alwaysAuthenticate },
		modifier = Modifier.focusKey("always_authenticate")
	)
}

@Composable
private fun AutoSignInButton(storedServers: List<Server>, onClick: () -> Unit) {
	val serverUserRepository = koinInject<ServerUserRepository>()
	val authenticationPreferences = koinInject<AuthenticationPreferences>()
	val autoLoginUserBehavior by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginUserBehavior)
	val autoLoginServerId by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginServerId)
	val autoLoginUserId by rememberPreference(authenticationPreferences, AuthenticationPreferences.autoLoginUserId)
	val autoLoginServer = remember(storedServers, autoLoginServerId) {
		storedServers.find { server -> server.id == autoLoginServerId.toUUIDOrNull() }
	}
	val autoLoginUser = remember(autoLoginServer, autoLoginUserId) {
		val users = autoLoginServer?.let(serverUserRepository::getStoredServerUsers)
		users?.find { user -> user.id == autoLoginUserId.toUUIDOrNull() }
	}

	ListButton(
		headingContent = { Text(stringResource(R.string.auto_sign_in)) },
		slots = ListItemSlots(
			captionContent = {
				when (autoLoginUserBehavior) {
					UserSelectBehavior.DISABLED -> Text(stringResource(R.string.user_picker_disable_title))
					UserSelectBehavior.LAST_USER -> Text(stringResource(R.string.user_picker_last_user_title))
					UserSelectBehavior.SPECIFIC_USER -> Text(autoLoginUser?.name ?: stringResource(R.string.loading))
				}
			},
		),
		onClick = onClick,
		modifier = Modifier.focusKey(Routes.AUTHENTICATION_AUTO_SIGN_IN)
	)
}
