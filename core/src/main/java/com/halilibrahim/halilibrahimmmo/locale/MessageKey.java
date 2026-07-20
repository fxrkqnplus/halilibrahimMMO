package com.halilibrahim.halilibrahimmmo.locale;

/**
 * Every user-facing message HalilIbrahimMMO can send, mapped to its dotted path in
 * {@code messages.yml}. Adding a new player-facing string anywhere in the plugin
 * always means adding a new constant here first - raw literal strings are never
 * sent directly to a player.
 */
public enum MessageKey {

    PREFIX("onek"),

    NO_PERMISSION("genel.yetkisiz"),
    PLAYER_ONLY("genel.sadece-oyuncu"),

    RELOAD_SUCCESS("rpg.yenile-basarili"),
    RELOAD_FAILURE("rpg.yenile-basarisiz"),
    VERSION_INFO("rpg.versiyon"),
    DEBUG_ENABLED("rpg.debug-acildi"),
    DEBUG_DISABLED("rpg.debug-kapatildi"),
    UNKNOWN_SUBCOMMAND("rpg.bilinmeyen-alt-komut"),

    PROFILE_OVERVIEW("profil.ozet"),
    PROFILE_OVERVIEW_NO_CLASS("profil.ozet-sinifsiz"),

    ENERGY_STATUS("enerji.durum"),
    ABILITY_LIST("kabiliyet.liste"),
    ABILITY_UNKNOWN("kabiliyet.bilinmeyen"),
    ABILITY_LOCKED("kabiliyet.kilitli"),
    ABILITY_ENERGY("kabiliyet.yetersiz-enerji"),
    ABILITY_COOLDOWN("kabiliyet.bekleme"),
    ABILITY_ARMED("kabiliyet.kuruldu"),
    ABILITY_ACTIVE("kabiliyet.etkin"),
    ABILITY_TRIGGERED("kabiliyet.tetiklendi"),

    CLASS_USAGE("sinif.kullanim"),
    CLASS_LIST("sinif.liste"),
    CLASS_SELECTED("sinif.secildi"),
    CLASS_ALREADY_SELECTED("sinif.zaten-secili"),
    CLASS_UNKNOWN("sinif.bilinmeyen"),
    CLASS_COOLDOWN("sinif.bekleme"),
    CLASS_NO_SELECTION("sinif.secim-yok"),
    CLASS_TREE("sinif.agac"),
    CLASS_TALENT_SPENT("sinif.yetenek-acildi"),
    CLASS_UNKNOWN_TALENT("sinif.bilinmeyen-yetenek"),
    CLASS_TALENT_MAXIMUM("sinif.maksimum-rank"),
    CLASS_NO_POINTS("sinif.puan-yok"),

    PRESTIGE_STATUS("prestij.durum"),
    PRESTIGE_SUCCESS("prestij.basarili"),
    PRESTIGE_LEVEL_REQUIRED("prestij.seviye-gerekli"),
    PRESTIGE_MAXIMUM("prestij.maksimum"),

    QUEST_LIST("gorev.liste"),
    QUEST_COMPLETED("gorev.tamamlandi"),

    COLLECTION_LIST("koleksiyon.liste"),
    COLLECTION_DETAIL("koleksiyon.detay"),
    COLLECTION_UNKNOWN("koleksiyon.bilinmeyen"),
    COLLECTION_ENTRY_OBTAINED("koleksiyon.oge-kazanildi"),
    COLLECTION_COMPLETED("koleksiyon.tamamlandi"),

    JOURNAL_LIST("gunluk.liste"),
    JOURNAL_EMPTY("gunluk.bos"),

    LEADERBOARD_USAGE("liderlik.kullanim"),
    LEADERBOARD_UNKNOWN("liderlik.bilinmeyen"),
    LEADERBOARD_EMPTY("liderlik.bos"),
    LEADERBOARD_LEVEL_LIST("liderlik.seviye-liste"),
    LEADERBOARD_PRESTIGE_LIST("liderlik.prestij-liste"),
    LEADERBOARD_QUEST_LIST("liderlik.gorev-liste"),
    LEADERBOARD_COLLECTION_LIST("liderlik.koleksiyon-liste"),
    LEADERBOARD_SKILL_LIST("liderlik.yetenek-liste"),

    TITLE_LIST("unvan.liste"),
    TITLE_EQUIPPED("unvan.kusanildi"),
    TITLE_ALREADY_EQUIPPED("unvan.zaten-kusanik"),
    TITLE_NOT_UNLOCKED("unvan.acilmadi"),
    TITLE_UNKNOWN("unvan.bilinmeyen"),
    TITLE_UNLOCKED("unvan.kazanildi"),
    TITLE_CHAT_PREFIX("unvan.sohbet-oneki"),

