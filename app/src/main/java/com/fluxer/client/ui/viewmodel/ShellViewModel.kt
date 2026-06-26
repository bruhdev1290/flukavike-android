package com.fluxer.client.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fluxer.client.data.model.Channel
import com.fluxer.client.data.remote.GatewayWebSocketManager
import com.fluxer.client.data.repository.AuthRepository
import com.fluxer.client.data.repository.ChatRepository
import com.fluxer.client.data.repository.NavigationRepository
import com.fluxer.client.navigation.FluxerRoute
import com.fluxer.client.navigation.RoutePaths
import com.fluxer.client.navigation.ShellBranch
import com.fluxer.client.navigation.branchForPath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShellUiState(
    val activePath: String = RoutePaths.Loading,
    val homePath: String = RoutePaths.Me,
    val notificationsPath: String = RoutePaths.Notifications,
    val youPath: String = RoutePaths.You,
    val activeBranch: ShellBranch = ShellBranch.Home,
    val preReconnectPath: String? = null,
    val pendingPath: String? = null,
    val restored: Boolean = false,
    val authenticated: Boolean = false,
    val gatewayReady: Boolean = false,
    val reconnecting: Boolean = false
)

@HiltViewModel
class ShellViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chatRepository: ChatRepository,
    private val navigationRepository: NavigationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(ShellUiState())
    val uiState: StateFlow<ShellUiState> = _uiState.asStateFlow()

    init {
        restoreNavigationState()
        observeAuth()
        observeGateway()
        observePendingRoutes()
    }

    fun navigate(route: FluxerRoute) {
        navigateToPath(route.path)
    }

    fun navigateToPath(path: String) {
        val targetPath = normalizePublicPlaceholder(path)
        val branch = branchForPath(targetPath)
        val shellPath = targetPath !in authTransientPaths
        _uiState.value = _uiState.value.copy(
            activePath = targetPath,
            activeBranch = branch,
            homePath = if (shellPath && branch == ShellBranch.Home) targetPath else _uiState.value.homePath,
            notificationsPath = if (shellPath && branch == ShellBranch.Notifications) targetPath else _uiState.value.notificationsPath,
            youPath = if (shellPath && branch == ShellBranch.You) targetPath else _uiState.value.youPath,
            pendingPath = null
        )
        persist()
    }

    fun navigateBranch(branch: ShellBranch) {
        val path = when (branch) {
            ShellBranch.Home -> _uiState.value.homePath
            ShellBranch.Notifications -> _uiState.value.notificationsPath
            ShellBranch.You -> _uiState.value.youPath
        }
        navigateToPath(path)
    }

    fun rememberGuildChannel(guildId: String, channelId: String) {
        viewModelScope.launch {
            navigationRepository.rememberLastGuildChannel(guildId, channelId)
        }
    }

    fun resolveGuildRoot(guildId: String, channels: List<Channel>) {
        viewModelScope.launch {
            navigate(navigationRepository.resolveGuildRoot(guildId, channels))
        }
    }

    fun consumeIntentRoute(route: FluxerRoute?) {
        if (route == null) return
        if (_uiState.value.authenticated) {
            navigate(route)
        } else {
            navigationRepository.setPendingRoute(route)
            _uiState.value = _uiState.value.copy(pendingPath = route.path)
            persist()
        }
    }

    fun onReconnectingScreenShown() {
        if (_uiState.value.preReconnectPath == null) {
            _uiState.value = _uiState.value.copy(preReconnectPath = _uiState.value.activePath)
        }
        _uiState.value = _uiState.value.copy(activePath = RoutePaths.Reconnecting)
        persist()
    }

    private fun restoreNavigationState() {
        viewModelScope.launch {
            val restored = navigationRepository.restoreState()
            val restoredHomePath = sanitizeHomePath(restored.homePath)
            val restoredActivePath = when {
                restored.activePath !in authTransientPaths -> restored.activePath
                authRepository.authState.value is AuthRepository.AuthState.Authenticated -> restoredHomePath
                authRepository.authState.value is AuthRepository.AuthState.Loading -> RoutePaths.Loading
                else -> RoutePaths.Login
            }
            _uiState.value = _uiState.value.copy(
                activePath = restoredActivePath,
                homePath = restoredHomePath,
                notificationsPath = sanitizeNotificationsPath(restored.notificationsPath),
                youPath = sanitizeYouPath(restored.youPath),
                activeBranch = branchForPath(restoredActivePath),
                preReconnectPath = restored.preReconnectPath?.takeUnless { it in authTransientPaths },
                pendingPath = restored.pendingPath?.takeUnless { it in authTransientPaths },
                restored = true
            )
        }
    }

    private fun observeAuth() {
        authRepository.authState
            .onEach { state ->
                val authenticated = state is AuthRepository.AuthState.Authenticated
                val current = _uiState.value
                val nextPath = when {
                    !current.restored -> current.activePath
                    authenticated && current.pendingPath != null -> current.pendingPath
                    authenticated && current.activePath in authTransientPaths -> sanitizeHomePath(current.homePath)
                    !authenticated && state !is AuthRepository.AuthState.Loading -> RoutePaths.Login
                    else -> current.activePath
                }
                _uiState.value = current.copy(
                    authenticated = authenticated,
                    activePath = nextPath,
                    activeBranch = branchForPath(nextPath),
                    pendingPath = if (authenticated) null else current.pendingPath
                )
                persist()
            }
            .launchIn(viewModelScope)
    }

    private fun observeGateway() {
        chatRepository.connectionState
            .onEach { state ->
                val isFailed = state is GatewayWebSocketManager.ConnectionState.Error
                val isConnected = state is GatewayWebSocketManager.ConnectionState.Connected
                val current = _uiState.value
                when {
                    current.authenticated && isFailed && current.activePath != RoutePaths.Reconnecting -> {
                        _uiState.value = current.copy(
                            activePath = RoutePaths.Reconnecting,
                            preReconnectPath = current.activePath,
                            reconnecting = true,
                            gatewayReady = false
                        )
                    }
                    current.authenticated && isConnected && current.activePath == RoutePaths.Reconnecting -> {
                        val restore = current.preReconnectPath ?: sanitizeHomePath(current.homePath)
                        _uiState.value = current.copy(
                            activePath = restore,
                            activeBranch = branchForPath(restore),
                            preReconnectPath = null,
                            reconnecting = false,
                            gatewayReady = true
                        )
                    }
                    else -> {
                        _uiState.value = current.copy(
                            gatewayReady = isConnected,
                            reconnecting = isFailed
                        )
                    }
                }
                persist()
            }
            .launchIn(viewModelScope)
    }

    private fun observePendingRoutes() {
        navigationRepository.pendingRoute
            .onEach { route ->
                if (route != null && _uiState.value.authenticated) {
                    navigate(route)
                    navigationRepository.setPendingRoute(null)
                }
            }
            .launchIn(viewModelScope)
    }

    private fun persist() {
        val state = _uiState.value
        viewModelScope.launch {
            navigationRepository.saveActivePath(
                activePath = state.activePath,
                homePath = state.homePath,
                notificationsPath = state.notificationsPath,
                youPath = state.youPath,
                preReconnectPath = state.preReconnectPath,
                pendingPath = state.pendingPath
            )
        }
    }

    private fun normalizePublicPlaceholder(path: String): String {
        return when {
            path.startsWith("/invite/") -> RoutePaths.Me
            path.startsWith("/gift/") -> RoutePaths.Me
            path.startsWith("/theme/") -> RoutePaths.Me
            else -> path
        }
    }

    private fun sanitizeHomePath(path: String): String =
        path.takeUnless { it in authTransientPaths } ?: RoutePaths.Me

    private fun sanitizeNotificationsPath(path: String): String =
        path.takeIf { it.startsWith(RoutePaths.Notifications) } ?: RoutePaths.Notifications

    private fun sanitizeYouPath(path: String): String =
        path.takeIf { it.startsWith(RoutePaths.You) } ?: RoutePaths.You

    companion object {
        private val authTransientPaths = setOf(
            RoutePaths.Login,
            RoutePaths.Loading,
            RoutePaths.Reconnecting
        )
    }
}
