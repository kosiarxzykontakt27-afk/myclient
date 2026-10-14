package pl.myclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.myclient.gui.ClickGuiScreen;
import pl.myclient.module.ModuleManager;

public class MyClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("myclient");
	private static KeyBinding openGui;

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
			ModuleManager.tick(mc);
		});

		WorldRenderEvents.AFTER_ENTITIES.register(ctx ->
				ModuleManager.render(ctx, MinecraftClient.getInstance()));

		LOGGER.info("MyClient zaladowany. Menu: Right Shift");
	}
}
