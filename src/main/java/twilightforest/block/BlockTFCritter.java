package twilightforest.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IProjectile;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntitySnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import twilightforest.item.TFItems;

import java.util.Random;

public abstract class BlockTFCritter extends Block {

	private final float WIDTH = getWidth();

	private final AxisAlignedBB DOWN_BB  = new AxisAlignedBB(0.5F - WIDTH, 1.0F - WIDTH * 2.0F, 0.2F, 0.5F + WIDTH, 1.0F, 0.8F);
	private final AxisAlignedBB UP_BB    = new AxisAlignedBB(0.5F - WIDTH, 0.0F, 0.2F, 0.5F + WIDTH, WIDTH * 2.0F, 0.8F);
	private final AxisAlignedBB NORTH_BB = new AxisAlignedBB(0.5F - WIDTH, 0.2F, 1.0F - WIDTH * 2.0F, 0.5F + WIDTH, 0.8F, 1.0F);
	private final AxisAlignedBB SOUTH_BB = new AxisAlignedBB(0.5F - WIDTH, 0.2F, 0.0F, 0.5F + WIDTH, 0.8F, WIDTH * 2.0F);
	private final AxisAlignedBB WEST_BB  = new AxisAlignedBB(1.0F - WIDTH * 2.0F, 0.2F, 0.5F - WIDTH, 1.0F, 0.8F, 0.5F + WIDTH);
	private final AxisAlignedBB EAST_BB  = new AxisAlignedBB(0.0F, 0.2F, 0.5F - WIDTH, WIDTH * 2.0F, 0.8F, 0.5F + WIDTH);

	protected BlockTFCritter() {
		super(Material.CIRCUITS);
		this.setHardness(0.0F);
		this.setCreativeTab(TFItems.creativeTab);
		this.setSoundType(SoundType.SLIME);
		this.setDefaultState(blockState.getBaseState().withProperty(BlockDirectional.FACING, EnumFacing.UP));
	}

	@Override
	public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
		return true;
	}

	@Override
	public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
		drops.add(this.getSquishResult());
	}

	public float getWidth() {
		return 0.15F;
	}

	@Override
	public BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, BlockDirectional.FACING);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(BlockDirectional.FACING).getIndex();
	}

	@Override
	@Deprecated
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(BlockDirectional.FACING, EnumFacing.byIndex(meta));
	}

	@Override
	@Deprecated
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		switch (state.getValue(BlockDirectional.FACING)) {
			case DOWN:
				return DOWN_BB;
			case UP:
			default:
				return UP_BB;
			case NORTH:
				return NORTH_BB;
			case SOUTH:
				return SOUTH_BB;
			case WEST:
				return WEST_BB;
			case EAST:
				return EAST_BB;
		}
	}

	@Override
	@Deprecated
	public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
		return NULL_AABB;
	}

	@Override
	@Deprecated
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	@Deprecated
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	@Deprecated
	public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos, EnumFacing face) {
		return BlockFaceShape.UNDEFINED;
	}

	@Override
	public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side) {
		return canPlaceAt(world, pos.offset(side.getOpposite()), side);
	}

	@Override
	public boolean canPlaceBlockAt(World world, BlockPos pos) {
		for (EnumFacing side : EnumFacing.values()) {
			if (canPlaceAt(world, pos.offset(side.getOpposite()), side)) {
				return true;
			}
		}
		return false;
	}

	@Override
	@Deprecated
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing sideHit, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		IBlockState state = getDefaultState();

		if (canPlaceAt(world, pos.offset(sideHit.getOpposite()), sideHit)) {
			state = state.withProperty(BlockDirectional.FACING, sideHit);
		}

		return state;
	}

	@Override
	public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
		world.scheduleUpdate(pos, this, 1);
	}

	@Override
	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
		checkAndDrop(world, pos, state);
	}

	protected boolean checkAndDrop(World world, BlockPos pos, IBlockState state) {
		EnumFacing facing = state.getValue(BlockDirectional.FACING);
		if (!canPlaceAt(world, pos.offset(facing.getOpposite()), facing)) {
			world.destroyBlock(pos, true);
			return false;
		}
		return true;
	}

	@Override
	@Deprecated
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block, BlockPos fromPos) {
		checkAndDrop(world, pos, state);
	}

	@Override
	public void onEntityCollision(World worldIn, BlockPos pos, IBlockState state, Entity entityIn) {
		if (!(entityIn instanceof IProjectile) || entityIn instanceof EntitySnowball) super.onEntityCollision(worldIn, pos, state, entityIn);
		float speed = MathHelper.sqrt(entityIn.motionX * entityIn.motionX + entityIn.motionY * entityIn.motionY + entityIn.motionZ * entityIn.motionZ);
		if (speed > 0.5F) this.squish(worldIn, pos, entityIn);
	}

	protected boolean canPlaceAt(IBlockAccess world, BlockPos pos, EnumFacing facing) {
		IBlockState state = world.getBlockState(pos);
		return state.getBlockFaceShape(world, pos, facing) == BlockFaceShape.SOLID
				&& (!isExceptBlockForAttachWithPiston(state.getBlock())
				|| state.getMaterial() == Material.LEAVES || state.getMaterial() == Material.CACTUS);
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	public void squish(World world, BlockPos pos, Entity entityIn) {
		if (!world.isRemote) {
			EntityItem entity = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, this.getSquishResult());
			entity.setPickupDelay(20);
			entity.motionX = (-0.5D + world.rand.nextDouble()) / 4D;
			entity.motionY = world.rand.nextDouble() / 3D;
			entity.motionZ = (-0.5D + world.rand.nextDouble()) / 4D;
			world.spawnEntity(entity);
			world.setBlockToAir(pos);
		}
		else {
			int stateId = Block.getStateId(this.getDefaultState());
			for (int i = 0; i < 8; ++i) {
				world.spawnParticle(EnumParticleTypes.BLOCK_CRACK, pos.getX() + 0.5D, pos.getY() + 0.1D, pos.getZ() + 0.5D, 0.0D, 0.0D, 0.0D, stateId);
			}
		}
		world.playSound(null, pos.getX(), pos.getY(), pos.getZ(), this.getSoundType(world.getBlockState(pos), world, pos, entityIn).getBreakSound(), entityIn.getSoundCategory(), 1F, 1F);
	}

	@Override
	public abstract TileEntity createTileEntity(World world, IBlockState state);

	public abstract ItemStack getSquishResult(); // oh no!
}
