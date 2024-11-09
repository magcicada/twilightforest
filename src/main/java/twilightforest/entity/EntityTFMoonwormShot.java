package twilightforest.entity;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import twilightforest.block.BlockTFMoonworm;
import twilightforest.block.TFBlocks;

public class EntityTFMoonwormShot extends EntityTFThrowable {

	public EntityTFMoonwormShot(World world) {
		super(world);
	}

	public EntityTFMoonwormShot(World world, EntityLivingBase thrower) {
		super(world, thrower);
		shoot(thrower, thrower.rotationPitch, thrower.rotationYaw, 0F, 1.5F, 1.0F);
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		makeTrail();
	}

	@Override
	public float getBrightness() {
		return 1.0F;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender() {
		return 15728880;
	}

	// TODO Add an advancement
	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		if (!source.isProjectile()) super.attackEntityFrom(source, amount);
		BlockTFMoonworm moonworm = (BlockTFMoonworm) TFBlocks.moonworm;
		if (!world.isRemote) {
			EntityItem entity = new EntityItem(world, this.posX, this.posY, this.posZ, moonworm.getSquishResult());
			entity.setPickupDelay(20);
			entity.motionX = this.motionX;
			entity.motionY = this.motionY;
			entity.motionZ = this.motionZ;
			world.spawnEntity(entity);
			this.setDead();
		}
		else {
			int stateId = Block.getStateId(moonworm.getDefaultState());
			for (int i = 0; i < 8; ++i) {
				world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D, stateId);
			}
		}
		this.playSound(moonworm.getSoundType(moonworm.getDefaultState(), world, new BlockPos(this), this).getBreakSound(), 1F, 1F);
		return true;
	}

	private void makeTrail() {
//		for (int i = 0; i < 5; i++) {
//			double dx = posX + 0.5 * (rand.nextDouble() - rand.nextDouble()); 
//			double dy = posY + 0.5 * (rand.nextDouble() - rand.nextDouble()); 
//			double dz = posZ + 0.5 * (rand.nextDouble() - rand.nextDouble()); 
//			
//			double s1 = ((rand.nextFloat() * 0.5F) + 0.5F) * 0.17F;
//			double s2 = ((rand.nextFloat() * 0.5F) + 0.5F) * 0.80F;
//			double s3 = ((rand.nextFloat() * 0.5F) + 0.5F) * 0.69F;
//
//			world.spawnParticle("mobSpell", dx, dy, dz, s1, s2, s3);
//		}
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public float getCollisionBorderSize() {
		return 1.0F;
	}

	@Override
	protected float getGravityVelocity() {
		return 0.03F;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public void handleStatusUpdate(byte id) {
		if (id == 3) {
			int stateId = Block.getStateId(TFBlocks.moonworm.getDefaultState());
			for (int i = 0; i < 8; ++i) {
				this.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, this.posX, this.posY, this.posZ, 0.0D, 0.0D, 0.0D, stateId);
			}
		} else {
			super.handleStatusUpdate(id);
		}
	}

	@Override
	protected void onImpact(RayTraceResult ray) {
		if (!world.isRemote) {
			if (ray.typeOfHit == Type.BLOCK) {

				BlockPos pos = ray.getBlockPos().offset(ray.sideHit);
				IBlockState currentState = world.getBlockState(pos);

				if (currentState.getBlock().isReplaceable(world, pos)) {
					world.setBlockState(pos, TFBlocks.moonworm.getDefaultState().withProperty(BlockDirectional.FACING, ray.sideHit));
					this.playSound(TFBlocks.moonworm.getSoundType(TFBlocks.moonworm.getDefaultState(), world, new BlockPos(this), this).getPlaceSound(), 1F, 1F);
				}
			}

			if (ray.entityHit != null) {
				Entity entity = ray.entityHit;
				// TODO Maybe a new damage source
				entity.attackEntityFrom(DamageSource.causeThrownDamage(this, this.getThrower()), 0);
				if (!entity.isDead && entity instanceof EntityLivingBase) {
					EntityLivingBase living = (EntityLivingBase) entity;
					ItemStack helmet = living.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
					if (helmet.getItem() == Item.getItemFromBlock(TFBlocks.moonworm)) return;
					if (!helmet.isEmpty()) {
						float chance = 0.5F;
						if (helmet.getItem() instanceof ItemArmor) {
							ItemArmor.ArmorMaterial material = ((ItemArmor) helmet.getItem()).getArmorMaterial();
							if (material.getToughness() > 0.0F) chance /= material.getToughness();
							if (material.getDamageReductionAmount(EntityEquipmentSlot.HEAD) > 1) chance -= Math.min(chance, 0.025F * material.getDamageReductionAmount(EntityEquipmentSlot.HEAD));
						}
						if (rand.nextFloat() < chance) {
							living.entityDropItem(helmet, 0.2F);
							living.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(TFBlocks.moonworm));
						}
						return;
					}
					living.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(TFBlocks.moonworm));
				}
			}

			this.world.setEntityState(this, (byte) 3);
			this.setDead();
		}
	}
}
