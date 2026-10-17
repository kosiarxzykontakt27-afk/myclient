package pl.myclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

/**
 * AirStuck: zamraza Cie w miejscu w powietrzu (bez spadania).
 * Mozesz dalej sie rozgladac, atakowac, stawiac bloki, uzywac przedmiotow oraz pisac komendy i czat.
 * Wlaczasz/wylaczasz przypisanym klawiszem (pole z kreska przy module w menu).
 */
public class AirStuckModule extends Module {
	private Vec3d frozen;
	private ClientWorld frozenWorld;

	public AirStuckModule() {
		super("AirStuck", "Zatrzymuje Cie w powietrzu", Category.PLAYER);
	}

	@Override
	public void onEnable() {
		frozen = null;
		frozenWorld = null;
	}

	@Override
	public void onTick(MinecraftClient mc) {
		ClientPlayerEntity p = mc.player;
		if (p == null || mc.world == null) {
			frozen = null;
			frozenWorld = null;
			return;
		}
		// zapamietaj miejsce zamrozenia (takze po zmianie swiata / odrodzeniu)
		if (frozen == null || frozenWorld != mc.world) {
			frozen = p.getPos();
			frozenWorld = mc.world;
		}
		p.setNoGravity(true);
		p.setVelocity(Vec3d.ZERO);
		p.setPosition(frozen.x, frozen.y, frozen.z);
		p.fallDistance = 0;
	}

	@Override
	public void onDisable() {
		frozen = null;
		frozenWorld = null;
		ClientPlayerEntity p = MinecraftClient.getInstance().player;
		if (p != null) p.setNoGravity(false);
	}
}
