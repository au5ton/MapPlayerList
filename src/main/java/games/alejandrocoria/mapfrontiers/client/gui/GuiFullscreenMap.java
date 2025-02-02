package games.alejandrocoria.mapfrontiers.client.gui;

import games.alejandrocoria.mapfrontiers.client.ClientProxy;
import games.alejandrocoria.mapfrontiers.client.FrontierOverlay;
import games.alejandrocoria.mapfrontiers.client.FrontiersOverlayManager;
import games.alejandrocoria.mapfrontiers.common.ConfigData;
import games.alejandrocoria.mapfrontiers.common.FrontierData;
import games.alejandrocoria.mapfrontiers.common.event.DeletedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.NewFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedFrontierEvent;
import games.alejandrocoria.mapfrontiers.common.event.UpdatedSettingsProfileEvent;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsProfile;
import games.alejandrocoria.mapfrontiers.common.settings.SettingsUser;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.display.Context;
import journeymap.client.api.display.IThemeButton;
import journeymap.client.api.display.ModPopupMenu;
import journeymap.client.api.display.ThemeButtonDisplay;
import journeymap.client.model.EntityDTO;
import journeymap.client.ui.UIManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import static java.lang.Math.max;
import static java.lang.Math.pow;

@ParametersAreNonnullByDefault
@OnlyIn(Dist.CLIENT)
public class GuiFullscreenMap {
    private enum ChunkDrawing {
        Nothing, Adding, Removing
    }

    private final IClientAPI jmAPI;

    private FrontierOverlay frontierHighlighted;

    private IThemeButton buttonPlayerList;
    private IThemeButton buttonInfo;

    private boolean editing = false;
    private ChunkDrawing drawingChunk = ChunkDrawing.Nothing;
    private ChunkPos lastEditedChunk;

