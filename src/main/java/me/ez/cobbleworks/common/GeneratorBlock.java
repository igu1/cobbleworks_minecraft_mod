package me.ez.cobbleworks.common;

import com.mojang.serialization.MapCodec;
import me.ez.cobbleworks.Init;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Model-matched chassis, two raised reservoirs and a recessed processing chamber. */
public final class GeneratorBlock extends BaseEntityBlock {
    public static final MapCodec<GeneratorBlock> CODEC = simpleCodec(GeneratorBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<MachinePhase> PHASE = EnumProperty.create("phase", MachinePhase.class);
    public static final BooleanProperty WATER = BooleanProperty.create("water");
    public static final BooleanProperty LAVA = BooleanProperty.create("lava");
    private static final VoxelShape NORTH = Shapes.or(
            box(0, 0, 0, 16, 3, 16), box(1, 3, 2, 15, 7, 15),
            box(1, 7, 3, 5, 16, 13), box(11, 7, 3, 15, 16, 13),
            box(5, 7, 11, 11, 14, 15), box(5, 7, 4, 11, 11, 11),
            box(5, 7, 0, 11, 9, 4));
    private static final VoxelShape EAST = turn(NORTH);
    private static final VoxelShape SOUTH = turn(EAST);
    private static final VoxelShape WEST = turn(SOUTH);

    public GeneratorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PHASE, MachinePhase.WAITING)
                .setValue(WATER, false).setValue(LAVA, false));
    }
    private static VoxelShape turn(VoxelShape shape) {
        VoxelShape[] rotated = {Shapes.empty()};
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) ->
                rotated[0] = Shapes.or(rotated[0], Shapes.box(1 - z2, y1, x1, 1 - z1, y2, x2)));
        return rotated[0].optimize();
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, PHASE, WATER, LAVA); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) { case EAST -> EAST; case SOUTH -> SOUTH; case WEST -> WEST; default -> NORTH; };
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GeneratorBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Init.GENERATOR_BE.get(), GeneratorBlockEntity::tick);
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                    Player player, InteractionHand hand, BlockHitResult hit) {
        return player.isShiftKeyDown() ? InteractionResult.PASS : useWithoutItem(state, level, pos, player, hit);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GeneratorBlockEntity machine) player.openMenu(machine);
        return level.isClientSide() ? InteractionResult.SUCCESS : InteractionResult.SUCCESS_SERVER;
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction side) {
        return level.getBlockEntity(pos) instanceof GeneratorBlockEntity machine ? GeneratorRules.comparator(machine.stored()) : 0;
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PHASE) == MachinePhase.RUNNING && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + .5, pos.getY() + .85, pos.getZ() + .5, 0, .015, 0);
        }
    }
}
