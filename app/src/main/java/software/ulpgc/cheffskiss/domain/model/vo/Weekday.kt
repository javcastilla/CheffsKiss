package software.ulpgc.cheffskiss.domain.model.vo

enum class Weekday(val shortName: String, val fullName: String) {
    MONDAY("Mon", "Monday"),
    TUESDAY("Tue", "Tuesday"),
    WEDNESDAY("Wed", "Wednesday"),
    THURSDAY("Thu", "Thursday"),
    FRIDAY("Fri", "Friday"),
    SATURDAY("Sat", "Saturday"),
    SUNDAY("Sun", "Sunday");

    companion object {
        fun fromName(name: String): Weekday {
            return entries.find { it.name.equals(name, ignoreCase = true) }
                ?: throw IllegalArgumentException("Invalid weekday: $name")
        }
    }
}