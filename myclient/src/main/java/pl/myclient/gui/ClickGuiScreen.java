package pl.myclient.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import pl.myclient.Config;
import pl.myclient.module.Module;
import pl.myclient.module.ModuleManager;
import pl.myclient.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen {
	private static final int W = 150, ROW_H = 18;

	private record Row(int x, int y, int w, int h, Module module, Setting setting) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private final List<Module> expanded = new ArrayList<>();
	private List<Row> rows = new ArrayList<>();
	private Row dragRow = null;

	public ClickGuiScreen() {
		super(Text.literal("MyClient"));
	}

	private void buildRows() {
		rows = new ArrayList<>();
		int x = this.width / 2 - W / 2;
		int y = this.height / 2 - 60 + ROW_H + 2;
		for (Module m : ModuleManager.all()) {
			rows.add(new Row(x, y, W, ROW_H, m, null));
			y += ROW_H;
			if (expanded.contains(m)) {
				for (Setting s : m.getSettings()) {
					rows.add(new Row(x + 6, y, W - 6, ROW_H, m, s));
					y += ROW_H;
				}
			}
		}
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		buildRows();

		int x = this.width / 2 - W / 2;
		int top = this.height / 2 - 60;
		ctx.fill(x, top, x + W, top + ROW_H, 0xFF1B1B1B);
		ctx.drawTextWithShadow(this.textRenderer, "MyClient", x + 6, top + 5, 0xFFFFFFFF);

		for (Row row : rows) {
			boolean hover = row.contains(mouseX, mouseY);
			if (row.setting() == null) {
				Module m = row.module();
				int bg = m.isEnabled() ? (hover ? 0xFF3E9A44 : 0xFF2E7D32) : (hover ? 0xFF404040 : 0xFF2A2A2A);
				ctx.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), bg);
				ctx.drawTextWithShadow(this.textRenderer, m.name, row.x() + 6, row.y() + 5, 0xFFFFFFFF);
				if (!m.getSettings().isEmpty()) {
					String arrow = expanded.contains(m) ? "v" : ">";
					ctx.drawTextWithShadow(this.textRenderer, arrow, row.x() + row.w() - 12, row.y() + 5, 0xFFAAAAAA);
				}
			} else if (row.setting() instanceof Setting.Bool b) {
				ctx.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), hover ? 0xFF353535 : 0xFF222222);
				String t = b.name + ": " + (b.value ? "ON" : "OFF");
				ctx.drawTextWithShadow(this.textRenderer, t, row.x() + 6, row.y() + 5, b.value ? 0xFF7CFC7C : 0xFFCCCCCC);
			} else if (row.setting() instanceof Setting.Num n) {
				ctx.fill(row.x(), row.y(), row.x() + row.w(), row.y() + row.h(), 0xFF222222);
				double frac = (n.value - n.min) / (n.max - n.min);
				ctx.fill(row.x(), row.y(), row.x() + (int) (row.w() * frac), row.y() + row.h(), 0xFF3A5F8A);
				String val = n.step >= 1 ? String.valueOf((int) n.value) : String.format("%.2f", n.value);
				ctx.drawTextWithShadow(this.textRenderer, n.name + ": " + val, row.x() + 6, row.y() + 5, 0xFFFFFFFF);
			}
		}

		ctx.drawTextWithShadow(this.textRenderer,
				"LPM: wlacz/wylacz  |  PPM: ustawienia", x - 20, this.height / 2 + 70, 0xFF888888);
	}

	private void applySlider(Row row, double mouseX) {
		if (row.setting() instanceof Setting.Num n) {
			double frac = (mouseX - row.x()) / row.w();
			frac = Math.max(0, Math.min(1, frac));
			n.set(n.min + frac * (n.max - n.min));
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		buildRows();
		for (Row row : rows) {
			if (!row.contains(mouseX, mouseY)) continue;
			if (row.setting() == null) {
				if (button == 0) {
					row.module().toggle();
				} else if (button == 1 && !row.module().getSettings().isEmpty()) {
					if (!expanded.remove(row.module())) expanded.add(row.module());
				}
			} else if (row.setting() instanceof Setting.Bool b) {
				if (button == 0) b.value = !b.value;
			} else if (row.setting() instanceof Setting.Num && button == 0) {
				dragRow = row;
				applySlider(row, mouseX);
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (dragRow != null) {
			applySlider(dragRow, mouseX);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, dx, dy);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (dragRow != null) {
			dragRow = null;
			Config.save();
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		Config.save();
		super.close();
	}
}
