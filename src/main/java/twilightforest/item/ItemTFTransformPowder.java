package twilightforest.item;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.OreDictionary;
import twilightforest.TwilightForestMod;
import twilightforest.util.TFEntityNames;
import twilightforest.util.VanillaEntityNames;

import java.util.*;

public class ItemTFTransformPowder extends ItemTF {

	public static Hash.Strategy<ItemStack> ITEMSTACK_STRATEGY = new Hash.Strategy<ItemStack>() {
		@Override
		public int hashCode(ItemStack o) {
			if (o == null || o.isEmpty()) return 0;
			int i = Item.getIdFromItem(o.getItem()) << 17;
			i |= (o.getMetadata() + 1) << 31;
			if (o.hasTagCompound()) i |= Objects.hashCode(o.getTagCompound()) << 13;
			return i;
		}

		@Override
		public boolean equals(ItemStack a, ItemStack b) {
			if (a == null || b == null) return false;
			boolean nbt = !a.hasTagCompound() || Objects.requireNonNull(a.getTagCompound()).equals(Objects.requireNonNull(b.getTagCompound()));
			return a.isItemEqual(b) && nbt;
		}
	};

	private static final Map<ResourceLocation, ResourceLocation> transformMap = new HashMap<>();
	private static final Map<ItemStack, ItemStack> transformItemMap = new Object2ObjectOpenCustomHashMap<>(ITEMSTACK_STRATEGY);

	protected ItemTFTransformPowder() {
		MinecraftForge.EVENT_BUS.register(this);
		this.maxStackSize = 64;
		this.setCreativeTab(TFItems.creativeTab);
		addTwoWayTransformation(TFEntityNames.MINOTAUR,       VanillaEntityNames.ZOMBIE_PIGMAN);
		addTwoWayTransformation(TFEntityNames.DEER,           VanillaEntityNames.COW);
		addTwoWayTransformation(TFEntityNames.BIGHORN_SHEEP,  VanillaEntityNames.SHEEP);
		addTwoWayTransformation(TFEntityNames.WILD_BOAR,      VanillaEntityNames.PIG);
		addTwoWayTransformation(TFEntityNames.BUNNY,          VanillaEntityNames.RABBIT);
		addTwoWayTransformation(TFEntityNames.TINY_BIRD,      VanillaEntityNames.PARROT);
		addTwoWayTransformation(TFEntityNames.RAVEN,          VanillaEntityNames.BAT);
		addTwoWayTransformation(TFEntityNames.HOSTILE_WOLF,   VanillaEntityNames.WOLF);
		addTwoWayTransformation(TFEntityNames.PENGUIN,        VanillaEntityNames.CHICKEN);
		addTwoWayTransformation(TFEntityNames.HEDGE_SPIDER,   VanillaEntityNames.SPIDER);
		addTwoWayTransformation(TFEntityNames.SWARM_SPIDER,   VanillaEntityNames.CAVE_SPIDER);
		addTwoWayTransformation(TFEntityNames.WRAITH,         VanillaEntityNames.BLAZE);
		addTwoWayTransformation(TFEntityNames.REDCAP,         VanillaEntityNames.VILLAGER);
		addTwoWayTransformation(TFEntityNames.SKELETON_DRUID, VanillaEntityNames.WITCH);
	}

	public static void addTwoWayTransformation(ResourceLocation from, ResourceLocation to) {
		transformMap.put(from, to);
		transformMap.put(to, from);
	}

	public static void addOneWayTransformation(ResourceLocation from, ResourceLocation to) {
		transformMap.put(from, to);
	}

	public static void addTwoWayTransformation(ItemStack from, ItemStack to) {
		transformItemMap.put(from, to);
		transformItemMap.put(to, from);
	}

	public static void addOneWayTransformation(ItemStack from, ItemStack to) {
		transformItemMap.put(from, to);
	}

	public static void addOneWayTransformation(Item from, ItemStack to) {
		transformItemMap.put(new ItemStack(from, 1, OreDictionary.WILDCARD_VALUE), to);
	}

