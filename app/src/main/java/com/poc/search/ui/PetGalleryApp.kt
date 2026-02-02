package com.poc.search.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poc.search.MainViewModel
import com.poc.search.ui.screens.*

@Composable
fun PetGalleryApp(vm: MainViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val ui = vm.ui.collectAsState().value

    LaunchedEffect(ui.message) {
        val msg = ui.message
        if (!msg.isNullOrBlank()) {
            snackbar.showSnackbar(msg)
            vm.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbar) }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "gallery",
            modifier = Modifier.padding(padding)
        ) {
            composable("setup") {
                SetupScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() }
                )
            }
            composable("gallery") {
                GalleryScreen(
                    vm = vm,
                    onOpenSetup = { nav.navigate("setup") },
                    onOpenDetail = { localUri ->
                        val encoded = Uri.encode(localUri)
                        nav.navigate("detail/$encoded")
                    },
                    onOpenServerGallery = { nav.navigate("server_gallery") }
                )
            }
            composable("server_gallery") {
                ServerGalleryScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    onOpenDetail = { imageId ->
                        val encoded = Uri.encode(imageId)
                        nav.navigate("server_detail/$encoded")
                    }
                )
            }
            composable(
                route = "server_detail/{imageId}",
                arguments = listOf(navArgument("imageId") { type = NavType.StringType })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("imageId").orEmpty()
                val imageId = Uri.decode(encoded)
                ServerDetailScreen(
                    vm = vm,
                    imageId = imageId,
                    onBack = { nav.popBackStack() }
                )
            }
            composable(
                route = "detail/{localUri}",
                arguments = listOf(navArgument("localUri") { type = NavType.StringType })
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("localUri").orEmpty()
                val localUri = Uri.decode(encoded)
                DetailScreen(
                    vm = vm,
                    localUri = localUri,
                    onBack = { nav.popBackStack() }
                )
            }
        }
    }
}
