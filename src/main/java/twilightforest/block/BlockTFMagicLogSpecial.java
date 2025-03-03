package twilightforest.block;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenCustomHashSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import twilightforest.network.TFPacketHandler;
import twilightforest.biomes.TFBiomes;
import twilightforest.item.ItemTFOreMagnet;
import twilightforest.item.TFItems;
import twilightforest.network.PacketChangeBiome;
import twilightforest.util.WorldUtil;

import java.util.*;

public class BlockTFMagicLogSpecial extends BlockTFMagicLog {

	protected BlockTFMagicLogSpecial() {
		this.setCreativeTab(TFItems.creativeTab);
	}

	@Override
	public int tickRate(World world) {
		return 20;
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		world.scheduleUpdate(pos, this, this.tickRate(world));
	}

	@Override
	public Item getItemDropped(IBlockState state, Random random, int fortune) {
		return Item.getItemFromBlock(TFBlocks.magic_log);
	}

	@Override
	public int damageDropped(IBlockState state) {
		return state.getValue(VARIANT).ordinal();
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {

		if (world.isRemote || state.getValue(LOG_AXIS) != EnumAxis.NONE) return;

		switch (state.getValue(VARIANT)) {
			case TIME:
				world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.1F, 0.5F);
				doTreeOfTimeEffect(world, pos, rand);
				break;
			case TRANS:
				world.playSound(null, pos, SoundEvents.BLOCK_NOTE_HARP, SoundCategory.BLOCKS, 0.1F, rand.nextFloat() * 2F);
				doTreeOfTransformationEffect(world, pos, rand);
				break;
			case MINE:
				doMinersTreeEffect(world, pos, rand);
				break;
			case SORT:
				doSortingTreeEffect(world, pos, rand);
				break;
		}

		world.scheduleUpdate(pos, this, this.tickRate(world));
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
		if (state.getValue(LOG_AXIS) != EnumAxis.NONE) {
			world.setBlockState(pos, state.withProperty(LOG_AXIS, EnumAxis.NONE));
			world.scheduleUpdate(pos, this, this.tickRate(world));
			return true;
		} else if (state.getValue(LOG_AXIS) == EnumAxis.NONE) {
			world.setBlockState(pos, state.withProperty(LOG_AXIS, EnumAxis.Y));
			return true;
		}

		return false;
	}

	/**
	 * The tree of time adds extra ticks to blocks, so that they have twice the normal chance to get a random tick
	 */
	private void doTreeOfTimeEffect(World world, BlockPos pos, Random rand) {

		int numticks = 8 * 3 * this.tickRate(world);

		for (int i = 0; i < numticks; i++) {

			BlockPos dPos = WorldUtil.randomOffset(rand, pos, 16);

			IBlockState state = world.getBlockState(dPos);
			Block block = state.getBlock();

			if (block != Blocks.AIR && block.getTickRandomly()) {
				block.updateTick(world, dPos, state, rand);
			}

			TileEntity te = world.getTileEntity(dPos);
			if (te instanceof ITickable && !te.isInvalid()) {
				((ITickable) te).update();
			}
		}
	}

	/**
	 * The tree of transformation transforms the biome in the area near it into the enchanted forest biome.
	 * <p>
	 * TODO: also change entities
	 */
	private void doTreeOfTransformationEffect(World world, BlockPos pos, Random rand) {

		Biome targetBiome = TFBiomes.enchantedForest;

		for (int i = 0; i < 16; i++) {

			BlockPos dPos = WorldUtil.randomOffset(rand, pos, 16, 0, 16);
			if (dPos.distanceSq(pos) > 256.0) continue;

			Biome biomeAt = world.getBiome(dPos);
			if (biomeAt == targetBiome) continue;

			Chunk chunkAt = world.getChunk(dPos);
			chunkAt.getBiomeArray()[(dPos.getZ() & 15) << 4 | (dPos.getX() & 15)] = (byte) Biome.getIdForBiome(targetBiome);

			if (world instanceof WorldServer) {
				sendChangedBiome(world, dPos, targetBiome);
			}
			break;
		}
	}