    public GuiFullscreenMap(IClientAPI jmAPI) {
        this.jmAPI = jmAPI;
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void close() {
        if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
        }
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    public void addButtons(ThemeButtonDisplay buttonDisplay) {
        buttonPlayerList = buttonDisplay.addThemeButton(I18n.get("mapplayerlist.title_players"), "open_player_list", b -> buttonPlayerListPressed());
//        buttonFrontiers = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_frontiers"), "frontiers", b -> buttonFrontiersPressed());
//        buttonNew = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_new_frontier"), "new_frontier", b -> buttonNewPressed());
//        buttonInfo = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_frontier_info"), "info_frontier", b -> buttonInfoPressed());
//        buttonEdit = buttonDisplay.addThemeToggleButton(I18n.get("mapfrontiers.button_done_editing"), I18n.get("mapfrontiers.button_edit_frontier"),
//                "edit_frontier", editing, b -> buttonEditToggled());
//        buttonVisible = buttonDisplay.addThemeToggleButton(I18n.get("mapfrontiers.button_hide_frontier"), I18n.get("mapfrontiers.button_show_frontier"),
//                "visible_frontier", false, b -> buttonVisibleToggled());
//        buttonDelete = buttonDisplay.addThemeButton(I18n.get("mapfrontiers.button_delete_frontier"), "delete_frontier", b -> buttonDelete());

        updatebuttons();
    }

    public void addPopupMenu(ModPopupMenu popupMenu) {
        if (editing && frontierHighlighted.getMode() == FrontierData.Mode.Vertex) {
            popupMenu.addMenuItem(I18n.get("mapfrontiers.add_vertex"), p -> buttonAddVertex(p));
            if (frontierHighlighted.getSelectedVertexIndex() != -1) {
                popupMenu.addMenuItem(I18n.get("mapfrontiers.remove_vertex"), p -> buttonRemoveVertex());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedSettingsProfileEvent(UpdatedSettingsProfileEvent event) {
        updatebuttons();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onNewFrontierEvent(NewFrontierEvent event) {
        if (event.frontierOverlay.getDimension() != jmAPI.getUIState(Context.UI.Fullscreen).dimension) {
            return;
        }

        if (event.playerID == -1 || Minecraft.getInstance().player.getId() == event.playerID) {
            stopEditing();
            if (frontierHighlighted != null) {
                frontierHighlighted.setHighlighted(false);
            }

            frontierHighlighted = event.frontierOverlay;
            frontierHighlighted.setHighlighted(true);

            updatebuttons();

            if (ConfigData.afterCreatingFrontier == ConfigData.AfterCreatingFrontier.Edit) {
                buttonEditToggled();
            } else if (ConfigData.afterCreatingFrontier == ConfigData.AfterCreatingFrontier.Info) {
                buttonInfoPressed();
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onUpdatedFrontierEvent(UpdatedFrontierEvent event) {
        if (frontierHighlighted != null && frontierHighlighted.getId().equals(event.frontierOverlay.getId())) {
            frontierHighlighted = event.frontierOverlay;
            frontierHighlighted.setHighlighted(true);
            editing = false;
            updatebuttons();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDeletedFrontierEvent(DeletedFrontierEvent event) {
        if (frontierHighlighted != null && frontierHighlighted.getId().equals(event.frontierID)) {
            frontierHighlighted = null;
            editing = false;
            updatebuttons();
        }
    }

    public void stopEditing() {
        if (editing) {
            editing = false;
            boolean personalFrontier = frontierHighlighted.getPersonal();
            FrontiersOverlayManager frontierManager = ClientProxy.getFrontiersOverlayManager(personalFrontier);
            frontierManager.clientUpdatefrontier(frontierHighlighted);
        }
    }

    public void updatebuttons() {
        if (buttonInfo == null) {
            return;
        }

        SettingsProfile profile = ClientProxy.getSettingsProfile();
        SettingsUser playerUser = new SettingsUser(Minecraft.getInstance().player);
        SettingsProfile.AvailableActions actions = SettingsProfile.getAvailableActions(profile, frontierHighlighted, playerUser);

        buttonPlayerList.setEnabled(!editing);
//        buttonFrontiers.setEnabled(!editing);
//        buttonNew.setEnabled(!editing);
//        buttonInfo.setEnabled(frontierHighlighted != null && !editing);
//        buttonEdit.setEnabled(actions.canUpdate && frontierHighlighted.getVisible() && frontierHighlighted.getFullscreenVisible());
//        buttonVisible.setEnabled(actions.canUpdate && !editing);
//        buttonDelete.setEnabled(actions.canDelete && !editing);
//
//        if (frontierHighlighted != null) {
//            buttonVisible.setToggled(frontierHighlighted.getVisible() && frontierHighlighted.getFullscreenVisible());
//        } else {
//            buttonVisible.setToggled(false);
//        }
    }

    private void buttonPlayerListPressed() {
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiPlayerList(jmAPI, this));
    }

    private void buttonInfoPressed() {
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), new GuiFrontierInfo(jmAPI, frontierHighlighted));
    }

    private void buttonEditToggled() {
//        buttonEdit.toggle();
//        boolean toggled = buttonEdit.getToggled();
//        if (toggled) {
//            editing = true;
//            drawingChunk = ChunkDrawing.Nothing;
//        } else {
//            stopEditing();
//        }

        updatebuttons();
    }

    private void buttonDelete() {
        if (editing) {
            stopEditing();
        }

        boolean personalFrontier = frontierHighlighted.getPersonal();
        FrontiersOverlayManager frontierManager = ClientProxy.getFrontiersOverlayManager(personalFrontier);
        frontierManager.clientDeleteFrontier(frontierHighlighted);
        frontierHighlighted = null;
        updatebuttons();
    }

    private void buttonAddVertex(BlockPos pos) {
        frontierHighlighted.selectClosestEdge(pos);
        frontierHighlighted.addVertex(pos);

        updatebuttons();
    }

    private void buttonRemoveVertex() {
        frontierHighlighted.removeSelectedVertex();

        updatebuttons();
    }

    public boolean isEditingVertices() {
        return editing && frontierHighlighted.getMode() == FrontierData.Mode.Vertex;
    }

    public boolean isEditingChunks() {
        return editing && frontierHighlighted.getMode() == FrontierData.Mode.Chunk;
    }

    public FrontierOverlay getSelected() {
        return frontierHighlighted;
    }

    public void selectFrontier(@Nullable FrontierOverlay frontier) {
        if (frontier != null && frontier.getDimension().equals(jmAPI.getUIState(Context.UI.Fullscreen).dimension)) {
            if (frontierHighlighted != frontier) {
                if (frontierHighlighted != null) {
                    frontierHighlighted.setHighlighted(false);
                }

                frontierHighlighted = frontier;
                frontierHighlighted.setHighlighted(true);
            }
        } else if (frontierHighlighted != null) {
            frontierHighlighted.setHighlighted(false);
            frontierHighlighted = null;
        }

        updatebuttons();
    }

    public void selectPlayer(@Nullable EntityDTO player) {
        if (player != null && player.getDimension().equals(jmAPI.getUIState(Context.UI.Fullscreen).dimension)) {
            // TODO: are we supposed to do something here?
        }

        updatebuttons();
    }

    public boolean mapClicked(ResourceKey<Level> dimension, BlockPos position, int button) {
        double maxDistanceToClosest = pow(2.0, max(4.0 - jmAPI.getUIState(Context.UI.Fullscreen).zoom, 1.0));

        if (editing && frontierHighlighted != null) {
            if (frontierHighlighted.getMode() == FrontierData.Mode.Vertex) {
                frontierHighlighted.selectClosestVertex(position, maxDistanceToClosest);
            } else if (button == 1) {
                lastEditedChunk = new ChunkPos(position);
                if (frontierHighlighted.toggleChunk(lastEditedChunk)) {
                    drawingChunk = ChunkDrawing.Adding;
                } else {
                    drawingChunk = ChunkDrawing.Removing;
                }
                return true;
            }
            return false;
        }

        FrontiersOverlayManager globalManager = ClientProxy.getFrontiersOverlayManager(false);
        FrontiersOverlayManager personalManager = ClientProxy.getFrontiersOverlayManager(true);

        if (globalManager == null || personalManager == null) {
            return false;
        }

        FrontierOverlay newFrontierHighlighted = personalManager.getFrontierInPosition(dimension, position, maxDistanceToClosest);
        if (newFrontierHighlighted == null) {
            newFrontierHighlighted = globalManager.getFrontierInPosition(dimension, position, maxDistanceToClosest);
        }

        selectFrontier(newFrontierHighlighted);

        return false;
    }

    public boolean mapDragged(ResourceKey<Level> dimension, BlockPos position) {
        if (!editing) {
            return false;
        }

        if (frontierHighlighted == null || !frontierHighlighted.getDimension().equals(dimension)) {
            return false;
        }

        if (frontierHighlighted.getMode() == FrontierData.Mode.Chunk) {
            return false;
        }

        if (frontierHighlighted.getSelectedVertexIndex() == -1) {
            return false;
        }

        float snapDistance = (float) pow(2.0, max(4.0 - jmAPI.getUIState(Context.UI.Fullscreen).zoom, 1.0));
        frontierHighlighted.moveSelectedVertex(position, snapDistance);
        return true;
    }

    public void mouseMoved(ResourceKey<Level> dimension, BlockPos position) {
        if (!editing || drawingChunk == ChunkDrawing.Nothing) {
            return;
        }

        if (frontierHighlighted == null || !frontierHighlighted.getDimension().equals(dimension)) {
            return;
        }

        if (frontierHighlighted.getMode() != FrontierData.Mode.Chunk) {
            return;
        }

        ChunkPos chunk = new ChunkPos(position);
        if (chunk.equals(lastEditedChunk)) {
            return;
        }

        lastEditedChunk = chunk;

        if (drawingChunk == ChunkDrawing.Adding) {
            frontierHighlighted.addChunk(chunk);
        } else {
            frontierHighlighted.removeChunk(chunk);
        }
    }

    @SubscribeEvent
    public void mouseEvent(InputEvent.MouseButton event) {
        if (event.getAction() != GLFW.GLFW_RELEASE || event.getButton() != 1) {
            return;
        }

        if (!editing || drawingChunk == ChunkDrawing.Nothing) {
            return;
        }

        if (frontierHighlighted.getMode() != FrontierData.Mode.Chunk) {
            return;
        }

        drawingChunk = ChunkDrawing.Nothing;
    }
}
