package software.ulpgc.cheffskiss.infrastructure.adapter.output

import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import software.ulpgc.cheffskiss.application.port.MealPlanRepository
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlan
import software.ulpgc.cheffskiss.domain.model.mealplan.MealPlanVersion
import software.ulpgc.cheffskiss.domain.model.mealplan.MealSlot
import software.ulpgc.cheffskiss.domain.enum.MealPlanStatus
import software.ulpgc.cheffskiss.domain.model.user.User
import software.ulpgc.cheffskiss.domain.enum.MealType
import software.ulpgc.cheffskiss.domain.enum.WeekDay
import software.ulpgc.cheffskiss.domain.model.mealplan.sortedBySchedule
import software.ulpgc.cheffskiss.infrastructure.adapter.input.FirebaseRecipeReader
import java.util.UUID

class FirebaseMealPlanService : MealPlanRepository {

    private val db = Firebase.firestore

    private fun plansCollection(userId: UUID) = db
        .collection("Users")
        .document(userId.toString())
        .collection("MealPlans")

    override suspend fun createMealPlan(mealPlan: MealPlan) {
        val userId = mealPlan.creator?.id ?: return
        val collection = plansCollection(userId)
        val hasPlans = !collection.get().await().isEmpty
        val planToSave = if (hasPlans) {
            mealPlan.copy(isPrimary = mealPlan.isPrimary)
        } else {
            mealPlan.copy(isPrimary = true)
        }
        collection
            .document(planToSave.id.toString())
            .set(planToSave.toMap())
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
                ),
            )
            .await()
        doc.set(mealPlan.toMap()).await()
    }

    override suspend fun deleteMealPlan(planId: UUID, userId: UUID) {
        val collection = plansCollection(userId)
        val doc = collection.document(planId.toString())
        doc.delete().await()
        val remaining = collection.get().await().documents
        if (remaining.isNotEmpty() && remaining.none { it.getBoolean("isPrimary") == true }) {
            remaining.first().reference.update("isPrimary", true).await()
        }
    }

    override suspend fun setActivePlan(planId: UUID, userId: UUID) {
        val collection = plansCollection(userId)
        val batch = db.batch()
        collection.get().await().documents.forEach { document ->
            batch.update(
                document.reference,
                mapOf("isPrimary" to (document.id == planId.toString())),
            )
        }
        batch.commit().await()
    }

    override fun getMealPlans(userId: UUID): Flow<List<MealPlan>> = callbackFlow {
        val recipeReader = FirebaseRecipeReader()
        val listener = plansCollection(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                launch {
                    val plans = snapshot?.documents?.mapNotNull { doc ->
                        runCatching { doc.toMealPlan(userId, recipeReader) }.getOrNull()
                    } ?: emptyList()
                    val sorted = plans.sortedWith(
                        compareByDescending<MealPlan> { it.isPrimary }.thenBy { it.name.lowercase() },
                    )
                    trySend(sorted)
                }
            }
        awaitClose { listener.remove() }
    }

    private fun MealPlan.toMap(): Map<String, Any?> = mapOf(
        "id" to id.toString(),
        "version" to version,
        "name" to name,
        "creatorId" to creator?.id?.toString(),
        "isPrimary" to isPrimary,
        "mealSlots" to mealSlots.sortedBySchedule().map { it.toMap() },
    )

    private fun MealSlot.toMap(): Map<String, Any?> = mapOf(
        "id" to id.toString(),
        "day" to day.name,
        "mealType" to mealType.name,
        "recipeId" to resolvedRecipeId()?.toString(),
    )

    @Suppress("UNCHECKED_CAST")
    private suspend fun com.google.firebase.firestore.DocumentSnapshot.toMealPlan(
        userId: UUID,
        recipeReader: FirebaseRecipeReader,
    ): MealPlan {
        val id = UUID.fromString(getString("id") ?: this.id)
        val version = getLong("version")?.toInt() ?: 0
        val name = getString("name") ?: ""
        val creatorId = getString("creatorId")
        val isPrimary = getBoolean("isPrimary") ?: false

        val rawSlots = get("mealSlots") as? List<*> ?: emptyList<Any>()
        val mealSlots = rawSlots
            .mapNotNull { (it as? Map<*, *>)?.toMealSlot(recipeReader) }
            .sortedBySchedule()

        val creator = creatorId?.let { User(UUID.fromString(it)) }

        return MealPlan(
            id = id,
            version = version,
            name = name,
            mealSlots = mealSlots,
            creator = creator,
            isPrimary = isPrimary,
        )
    }

    private suspend fun Map<*, *>.toMealSlot(recipeReader: FirebaseRecipeReader): MealSlot? = runCatching {
        val dayStr = get("day") as? String ?: return null
        val mealTypeStr = get("mealType") as? String ?: return null
        val recipeIdStr = (get("recipeId") as? String)
            ?: (get("recipeID") as? String)
            ?: (get("recipe") as? Map<*, *>)?.get("id") as? String

        val recipeUuid = recipeIdStr?.let { UUID.fromString(it) }
        val recipe = recipeUuid?.let { recipeReader.getById(it.toString()) }

        MealSlot(
            id = UUID.fromString(get("id") as? String ?: return null),
            day = WeekDay.valueOf(dayStr),
            mealType = MealType.valueOf(mealTypeStr),
            recipe = recipe,
            recipeId = recipe?.id ?: recipeUuid,
        )
    }.getOrNull()
}
