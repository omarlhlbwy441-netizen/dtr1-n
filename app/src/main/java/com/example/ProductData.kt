package com.example

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

/**
 * Data model representing a Product document in Firestore (`products` collection)
 */
@IgnoreExtraProperties
data class Product(
    @get:PropertyName("id") @set:PropertyName("id") var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name") var name: String = "",
    @get:PropertyName("category") @set:PropertyName("category") var category: String = "إلكترونيات",
    @get:PropertyName("price") @set:PropertyName("price") var price: Double = 0.0,
    @get:PropertyName("currency") @set:PropertyName("currency") var currency: String = "SAR",
    @get:PropertyName("description") @set:PropertyName("description") var description: String = "",
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String = "",
    @get:PropertyName("storeName") @set:PropertyName("storeName") var storeName: String = "متجر رفيق",
    @get:PropertyName("rating") @set:PropertyName("rating") var rating: Double = 4.8,
    @get:PropertyName("inStock") @set:PropertyName("inStock") var inStock: Boolean = true,
    @get:PropertyName("discountPercent") @set:PropertyName("discountPercent") var discountPercent: Int = 0,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") var createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "category" to category,
            "price" to price,
            "currency" to currency,
            "description" to description,
            "imageUrl" to imageUrl,
            "storeName" to storeName,
            "rating" to rating,
            "inStock" to inStock,
            "discountPercent" to discountPercent,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(docId: String, map: Map<String, Any?>): Product {
            return Product(
                id = docId.ifBlank { map["id"] as? String ?: "" },
                name = map["name"] as? String ?: "",
                category = map["category"] as? String ?: "إلكترونيات",
                price = (map["price"] as? Number)?.toDouble() ?: 0.0,
                currency = map["currency"] as? String ?: "SAR",
                description = map["description"] as? String ?: "",
                imageUrl = map["imageUrl"] as? String ?: "",
                storeName = map["storeName"] as? String ?: "متجر رفيق",
                rating = (map["rating"] as? Number)?.toDouble() ?: 4.5,
                inStock = map["inStock"] as? Boolean ?: true,
                discountPercent = (map["discountPercent"] as? Number)?.toInt() ?: 0,
                createdAt = (map["createdAt"] as? Long) ?: System.currentTimeMillis()
            )
        }

        fun getSampleProducts(): List<Product> {
            val now = System.currentTimeMillis()
            return listOf(
                Product(
                    id = "p1",
                    name = "خنجر الرفيق الملكي الأصيل",
                    category = "ساعات واكسسوارات",
                    price = 350.00,
                    currency = "SAR",
                    description = "خنجر ملكي مصنوع يدويًا بتفاصيل من الفضة الخالصة ومقبض فاخر من العاج العصري.",
                    imageUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500&auto=format&fit=crop&q=80",
                    storeName = "متجر النخبة الملكي",
                    rating = 4.9,
                    inStock = true,
                    discountPercent = 10,
                    createdAt = now - 100000
                ),
                Product(
                    id = "p2",
                    name = "ساعة الرفيق الذكية Pro Max",
                    category = "إلكترونيات",
                    price = 499.00,
                    currency = "SAR",
                    description = "ساعة ذكية بشاشة AMOLED مقاومة للمياه مع تتبع نبضات القلب والأنشطة الرياضية بدقة متناهية.",
                    imageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=500&auto=format&fit=crop&q=80",
                    storeName = "عالم التقنية الرقمية",
                    rating = 4.8,
                    inStock = true,
                    discountPercent = 15,
                    createdAt = now - 200000
                ),
                Product(
                    id = "p3",
                    name = "عطر اللافندر والمسك الملكي 100ml",
                    category = "عطور ومستحضرات",
                    price = 180.00,
                    currency = "SAR",
                    description = "تركيبة عطرية فاخرة تدوم طويلاً بمزيج من أزهار اللافندر والمسك الأبيض الأصيل.",
                    imageUrl = "https://images.unsplash.com/photo-1541643600914-78b084683601?w=500&auto=format&fit=crop&q=80",
                    storeName = "دار العطور الملكية",
                    rating = 4.7,
                    inStock = true,
                    discountPercent = 0,
                    createdAt = now - 300000
                ),
                Product(
                    id = "p4",
                    name = "سماعات الرفيق اللاسلكية ANC Noise-Canceling",
                    category = "إلكترونيات",
                    price = 299.00,
                    currency = "SAR",
                    description = "عزل صوتي فعال حتى 35dB وصوت محيطي Hi-Res مع بطارية تدوم حتى 40 ساعة متواصلة.",
                    imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80",
                    storeName = "متجر الصوتيات الذهبي",
                    rating = 4.9,
                    inStock = true,
                    discountPercent = 20,
                    createdAt = now - 400000
                ),
                Product(
                    id = "p5",
                    name = "حقيبة جلدية فاخرة للأعمال",
                    category = "ملابس وأزياء",
                    price = 240.00,
                    currency = "SAR",
                    description = "مصنوعة من الجلد الطبيعي 100% مع مساحة تتسع لجهاز حاسوب محمول مقاس 15.6 بوصة.",
                    imageUrl = "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=500&auto=format&fit=crop&q=80",
                    storeName = "متجر الجلود الأنيقة",
                    rating = 4.6,
                    inStock = true,
                    discountPercent = 5,
                    createdAt = now - 500000
                ),
                Product(
                    id = "p6",
                    name = "نظارة شمسية كلاسيكية قطبية Polarized",
                    category = "ساعات واكسسوارات",
                    price = 150.00,
                    currency = "SAR",
                    description = "عدسات قطبية حامية من الأشعة فوق البنفسجية UV400 بإطار خفيف المتانة للغاية.",
                    imageUrl = "https://images.unsplash.com/photo-1511499767150-a48a237f0083?w=500&auto=format&fit=crop&q=80",
                    storeName = "بوتيك البصريات",
                    rating = 4.5,
                    inStock = false,
                    discountPercent = 0,
                    createdAt = now - 600000
                ),
                Product(
                    id = "p7",
                    name = "اشتراك VIP سنوي للشركات والمتاجر",
                    category = "خدمات رقمية",
                    price = 799.00,
                    currency = "SAR",
                    description = "تفعيل حزمتك التسويقية، شارة التوثيق الذهبية، وإمكانية إضافة 50 فرع متجر في تطبيق رفيق.",
                    imageUrl = "https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=500&auto=format&fit=crop&q=80",
                    storeName = "منصة رفيق الرسمية",
                    rating = 5.0,
                    inStock = true,
                    discountPercent = 25,
                    createdAt = now - 700000
                )
            )
        }
    }
}
