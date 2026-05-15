package software.ulpgc.cheffskiss.domain.store

import software.ulpgc.cheffskiss.domain.model.MealPlan
import software.ulpgc.cheffskiss.domain.model.MealPlanVersion
import software.ulpgc.cheffskiss.domain.model.User
import java.util.Optional
import java.util.UUID
import java.util.stream.Stream

interface MealPlanStore {
    fun of(user: User): Stream<MealPlan>
    fun with(id: UUID): Optional<MealPlan>
    fun add(mealPlan: MealPlan): MealPlanStore
    fun history(id: UUID): Stream<MealPlanVersion>
}