    COSMETIC_LIST("kozmetik.liste"),
    COSMETIC_EQUIPPED("kozmetik.kusanildi"),
    COSMETIC_ALREADY_EQUIPPED("kozmetik.zaten-kusanik"),
    COSMETIC_NOT_UNLOCKED("kozmetik.acilmadi"),
    COSMETIC_UNKNOWN("kozmetik.bilinmeyen"),
    COSMETIC_UNLOCKED("kozmetik.kazanildi"),

    FEEDBACK_LEVEL_UP_BOSSBAR("geribildirim.seviye-bossbar"),
    FEEDBACK_LEVEL_UP_TITLE("geribildirim.seviye-baslik"),
    FEEDBACK_LEVEL_UP_SUBTITLE("geribildirim.seviye-altbaslik"),
    FEEDBACK_QUEST_BOSSBAR("geribildirim.gorev-bossbar"),
    FEEDBACK_QUEST_TITLE("geribildirim.gorev-baslik"),
    FEEDBACK_QUEST_SUBTITLE("geribildirim.gorev-altbaslik"),
    FEEDBACK_COLLECTION_BOSSBAR("geribildirim.koleksiyon-bossbar"),
    FEEDBACK_COLLECTION_TITLE("geribildirim.koleksiyon-baslik"),
    FEEDBACK_COLLECTION_SUBTITLE("geribildirim.koleksiyon-altbaslik"),
    FEEDBACK_PRESTIGE_BOSSBAR("geribildirim.prestij-bossbar"),
    FEEDBACK_PRESTIGE_TITLE("geribildirim.prestij-baslik"),
    FEEDBACK_PRESTIGE_SUBTITLE("geribildirim.prestij-altbaslik"),

    GUI_BACK("gui.geri"),

    GUI_MAIN_TITLE("gui.ana.baslik"),
    GUI_MAIN_PROFILE("gui.ana.profil"),
    GUI_MAIN_SKILLS("gui.ana.yetenekler"),
    GUI_MAIN_ABILITIES("gui.ana.kabiliyetler"),
    GUI_MAIN_CLASS("gui.ana.sinif"),
    GUI_MAIN_PRESTIGE("gui.ana.prestij"),
    GUI_MAIN_QUESTS("gui.ana.gorevler"),
    GUI_MAIN_COLLECTIONS("gui.ana.koleksiyonlar"),
    GUI_MAIN_JOURNAL("gui.ana.gunluk"),
    GUI_MAIN_LEADERBOARDS("gui.ana.liderlik"),
    GUI_MAIN_TITLES("gui.ana.unvan"),
    GUI_MAIN_COSMETICS("gui.ana.kozmetik"),

    GUI_PROFILE_TITLE("gui.profil.baslik"),
    GUI_PROFILE_ITEM("gui.profil.oge"),
    GUI_PROFILE_ITEM_LORE("gui.profil.oge-aciklama"),
    GUI_PROFILE_ITEM_LORE_NO_CLASS("gui.profil.oge-aciklama-sinifsiz"),

    GUI_SKILLS_TITLE("gui.yetenekler.baslik"),
    GUI_SKILL_ITEM("gui.yetenekler.oge"),
    GUI_SKILL_ITEM_LORE("gui.yetenekler.oge-aciklama"),

    GUI_ABILITIES_TITLE("gui.kabiliyetler.baslik"),
    GUI_ABILITY_ITEM("gui.kabiliyetler.oge"),
    GUI_ABILITY_ITEM_LORE("gui.kabiliyetler.oge-aciklama"),

    GUI_CLASS_TITLE("gui.sinif.baslik"),
    GUI_CLASS_ITEM("gui.sinif.oge"),
    GUI_CLASS_ITEM_LORE_SELECTED("gui.sinif.oge-aciklama-secili"),
    GUI_CLASS_ITEM_LORE_UNSELECTED("gui.sinif.oge-aciklama-secilmemis"),
    GUI_TALENT_ITEM("gui.sinif.yetenek-oge"),
    GUI_TALENT_ITEM_LORE("gui.sinif.yetenek-aciklama"),
    GUI_CLASS_INFO_ITEM("gui.sinif.bilgi-oge"),
    GUI_CLASS_INFO_ITEM_LORE("gui.sinif.bilgi-aciklama"),

