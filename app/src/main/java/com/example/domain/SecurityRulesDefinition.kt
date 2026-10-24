package com.example.domain

/**
 * Plain-language Bengali privacy and data protection summaries for the RoktoSetu application.
 * Free of technical paths, collection names, or developer jargon.
 */
object SecurityRulesDefinition {

    data class AccessControlSummaryItem(
        val titleBn: String,
        val protectedDataBn: String,
        val whoCanAccessBn: String,
        val securityGuaranteeBn: String
    )

    val PRIVACY_SUMMARY_ITEMS = listOf(
        AccessControlSummaryItem(
            titleBn = "ব্যক্তিগত প্রোফাইল ও অ্যাকাউন্ট তথ্য",
            protectedDataBn = "ইমেইল ঠিকানা, জন্মতারিখ, গোপন মোবাইল নম্বর এবং অ্যাকাউন্ট সেটিংস",
            whoCanAccessBn = "শুধুমাত্র আপনি নিজে এবং অনুমোদিত প্রশাসক",
            securityGuaranteeBn = "অন্য কোনো সাধারণ ব্যবহারকারী আপনার ব্যক্তিগত প্রোফাইল দেখতে বা পরিবর্তন করতে পারবেন না।"
        ),
        AccessControlSummaryItem(
            titleBn = "প্রকাশ্য রক্তদাতা তালিকা",
            protectedDataBn = "রক্তের গ্রুপ, বিভাগ/জেলা/উপজেলা, রক্তদানের প্রাপ্যতা এবং যাচাই ব্যাজ",
            whoCanAccessBn = "সাইন-ইন করা নিবন্ধিত ব্যবহারকারীগণ",
            securityGuaranteeBn = "আপনি নিজে সুইচ চালু না করা পর্যন্ত আপনার ফোন নম্বর প্রকাশ্য তালিকায় দেখানো হয় না।"
        ),
        AccessControlSummaryItem(
            titleBn = "জরুরি রক্তের অনুরোধ ও রক্তদান রেকর্ড",
            protectedDataBn = "রোগীর নাম, হাসপাতালের তথ্য, রক্তের ব্যাগের সংখ্যা এবং রক্তদান সনদ",
            whoCanAccessBn = "আবেদনকারী, সাড়া দেওয়া রক্তদাতা এবং অনুমোদিত প্রশাসক",
            securityGuaranteeBn = "নিজের রক্তের অনুরোধ নিজে গ্রহণ করা যায় না এবং যাচাই ছাড়া রক্তদান সংখ্যা পরিবর্তন হয় না।"
        ),
        AccessControlSummaryItem(
            titleBn = "নিরাপদ বার্তা আদান-প্রদান",
            protectedDataBn = "রোগীর স্বজন ও রক্তদাতার মধ্যকার ব্যক্তিগত কথোপকথন",
            whoCanAccessBn = "শুধুমাত্র কথোপকথনে যুক্ত দুইজন ব্যবহারকারী",
            securityGuaranteeBn = "তৃতীয় কোনো ব্যক্তি আপনার ব্যক্তিগত চ্যাট পড়তে বা বার্তা পাঠাতে পারবেন না।"
        )
    )
}
