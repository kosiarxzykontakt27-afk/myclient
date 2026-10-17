package pl.myclient.module;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * AntiWeb: ignoruje spowolnienie od pajeczyn (i innych blokow spowalniajacych, np. jagod, sniegu puszystego).
 * Mozesz biec przez pajeczyny bez zwalniania oraz spadac przez nie normalnie.
 * Dziala przez zerowanie "mnoznika spowolnienia" gracza po kazdym ticku.
 */
public class AntiWebModule extends Module {
	public AntiWebModule() {
		super("AntiWeb", "Ignoruje pajeczyny", Category.PLAYER);
	}

	@Override
	public void onTick(MinecraftClient mc) {
		ClientPlayerEntity p = mc.player;
		if (p == null) return;
		// Blok ustawia mnoznik spowolnienia, ktory zadzialalby w NASTEPNYM ruchu - zerujemy go.
		p.slowMovement(Blocks.AIR.getDefaultState(), Vec3d.ZERO);
	}
}
