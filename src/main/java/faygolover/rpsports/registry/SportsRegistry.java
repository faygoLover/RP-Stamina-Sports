package faygolover.rpsports.registry;

import faygolover.rpsports.RpSports;
import faygolover.rpsports.block.BenchBlock;
import faygolover.rpsports.block.BenchBlockEntity;
import faygolover.rpsports.block.MachineBlock;
import faygolover.rpsports.block.MultiBlockMachine;
import faygolover.rpsports.core.Exercise;
import faygolover.rpsports.entity.SeatEntity;
import faygolover.rpsports.item.DumbbellItem;
import faygolover.rpsports.item.PlateItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

/** Блоки (тренажёры), предметы (гантели, блины), сиденье, эффект «Боль в мышцах», вкладка творческого режима. */
public final class SportsRegistry {
    private SportsRegistry() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, RpSports.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, RpSports.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, RpSports.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, RpSports.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, RpSports.MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RpSports.MODID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5f, 6f).sound(SoundType.METAL).noOcclusion();
    }

    // Формы (коллизия) — коробки по модели, «на север»; у многоблочных — по части. Модели тоже коробки (решение
    // брата: настоящие — потом). Столбцы многоблочных идут на восток (вправо от ставившего), ряды — вверх.
    private static final VoxelShape BENCH_HEAD = Shapes.or(Block.box(2, 0, 4, 16, 8, 12),
            Block.box(12, 0, 3, 13, 16, 4), Block.box(12, 0, 12, 13, 16, 13));
    private static final VoxelShape BENCH_FOOT = Block.box(0, 0, 4, 13, 8, 12);
    private static final VoxelShape POST_L = Block.box(1, 0, 6, 3, 16, 10), POST_R = Block.box(13, 0, 6, 15, 16, 10);
    private static final VoxelShape TOP_L = Shapes.or(Block.box(1, 0, 6, 3, 8, 10), Block.box(3, 6, 7.5, 16, 7, 8.5));
    private static final VoxelShape TOP_R = Shapes.or(Block.box(13, 0, 6, 15, 8, 10), Block.box(0, 6, 7.5, 13, 7, 8.5));
    private static final VoxelShape BAG_LOW = Block.box(4, 11, 4, 12, 16, 12);
    private static final VoxelShape BAG_HIGH = Shapes.or(Block.box(4, 0, 4, 12, 12, 12), Block.box(7.5, 12, 7.5, 8.5, 16, 8.5));
    private static final VoxelShape TREADMILL_SHAPE = Block.box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape MAT_SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    /** Скамья 2 блока в длину (стойка с грифом + под ноги). */
    public static final RegistryObject<Block> BENCH = BLOCKS.register("bench", () -> new BenchBlock(metal(), BENCH_HEAD, BENCH_FOOT));
    /** Турник 2 в ширину и 3 в высоту, перекладина на ~2,4 блока; игрок висит посередине. */
    public static final RegistryObject<Block> PULLUP_BAR = BLOCKS.register("pullup_bar",
            () -> new MultiBlockMachine(metal(), Exercise.PULLUP, 2, 3, POST_L, POST_R, POST_L, POST_R, TOP_L, TOP_R));
    /** Груша 2 блока в высоту, висит в верхних двух третях. */
    public static final RegistryObject<Block> PUNCHING_BAG = BLOCKS.register("punching_bag",
            () -> new MultiBlockMachine(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1f).sound(SoundType.WOOL).noOcclusion(),
                    Exercise.BAG, 1, 2, BAG_LOW, BAG_HIGH));
    public static final RegistryObject<Block> TREADMILL = BLOCKS.register("treadmill", () -> new MachineBlock(metal(), TREADMILL_SHAPE, Exercise.TREADMILL));
    public static final RegistryObject<Block> GYM_MAT = BLOCKS.register("gym_mat",
            () -> new MachineBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.3f).sound(SoundType.WOOL).noOcclusion(),
                    MAT_SHAPE, null));

    public static final List<RegistryObject<Item>> DUMBBELLS = new ArrayList<>();
    public static final List<RegistryObject<Item>> PLATES = new ArrayList<>();

    static {
        for (Object[] b : new Object[][]{{BENCH, "bench"}, {PULLUP_BAR, "pullup_bar"}, {PUNCHING_BAG, "punching_bag"},
                {TREADMILL, "treadmill"}, {GYM_MAT, "gym_mat"}}) {
            @SuppressWarnings("unchecked") RegistryObject<Block> block = (RegistryObject<Block>) b[0];
            ITEMS.register((String) b[1], () -> new BlockItem(block.get(), new Item.Properties()));
        }
        for (int kg : new int[]{2, 5, 8, 12, 16, 20}) {
            DUMBBELLS.add(ITEMS.register("dumbbell_" + kg, () -> new DumbbellItem(kg, new Item.Properties().stacksTo(1))));
        }
        for (String[] p : new String[][]{{"2_5", "2.5"}, {"5", "5"}, {"10", "10"}, {"20", "20"}}) {
            double kg = Double.parseDouble(p[1]);
            PLATES.add(ITEMS.register("plate_" + p[0], () -> new PlateItem(kg, new Item.Properties().stacksTo(16))));
        }
    }

    public static final RegistryObject<BlockEntityType<BenchBlockEntity>> BENCH_BE = BLOCK_ENTITIES.register("bench",
            () -> BlockEntityType.Builder.of(BenchBlockEntity::new, BENCH.get()).build(null));

    public static final RegistryObject<EntityType<SeatEntity>> SEAT = ENTITIES.register("seat",
            () -> EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC).sized(0.01f, 0.01f)
                    .noSummon().fireImmune().clientTrackingRange(8).build("seat"));

    /** Видимый эффект «Боль в мышцах» — только без RP Medicine (с ней боль ведёт она, через ощущения). */
    public static final RegistryObject<MobEffect> MUSCLE_PAIN = EFFECTS.register("muscle_pain",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xB03A2E) {});

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rpsports"))
            .icon(() -> new ItemStack(DUMBBELLS.get(2).get()))
            .displayItems((params, out) -> {
                for (RegistryObject<Item> i : ITEMS.getEntries()) out.accept(i.get());
            })
            .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITIES.register(bus);
        EFFECTS.register(bus);
        TABS.register(bus);
    }
}
