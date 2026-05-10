package com.example.cityapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cityapp.presentation.incident.IncidentReportScreen
import com.example.cityapp.presentation.login.LoginScreen
import com.example.cityapp.presentation.route.RouteDashboardScreen
import com.example.cityapp.presentation.trip.ActiveTripScreen

object Destinations {
    const val Login = "login"
    const val Dashboard = "dashboard"
    const val ActiveTrip = "active_trip/{waybillId}"
    const val Incident = "incident/{waybillId}"

    fun activeTrip(waybillId: String) = "active_trip/$waybillId"
    fun incident(waybillId: String) = "incident/$waybillId"
}

@Composable
fun CityAppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Destinations.Login) {
        composable(Destinations.Login) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Destinations.Dashboard) {
                    popUpTo(Destinations.Login) { inclusive = true }
                }
            })
        }
        composable(Destinations.Dashboard) {
            RouteDashboardScreen(onStartTrip = { waybillId ->
                navController.navigate(Destinations.activeTrip(waybillId))
            })
        }
        composable(
            route = Destinations.ActiveTrip,
            arguments = listOf(navArgument("waybillId") { type = NavType.StringType })
        ) { entry ->
            val waybillId = entry.arguments?.getString("waybillId").orEmpty()
            ActiveTripScreen(
                waybillId = waybillId,
                onOpenIncident = { id -> navController.navigate(Destinations.incident(id)) },
                onTripCompleted = {
                    navController.navigate(Destinations.Dashboard) {
                        popUpTo(Destinations.Dashboard) { inclusive = true }
                    }
                }
            )
        }
        composable(
            route = Destinations.Incident,
            arguments = listOf(navArgument("waybillId") { type = NavType.StringType })
        ) { entry ->
            val waybillId = entry.arguments?.getString("waybillId").orEmpty()
            IncidentReportScreen(
                waybillId = waybillId,
                onSubmitted = { navController.popBackStack() }
            )
        }
    }
}
