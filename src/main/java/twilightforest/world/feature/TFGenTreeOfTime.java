package twilightforest.world.feature;

import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockLog;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import twilightforest.block.TFBlocks;
import twilightforest.world.TFWorld;

import java.util.Random;

public class TFGenTreeOfTime extends TFGenHollowTree {

	public TFGenTreeOfTime(boolean notify) {
		super(notify);

		this.treeState = TFBlocks.magic_log.getDefaultState();
		this.branchState = treeState.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.NONE);
		this.leafState = TFBlocks.magic_leaves.getDefaultState().withProperty(BlockLeaves.CHECK_DECAY, false);
	}

	@Override
	public boolean generate(World world, Random random, BlockPos pos) {

		int height = 8;
		int diameter = 1;

		// do we have enough height?
		if (pos.getY() < 1 || pos.getY() + height + diameter > TFWorld.MAXHEIGHT) {
			return false;
		}

		// check if we're on dirt or grass
		IBlockState state = world.getBlockState(pos.down());
		if (!state.getBlock().canSustainPlant(state, world, pos.down(), EnumFacing.UP, source)) {
			return false;
		}

		// make a tree!

		// build the trunk
		buildTrunk(world, random, pos, diameter, height);

		// build the crown
		buildTinyCrown(world, random, pos, diameter, height);

		// 3-5 roots at the bottom
		buildBranchRing(world, random, pos, diameter, 1, 0, 12, 0, 0.75D, 0, 3, 5, 3, false);

		// several more taproots
		buildBranchRing(world, random, pos, diameter, 1, 2, 18, 0, 0.9D, 0, 3, 5, 3, false);

		// add clock block
		EnumFacing facing = EnumFacing.HORIZONTALS[random.nextInt(4)];
		int xOff = facing.getXOffset();
		int zOff = facing.getZOffset();
		this.setBlockAndNotifyAdequately(world, pos.add(xOff, 2, zOff), TFBlocks.magic_log_core.getDefaultState());

		return true;
	}

	/**
	 * Build the crown of the tree. This builds a smaller crown, since the large
	 * ones were causing some performance issues
	 */
	protected void buildTinyCrown(World world, Random random, BlockPos pos, int diameter, int height) {
		int crownRadius = 4;
		int bvar = 1;

		// 3-5 medium branches starting at the bottom of the crown
		buildBranchRing(world, random, pos, diameter, height - crownRadius, 0, crownRadius, 0, 0.35D, 0, bvar, bvar + 2, 1, true);

		// 3-5 medium branches at the crown middle
		buildBranchRing(world, random, pos, diameter, height - (crownRadius / 2), 0, crownRadius, 0, 0.28D, 0, bvar, bvar + 2, 1, true);

		// 2-4 medium branches at the crown top
		buildBranchRing(world, random, pos, diameter, height, 0, crownRadius, 0, 0.15D, 0, 2, 4, 0, true);

		// 3-6 medium branches going straight up
		buildBranchRing(world, random, pos, diameter, height, 0, (crownRadius / 2), 0, 0.05D, 0, bvar, bvar + 2, 0, true);
	}

}
