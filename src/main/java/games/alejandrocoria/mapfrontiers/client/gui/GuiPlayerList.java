package games.alejandrocoria.mapfrontiers.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.client.util.ScreenHelper;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.event.DeletedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.NewFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import journeymap.client.api.IClientAPI;
import journeymap.client.data.WorldData;
import journeymap.client.model.EntityDTO;
import journeymap.client.model.EntityHelper;
import journeymap.client.ui.UIManager;
import journeymap.client.waypoint.WaypointStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiPlayerList extends Screen implements GuiScrollBox.ScrollBoxResponder {
    private final IClientAPI jmAPI;
    private final GuiFullscreenMap fullscreenMap;

    private float scaleFactor;
    private int actualWidth;
    private int actualHeight;

    private GuiScrollBox frontiers;
    private GuiScrollBox filterDimension;
    private GuiSettingsButton buttonResetFilters;
    private GuiSettingsButton buttonViewInMap;
    private GuiSettingsButton buttonDone;

    public GuiPlayerList(IClientAPI jmAPI, GuiFullscreenMap fullscreenMap) {
        super(CommonComponents.EMPTY);
        this.jmAPI = jmAPI;
        this.fullscreenMap = fullscreenMap;

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init() {
        scaleFactor = ScreenHelper.getScaleFactorThatFit(this, 772, 332);
        actualWidth = (int) (width * scaleFactor);
        actualHeight = (int) (height * scaleFactor);

        Component title = Component.translatable("mapplayerlist.title_players");
        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2, 8, GuiSimpleLabel.Align.Center, title, GuiColors.WHITE));

        frontiers = new GuiScrollBox(actualWidth / 2 - 300, 50, 450, actualHeight - 100, 24, this);

//        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2 + 170, 74, GuiSimpleLabel.Align.Left,
//                Component.translatable("mapfrontiers.filter_type"), GuiColors.SETTINGS_TEXT));
//
//        filterType = new GuiScrollBox(actualWidth / 2 + 170, 86, 200, 48, 16, this);
//        filterType.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.All), ConfigData.FilterFrontierType.All.ordinal()));
//        filterType.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.Global), ConfigData.FilterFrontierType.Global.ordinal()));
//        filterType.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierType.Personal), ConfigData.FilterFrontierType.Personal.ordinal()));
//        filterType.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierType.ordinal());

//        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2 + 170, 144, GuiSimpleLabel.Align.Left,
//                Component.translatable("mapfrontiers.filter_owner"), GuiColors.SETTINGS_TEXT));
//
//        filterOwner = new GuiScrollBox(actualWidth / 2 + 170, 156, 200, 48, 16, this);
//        filterOwner.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierOwner.All), ConfigData.FilterFrontierOwner.All.ordinal()));
//        filterOwner.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierOwner.You), ConfigData.FilterFrontierOwner.You.ordinal()));
//        filterOwner.addElement(new GuiRadioListElement(font, ConfigData.getTranslatedEnum(ConfigData.FilterFrontierOwner.Others), ConfigData.FilterFrontierOwner.Others.ordinal()));
//        filterOwner.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierOwner.ordinal());

        // y used to be 214
        addRenderableOnly(new GuiSimpleLabel(font, actualWidth / 2 + 170, 214 - 140, GuiSimpleLabel.Align.Left,
                Component.translatable("mapfrontiers.filter_dimension"), GuiColors.SETTINGS_TEXT));

        filterDimension = new GuiScrollBox(actualWidth / 2 + 170, 226 - 140, 200, actualHeight - 296, 16, this);
        filterDimension.addElement(new GuiRadioListElement(font, Component.translatable("mapfrontiers.config.All"), "all".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, Component.translatable("mapfrontiers.config.Current"), "current".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, Component.literal("minecraft:overworld"), "minecraft:overworld".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, Component.literal("minecraft:the_nether"), "minecraft:the_nether".hashCode()));
        filterDimension.addElement(new GuiRadioListElement(font, Component.literal("minecraft:the_end"), "minecraft:the_end".hashCode()));
        addDimensionsToFilter();
        filterDimension.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierDimension.hashCode());
        if (filterDimension.getSelectedElement() == null) {
            ConfigData.filterFrontierDimension = "all";
            filterDimension.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierDimension.hashCode());
        }

        buttonResetFilters = new GuiSettingsButton(font, actualWidth / 2 + 170, 50, 110,
                Component.translatable("mapfrontiers.reset_filters"), this::buttonPressed);

        buttonViewInMap = new GuiSettingsButton(font, actualWidth / 2 - 175, actualHeight - 28, 110,
                Component.translatable("mapplayerlist.view_in_map"), this::buttonPressed);

