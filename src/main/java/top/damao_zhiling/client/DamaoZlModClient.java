package top.damao_zhiling.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import top.damao_zhiling.DamaoZlMod;

import javax.swing.*;

@Environment(EnvType.CLIENT)
public class DamaoZlModClient implements ClientModInitializer {

    private static boolean windowOpened = false;

    @Override
    public void onInitializeClient() {
        DamaoZlMod.LOGGER.info("Damao Mod Client started");

        // 启动后台线程
        new Thread(() -> {
            while (!windowOpened) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (client.player != null) {
                    windowOpened = true;
                    client.execute(() -> SwingUtilities.invokeLater(
                            () -> new CommandManagerWindow().setVisible(true)
                    ));
                }
            }
        }, "DamaoWindowOpener").start();
    }
}