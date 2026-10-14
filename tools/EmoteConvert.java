import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import io.github.kosmx.emotes.server.serializer.type.BinaryFormat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

/**
 * Перевод эмоций Emotecraft (.emotecraft, двоичный формат) в JSON, который player-animation-lib грузит из ресурсов
 * мода ({@code assets/rpsports/player_animation/*.json}). Аддону Emotecraft не нужен — только при разработке.
 *
 * <pre>
 * java -cp "emotecraft.jar;player-animation-lib.jar;gson.jar" tools/EmoteConvert.java вход.emotecraft выход.json [--info|--dump]
 * </pre>
 * {@code --info} — длина, повтор, какие части двигаются; {@code --dump} — ещё и все ключевые кадры по частям.
 */
public class EmoteConvert {
    public static void main(String[] args) throws Exception {
        Path in = Path.of(args[0]), out = Path.of(args[1]);
        List<KeyframeAnimation> list;
        try (InputStream s = new FileInputStream(in.toFile())) {
            list = new BinaryFormat().read(s, in.getFileName().toString());
        }
        KeyframeAnimation a = list.get(0);
        String mode = args.length > 2 ? args[2] : "";
        if (mode.equals("--info") || mode.equals("--dump")) {
            System.out.printf("%s: begin %d, end %d, stop %d, loop %s from %d, easeBefore %s%n", a.extraData.get("name"),
                    a.beginTick, a.endTick, a.stopTick, a.isInfinite, a.returnToTick, a.isEasingBefore);
            a.getBodyParts().forEach((name, part) -> {
                if (!part.isEnabled()) return;
                System.out.println("  " + name + (part.isBendable ? " (bend)" : ""));
                if (!mode.equals("--dump")) return;
                for (KeyframeAnimation.StateCollection.State st : new KeyframeAnimation.StateCollection.State[]{
                        part.x, part.y, part.z, part.pitch, part.yaw, part.roll, part.bend, part.bendDirection}) {
                    if (st == null || !st.isEnabled() || st.getKeyFrames().isEmpty()) continue;
                    StringBuilder b = new StringBuilder("    " + st.name + " [def " + fmt(st.defaultValue) + "]:");
                    TreeMap<Integer, Float> frames = new TreeMap<>();
                    for (KeyframeAnimation.KeyFrame k : st.getKeyFrames()) frames.put(k.tick, k.value);
                    frames.forEach((t, v) -> b.append(' ').append(t).append('=').append(fmt(v)));
                    System.out.println(b);
                }
            });
        }
        Files.writeString(out, AnimationSerializing.serializeAnimation(a), StandardCharsets.UTF_8);
        System.out.println("ok: " + out);
    }

    private static String fmt(float v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }
}