//        buttonCreate = new GuiSettingsButton(font, actualWidth / 2 - 295, actualHeight - 28, 110,
//                Component.translatable("mapfrontiers.create"), this::buttonPressed);
//        buttonInfo = new GuiSettingsButton(font, actualWidth / 2 - 175, actualHeight - 28, 110,
//                Component.translatable("mapfrontiers.info"), this::buttonPressed);
//        buttonDelete = new GuiSettingsButton(font, actualWidth / 2 - 55, actualHeight - 28, 110,
//                Component.translatable("mapfrontiers.delete"), this::buttonPressed);
//        buttonDelete.setTextColors(GuiColors.SETTINGS_BUTTON_TEXT_DELETE, GuiColors.SETTINGS_BUTTON_TEXT_DELETE_HIGHLIGHT);
//        buttonVisible = new GuiSettingsButton(font, actualWidth / 2 + 65, actualHeight - 28, 110,
//                Component.translatable("mapfrontiers.hide"), this::buttonPressed);
        buttonDone = new GuiSettingsButton(font, actualWidth / 2 + 185, actualHeight - 28, 110,
                Component.translatable("gui.done"), this::buttonPressed);

        addRenderableWidget(frontiers);
        //addRenderableWidget(filterType);
        //addRenderableWidget(filterOwner);
        addRenderableWidget(filterDimension);
        addRenderableWidget(buttonResetFilters);
        addRenderableWidget(buttonViewInMap);
//        addRenderableWidget(buttonCreate);
//        addRenderableWidget(buttonInfo);
//        addRenderableWidget(buttonDelete);
//        addRenderableWidget(buttonVisible);
        addRenderableWidget(buttonDone);

        updateFrontiers();

