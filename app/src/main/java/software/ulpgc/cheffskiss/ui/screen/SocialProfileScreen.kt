package software.ulpgc.cheffskiss.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.ulpgc.cheffskiss.application.services.UserDisplayService
import software.ulpgc.cheffskiss.ui.components.TabScaffold
import software.ulpgc.cheffskiss.ui.navigation.MainBottomNavigation
import software.ulpgc.cheffskiss.ui.theme.Background
import software.ulpgc.cheffskiss.ui.theme.CKOnSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.CKSurfaceVariant
import software.ulpgc.cheffskiss.ui.theme.OnBackground
import software.ulpgc.cheffskiss.ui.theme.OnSurface
import software.ulpgc.cheffskiss.ui.theme.Primary
import java.util.UUID

@Composable
fun SocialProfileScreen(
    onHomeClick: () -> Unit,
    onExploreClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onCreateRecipe: () -> Unit,
    onCreateMealPlan: () -> Unit,
    onCreateList: () -> Unit,
    onLogout: () -> Unit,
) {
    var displayName by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    val firebaseUser = FirebaseAuth.getInstance().currentUser

    LaunchedEffect(firebaseUser?.uid) {
        loading = true
        val uid = firebaseUser?.uid
        displayName = if (uid == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    UserDisplayService().displayNameFor(UUID.nameUUIDFromBytes(uid.toByteArray()))
                }.getOrNull()
            }
        }
        loading = false
    }

    TabScaffold(
        currentRoute = MainBottomNavigation.PROFILE,
        onHomeClick = onHomeClick,
        onExploreClick = onExploreClick,
        onLibraryClick = onLibraryClick,
        onProfileClick = {},
        onCreateRecipe = onCreateRecipe,
        onCreateMealPlan = onCreateMealPlan,
        onCreateList = onCreateList,
        containerColor = Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Profile",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    color = Primary,
                )
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = OnBackground)
                }
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Primary)
                }
                return@Column
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(CKSurfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(44.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = displayName ?: firebaseUser?.email?.substringBefore("@") ?: "Chef",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = OnSurface,
                )
                Text(
                    text = firebaseUser?.email ?: "",
                    fontSize = 13.sp,
                    color = CKOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(28.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Groups, null, tint = Primary, modifier = Modifier.size(18.dp))
                    Text(
                        "Your social space is coming soon",
                        fontSize = 14.sp,
                        color = CKOnSurfaceVariant,
                    )
                }
            }
        }
    }
}
