sed -i '/val doses: StateFlow<List<DoseFirebaseModel>> = _doses.asStateFlow()/a \
\
    private val _affiliateClicks = MutableStateFlow<List<AffiliateClickFirebaseModel>>(emptyList())\n    val affiliateClicks: StateFlow<List<AffiliateClickFirebaseModel>> = _affiliateClicks.asStateFlow()\n\n    fun logAffiliateClick(userId: String, medicine: String, pharmacy: String) {\n        val click = AffiliateClickFirebaseModel(userId = userId, medicine = medicine, pharmacy = pharmacy)\n        val current = _affiliateClicks.value.toMutableList()\n        current.add(click)\n        _affiliateClicks.value = current\n    }\n\
' app/src/main/java/com/example/data/firebase/FirebaseModels.kt
