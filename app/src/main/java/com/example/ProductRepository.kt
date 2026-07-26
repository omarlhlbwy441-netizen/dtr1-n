package com.example

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for querying and filtering products from Firestore (`products` collection)
 */
class ProductRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Real-time stream of Products from Firestore collection `products`
     * Supports search filtering by query text and selected category
     */
    fun observeProducts(
        queryText: String = "",
        categoryFilter: String = "الكل"
    ): Flow<List<Product>> = callbackFlow {
        val collectionRef = firestore.collection("products")

        val listener = collectionRef
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Fallback to sample products filtered if Firestore error occurs or permissions missing
                    val samples = Product.getSampleProducts()
                    trySend(filterProductList(samples, queryText, categoryFilter))
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val products = snapshot.documents.mapNotNull { doc ->
                        doc.data?.let { map -> Product.fromMap(doc.id, map) }
                    }
                    trySend(filterProductList(products, queryText, categoryFilter))
                } else {
                    // Collection empty, return sample products and seed Firestore asynchronously
                    val samples = Product.getSampleProducts()
                    trySend(filterProductList(samples, queryText, categoryFilter))
                }
            }

        awaitClose { listener.remove() }
    }

    /**
     * Seeds initial demo products into Firestore if the `products` collection is empty
     */
    suspend fun seedInitialProductsIfEmpty(): Result<Unit> {
        return try {
            val snapshot = firestore.collection("products").limit(1).get().await()
            if (snapshot.isEmpty) {
                val samples = Product.getSampleProducts()
                val batch = firestore.batch()
                for (prod in samples) {
                    val docRef = firestore.collection("products").document(prod.id)
                    batch.set(docRef, prod.toMap())
                }
                batch.commit().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Adds a new product to the Firestore `products` collection
     */
    suspend fun addProduct(product: Product): Result<String> {
        return try {
            val docId = if (product.id.isBlank()) UUID.randomUUID().toString() else product.id
            val finalProduct = product.copy(id = docId, createdAt = System.currentTimeMillis())
            firestore.collection("products")
                .document(docId)
                .set(finalProduct.toMap())
                .await()
            Result.success(docId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun filterProductList(
        list: List<Product>,
        queryText: String,
        categoryFilter: String
    ): List<Product> {
        return list.filter { product ->
            val matchesCategory = categoryFilter == "الكل" || product.category.equals(categoryFilter, ignoreCase = true)
            
            val query = queryText.trim().lowercase()
            val matchesSearch = query.isBlank() ||
                    product.name.lowercase().contains(query) ||
                    product.description.lowercase().contains(query) ||
                    product.storeName.lowercase().contains(query) ||
                    product.category.lowercase().contains(query)

            matchesCategory && matchesSearch
        }
    }
}
