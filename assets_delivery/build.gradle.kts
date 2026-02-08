plugins {
    alias(libs.plugins.android.asset.pack)
}

assetPack {
    packName = "assets_delivery"
    dynamicDelivery {
        deliveryType = "on-demand"
    }
}
