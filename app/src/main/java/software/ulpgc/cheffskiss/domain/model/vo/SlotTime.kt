package software.ulpgc.cheffskiss.domain.model.vo

data class SlotTime(
    val hour: Int,
    val minute: Int
) : Comparable<SlotTime> {

    init {
        require(hour in 0..23) { "Hour must be in 0..23, got $hour" }
        require(minute in 0..59) { "Minute must be in 0..59, got $minute" }
    }

    val totalMinutes: Int get() = hour * 60 + minute

    override fun compareTo(other: SlotTime): Int =
        totalMinutes.compareTo(other.totalMinutes)

    override fun toString(): String = "%02d:%02d".format(hour, minute)

    companion object {
        fun at(hour: Int, minute: Int): SlotTime = SlotTime(hour, minute)

        fun fromHHmm(value: String): SlotTime {
            val parts = value.split(":")
            require(parts.size == 2) { "Expected HH:mm format, got '$value'" }
            val hour = parts[0].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid hour in '$value'")
            val minute = parts[1].toIntOrNull()
                ?: throw IllegalArgumentException("Invalid minute in '$value'")
            return SlotTime(hour, minute)
        }

        fun fromTotalMinutes(totalMinutes: Int): SlotTime =
            SlotTime(hour = totalMinutes / 60, minute = totalMinutes % 60)
    }
}