	public static ResourceLocation getTransformationEntity(ResourceLocation from) {
		return transformMap.get(from);
	}

	public static ItemStack getTransformationItem(ItemStack from) {
		ItemStack out = transformItemMap.get(from);
		if (out == null) transformItemMap.get(new ItemStack(from.getItem(), 1, OreDictionary.WILDCARD_VALUE));
		return out == null ? ItemStack.EMPTY : out;
	}

	@SubscribeEvent
	public void onEntityRightClick(PlayerInteractEvent.EntityInteract event) {
		World world = event.getWorld();
		Entity entity = event.getTarget();
		ItemStack heldItem = event.getItemStack();
		boolean result = false;
		if (entity.isDead || heldItem.getItem() != this) return;
		if (entity instanceof EntityItem) {
			EntityItem entityItem = (EntityItem) entity;
			ItemStack stack = entityItem.getItem();
			ItemStack out = getTransformationItem(stack);
			if (!out.isEmpty()) {
				if (world.isRemote) {
					AxisAlignedBB fanBox = getEffectAABB(event.getEntityPlayer());
					// particle effect
					for (int i = 0; i < 30; i++) {
						world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, fanBox.minX + world.rand.nextFloat() * (fanBox.maxX - fanBox.minX),
								fanBox.minY + world.rand.nextFloat() * (fanBox.maxY - fanBox.minY),
								fanBox.minZ + world.rand.nextFloat() * (fanBox.maxZ - fanBox.minZ),
								0, 0, 0);
					}
					event.setResult(Event.Result.ALLOW);
					return;
				}
				if (stack.getCount() == 1) {
					entityItem.setItem(out.copy());
					entityItem.setPickupDelay(20);
				}
				else {
					EntityItem outEntity = new EntityItem(world, entity.posX, entity.posY, entity.posZ, out.copy());
					outEntity.setPickupDelay(20);
					world.spawnEntity(outEntity);
				}
				result = true;
			}
		}
		else {
			if (!world.isRemote) {
				System.out.println(entity);
				System.out.println(EntityList.getKey(entity));
			}
			ResourceLocation location = transformMap.get(EntityList.getKey(entity));
			if (location == null) return;
			Entity newEntity = EntityList.createEntityByIDFromName(location, world);
			if (newEntity == null) return;
			if (world.isRemote) {
				AxisAlignedBB fanBox = getEffectAABB(event.getEntityPlayer());
				// particle effect
				for (int i = 0; i < 30; i++) {
					world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, fanBox.minX + world.rand.nextFloat() * (fanBox.maxX - fanBox.minX),
							fanBox.minY + world.rand.nextFloat() * (fanBox.maxY - fanBox.minY),
							fanBox.minZ + world.rand.nextFloat() * (fanBox.maxZ - fanBox.minZ),
							0, 0, 0);
				}
				event.setResult(Event.Result.ALLOW);
				return;
			}
			newEntity.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, entity.rotationPitch);
			if (newEntity instanceof EntityLiving) {
				((EntityLiving) newEntity).onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), null);
			}
			try { // try copying what can be copied
				UUID uuid = newEntity.getUniqueID();
				newEntity.readFromNBT(entity.writeToNBT(newEntity.writeToNBT(new NBTTagCompound())));
				newEntity.setUniqueId(uuid);
			} catch (Exception e) {
				TwilightForestMod.LOGGER.warn("Couldn't transform entity NBT data: {}", e);
			}
			world.spawnEntity(newEntity);
			result = true;
			entity.setDead();
		}

		if (result) {
			event.getEntityPlayer().swingArm(event.getHand());
			if (!event.getEntityPlayer().isCreative()) heldItem.shrink(1);
			if (entity instanceof EntityLiving) {
				((EntityLiving) entity).spawnExplosionParticle();
				((EntityLiving) entity).spawnExplosionParticle();
			}
			else {
				this.spawnExplosionParticles(world, entity);
				this.spawnExplosionParticles(world, entity);
			}
			entity.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F + itemRand.nextFloat(), itemRand.nextFloat() * 0.7F + 0.3F);
			event.setResult(Event.Result.ALLOW);
		}
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		IBlockState state = world.getBlockState(pos);
		ItemStack heldItem = player.getHeldItem(hand);
		ItemStack stack = state.getBlock().getPickBlock(state, new RayTraceResult(RayTraceResult.Type.BLOCK, new Vec3d(hitX, hitY, hitZ), facing, pos), world, pos, player);
		if (!stack.isEmpty()) {
			ItemStack outStack = getTransformationItem(stack);
			if (!outStack.isEmpty()) {
				if (world.isRemote) {
					AxisAlignedBB fanBox = getEffectAABB(player);
					// particle effect
					for (int i = 0; i < 30; i++) {
						world.spawnParticle(EnumParticleTypes.CRIT_MAGIC, fanBox.minX + world.rand.nextFloat() * (fanBox.maxX - fanBox.minX),
								fanBox.minY + world.rand.nextFloat() * (fanBox.maxY - fanBox.minY),
								fanBox.minZ + world.rand.nextFloat() * (fanBox.maxZ - fanBox.minZ),
								0, 0, 0);
					}
					return EnumActionResult.SUCCESS;
				}
				if (outStack.getItem() instanceof ItemBlock) {
					ItemBlock itemBlock = (ItemBlock) outStack.getItem();
					Block block = itemBlock.getBlock();
					IBlockState placeState = block.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, itemBlock.getMetadata(stack.getMetadata()), player, hand);
					world.setBlockState(pos, placeState);
				}
				else {
					EntityItem entityItem = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, outStack.copy());
					entityItem.setPickupDelay(20);
					world.spawnEntity(entityItem);
					world.setBlockToAir(pos);
				}
				if (!player.isCreative()) heldItem.shrink(1);
				this.spawnExplosionParticles(world, pos);
				this.spawnExplosionParticles(world, pos);
				world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.BLOCKS, 1.0F + itemRand.nextFloat(), itemRand.nextFloat() * 0.7F + 0.3F);
				return EnumActionResult.SUCCESS;
			}
		}
		return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
	}

	private boolean handleBlockTransformation(World world, BlockPos pos, RayTraceResult rayTrace, EntityPlayer player, EnumHand hand, float hitX, float hitY, float hitZ, IBlockState state, EnumFacing facing) {
		ItemStack stack = state.getBlock().getPickBlock(state, rayTrace, world, pos, player);
		if (!stack.isEmpty()) {
			ItemStack outStack = getTransformationItem(stack);
			if (!outStack.isEmpty()) {
				if (world.isRemote) return true;
				if (outStack.getItem() instanceof ItemBlock) {
					ItemBlock itemBlock = (ItemBlock) outStack.getItem();
					Block block = itemBlock.getBlock();
					IBlockState placeState = block.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, itemBlock.getMetadata(stack.getMetadata()), player, hand);
					world.setBlockState(pos, placeState);
				}
				else {
					EntityItem entityItem = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, outStack.copy());
					entityItem.setPickupDelay(20);
					world.spawnEntity(entityItem);
					world.setBlockToAir(pos);
				}
				this.spawnExplosionParticles(world, pos);
				this.spawnExplosionParticles(world, pos);
				world.playSound(null, pos, SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.BLOCKS, 1.0F + itemRand.nextFloat(), itemRand.nextFloat() * 0.7F + 0.3F);
				return true;
			}
		}
		return false;
	}

	private boolean handleEntityTransformation(World world, Entity entity) {
		if (entity.isDead) return false;
		if (entity instanceof EntityItem) {
			EntityItem entityItem = (EntityItem) entity;
			ItemStack stack = entityItem.getItem();
			ItemStack out = getTransformationItem(stack);
			if (!out.isEmpty()) {
				if (stack.getCount() == 1) {
					entityItem.setItem(out.copy());
					entityItem.setPickupDelay(20);
				}
				else {
					stack.shrink(1);
					EntityItem outEntity = new EntityItem(world, entity.posX, entity.posY, entity.posZ, out.copy());
					outEntity.setPickupDelay(20);
					world.spawnEntity(outEntity);
				}
				this.spawnExplosionParticles(world, entity);
				this.spawnExplosionParticles(world, entity);
				entity.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F + itemRand.nextFloat(), itemRand.nextFloat() * 0.7F + 0.3F);
				return true;
			}
		}
		else {
			ResourceLocation location = transformMap.get(EntityList.getKey(entity));
			if (location == null) return false;
			if (world.isRemote) return true;
			Entity newEntity = EntityList.createEntityByIDFromName(location, world);
			if (newEntity == null) return false;
			newEntity.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, entity.rotationPitch);
			if (newEntity instanceof EntityLiving) {
				((EntityLiving) newEntity).onInitialSpawn(world.getDifficultyForLocation(new BlockPos(entity)), null);
			}
			try { // try copying what can be copied
				UUID uuid = newEntity.getUniqueID();
				newEntity.readFromNBT(entity.writeToNBT(newEntity.writeToNBT(new NBTTagCompound())));
				newEntity.setUniqueId(uuid);
			} catch (Exception e) {
				TwilightForestMod.LOGGER.warn("Couldn't transform entity NBT data: {}", e);
			}
			world.spawnEntity(newEntity);

			if (entity instanceof EntityLiving) {
				((EntityLiving) entity).spawnExplosionParticle();
				((EntityLiving) entity).spawnExplosionParticle();
			}
			else {
				this.spawnExplosionParticles(world, entity);
				this.spawnExplosionParticles(world, entity);
			}
			entity.setDead();
			entity.playSound(SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, 1.0F + itemRand.nextFloat(), itemRand.nextFloat() * 0.7F + 0.3F);
			return true;
		}
		return false;
	}

	private AxisAlignedBB getEffectAABB(EntityPlayer player) {
		double range = 2.0D;
		double radius = 1.0D;
		Vec3d srcVec = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);
		Vec3d lookVec = player.getLookVec();
		Vec3d destVec = srcVec.add(lookVec.x * range, lookVec.y * range, lookVec.z * range);

		return new AxisAlignedBB(destVec.x - radius, destVec.y - radius, destVec.z - radius, destVec.x + radius, destVec.y + radius, destVec.z + radius);
	}

	private void spawnExplosionParticles(World world, Entity entity) {
		for (int i = 0; i < 20; ++i) {
			double d0 = world.rand.nextGaussian() * 0.02D;
			double d1 = world.rand.nextGaussian() * 0.02D;
			double d2 = world.rand.nextGaussian() * 0.02D;
			world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, entity.posX + (double)(world.rand.nextFloat() * entity.width * 2.0F) - (double)entity.width - d0 * 10.0D, entity.posY + (double)(world.rand.nextFloat() * entity.height) - d1 * 10.0D, entity.posZ + (double)(world.rand.nextFloat() * entity.width * 2.0F) - (double)entity.width - d2 * 10.0D, d0, d1, d2);
		}
	}

	private void spawnExplosionParticles(World world, BlockPos pos) {
		for (int i = 0; i < 20; ++i) {
			double d0 = world.rand.nextGaussian() * 0.02D;
			double d1 = world.rand.nextGaussian() * 0.02D;
			double d2 = world.rand.nextGaussian() * 0.02D;
			world.spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, pos.getX() + 0.5D + (double)(world.rand.nextFloat() * 2.0F) - 1.0D - d0 * 10.0D, pos.getY() + 0.5D + (double)(world.rand.nextFloat()) - d1 * 10.0D, pos.getZ() + 0.5D + (double)(world.rand.nextFloat() * 2.0F) - 1.0D - d2 * 10.0D, d0, d1, d2);
		}
	}
}