    GUI_PRESTIGE_TITLE("gui.prestij.baslik"),
    GUI_PRESTIGE_INFO_ITEM("gui.prestij.bilgi-oge"),
    GUI_PRESTIGE_INFO_ITEM_LORE("gui.prestij.bilgi-aciklama"),
    GUI_PRESTIGE_CONFIRM_ITEM("gui.prestij.onayla-oge"),
    GUI_PRESTIGE_CONFIRM_ITEM_LORE("gui.prestij.onayla-aciklama"),

    GUI_QUESTS_TITLE("gui.gorevler.baslik"),
    GUI_QUEST_ITEM("gui.gorevler.oge"),
    GUI_QUEST_ITEM_LORE("gui.gorevler.oge-aciklama"),

    GUI_COLLECTIONS_TITLE("gui.koleksiyonlar.baslik"),
    GUI_COLLECTION_ITEM("gui.koleksiyonlar.oge"),
    GUI_COLLECTION_ITEM_LORE("gui.koleksiyonlar.oge-aciklama"),
    GUI_COLLECTION_DETAIL_TITLE("gui.koleksiyonlar.detay-baslik"),
    GUI_COLLECTION_ENTRY_ITEM("gui.koleksiyonlar.detay-oge"),
    GUI_COLLECTION_ENTRY_LORE_OBTAINED("gui.koleksiyonlar.oge-aciklama-kazanildi"),
    GUI_COLLECTION_ENTRY_LORE_MISSING("gui.koleksiyonlar.oge-aciklama-eksik"),

    GUI_JOURNAL_TITLE("gui.gunluk.baslik"),
    GUI_JOURNAL_ITEM("gui.gunluk.oge"),
    GUI_JOURNAL_ITEM_LORE("gui.gunluk.oge-aciklama"),
    GUI_JOURNAL_EMPTY_ITEM("gui.gunluk.bos-oge"),

    GUI_LEADERBOARDS_TITLE("gui.liderlik.baslik"),
    GUI_LEADERBOARD_CATEGORY_LEVEL("gui.liderlik.seviye"),
    GUI_LEADERBOARD_CATEGORY_PRESTIGE("gui.liderlik.prestij"),
    GUI_LEADERBOARD_CATEGORY_QUESTS("gui.liderlik.gorev"),
    GUI_LEADERBOARD_CATEGORY_COLLECTIONS("gui.liderlik.koleksiyon"),
    GUI_LEADERBOARD_SKILL_ITEM("gui.liderlik.yetenek-oge"),
    GUI_LEADERBOARD_DETAIL_TITLE_LEVEL("gui.liderlik.detay-baslik-seviye"),
    GUI_LEADERBOARD_DETAIL_TITLE_PRESTIGE("gui.liderlik.detay-baslik-prestij"),
    GUI_LEADERBOARD_DETAIL_TITLE_QUESTS("gui.liderlik.detay-baslik-gorev"),
    GUI_LEADERBOARD_DETAIL_TITLE_COLLECTIONS("gui.liderlik.detay-baslik-koleksiyon"),
    GUI_LEADERBOARD_DETAIL_TITLE_SKILL("gui.liderlik.detay-baslik-yetenek"),
    GUI_LEADERBOARD_ENTRY_ITEM("gui.liderlik.detay-oge"),
    GUI_LEADERBOARD_ENTRY_LEVEL_LORE("gui.liderlik.oge-seviye"),
    GUI_LEADERBOARD_ENTRY_PRESTIGE_LORE("gui.liderlik.oge-prestij"),
    GUI_LEADERBOARD_ENTRY_QUEST_LORE("gui.liderlik.oge-gorev"),
    GUI_LEADERBOARD_ENTRY_COLLECTION_LORE("gui.liderlik.oge-koleksiyon"),
    GUI_LEADERBOARD_EMPTY_ITEM("gui.liderlik.bos-oge"),

    GUI_TITLES_TITLE("gui.unvan.baslik"),
    GUI_TITLE_ITEM("gui.unvan.oge"),
    GUI_TITLE_ITEM_LORE_UNLOCKED("gui.unvan.oge-aciklama-acik"),
    GUI_TITLE_ITEM_LORE_LOCKED("gui.unvan.oge-aciklama-kilitli"),

    GUI_COSMETICS_TITLE("gui.kozmetik.baslik"),
    GUI_COSMETIC_ITEM("gui.kozmetik.oge"),
    GUI_COSMETIC_ITEM_LORE_UNLOCKED("gui.kozmetik.oge-aciklama-acik"),
    GUI_COSMETIC_ITEM_LORE_LOCKED("gui.kozmetik.oge-aciklama-kilitli");

    private final String path;

    MessageKey(String path) {
        this.path = path;
    }

    public String path() {
        return path;
    }
}
