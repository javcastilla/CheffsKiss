package software.ulpgc.cheffskiss.domain.model.vo

data class SlotTime(
    val hour: Int,
    val minute: Int
) : Comparable<SlotTime> {

    init {
        require(hour in 0..23)
        require(minute in 0..59)
    }

    companion object {
        fun at(hour: Int, minute: Int) = SlotTime(hour, minute)

        fun fromIsoString(value: String): SlotTime {
            val parts = value.split(":")
            require(parts.size == 2)
            return SlotTime(
                parts[0].toInt(),
                parts[1].toInt()
            )
        }

        fun fromTotalMinutes(totalMinutes: Int) = SlotTime(
            hour = totalMinutes / 60,
            minute = totalMinutes % 60
        )
    }

    val totalMinutes: Int get() = hour * 60 + minute

    override fun compareTo(other: SlotTime): Int =
        this.totalMinutes.compareTo(other.totalMinutes)

    override fun toString(): String = "%02d:%02d".format(hour, minute)
}