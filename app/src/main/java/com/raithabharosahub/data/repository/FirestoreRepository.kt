package com.raithabharosahub.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.raithabharosahub.data.local.entity.FarmerEntity
import com.raithabharosahub.data.local.entity.NpkEntity
import com.raithabharosahub.data.local.entity.SeasonEntity
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

import com.raithabharosahub.data.local.entity.PlotEntity

data class UserData(
    val farmer: FarmerEntity?,
    val plot: PlotEntity?,
    val npkHistory: List<NpkEntity>,
    val seasonHistory: List<SeasonEntity>
)

@Singleton
class FirestoreRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    private val uid: String?
        get() = auth.currentUser?.uid

    fun syncFarmerProfile(farmer: FarmerEntity, plot: PlotEntity) {
        val currentUid = uid ?: return
        
        val data = hashMapOf(
            "id" to farmer.id,
            "name" to farmer.name,
            "mobile" to farmer.mobile,
            "primaryCrop" to farmer.primaryCrop,
            "district" to farmer.district,
            "languagePref" to farmer.languagePref,
            "plotId" to plot.id,
            "latitude" to plot.latitude,
            "longitude" to plot.longitude,
            "label" to plot.label
        )
        
        firestore.collection("users").document(currentUid)
            .collection("profile").document("farmer_info")
            .set(data, SetOptions.merge())
    }

    fun syncNpkEntry(npk: NpkEntity) {
        val currentUid = uid ?: return
        
        val data = hashMapOf(
            "id" to npk.id,
            "plotId" to npk.plotId,
            "nitrogen" to npk.nitrogen,
            "phosphorus" to npk.phosphorus,
            "potassium" to npk.potassium,
            "testDate" to npk.testDate.time,
            "labName" to npk.labName
        )
        
        firestore.collection("users").document(currentUid)
            .collection("npkHistory").document(npk.id.toString())
            .set(data, SetOptions.merge())
    }

    fun syncSeasonEntry(season: SeasonEntity) {
        val currentUid = uid ?: return
        
        val data = hashMapOf(
            "id" to season.id,
            "plotId" to season.plotId,
            "crop" to season.crop,
            "sowDate" to season.sowDate.time,
            "harvestDate" to season.harvestDate?.time,
            "yieldKg" to season.yieldKg,
            "notes" to season.notes
        )
        
        firestore.collection("users").document(currentUid)
            .collection("seasonHistory").document(season.id.toString())
            .set(data, SetOptions.merge())
    }

    suspend fun loadUserData(): UserData? {
        val currentUid = uid ?: return null

        return try {
            val profileDoc = firestore.collection("users").document(currentUid)
                .collection("profile").document("farmer_info")
                .get()
                .await()
            
            var farmer: FarmerEntity? = null
            var plot: PlotEntity? = null
            if (profileDoc.exists()) {
                farmer = FarmerEntity(
                    id = profileDoc.getLong("id") ?: 0L,
                    name = profileDoc.getString("name") ?: "",
                    mobile = profileDoc.getString("mobile") ?: "",
                    primaryCrop = profileDoc.getString("primaryCrop") ?: "",
                    district = profileDoc.getString("district") ?: "",
                    languagePref = profileDoc.getString("languagePref") ?: "kn"
                )
                plot = PlotEntity(
                    id = profileDoc.getLong("plotId") ?: 0L,
                    farmerId = farmer.id,
                    latitude = profileDoc.getDouble("latitude") ?: 0.0,
                    longitude = profileDoc.getDouble("longitude") ?: 0.0,
                    label = profileDoc.getString("label") ?: ""
                )
            }

            val npkSnapshot = firestore.collection("users").document(currentUid)
                .collection("npkHistory")
                .get()
                .await()
                
            val npkHistory = npkSnapshot.documents.mapNotNull { doc ->
                try {
                    NpkEntity(
                        id = doc.getLong("id") ?: 0L,
                        plotId = doc.getLong("plotId") ?: 0L,
                        nitrogen = doc.getDouble("nitrogen")?.toFloat() ?: 0f,
                        phosphorus = doc.getDouble("phosphorus")?.toFloat() ?: 0f,
                        potassium = doc.getDouble("potassium")?.toFloat() ?: 0f,
                        testDate = Date(doc.getLong("testDate") ?: 0L),
                        labName = doc.getString("labName") ?: ""
                    )
                } catch (e: Exception) { null }
            }

            val seasonSnapshot = firestore.collection("users").document(currentUid)
                .collection("seasonHistory")
                .get()
                .await()
                
            val seasonHistory = seasonSnapshot.documents.mapNotNull { doc ->
                try {
                    SeasonEntity(
                        id = doc.getLong("id") ?: 0L,
                        plotId = doc.getLong("plotId") ?: 0L,
                        crop = doc.getString("crop") ?: "",
                        sowDate = Date(doc.getLong("sowDate") ?: 0L),
                        harvestDate = doc.getLong("harvestDate")?.let { Date(it) },
                        yieldKg = doc.getDouble("yieldKg")?.toFloat(),
                        notes = doc.getString("notes")
                    )
                } catch (e: Exception) { null }
            }

            UserData(farmer, plot, npkHistory, seasonHistory)
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "Failed to load user data", e)
            null
        }
    }
}
