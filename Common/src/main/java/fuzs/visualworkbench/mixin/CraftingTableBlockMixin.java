package fuzs.visualworkbench.mixin;

import fuzs.visualworkbench.config.JsonConfigBuilder;
import fuzs.visualworkbench.init.ModRegistry;
import fuzs.visualworkbench.world.level.block.VisualCraftingTableBlock;
import fuzs.visualworkbench.world.level.block.entity.CraftingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingTableBlock.class)
public abstract class CraftingTableBlockMixin extends Block implements EntityBlock, VisualCraftingTableBlock {

    public CraftingTableBlockMixin(Properties p_i48440_1_) {
        super(p_i48440_1_);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return this.hasBlockEntity() ? new CraftingTableBlockEntity(pPos, pState) : null;
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state, BlockEntityType<T> type) {
        return checkType(type, ModRegistry.CRAFTING_TABLE_BLOCK_ENTITY.get(), CraftingTableBlockEntity::tick);
    }

    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> checkType(BlockEntityType<A> givenType, BlockEntityType<E> expectedType, BlockEntityTicker<? super E> ticker) {
        return expectedType == givenType ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (this.hasBlockEntity() && world.getBlockEntity(pos) instanceof CraftingTableBlockEntity blockEntity) {
            if (stack.hasCustomHoverName()) {
                blockEntity.setCustomName(stack.getHoverName());
            }
        } else {
            super.setPlacedBy(world, pos, state, placer, stack);
        }
    }

    @Override
    public void onRemove(BlockState state, Level world, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (this.hasBlockEntity() && world.getBlockEntity(pos) instanceof CraftingTableBlockEntity blockEntity) {
                Containers.dropContents(world, pos, blockEntity);
            }
        }
        super.onRemove(state, world, pos, newState, isMoving);
    }

    @Override
    public boolean triggerEvent(BlockState state, Level world, BlockPos pos, int id, int param) {
        final boolean result = super.triggerEvent(state, world, pos, id, param);
        if (this.hasBlockEntity() && world.getBlockEntity(pos) instanceof CraftingTableBlockEntity blockEntity) {
            return blockEntity.triggerEvent(id, param);
        }
        return result;
    }

    @Override
    public boolean hasBlockEntity() {
        return JsonConfigBuilder.INSTANCE.contains(this);
    }

    // this is done via an event in case a mod overrides this in their custom crafting table class, so we have a chance to be compatible if needed
//    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
//    public void use$head(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> callbackInfo) {
//        if (this.hasBlockEntity() && world.getBlockEntity(pos) instanceof CraftingTableBlockEntity blockEntity) {
//            if (world.isClientSide) {
//                callbackInfo.setReturnValue(InteractionResult.SUCCESS);
//            } else {
//                player.openMenu(blockEntity);
//                player.awardStat(Stats.INTERACT_WITH_CRAFTING_TABLE);
//                callbackInfo.setReturnValue(InteractionResult.CONSUME);
//            }
//        }
//    }

    // not sure if this is even needed
    @Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
    public void getMenuProvider$head(BlockState state, Level world, BlockPos pos, CallbackInfoReturnable<MenuProvider> callbackInfo) {
        if (this.hasBlockEntity() && world.getBlockEntity(pos) instanceof CraftingTableBlockEntity blockEntity) {
            callbackInfo.setReturnValue(blockEntity);
        }
    }
    
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (world.getBlockEntity(pos) instanceof CraftingTableBlockEntity table) {
            int stacks = 0;
            for (ItemStack stack : table.getInventory()) {
                if (!stack.isEmpty()) stacks++;
            }
            return stacks;
        }
        return 0;
    }
}
