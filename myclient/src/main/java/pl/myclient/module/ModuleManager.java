package pl.myclient.module;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();

	public static void init() {
		// Tu dodajesz kolejne moduly:
		MODULES.add(new FullbrightModule());
		MODULES.add(new HitboxModule());
	}

	public static List<Module> all() {
		return Collections.unmodifiableList(MODULES);
	}

	public static void tick(MinecraftClient mc) {
		for (Module m : MODULES) if (m.isEnabled()) m.onTick(mc);
	}

	public static void render(WorldRenderContext ctx, MinecraftClient mc) {
		for (Module m : MODULES) if (m.isEnabled()) m.onWorldRender(ctx, mc);
	}
}
