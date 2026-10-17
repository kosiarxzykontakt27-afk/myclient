package pl.myclient.module;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import pl.myclient.setting.Setting;

/** Wlasne rysowanie hitboxow - dziala bez F3+B. */
public class HitboxModule extends Module {
	private final Setting.Bool playersOnly = add(new Setting.Bool("Tylko gracze", false));
	private final Setting.Num expand = add(new Setting.Num("Powi\u0119kszenie", 0.0, 0.0, 1.0, 0.05));
	private final Setting.Num red = add(new Setting.Num("Czerwony", 255, 0, 255, 1));
	private final Setting.Num green = add(new Setting.Num("Zielony", 60, 0, 255, 1));
	private final Setting.Num blue = add(new Setting.Num("Niebieski", 60, 0, 255, 1));

	public HitboxModule() {
		super("Hitbox+", "Widoczne hitboxy z kolorem", Category.COMBAT);
	}

	@Override
	public void onWorldRender(WorldRenderContext ctx, MinecraftClient mc) {
		if (mc.world == null || mc.player == null) return;
		MatrixStack matrices = ctx.matrixStack();
		VertexConsumerProvider consumers = ctx.consumers();
		if (matrices == null || consumers == null) return;

		Vec3d cam = ctx.camera().getPos();
		float delta = ctx.tickCounter().getTickDelta(false);
		boolean firstPerson = mc.options.getPerspective().isFirstPerson();
		VertexConsumer lines = consumers.getBuffer(RenderLayer.getLines());

		float r = red.f() / 255f, g = green.f() / 255f, b = blue.f() / 255f;

		matrices.push();
		matrices.translate(-cam.x, -cam.y, -cam.z);
		for (Entity e : mc.world.getEntities()) {
			if (e == mc.player && firstPerson) continue;
			if (playersOnly.value && !(e instanceof PlayerEntity)) continue;

			Vec3d offset = e.getLerpedPos(delta).subtract(e.getPos());
			Box box = e.getBoundingBox().offset(offset).expand(expand.value);
			VertexRendering.drawBox(matrices, lines, box, r, g, b, 1.0f);
		}
		matrices.pop();
	}
}
