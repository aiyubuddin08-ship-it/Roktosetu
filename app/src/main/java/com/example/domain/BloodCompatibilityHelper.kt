package com.example.domain

import com.example.model.BloodGroup

enum class CompatibilityMatchType(val labelEn: String, val labelBn: String) {
    EXACT_MATCH("Exact Blood Group Match", "হুবহু রক্তের গ্রুপ ম্যাচ"),
    COMPATIBLE_ALTERNATIVE("Medically Compatible (RBC)", "চিকিৎসাগতভাবে সামঞ্জস্যপূর্ণ"),
    INCOMPATIBLE("Not Compatible", "সামঞ্জস্যপূর্ণ নয়")
}

/**
 * Domain utility providing red blood cell (ABO + Rh) compatibility rules,
 * distinguishing exact blood group matches from compatible alternatives,
 * and supplying mandatory clinical crossmatching disclaimers.
 */
object BloodCompatibilityHelper {

    const val CLINICAL_DISCLAIMER_EN: String =
        "Important Clinical Notice: Blood group compatibility shown in the app applies to standard red blood cell (RBC) transfusion rules and does NOT replace clinical screening, infectious disease testing, or mandatory hospital laboratory crossmatching."

    const val CLINICAL_DISCLAIMER_BN: String =
        "গুরুত্বপূর্ণ চিকিৎসা সতর্কতা: অ্যাপে প্রদর্শিত রক্তের সামঞ্জস্যতা শুধুমাত্র প্রাথমিক লোহিত রক্তকণিকা (RBC) ট্রান্সফিউশন তথ্যের জন্য। রক্তদানের পূর্বে অনুমোদিত ব্লাড ব্যাংক বা হাসপাতালে সরাসরি ক্রস-ম্যাচিং (Cross-matching) এবং ক্লিনিক্যাল স্ক্রিনিং বাধ্যতামূলক।"

    /**
     * Checks if a donor blood group is compatible for a recipient patient (red blood cell transfusion).
     */
    fun isCompatible(donor: BloodGroup, recipient: BloodGroup): Boolean {
        return recipient.canReceiveFrom().contains(donor)
    }

    /**
     * Checks if a donor blood group is an exact ABO + Rh match for the recipient.
     */
    fun isExactMatch(donor: BloodGroup, recipient: BloodGroup): Boolean {
        return donor == recipient
    }

    /**
     * Classifies the match type between a donor and a recipient.
     */
    fun getMatchType(donor: BloodGroup, recipient: BloodGroup): CompatibilityMatchType {
        return when {
            donor == recipient -> CompatibilityMatchType.EXACT_MATCH
            isCompatible(donor, recipient) -> CompatibilityMatchType.COMPATIBLE_ALTERNATIVE
            else -> CompatibilityMatchType.INCOMPATIBLE
        }
    }

    fun classifyDonorMatch(donor: BloodGroup, recipient: BloodGroup): CompatibilityMatchType =
        getMatchType(donor, recipient)

    /**
     * Returns a list of compatible donor blood groups for a given patient recipient blood group,
     * ordered with the exact match first followed by compatible alternatives.
     */
    fun getCompatibleDonorGroups(recipient: BloodGroup): List<BloodGroup> {
        val all = recipient.canReceiveFrom()
        return listOf(recipient) + all.filter { it != recipient }
    }

    /**
     * Returns an explanation message in Bengali & English distinguishing exact vs compatible matches.
     */
    fun getCompatibilityExplanation(donor: BloodGroup, recipient: BloodGroup): String {
        return when (getMatchType(donor, recipient)) {
            CompatibilityMatchType.EXACT_MATCH ->
                "✓ হুবহু ম্যাচ (Exact Match): ${donor.display} রক্তদাতা ${recipient.display} রোগীর জন্য সর্বোত্তম। হাসপাতালে ক্রস-ম্যাচিং আবশ্যক।"
            CompatibilityMatchType.COMPATIBLE_ALTERNATIVE ->
                "✓ বিকল্প সামঞ্জস্যপূর্ণ (Compatible RBC): ${donor.display} রক্তদাতা জরুরী প্রয়োজনে ${recipient.display} রোগীকে লোহিত রক্তকণিকা দিতে পারেন (ডাক্তারের পরামর্শ ও ক্রস-ম্যাচিং সাপেক্ষে)।"
            CompatibilityMatchType.INCOMPATIBLE ->
                "✗ অসামঞ্জস্যপূর্ণ (Incompatible): ${donor.display} রক্তদাতা ${recipient.display} রোগীকে রক্ত দিতে পারবেন না।"
        }
    }
}
