package com.halilibrahim.halilibrahimmmo.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The wrapping rules {@code LoreWrapper} is expected to keep, written against
 * the two failures Faz 24 fixed rather than against its implementation.
 */
class LoreWrapperTest {

    private static final int MAXIMUM = 42;

    private static String plain(Component component) {
        return PlainTextComponentSerializer.plainText().serialize(component);
    }

    @Test
    @DisplayName("A line inside the budget is returned untouched")
    void shortLineIsNotWrapped() {
        Component line = Component.text("Kısa bir açıklama.");
        List<Component> wrapped = LoreWrapper.wrap(line);
        assertEquals(1, wrapped.size());
        assertEquals(line, wrapped.get(0));
    }

    @Test
    @DisplayName("No produced line exceeds the budget")
    void everyLineFitsTheBudget() {
        Component line = Component.text(
                "Çevrendeki yedi blok yarıçapındaki bütün olmamış ekinler kademe kademe, "
                        + "dalga dalga büyür ve hiç kemik tozu harcamazsın.");
        for (Component wrapped : LoreWrapper.wrap(line)) {
            assertTrue(plain(wrapped).length() <= MAXIMUM,
                    "Satır bütçeyi aştı: " + plain(wrapped));
        }
    }

    @Test
    @DisplayName("Wrapping loses no word and introduces none")
    void wrappingPreservesEveryWord() {
        String source = "Bekleme süresini kısaltır ve etkisini arttırır; her rank bir kademe ekler.";
        List<Component> wrapped = LoreWrapper.wrap(Component.text(source));
        String rejoined = String.join(" ", wrapped.stream().map(LoreWrapperTest::plain).toList());
        assertEquals(List.of(source.split(" +")), List.of(rejoined.split(" +")));
    }

    /**
     * The Faz 24 fix. The break used to be searched for inside whichever styled
     * fragment happened to overflow, so a fragment carrying no space of its own
     * was cut straight through a word even though the line had plenty of
     * earlier break opportunities.
     */
    @Test
    @DisplayName("A break lands between words even when it falls inside a styled fragment")
    void breaksAtWordBoundariesAcrossFragments() {
        Component line = Component.text("Bekleme süresi: ", NamedTextColor.GRAY)
                .append(Component.text("yedi dakika otuz saniye sürer ve azalır", NamedTextColor.WHITE));
        List<Component> wrapped = LoreWrapper.wrap(line);
        assertTrue(wrapped.size() > 1, "Bu satır sarılmalıydı.");
        for (Component produced : wrapped) {
            String text = plain(produced);
            assertTrue(text.equals(text.strip()), "Satır boşlukla başlıyor ya da bitiyor: '" + text + "'");
        }
        // Every word from the source survives whole - a mid-word cut would show
        // up here as two fragments that are not words of the original.
        String rejoined = String.join(" ", wrapped.stream().map(LoreWrapperTest::plain).toList());
        assertEquals("Bekleme süresi: yedi dakika otuz saniye sürer ve azalır", rejoined);
    }

    @Test
    @DisplayName("A single word longer than the budget is still cut, rather than running away")
    void oversizedWordIsHardCut() {
        String monster = "A".repeat(MAXIMUM * 2 + 5);
        List<Component> wrapped = LoreWrapper.wrap(Component.text(monster));
        assertEquals(3, wrapped.size());
        for (Component produced : wrapped) {
            assertTrue(plain(produced).length() <= MAXIMUM);
        }
    }

    @Test
    @DisplayName("A wrapped fragment keeps the colour it was given")
    void colourSurvivesTheBreak() {
        Component line = Component.text("Bu gri bir giriş cümlesidir ve uzundur. ", NamedTextColor.GRAY)
                .append(Component.text("Bu kısım kırmızıdır ve ayrı durmalıdır.", NamedTextColor.RED));
        List<Component> wrapped = LoreWrapper.wrap(line);
        boolean sawRed = wrapped.stream()
                .flatMap(component -> component.children().stream())
                .anyMatch(child -> NamedTextColor.RED.equals(child.color()));
        assertTrue(sawRed, "Kırmızı parça rengini kaybetti.");
    }
}
