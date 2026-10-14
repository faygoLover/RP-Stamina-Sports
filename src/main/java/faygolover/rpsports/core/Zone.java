package faygolover.rpsports.core;

import faygolover.rpcore.api.RpIds;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Зоны мышц (docs/00 п. 1): то, что устаёт и ограничивает упражнения. Туда же попадают боль и травмы (RP Medicine).
 * «Сердца» нет — кардио ограничивает сама выносливость.
 */
public enum Zone {
    LEFT_ARM("left_arm", List.of(RpIds.PART_LEFT_ARM)),
    RIGHT_ARM("right_arm", List.of(RpIds.PART_RIGHT_ARM)),
    CHEST("chest", List.of(RpIds.PART_CHEST)),
    ABS("abs", List.of(RpIds.PART_ABDOMEN)),
    LEGS("legs", List.of(RpIds.PART_LEFT_LEG, RpIds.PART_RIGHT_LEG));

    public static final Zone[] VALUES = values();

    public final String id;
    /** Части тела RP Core для мода тела: боль, растяжения, «можно ли нагружать». */
    public final List<ResourceLocation> parts;

    Zone(String id, List<ResourceLocation> parts) {
        this.id = id;
        this.parts = parts;
    }

    /** Ключ перевода: «руки», «грудь и спина»… (для ощущений). */
    public String key() {
        return "rpsports.zone." + id;
    }
}
