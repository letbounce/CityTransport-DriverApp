package com.example.cityapp.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cityapp.presentation.home.HomeMenuScreen
import com.example.cityapp.presentation.incident.IncidentEditScreen
import com.example.cityapp.presentation.incident.IncidentReportScreen
import com.example.cityapp.presentation.incidents.IncidentsListScreen
import com.example.cityapp.presentation.login.LoginScreen
import com.example.cityapp.presentation.map.TripMapHubScreen
import com.example.cityapp.presentation.map.TripMapRouteScreen
import com.example.cityapp.presentation.route.RouteDashboardScreen
import com.example.cityapp.presentation.trip.ActiveTripScreen

object Destinations {
    const val Login = "login"
    const val Home = "home"
    const val Dashboard = "dashboard"
    const val ActiveTrip = "active_trip/{waybillId}"
    const val Incident = "incident/{waybillId}"
    const val IncidentsList = "incidents"
    const val IncidentEdit = "incident_edit/{incidentId}"
    const val TripMap = "trip_map"
    const val TripMapRoute = "trip_map_route/{routeId}"

    fun activeTrip(waybillId: String) = "active_trip/$waybillId"
    fun tripMapRoute(routeId: String) = "trip_map_route/$routeId"
    fun incident(waybillId: String) = "incident/$waybillId"
    fun incidentEdit(incidentId: String) = "incident_edit/$incidentId"
}

@Composable
fun CityAppNavGraph() {
    val navController = rememberNavController()

    fun navigateHome() {
        navController.navigate(Destinations.Home) {
            launchSingleTop = true
            popUpTo(Destinations.Home) { inclusive = false }
        }
    }

    NavHost(navController = navController, startDestination = Destinations.Login) {
        composable(Destinations.Login) {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Destinations.Home) {
                    popUpTo(Destinations.Login) { inclusive = true }
                }
            })
        }
        composable(Destinations.Home) {
            HomeMenuScreen(
                onOpenWaybills = { navController.navigate(Destinations.Dashboard) },
                onOpenIncidents = { navController.navigate(Destinations.IncidentsList) },
                onOpenTripMap = { navController.navigate(Destinations.TripMap) },
                onLoggedOut = {
                    navController.navigate(Destinations.Login) {
                        popUpTo(Destinations.Home) { inclusive = true }
                    }
                }
            )
        }
        composable(Destinations.TripMap) {
            TripMapHubScreen(
                onBack = { navController.popBackStack() },
                onNavigateHome = { navigateHome() },
                onOpenRouteMap = { id -> navController.navigate(Destinations.tripMapRoute(id)) }
            )
        }
        composable(
            route = Destinations.TripMapRoute,
            arguments = listOf(navArgument("routeId") { type = NavType.StringType })
        ) { entry ->
            val routeId = entry.arguments?.getString("routeId").orEmpty()
            TripMapRouteScreen(
                routeId = routeId,
                onBack = { navController.popBackStack() },
                onNavigateHome = { navigateHome() }
            )
        }
        composable(Destinations.Dashboard) {
            RouteDashboardScreen(
                onStartTrip = { waybillId ->
                    navController.navigate(Destinations.activeTrip(waybillId))
                },
                onNavigateToMenu = { navigateHome() }
            )
        }
        composable(Destinations.IncidentsList) {
            IncidentsListScreen(
                onBack = { navController.popBackStack() },
                onNavigateHome = { navigateHome() },
                onEditIncident = { id ->
                    navController.navigate(Destinations.incidentEdit(id))
                }
            )
        }
        composable(
            route = Destinations.IncidentEdit,
            arguments = listOf(navArgument("incidentId") { type = NavType.StringType })
        ) { entry ->
            val incidentId = entry.arguments?.getString("incidentId").orEmpty()
            IncidentEditScreen(
                incidentId = incidentId,
                onBack = { navController.popBackStack() },
                onNavigateHome = { navigateHome() },
                onSaved = { navController.popBackStack() }
            )
        }
        composable(
            route = Destinations.ActiveTrip,
            arguments = listOf(navArgument("waybillId") { type = NavType.StringType })
        ) { entry ->
            val waybillId = entry.arguments?.getString("waybillId").orEmpty()
            ActiveTripScreen(
                waybillId = waybillId,
                onOpenIncident = { id -> navController.navigate(Destinations.incident(id)) },
                onTripCompleted = { navigateHome() },
                onBack = { navController.popBackStack() },
                onNavigateHome = { navigateHome() }
            )
        }
        composable(
            route = Destinations.Incident,
            arguments = listOf(navArgument("waybillId") { type = NavType.StringType })
        ) { entry ->
            val waybillId = entry.arguments?.getString("waybillId").orEmpty()
            IncidentReportScreen(
                waybillId = waybillId,
                onSubmitted = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
                onNavigateHome = { navigateHome() }
            )
        }
    }
}
