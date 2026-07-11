package com.networth.tracker.data

data class ReturnMetrics(
    val investedInInr: Double,
    val currentInInr: Double,
    val returnInInr: Double,
    val returnPercent: Double
) {
    val hasReturnData: Boolean
        get() = investedInInr > 0
}

object ReturnCalculator {
    fun forAsset(asset: AssetEntity, usdToInrRate: Double): ReturnMetrics? {
        if (asset.category.isLiability) return null

        val investedInInr = AssetRepository.toInr(asset.investedAmount, asset.currency, usdToInrRate)
        val currentInInr = AssetRepository.toInr(asset.amount, asset.currency, usdToInrRate)
        val returnInInr = currentInInr - investedInInr
        val returnPercent = if (investedInInr > 0) {
            (returnInInr / investedInInr) * 100
        } else {
            0.0
        }

        return ReturnMetrics(
            investedInInr = investedInInr,
            currentInInr = currentInInr,
            returnInInr = returnInInr,
            returnPercent = returnPercent
        )
    }

    fun forAssets(assets: List<AssetEntity>, usdToInrRate: Double): ReturnMetrics {
        val assetEntries = assets.filter { !it.category.isLiability }
        val investedInInr = assetEntries.sumOf {
            AssetRepository.toInr(it.investedAmount, it.currency, usdToInrRate)
        }
        val currentInInr = assetEntries.sumOf {
            AssetRepository.toInr(it.amount, it.currency, usdToInrRate)
        }
        val returnInInr = currentInInr - investedInInr
        val returnPercent = if (investedInInr > 0) {
            (returnInInr / investedInInr) * 100
        } else {
            0.0
        }

        return ReturnMetrics(
            investedInInr = investedInInr,
            currentInInr = currentInInr,
            returnInInr = returnInInr,
            returnPercent = returnPercent
        )
    }
}
