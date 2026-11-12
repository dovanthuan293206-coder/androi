package com.example.aistudymentor.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

internal data class StudyEntry(
    val id: String,
    val kind: String,
    val question: String,
    val content: String,
    val subject: String,
    val bookmarked: Boolean,
    val selection: Int?
)

internal class StudyCloud(uid: String) {
    private val profile = FirebaseFirestore.getInstance().collection("users").document(uid)
    private val entries = profile.collection("studyEntries")

    fun observeProfile(update: (String, String, List<String>) -> Unit, failure: () -> Unit) =
        profile.addSnapshotListener { snapshot, error ->
            if (error != null) failure()
            else if (snapshot != null && snapshot.exists()) {
                update(
                    snapshot.getString("level") ?: "THPT",
                    snapshot.getString("style") ?: "Từng bước",
                    (snapshot.get("subjects") as? List<*>)?.filterIsInstance<String>().orEmpty()
                )
            }
        }

    fun observeEntries(update: (List<StudyEntry>) -> Unit, failure: () -> Unit) =
        entries.orderBy("createdAt", Query.Direction.DESCENDING).addSnapshotListener { snapshot, error ->
            if (error != null) failure()
            else if (snapshot != null) {
                update(snapshot.documents.mapNotNull { doc ->
                    StudyEntry(
                        doc.id,
                        doc.getString("kind") ?: return@mapNotNull null,
                        doc.getString("question") ?: return@mapNotNull null,
                        doc.getString("content") ?: return@mapNotNull null,
                        doc.getString("subject").orEmpty(),
                        doc.getBoolean("bookmarked") ?: false,
                        doc.getLong("selection")?.toInt()
                    )
                })
            }
        }

    fun saveProfile(level: String, style: String, subjects: List<String>, failure: () -> Unit) {
        profile.set(mapOf("level" to level, "style" to style, "subjects" to subjects))
            .addOnFailureListener { failure() }
    }

    fun add(kind: String, question: String, content: String, subject: String, failure: () -> Unit): String {
        val doc = entries.document()
        doc.set(mapOf(
            "kind" to kind, "question" to question, "content" to content, "subject" to subject,
            "bookmarked" to false, "createdAt" to FieldValue.serverTimestamp()
        )).addOnFailureListener { failure() }
        return doc.id
    }

    fun bookmark(id: String, value: Boolean, failure: () -> Unit) {
        entries.document(id).update("bookmarked", value).addOnFailureListener { failure() }
    }

    fun select(id: String, selection: Int, failure: () -> Unit) {
        entries.document(id).update("selection", selection).addOnFailureListener { failure() }
    }
}
