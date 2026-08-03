package de.timi.simplebackpack;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(SimpleBackpackMod.MOD_ID)
public final class SimpleBackpackMod {
    public static final String MOD_ID = "simplebackpack";
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, MOD_ID);
    public static final RegistryObject<Item> BACKPACK = ITEMS.register("backpack", () -> new BackpackItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<MenuType<BackpackMenu>> BACKPACK_MENU = MENUS.register("backpack", () -> IForgeMenuType.create(BackpackMenu::new));

    public SimpleBackpackMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        MENUS.register(bus);
        bus.addListener(this::addCreativeItems);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) event.accept(BACKPACK);
    }

    public static final class BackpackItem extends Item {
        private static final String TAG = "Inventory";
        public BackpackItem(Properties properties) { super(properties); }

        @Override
        public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
            ItemStack stack = player.getItemInHand(hand);
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer,
                        new SimpleMenuProvider((id, inventory, menuPlayer) -> new BackpackMenu(id, inventory, hand), stack.getHoverName()),
                        buffer -> buffer.writeEnum(hand));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        static ItemStackHandler inventory(ItemStack backpack) {
            return new ItemStackHandler(27) {
                {
                    CompoundTag tag = backpack.getTagElement(TAG);
                    if (tag != null) deserializeNBT(tag);
                }
                @Override protected void onContentsChanged(int slot) { backpack.getOrCreateTag().put(TAG, serializeNBT()); }
                @Override public boolean isItemValid(int slot, ItemStack stack) { return !(stack.getItem() instanceof BackpackItem); }
            };
        }
    }

    public static final class BackpackMenu extends AbstractContainerMenu {
        private final InteractionHand hand;
        private final ItemStack backpack;
        private static final int BAG_SLOTS = 27;

        public BackpackMenu(int id, Inventory inventory, FriendlyByteBuf data) { this(id, inventory, data.readEnum(InteractionHand.class)); }
        public BackpackMenu(int id, Inventory inventory, InteractionHand hand) {
            super(BACKPACK_MENU.get(), id);
            this.hand = hand;
            this.backpack = inventory.player.getItemInHand(hand);
            ItemStackHandler bag = BackpackItem.inventory(backpack);
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new SlotItemHandler(bag, col + row * 9, 8 + col * 18, 18 + row * 18));
            for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 85 + row * 18));
            for (int col = 0; col < 9; col++) addSlot(new Slot(inventory, col, 8 + col * 18, 143));
        }
        @Override public boolean stillValid(Player player) {
            if (player.level().isClientSide) return true;
            ItemStack held = player.getItemInHand(hand);
            return held == backpack && held.getItem() instanceof BackpackItem;
        }
        @Override public ItemStack quickMoveStack(Player player, int index) {
            Slot slot = slots.get(index);
            if (!slot.hasItem()) return ItemStack.EMPTY;
            ItemStack stack = slot.getItem();
            ItemStack original = stack.copy();
            if (index < BAG_SLOTS) {
                if (!moveItemStackTo(stack, BAG_SLOTS, BAG_SLOTS + 36, true)) return ItemStack.EMPTY;
            } else {
                if (stack.getItem() instanceof BackpackItem || !moveItemStackTo(stack, 0, BAG_SLOTS, false)) return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
            return original;
        }
    }

    public static final class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {
        private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "textures/gui/container/generic_54.png");
        public BackpackScreen(BackpackMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
            imageHeight = 166;
            inventoryLabelY = imageHeight - 94;
        }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partialTick);
            renderTooltip(graphics, mouseX, mouseY);
        }
        @Override protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
            RenderSystem.setShaderColor(1, 1, 1, 1);
            int topHeight = 71;
            graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, topHeight);
            graphics.blit(TEXTURE, leftPos, topPos + topHeight, 0, 126, imageWidth, 96);
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ClientEvents {
        @SubscribeEvent public static void setup(FMLClientSetupEvent event) {
            event.enqueueWork(() -> MenuScreens.register(BACKPACK_MENU.get(), BackpackScreen::new));
        }
    }
}
