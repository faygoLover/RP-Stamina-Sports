import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import io.github.kosmx.emotes.server.serializer.type.BinaryFormat;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Перевод эмоций Emotecraft (.emotecraft, двоичный формат) в JSON, который player-animation-lib грузит из ресурсов
 * мода ({@code assets/rpsports/player_animation/*.json}). Аддону Emotecraft не нужен — только при разработке.
 *
 * <pre>
 * java -cp "emotecraft.jar;player-animation-lib.jar;gson.jar" tools/EmoteConvert.java вход.emotecraft выход.json [--info]
 * </pre>
 * {@code --info} — напечатать длину, начало и конец повтора, какие части тела двигаются.
 */
public class EmoteConvert {
    public static void main(String[] args) throws Exception {
        Path in = Path.of(args[0]), out = Path.of(args[1]);
        List<KeyframeAnimation> list;
        try (InputStream s = new FileInputStream(in.toFile())) {
            list = new BinaryFormat().read(s, in.getFileName().toString());
        }
        KeyframeAnimation a = list.get(0);
        if (args.length > 2 && args[2].equals("--info")) {
            System.out.printf("ticks: begin %d, end %d, stop %d, loop %s from %d%n", a.beginTick, a.endTick, a.stopTick,
                    a.isInfinite, a.returnToTick);
            System.out.println("name: " + a.extraData.get("name"));
            a.getBodyParts().forEach((name, part) -> {
                if (part.isEnabled()) System.out.println("  part: " + name);
            });
        }
        Files.writeString(out, AnimationSerializing.serializeAnimation(a), StandardCharsets.UTF_8);
        System.out.println("ok: " + out);
    }
}
