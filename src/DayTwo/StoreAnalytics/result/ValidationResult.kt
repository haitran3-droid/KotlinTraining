package DayTwo.StoreAnalytics.result

data class ValidationResult(
    val hasEmptyOrder: Boolean,
    val allItemsValid: Boolean,
    val hasNoUnknownCompletedCustomer: Boolean
)
