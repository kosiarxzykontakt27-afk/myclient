package pl.myclient.module;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
	private static final List<Module> MODULES = new ArrayList<>();

	private static void simple(String name, Category category) {
		MODULES.add(new SimpleModule(name, "Jeszcze bez funkcji", category));
	}

	public static void init() {
		// Moduly z prawdziwym dzialaniem:
		MODULES.add(new FullbrightModule());
		MODULES.add(new HitboxModule());
		MODULES.add(new AirStuckModule());
		MODULES.add(new AntiWebModule());

		// Moduly bez funkcji (tylko przelacznik ON/OFF) - do uzupelnienia:
		simple("AntyKostka", Category.PLAYER);
		simple("AutoDrip", Category.PLAYER);
		simple("AutoNielota", Category.PLAYER);
		simple("EntityVisibility", Category.VISUALS);

		simple("BetterPainting", Category.UTILS);
		simple("NoSignGui", Category.UTILS);
		simple("ErrorKiller", Category.UTILS);
		simple("NameProtect", Category.UTILS);
		simple("Slot-Lock", Category.UTILS);

		simple("ElytraSwapper", Category.PLAYER);
		simple("FastPlace", Category.PLAYER);
		simple("FastBreak", Category.PLAYER);
		simple("FreeCam", Category.PLAYER);
		simple("NoPush", Category.PLAYER);
		simple("AutoSprint", Category.PLAYER);
		simple("InvMove", Category.PLAYER);
		simple("FastExp", Category.PLAYER);

		simple("NameTags", Category.VISUALS);
		simple("Tracers", Category.VISUALS);
		simple("ESP", Category.VISUALS);
		simple("BlockESP", Category.VISUALS);
		simple("ItemESP", Category.VISUALS);
		simple("TrapESP", Category.VISUALS);
		simple("X-Ray", Category.VISUALS);

		simple("FakeLag", Category.COMBAT);
		simple("AutoArmor", Category.COMBAT);
		simple("AutoTotem", Category.COMBAT);
		simple("NoFriendDamage", Category.COMBAT);

		simple("NukerLegit", Category.PLAYER);
		simple("Nuker+", Category.PLAYER);

		simple("AutoTrap", Category.COMBAT);
		simple("ShieldBreaker", Category.COMBAT);

		simple("CustomCape", Category.VISUALS);

		simple("PlayerInfo", Category.UTILS);
		simple("Spammer", Category.UTILS);
		simple("CPS+", Category.UTILS);
		simple("FakeEventowka", Category.UTILS);
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

	public static void hud(DrawContext ctx, MinecraftClient mc) {
		for (Module m : MODULES) if (m.isEnabled()) m.onHudRender(ctx, mc);
	}
}
