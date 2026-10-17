package pl.myclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.myclient.gui.ClickGuiScreen;
import pl.myclient.module.Module;
import pl.myclient.module.ModuleManager;

import java.util.HashMap;
import java.util.Map;

public class MyClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("myclient");
	private static KeyBinding openGui;
	private static final Map<Module, Boolean> KEY_DOWN = new HashMap<>();

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		Config.load();

		openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.myclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
				"key.categories.myclient"));

		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (openGui.wasPressed()) {
				if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
			}
			handleModuleKeys(mc);
			ModuleManager.tick(mc);
		});

		WorldRenderEvents.AFTER_ENTITIES.register(ctx ->
				ModuleManager.render(ctx, MinecraftClient.getInstance()));

		HudRenderCallback.EVENT.register((ctx, tickCounter) ->
				ModuleManager.hud(ctx, MinecraftClient.getInstance()));

		LOGGER.info("Kazuma Client zaladowany. Menu: Right Shift");
	}

	/** Wlacza/wylacza moduly przypisanymi klawiszami (tylko gdy nie ma otwartego ekranu). */
	private static void handleModuleKeys(MinecraftClient mc) {
		if (mc.currentScreen != null || mc.getWindow() == null) return;
		long handle = mc.getWindow().getHandle();
		for (Module m : ModuleManager.all()) {
			if (m.key < 0) continue;
			boolean down = InputUtil.isKeyPressed(handle, m.key);
			boolean was = KEY_DOWN.getOrDefault(m, false);
			if (down && !was) m.toggle();
			KEY_DOWN.put(m, down);
		}
	}
}
