package faygolover.rpsports.item;

import faygolover.rpsports.server.SportsService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Гантель: ПКМ — начать подъёмы на бицепс той рукой, в которой она. Ходить можно. */
public class DumbbellItem extends Item {
    private final int kg;

    public DumbbellItem(int kg, Properties props) {
        super(props);
        this.kg = kg;
    }

    public int kg() {
        return kg;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer sp) SportsService.useDumbbell(sp, hand, kg);
        return InteractionResultHolder.consume(st);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("rpsports.tip.dumbbell", kg).withStyle(ChatFormatting.GRAY));
    }
}
