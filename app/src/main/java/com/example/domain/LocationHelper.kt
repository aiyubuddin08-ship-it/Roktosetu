package com.example.domain

/**
 * Text-based Administrative Location Helper for Bangladesh (Division -> District -> Upazila -> Area).
 * Completely map-free and GPS-free: protects donor privacy while enabling accurate regional search.
 */
object LocationHelper {

    val DIVISIONS: List<String> = listOf(
        "Dhaka",
        "Chattogram",
        "Rajshahi",
        "Khulna",
        "Sylhet",
        "Barishal",
        "Rangpur",
        "Mymensingh"
    )

    val DISTRICTS_BY_DIVISION: Map<String, List<String>> = mapOf(
        "Dhaka" to listOf("Dhaka", "Gazipur", "Narayanganj", "Tangail", "Faridpur", "Manikganj", "Munshiganj", "Narsingdi"),
        "Chattogram" to listOf("Chattogram", "Cox's Bazar", "Cumilla", "Feni", "Noakhali", "Brahmanbaria", "Chandpur", "Rangamati"),
        "Rajshahi" to listOf("Rajshahi", "Bogura", "Pabna", "Sirajganj", "Natore", "Naogaon", "Chapainawabganj", "Joypurhat"),
        "Khulna" to listOf("Khulna", "Jashore", "Kushtia", "Satkhira", "Bagerhat", "Jhenaidah", "Magura"),
        "Sylhet" to listOf("Sylhet", "Moulvibazar", "Habiganj", "Sunamganj"),
        "Barishal" to listOf("Barishal", "Patuakhali", "Bhola", "Pirojpur", "Barguna", "Jhalokati"),
        "Rangpur" to listOf("Rangpur", "Dinajpur", "Kurigram", "Gaibandha", "Nilphamari", "Lalmonirhat", "Thakurgaon", "Panchagarh"),
        "Mymensingh" to listOf("Mymensingh", "Jamalpur", "Netrokona", "Sherpur")
    )

    val UPAZILAS_BY_DISTRICT: Map<String, List<String>> = mapOf(
        "Dhaka" to listOf("Dhanmondi", "Mirpur", "Uttara", "Mohammadpur", "Gulshan", "Banani", "Shahbagh", "Motijheel", "Savar", "Keraniganj"),
        "Gazipur" to listOf("Tongi", "Gazipur Sadar", "Kaliakair", "Sreepur", "Kapasia"),
        "Narayanganj" to listOf("Narayanganj Sadar", "Siddhirganj", "Fatullah", "Rupganj", "Sonargaon"),
        "Chattogram" to listOf("Panchlaish", "Kotwali", "Pahartali", "Halishahar", "Agrabad", "Double Mooring", "Hathazari", "Sitakunda"),
        "Cox's Bazar" to listOf("Cox's Bazar Sadar", "Chakaria", "Teknaf", "Ukhiya", "Ramu"),
        "Rajshahi" to listOf("Boalia", "Rajpara", "Motihar", "Shah Makhdum", "Paba", "Godagari"),
        "Khulna" to listOf("Khalishpur", "Sonadanga", "Khulna Sadar", "Daulatpur", "Dumuria"),
        "Sylhet" to listOf("Sylhet Sadar", "Beanibazar", "Golapganj", "South Surma", "Bishwanath"),
        "Barishal" to listOf("Barishal Sadar", "Bakerganj", "Babuganj", "Mehendiganj"),
        "Rangpur" to listOf("Rangpur Sadar", "Mithapukur", "Pirganj", "Badarganj"),
        "Mymensingh" to listOf("Mymensingh Sadar", "Trishal", "Bhaluka", "Muktagachha", "Gafargaon")
    )

    val ALL_DISTRICTS: List<String> = DISTRICTS_BY_DIVISION.values.flatten().distinct()

    fun getDistrictsForDivision(division: String): List<String> {
        if (division.isBlank() || division.equals("All", ignoreCase = true)) {
            return ALL_DISTRICTS
        }
        return DISTRICTS_BY_DIVISION[division] ?: listOf(division)
    }

    fun getUpazilasForDistrict(district: String): List<String> {
        if (district.isBlank() || district.equals("All", ignoreCase = true)) {
            return emptyList()
        }
        return UPAZILAS_BY_DISTRICT[district] ?: emptyList()
    }

    fun inferDivisionForDistrict(district: String): String {
        val trimmed = district.trim()
        for ((div, districts) in DISTRICTS_BY_DIVISION) {
            if (districts.any { it.equals(trimmed, ignoreCase = true) }) {
                return div
            }
        }
        return "Dhaka"
    }

    /**
     * Formats non-sensitive administrative location fields into a readable display string.
     */
    fun formatGeneralLocation(
        division: String = "",
        district: String = "",
        upazila: String = "",
        area: String = "",
        city: String = ""
    ): String {
        val effectiveDistrict = district.ifBlank { city }
        val parts = listOf(area.trim(), upazila.trim(), effectiveDistrict.trim(), division.trim())
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        return if (parts.isEmpty()) "Bangladesh" else parts.joinToString(", ")
    }

    /**
     * Checks whether a donor or request matches text-based location filters.
     */
    fun matchesLocationFilter(
        itemDivision: String,
        itemDistrict: String,
        itemUpazila: String,
        itemArea: String,
        divisionFilter: String? = null,
        districtFilter: String? = null,
        upazilaFilter: String? = null,
        areaQuery: String? = null,
        filterDivision: String? = null,
        filterDistrict: String? = null,
        filterUpazila: String? = null,
        filterAreaQuery: String? = null
    ): Boolean {
        val effDivFilter = divisionFilter ?: filterDivision
        val effDistFilter = districtFilter ?: filterDistrict
        val effUpaFilter = upazilaFilter ?: filterUpazila
        val effAreaQuery = areaQuery ?: filterAreaQuery

        if (!effDivFilter.isNullOrBlank() && !effDivFilter.equals("All", ignoreCase = true)) {
            if (!itemDivision.equals(effDivFilter.trim(), ignoreCase = true)) return false
        }
        if (!effDistFilter.isNullOrBlank() && !effDistFilter.equals("All", ignoreCase = true)) {
            if (!itemDistrict.equals(effDistFilter.trim(), ignoreCase = true)) return false
        }
        if (!effUpaFilter.isNullOrBlank() && !effUpaFilter.equals("All", ignoreCase = true)) {
            if (!itemUpazila.contains(effUpaFilter.trim(), ignoreCase = true) &&
                !itemArea.contains(effUpaFilter.trim(), ignoreCase = true)
            ) {
                return false
            }
        }
        if (!effAreaQuery.isNullOrBlank()) {
            val q = effAreaQuery.trim()
            val combined = "$itemArea $itemUpazila $itemDistrict $itemDivision"
            if (!combined.contains(q, ignoreCase = true)) return false
        }
        return true
    }
}
