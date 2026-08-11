sed -i 's/val affiliateClicks = PharmacyAffiliateService.affiliateClicksTable/val affiliateClicks = firebaseSyncRepo.affiliateClicks/' app/src/main/java/com/example/ui/viewmodel/MedicineViewModel.kt
