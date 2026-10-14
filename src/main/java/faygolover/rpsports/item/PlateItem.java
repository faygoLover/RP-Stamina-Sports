package faygolover.rpsports.item;

import faygolover.rpsports.registry.SportsRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

/** Блин для штанги: вешается парой на скамью. */
public class PlateItem extends Item {
    private final double kg;

    public PlateItem(double kg, Properties props) {
        super(props);
        this.kg = kg;
    }

    public double kg() {
        return kg;
    }

    public static String format(double kg) {
        return Math.abs(kg - Math.rint(kg)) < 1e-6 ? String.valueOf((int) Math.rint(kg)) : String.format(Locale.ROOT, "%.1f", kg);
    }

    /** Стопка блинов этого веса (пусто — такого блина нет). */
    public static ItemStack stackFor(double kg, int count) {
        for (RegistryObject<Item> r : SportsRegistry.PLATES) {
            if (r.get() instanceof PlateItem p && Math.abs(p.kg - kg) < 1e-6) return new ItemStack(p, count);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("rpsports.tip.plate", format(kg)).withStyle(ChatFormatting.GRAY));
    }
}
