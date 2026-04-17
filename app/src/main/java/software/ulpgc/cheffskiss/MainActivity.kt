package software.ulpgc.cheffskiss

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import software.ulpgc.cheffskiss.ui.HomeViewModel
import software.ulpgc.cheffskiss.ui.RecipeViewModel
import software.ulpgc.cheffskiss.ui.screen.CreateRecipeScreen
import software.ulpgc.cheffskiss.ui.screen.HomeRoute
import software.ulpgc.cheffskiss.ui.screen.LibraryScreen
import software.ulpgc.cheffskiss.ui.screen.LoginScreen
import software.ulpgc.cheffskiss.ui.screen.RecipeDetailScreen
import software.ulpgc.cheffskiss.ui.screen.RegisterScreen
import software.ulpgc.cheffskiss.ui.theme.CheffsKissTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CheffsKissTheme {
                val navController = rememberNavController()
                val startDestination = if (FirebaseAuth.getInstance().currentUser!=null){
                    "home"
                } else {
                    "login"
                }
                NavHost(
                    navController = navController,
                    startDestination = startDestination
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
                            viewModel = viewModel(),
                            onCreateRecipe = { navController.navigate("create_recipe") },
                            onLibraryClick = {
                                navController.navigate("library") {
                                    launchSingleTop = true
                                }
                            },
                            onLogout = {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            },
                            onRecipeClick = { recipe ->
                                navController.navigate("recipe_detail/${recipe.id}")
                            })
                    }

                    composable("library") {
                        LibraryScreen(
                            viewModel = viewModel(),
                            authViewModel = viewModel(),
                            onGoHome = {
                                navController.navigate("home") {
                                    launchSingleTop = true
                                }
                            },
                            onCreateRecipe = {
                                navController.navigate("create_recipe")
                            }
                        )
                    }

                    composable("create_recipe") {
                        CreateRecipeScreen(
                            onBack = { navController.popBackStack() },
                            onPublishSuccess = {
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                }
                            },
                            onSaveDraft = { navController.popBackStack() }
                        )
                    }
                    composable(
                        route = "recipe_detail/{recipeId}",
                        arguments = listOf(navArgument("recipeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val recipeId = backStackEntry.arguments?.getString("recipeId") ?: return@composable
                        val viewModel: RecipeViewModel = viewModel()
                        val recipe by viewModel.recipe.collectAsState()        // ← ahora existe
                        val authorName by viewModel.authorName.collectAsState() // ← ahora existe

                        LaunchedEffect(recipeId) {
                            viewModel.loadRecipe(recipeId)
                        }

                        recipe?.let {
                            RecipeDetailScreen(
                                recipe = it,
                                authorName = authorName,
                                isSaved = false,
                                isOwner = false,
                                onBack = { navController.popBackStack() },
                                onSave = { },
                                onDelete = { navController.popBackStack() }
                            )
                        }
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