	/**
	 * Send a tiny update packet to the client to inform it of the changed biome
	 */
	private void sendChangedBiome(World world, BlockPos pos, Biome biome) {
		IMessage message = new PacketChangeBiome(pos, biome);
		NetworkRegistry.TargetPoint targetPoint = new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 128);
		TFPacketHandler.CHANNEL.sendToAllTracking(message, targetPoint);
	}

	/**
	 * The miner's tree generates the ore magnet effect randomly every second
	 */
	private void doMinersTreeEffect(World world, BlockPos pos, Random rand) {

		BlockPos dPos = WorldUtil.randomOffset(rand, pos, 32);

		//world.playSoundEffect(x + 0.5D, y + 0.5D, z + 0.5D, "random.click", 0.1F, 0.5F);

		int moved = ItemTFOreMagnet.doMagnet(world, pos, dPos);

		if (moved > 0) {
			world.playSound(null, pos, SoundEvents.ENTITY_ENDERMEN_TELEPORT, SoundCategory.BLOCKS, 0.1F, 1.0F);
		}
	}

	/**
	 * The sorting tree finds two chests nearby and then attempts to sort a random item.
	 */
	private void doSortingTreeEffect(World world, BlockPos pos, Random rand) {
		for (BlockPos iPos : WorldUtil.getAllAround(pos, 16)) {
			Chunk chunk = world.getChunk(iPos);
			if (!chunk.isLoaded()) continue;
			IBlockState state = world.getBlockState(iPos);
			Block block = state.getBlock();
			if (block instanceof ITileEntityProvider) {
				TileEntity te = world.getTileEntity(iPos);
				if (te == null) continue;
				IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
				if (handler == null || handler.getSlots() == 1) continue;
				List<ItemStack> stacks = new ArrayList<>();
				for (int i = 0; i < handler.getSlots(); i++) {
					ItemStack stack = handler.getStackInSlot(i);
					if (!stack.isEmpty() && stack.getCount() != stack.getMaxStackSize()) {
						stacks.add(stack);
					}
				}
				if (stacks.isEmpty() || stacks.size() == 1) continue;
				boolean didAnything = false;
				for (int i1 = 0; i1 < stacks.size(); i1++) {
					ItemStack stack1 = stacks.get(i1);
					for (int i2 = 0; i2 < stacks.size(); i2++) {
						if (i1 != i2) {
							ItemStack stack2 = stacks.get(i2);
							if (this.areItemsStackable(stack1, stack2)) {
								didAnything = true;
								int toInsert = Math.min(stack1.getMaxStackSize() - stack1.getCount(), stack2.getCount());
								stack1.grow(toInsert);
								stack2.shrink(toInsert);
							}
						}
					}
					if (didAnything) break;
				}
			}
		}
	}

	private boolean areItemsStackable(ItemStack stack1, ItemStack stack2) {
		if (!stack1.isStackable() || !stack2.isStackable()) return false;
		if (stack1.getCount() == stack1.getMaxStackSize() || stack2.getCount() == stack2.getMaxStackSize()) return false;
		return stack1.getItem() == stack2.getItem() && stack1.getMetadata() == stack2.getMetadata() && ((!stack1.hasTagCompound() && !stack2.hasTagCompound()) || (stack1.hasTagCompound() == stack2.hasTagCompound() && Objects.requireNonNull(stack1.getTagCompound()).equals(Objects.requireNonNull(stack2.getTagCompound()))));
	}

	@Override
	@Deprecated
	public int getLightValue(IBlockState state) {
		return 15;
	}

	@Override
	public void getSubBlocks(CreativeTabs creativeTab, NonNullList<ItemStack> list) {
		list.add(new ItemStack(this, 1, 0));
		list.add(new ItemStack(this, 1, 1));
		list.add(new ItemStack(this, 1, 2));
		list.add(new ItemStack(this, 1, 3));
	}

	@Override
	protected boolean canSilkHarvest() {
		return false;
	}

	@Override
	public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		return false;
	}
}
