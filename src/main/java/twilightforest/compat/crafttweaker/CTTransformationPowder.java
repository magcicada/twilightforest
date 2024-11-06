package twilightforest.compat.crafttweaker;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.block.IBlockDefinition;
import crafttweaker.api.block.IBlockStateMatcher;
import crafttweaker.api.entity.IEntityDefinition;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.oredict.OreDictionary;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;
import twilightforest.item.ItemTFTransformPowder;

import java.util.Objects;

@SuppressWarnings("unused")
@ZenRegister
@ZenClass("mods.twilightforest.Transformation")
public class CTTransformationPowder {

    @ZenMethod
    public static void addEntityTransformation(IEntityDefinition from, IEntityDefinition to) {
        EntityEntry in = (EntityEntry) from.getInternal();
        EntityEntry out = (EntityEntry) to.getInternal();
        ItemTFTransformPowder.addOneWayTransformation(Objects.requireNonNull(in.getRegistryName()), Objects.requireNonNull(out.getRegistryName()));
    }

    @ZenMethod
    public static void addEntityTransformation(String from, String to) {
        ItemTFTransformPowder.addOneWayTransformation(new ResourceLocation(from), new ResourceLocation(to));
    }

    @ZenMethod
    public static void addBlockTransformation(IIngredient from, IItemStack to) {
        ItemStack outStack = CraftTweakerMC.getItemStack(to);
        if (!(outStack.getItem() instanceof ItemBlock)) {
            CraftTweakerAPI.logError("Output " + to.toString() + " is not an item of a block");
            return;
        }
        Block outBlock = ((ItemBlock) outStack.getItem()).getBlock();
        int metadata = outStack.getItem().getMetadata(outStack.getMetadata());
        IBlockState out = outBlock.getStateFromMeta(metadata);
        for (IItemStack input : from.getItemArray()) {
            ItemStack inStack = CraftTweakerMC.getItemStack(input);
            if (!(inStack.getItem() instanceof ItemBlock)) {
                CraftTweakerAPI.logError("Input " + input.toString() + " is not an item of a block");
                continue;
            }
            if (inStack.getMetadata() == OreDictionary.WILDCARD_VALUE) ItemTFTransformPowder.addBlockTransformation(((ItemBlock) inStack.getItem()).getBlock(), out);
            else {
                IBlockState in = ((ItemBlock) inStack.getItem()).getBlock().getStateFromMeta(inStack.getItem().getMetadata(inStack.getMetadata()));
                ItemTFTransformPowder.addBlockTransformation(in, out);
            }
        }
    }

    @ZenMethod
    public static void addBlockTransformation(crafttweaker.api.block.IBlockState from, crafttweaker.api.block.IBlockState to) {
        ItemTFTransformPowder.addBlockTransformation(CraftTweakerMC.getBlockState(from), CraftTweakerMC.getBlockState(to));
    }

    @ZenMethod
    public static void addBlockTransformation(IBlockDefinition from, crafttweaker.api.block.IBlockState to, int... metadatas) {
        Block f = CraftTweakerMC.getBlock(from);
        IBlockState t = CraftTweakerMC.getBlockState(to);
        if (metadatas.length == 0) ItemTFTransformPowder.addBlockTransformation(f, t);
        else ItemTFTransformPowder.addBlockTransformation(f, state -> {
            for (int i : metadatas) if (f.getMetaFromState(state) == i) return true;
            return false;
        }, t);
    }

    @ZenMethod
    public static void addBlockTransformation(IBlockDefinition from, IBlockStateMatcher matcher, crafttweaker.api.block.IBlockState to) {
        Block f = CraftTweakerMC.getBlock(from);
        IBlockState t = CraftTweakerMC.getBlockState(to);
        ItemTFTransformPowder.addBlockTransformation(f, state -> matcher.matches(CraftTweakerMC.getBlockState(state)), t);
    }
}