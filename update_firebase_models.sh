sed -i '/data class InventoryFirebaseModel(/i \
data class AffiliateClickFirebaseModel(\n    val id: String = UUID.randomUUID().toString(),\n    val userId: String,\n    val medicine: String,\n    val pharmacy: String,\n    val timestamp: String = SimpleDateFormat("yyyy-MM-dd'\''T'\''HH:mm:ss.SSS'\''Z'\''", Locale.US).format(Date()),\n    val user_id: String = userId\n)\n\
' app/src/main/java/com/example/data/firebase/FirebaseModels.kt
