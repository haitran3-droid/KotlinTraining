package DayTwo.StoreAnalytics.result

import DayTwo.StoreAnalytics.model.Product

data class PriceBands(
    val affordable: List<Product>,
    val expensive: List<Product>
)
