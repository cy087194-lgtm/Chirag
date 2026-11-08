package com.Lia.assistant.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder

fun NavGraphBuilder.registerAccessibilityDisclosureRoute(navController: NavController) {
    // Play flavor does not register accessibility disclosure as it does not use AccessibilityService
}
