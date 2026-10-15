package com.maxusclient.client.controller;

import com.maxusclient.client.gui.MaxusMenuScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;

public class InputController {
    private static boolean wasPeriodPressed = false;

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean isPeriodPressed = InputConstants.isKeyDown(InputConstants.KEY_PERIOD);
            if (isPeriodPressed && !wasPeriodPressed) {
                if (client.gui.screen() == null) {
                    Minecraft.getInstance().setScreenAndShow(new MaxusMenuScreen());
                }
            }
            wasPeriodPressed = isPeriodPressed;
        });
    }
}
