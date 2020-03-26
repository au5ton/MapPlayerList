package games.alejandrocoria.mapfrontiers.common;

import java.util.ArrayList;

import javax.annotation.ParametersAreNonnullByDefault;

import games.alejandrocoria.mapfrontiers.common.network.PacketFrontier;
import games.alejandrocoria.mapfrontiers.common.network.PacketHandler;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

@ParametersAreNonnullByDefault
public class CommonProxy {
    private FrontiersManager frontiersManager;

    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void init(FMLInitializationEvent event) {

    }

    public void postInit(FMLPostInitializationEvent event) {

    }

    public void serverStarting(FMLServerStartingEvent event) {
        frontiersManager = new FrontiersManager();
        frontiersManager.loadOrCreateData();
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        frontiersManager = null;
    }

    @SubscribeEvent
    public void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (frontiersManager == null) {
            return;
        }

        for (ArrayList<FrontierData> frontiers : frontiersManager.getAllFrontiers().values()) {
            for (FrontierData frontier : frontiers) {
                PacketHandler.INSTANCE.sendTo(new PacketFrontier(frontier), (EntityPlayerMP) event.player);
            }
        }
    }
}