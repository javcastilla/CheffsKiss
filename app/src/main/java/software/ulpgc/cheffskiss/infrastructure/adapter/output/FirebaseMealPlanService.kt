package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlanVersion
import software.ulpgc.cheffskiss.domain.model.mealplan.MealSlot
import software.ulpgc.cheffskiss.domain.enum.MealPlanStatus
import software.ulpgc.cheffskiss.domain.model.user.User
import software.ulpgc.cheffskiss.domain.enum.MealType
import software.ulpgc.cheffskiss.domain.enum.WeekDay
import java.util.UUID

class FirebaseMealPlanService : MealPlanRepository {

    private fun plansCollection(userId: UUID) = Firebase.firestore
        .collection("Users")
        .document(userId.toString())
        .collection("MealPlans")

    // ── CRUD ─────────────────────────────────────────────────────────────────

    override suspend fun createMealPlan(mealPlan: MealPlan) {
        val userId = mealPlan.creator?.id ?: return
        plansCollection(userId)
            .document(mealPlan.id.toString())
            .set(mealPlan.toMap())
            .await()
    }

    override suspend fun updateMealPlan(mealPlan: MealPlan) {
        val userId = mealPlan.creator?.id ?: return
        val doc = plansCollection(userId).document(mealPlan.id.toString())
        val snapshot = MealPlanVersion(mealPlan = mealPlan, status = MealPlanStatus.PRIMARY)
        doc.collection("versions")
            .document(mealPlan.version.toString())
            .set(
                mapOf(
                    "id" to snapshot.id.toString(),
                    "timestamp" to snapshot.timestamp.toString(),
                    "status" to snapshot.status.name,
                    "mealPlan" to mealPlan.toMap(),
                )
            )
            .await()
        doc.set(mealPlan.toMap()).await()
    }

    override suspend fun deleteMealPlan(planId: UUID, userId: UUID) {
        plansCollection(userId)
            .document(planId.toString())
            .delete()
            .await()
    }

    override suspend fun setActivePlan(planId: UUID, userId: UUID) {
        // No se necesita implementación para el nuevo modelo
    }

    override fun getMealPlans(userId: UUID): Flow<List<MealPlan>> = callbackFlow {
        val listener = plansCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val plans = snapshot?.documents?.mapNotNull { doc ->
                    runCatching { doc.toMealPlan(userId) }.getOrNull()
                } ?: emptyList()
                trySend(plans)
            }
        awaitClose { listener.remove() }
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun MealPlan.toMap(): Map<String, Any?> = mapOf(
        "id" to id.toString(),
        "version" to version,
        "name" to name,
        "creatorId" to creator?.id?.toString(),
        "mealSlots" to mealSlots.map { it.toMap() }
    )

    private fun MealSlot.toMap(): Map<String, Any?> = mapOf(
        "id" to id.toString(),
        "day" to day.name,
        "mealType" to mealType.name,
        "recipeId" to recipe?.id?.toString()
    )

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toMealPlan(userId: UUID): MealPlan {
        val id        = UUID.fromString(getString("id") ?: id)
        val version   = getLong("version")?.toInt() ?: 0
        val name      = getString("name") ?: ""
        val creatorId = getString("creatorId")

        val rawSlots = get("mealSlots") as? List<Map<String, Any?>> ?: emptyList()
        val mealSlots = rawSlots.mapNotNull { (it as? Map<*, *>)?.toMealSlot() }

        val creator = creatorId?.let { User(UUID.fromString(it)) }

        return MealPlan(
            id        = id,
            version   = version,
            name      = name,
            mealSlots = mealSlots,
            creator   = creator
        )
    }

    private fun Map<*, *>.toMealSlot(): MealSlot? = runCatching {
        val dayStr = get("day") as? String ?: return null
        val mealTypeStr = get("mealType") as? String ?: return null
        
        MealSlot(
            id       = UUID.fromString(get("id") as? String ?: return null),
            day      = WeekDay.valueOf(dayStr),
            mealType = MealType.valueOf(mealTypeStr),
            recipe   = null  // La receta se cargará si es necesario
        )
    }.getOrNull()
}
