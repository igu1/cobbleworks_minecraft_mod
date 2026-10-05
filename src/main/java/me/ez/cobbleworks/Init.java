package me.ez.cobbleworks;

import me.ez.cobbleworks.common.GeneratorBlock;
import me.ez.cobbleworks.common.GeneratorBlockEntity;
import me.ez.cobbleworks.common.GeneratorMenu;
import me.ez.cobbleworks.common.MachinePhase;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Init {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Main.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Main.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Main.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Main.MOD_ID);

    public static final DeferredBlock<GeneratorBlock> GENERATOR = BLOCKS.registerBlock("cobble_generator",
            GeneratorBlock::new, p -> p.strength(3.5f, 6).sound(SoundType.METAL).noOcclusion()
                    .requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK)
                    .lightLevel(s -> s.getValue(GeneratorBlock.PHASE) == MachinePhase.RUNNING ? 7 : 0));
    public static final DeferredItem<BlockItem> GENERATOR_ITEM = ITEMS.registerSimpleBlockItem("cobble_generator", GENERATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR_BE =
            BLOCK_ENTITIES.register("cobble_generator", () -> new BlockEntityType<>(GeneratorBlockEntity::new, GENERATOR.get()));
    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> GENERATOR_MENU =
            MENUS.register("cobble_generator", () -> new MenuType<>(GeneratorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private Init() {}

    public static void creativeContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.FUNCTIONAL_BLOCKS)) event.accept(GENERATOR_ITEM.get());
    }

    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.Item.BLOCK, GENERATOR_BE.get(),
                (be, side) -> be.handler(side == null ? Direction.DOWN : side));
    }
}
