package software.ulpgc.cheffskiss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import software.ulpgc.cheffskiss.ui.theme.CheffsKissTheme
import software.ulpgc.cheffskiss.ui.screen.LoginScreen
import software.ulpgc.cheffskiss.ui.screen.RegisterScreen
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import software.ulpgc.cheffskiss.application.services.GetAllRecipesQuery
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import software.ulpgc.cheffskiss.ui.HomeViewModel
import software.ulpgc.cheffskiss.ui.screen.CreateRecipeScreen
import software.ulpgc.cheffskiss.ui.screen.HomeRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheffsKissTheme {
                val navController = rememberNavController()
                val homeViewModel = remember {
                    HomeViewModel(
                        getAllRecipesQuery = GetAllRecipesQuery(FirebaseRecipeReader())
                    )
                }

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToRegister = { navController.navigate("register") }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onGoToLogin = { navController.popBackStack() }
                        )
                    }
                    composable("home") {
                        HomeRoute(
                            viewModel = homeViewModel,
                            onCreateRecipe = { navController.navigate("create_recipe") },
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("create_recipe") {
                        CreateRecipeScreen(
                            onBack = { navController.popBackStack() },
                            onPublishSuccess = { // Antes decía solo "onPublish"
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onSaveDraft = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CheffsKissTheme {
        Greeting("Android")
    }
}