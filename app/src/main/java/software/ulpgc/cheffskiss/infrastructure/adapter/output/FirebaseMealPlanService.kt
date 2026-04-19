package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.model.MealPlan
import software.ulpgc.cheffskiss.domain.model.MealSlot
import software.ulpgc.cheffskiss.domain.model.vo.SlotTime
import software.ulpgc.cheffskiss.domain.model.vo.Weekday
import java.util.UUID

class FirebaseMealPlanService : MealPlanRepository {

    private fun plansCollection(userId: UUID) = Firebase.firestore
        .collection("users")
        .document(userId.toString())
        .collection("mealPlans")

    // ── CRUD ─────────────────────────────────────────────────────────────────

    override suspend fun createMealPlan(mealPlan: MealPlan) {
        plansCollection(mealPlan.userId)
            .document(mealPlan.id.toString())
            .set(mealPlan.toMap())
            .await()
    }

    override suspend fun updateMealPlan(mealPlan: MealPlan) {
        plansCollection(mealPlan.userId)
            .document(mealPlan.id.toString())
            .set(mealPlan.toMap())
            .await()
    }

    override suspend fun deleteMealPlan(planId: UUID, userId: UUID) {
        plansCollection(userId)
            .document(planId.toString())
            .delete()
            .await()
    }

    override suspend fun setActivePlan(planId: UUID, userId: UUID) {
        val col = plansCollection(userId)
        val snapshot = col.get().await()
        val batch = Firebase.firestore.batch()
        snapshot.documents.forEach { doc ->
            batch.update(doc.reference, "isActive", doc.id == planId.toString())
        }
        batch.commit().await()
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
        "userId" to userId.toString(),
        "name" to name,
        "isActive" to isActive,
        "createdAt" to createdAt.toString(),
        "days" to days.map { (day, slots) ->
            day.name to slots.map { it.toMap() }
        }.toMap()
    )

    private fun MealSlot.toMap(): Map<String, Any?> = mapOf(
        "id" to id.toString(),
        "name" to name,
        "startTime" to startTime.toString(),
        "endTime" to endTime.toString(),
        "recipeId" to recipeId?.toString(),
        "colorIndex" to colorIndex
    )

    @Suppress("UNCHECKED_CAST")
    private fun com.google.firebase.firestore.DocumentSnapshot.toMealPlan(userId: UUID): MealPlan {
        val id        = UUID.fromString(getString("id") ?: id)
        val name      = getString("name") ?: ""
        val isActive  = getBoolean("isActive") ?: false
        val createdAt = getString("createdAt")?.let { Instant.parse(it) } ?: Clock.System.now()

        val rawDays = get("days") as? Map<String, List<Map<String, Any?>>> ?: emptyMap()
        val days = Weekday.entries.associateWith { weekday ->
            val rawSlots = rawDays[weekday.name] as? List<*> ?: emptyList<Any>()
            rawSlots.mapNotNull { (it as? Map<*, *>)?.toMealSlot() }
        }

        return MealPlan(
            id        = id,
            userId    = userId,
            name      = name,
            isActive  = isActive,
            createdAt = createdAt,
            days      = days
        )
    }

    private fun Map<*, *>.toMealSlot(): MealSlot? = runCatching {
        MealSlot(
            id         = UUID.fromString(get("id") as? String ?: return null),
            name       = get("name") as? String ?: "",
            startTime = SlotTime.fromHHmm(get("startTime") as? String ?: "08:00"),
            endTime   = SlotTime.fromHHmm(get("endTime")   as? String ?: "09:00"),
            recipeId   = (get("recipeId") as? String)?.let { UUID.fromString(it) },
            colorIndex = when (val raw = get("colorIndex")) {
                is Long   -> raw.toInt()
                is Int    -> raw
                is Double -> raw.toInt()
                else      -> 0
            }
        )
    }.getOrNull()
}
