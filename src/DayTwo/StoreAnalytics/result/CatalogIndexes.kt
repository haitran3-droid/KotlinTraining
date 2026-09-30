package DayTwo.StoreAnalytics.result

import DayTwo.StoreAnalytics.model.Product

data class CatalogIndexes(
    val byId: Map<String, Product>,
    val nameById: Map<String, String>,
    val tagCountByProduct: Map<Product, Int>
)
