sed -i '/firebaseSyncRepo.addMedicineWithDosesAndReminders(/i \            updateInventoryInFirestore(name, 14)\n' app/src/main/java/com/example/ui/viewmodel/MedicineViewModel.kt
sed -i '/firebaseSyncRepo.addMedicineWithDosesAndReminders(/i \                updateInventoryInFirestore(item.medicine, item.durationDays * 2)\n' app/src/main/java/com/example/ui/viewmodel/MedicineViewModel.kt