//        if (fullscreenMap.getSelected() != null) {
//            frontiers.selectElementIf((element) -> ((GuiPlayerListElement) element).getFrontier().getId().equals(fullscreenMap.getSelected().getId()));
//        }

        updateButtons();
    }

    @Override
    public void render(PoseStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        renderBackground(matrixStack);

        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        if (scaleFactor != 1.f) {
            matrixStack.pushPose();
            matrixStack.scale(1.0f / scaleFactor, 1.0f / scaleFactor, 1.0f);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (scaleFactor != 1.f) {
            matrixStack.popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        return super.mouseClicked(mouseX * scaleFactor, mouseY * scaleFactor, mouseButton);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        mouseX *= scaleFactor;
        mouseY *= scaleFactor;

        for (GuiEventListener w : children()) {
            if (w instanceof GuiScrollBox) {
                ((GuiScrollBox) w).mouseReleased();
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX * scaleFactor, mouseY * scaleFactor, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX * scaleFactor, mouseY * scaleFactor, button, dragX * scaleFactor, dragY * scaleFactor);
    }

    protected void buttonPressed(Button button) {
        if (button == buttonResetFilters) {
            ConfigData.filterFrontierDimension = "all";
            filterDimension.selectElementIf((element) -> ((GuiRadioListElement) element).getId() == ConfigData.filterFrontierDimension.hashCode());
            updateFrontiers();
            updateButtons();
        } else if (button == buttonViewInMap) {
            // Open selected player in map
            EntityDTO player = ((GuiPlayerListElement) frontiers.getSelectedElement()).getPlayer();
            var position = player.getPosition();
            ForgeHooksClient.popGuiLayer(minecraft);
            UIManager.INSTANCE.openFullscreenMap().centerOn(position.x, position.z);
        } else if (button == buttonDone) {
            ForgeHooksClient.popGuiLayer(minecraft);
        }
    }

    @Override
    public void elementClicked(GuiScrollBox scrollBox, GuiScrollBox.ScrollElement element) {
        if (scrollBox == frontiers) {
            EntityDTO player = ((GuiPlayerListElement) element).getPlayer();
            fullscreenMap.selectPlayer(player);
        } else if (scrollBox == filterDimension) {
            int selected = ((GuiRadioListElement) element).getId();
            if (selected == "all".hashCode()) {
                ConfigData.filterFrontierDimension = "all";
            } else if (selected == "current".hashCode()) {
                ConfigData.filterFrontierDimension = "current";
            } else {
                ConfigData.filterFrontierDimension = getDimensionFromHash(selected);
            }
            updateFrontiers();
        }

        updateButtons();
    }

    @Override
    public void elementDelete(GuiScrollBox scrollBox, GuiScrollBox.ScrollElement element) {
        updateButtons();
    }

    @Override
    public void removed() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    private void addDimensionsToFilter() {
        List<WorldData.DimensionProvider> dimensionProviders = WorldData.getDimensionProviders(WaypointStore.INSTANCE.getLoadedDimensions());
        for (WorldData.DimensionProvider dimension : dimensionProviders) {
            if (!dimension.getDimensionId().equals("minecraft:overworld") && !dimension.getDimensionId().equals("minecraft:the_nether") && !dimension.getDimensionId().equals("minecraft:the_end")) {
                filterDimension.addElement(new GuiRadioListElement(font, Component.literal(dimension.getDimensionId()), dimension.getDimensionId().hashCode()));
            }
        }
    }

    private String getDimensionFromHash(int hash) {
        List<WorldData.DimensionProvider> dimensionProviders = WorldData.getDimensionProviders(WaypointStore.INSTANCE.getLoadedDimensions());
        for (WorldData.DimensionProvider dimension : dimensionProviders) {
            if (dimension.getDimensionId().hashCode() == hash) {
                return dimension.getDimensionId();
            }
        }

        return "";
    }

    private void updateFrontiers() {
        EntityDTO selectedPlayer = frontiers.getSelectedElement() == null ? null : ((GuiPlayerListElement) frontiers.getSelectedElement()).getPlayer();
        String playerID = selectedPlayer == null ? null : selectedPlayer.entityId;

        frontiers.removeAll();

        for (EntityDTO player : EntityHelper.getPlayersNearby()) {
            if (checkFilterDimension(player)) {
                frontiers.addElement(new GuiPlayerListElement(font, renderables, player));
            }
        }

        if (playerID != null) {
            frontiers.selectElementIf((element) -> ((GuiPlayerListElement) element).getPlayer().entityId.equals(playerID));
        }
    }

    private boolean checkFilterDimension(EntityDTO player) {
        if (ConfigData.filterFrontierDimension.equals("all")) {
            return true;
        }

        String dimension = ConfigData.filterFrontierDimension;
        if (dimension.equals("current")) {
            dimension = minecraft.level.dimension().location().toString();
        }

        return player.getDimension().location().toString().equals(dimension);
    }

    private void updateButtons() {
        buttonViewInMap.visible = frontiers.getSelectedElement() != null;

//        SettingsProfile profile = ClientProxy.getSettingsProfile();
//        SettingsUser playerUser = new SettingsUser(Minecraft.getInstance().player);
        //SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontier, playerUser);



//        buttonInfo.visible = frontiers.getSelectedElement() != null;
//        buttonDelete.visible = actions.canDelete;
//        buttonVisible.visible = actions.canUpdate;
//
//        if (frontier != null && frontier.getVisible()) {
//            buttonVisible.setMessage(Component.translatable("mapfrontiers.hide"));
//        } else {
//            buttonVisible.setMessage(Component.translatable("mapfrontiers.show"));
//        }
    }
}
