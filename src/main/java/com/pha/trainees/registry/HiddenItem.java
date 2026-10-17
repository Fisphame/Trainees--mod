package com.pha.trainees.registry;

import com.pha.trainees.Main;
import com.pha.trainees.block.PullusionPortalBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class HiddenItem {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Main.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Main.MODID);

//    //RGT反相篮球
//    public static final RegistryObject<Item> BASKETBALL_ANTI_BLOCK_RGT_ITEM= ITEMS.register("basketball_anti_block_rgt",
//            () -> new BlockItem(ModBlocks.BASKETBALL_ANTI_BLOCK_RGT.get(),
//                    new Item.Properties()
//            ));

    //RGT反相素方块
    public static final RegistryObject<Item> CHE_JIBP_BLOCK_RGT_ITEM = ITEMS.register("che_jibp_block_rgt",
            () -> new BlockItem(ModChemistry.ModChemistryBlocks.CHE_JIBP_BLOCK_RGT.get(),
                    new Item.Properties()
            ));

    public static final RegistryObject<Block> PULLUSION_PORTAL = BLOCKS.register("pullusion_portal",
            () -> new PullusionPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.5f, 6.0f)
                    .lightLevel(state -> state.getValue(PullusionPortalBlock.ACTIVE) ? 11 : 0)
                    .noOcclusion()
            )
    );  // 允许看到方块后面的东西

}
