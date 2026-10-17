package pl.myclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

/** Fullbright przez klienckie Night Vision (bez mixinow, dziala tylko lokalnie). */
public class FullbrightModule extends Module {
	public FullbrightModule() {
		super("Fullbright", "Widzisz w ciemno\u015bci", Category.VISUALS);
	}

	@Override
	public void onTick(MinecraftClient mc) {
		if (mc.player == null) return;
		StatusEffectInstance current = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
		if (current == null || current.getDuration() < 400) {
			mc.player.addStatusEffect(new StatusEffectInstance(
					StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
		}
	}

	@Override
	public void onDisable() {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player != null) {
			mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
		}
	}
}
