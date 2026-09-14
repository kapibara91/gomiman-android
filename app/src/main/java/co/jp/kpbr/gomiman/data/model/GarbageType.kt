package co.jp.kpbr.gomiman.data.model

enum class GarbageType(
    val id: Int,
    val typeName: String,
    val shortName: String
) {
    BURNABLE(1, "可燃", "可燃"),
    NON_BURNABLE(2, "不燃", "不燃"),
    GLASS_BOTTLE(3, "びん", "びん"),
    CAN(4, "かん", "かん"),
    PLASTIC(5, "プラスチック", "プラ"),
    PAPER(6, "古紙", "古紙"),
    PET_BOTTLE(7, "ペットボトル", "ボトル");

    companion object {
        fun fromId(id: Int): GarbageType? = entries.find { it.id == id }

        val allTypes: List<GarbageType> = entries.toList()
    }
}
