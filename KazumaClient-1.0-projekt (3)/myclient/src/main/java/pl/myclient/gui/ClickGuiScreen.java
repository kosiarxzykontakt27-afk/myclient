package pl.myclient.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import pl.myclient.Config;
import pl.myclient.Waypoints;
import pl.myclient.module.Category;
import pl.myclient.module.Module;
import pl.myclient.module.ModuleManager;
import pl.myclient.setting.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClickGuiScreen extends Screen {
	private static final float TS = 0.85f;
	private static final String VERSION_LABEL = "Kazuma Client 1.0";
	private static final int DEF_W = 482, DEF_H = 308, SIDE_W = 99, TOP_H = 36, FOOT_H = 19, CARD_H = 26;

	private static final int[] HUE = {0, 30, 275, 220, 135};
	private static final String[] THEME_PL = {"Czerwony", "Pomara\u0144czowy", "Fioletowy", "Niebieski", "Zielony"};
	private static final String[] THEME_EN = {"Red", "Orange", "Purple", "Blue", "Green"};

	// kolory (przezroczyste, jak na wzorze)
	private static final int C_WIN = 0xC4101010;
	private static final int C_LINE = 0x40FFFFFF;
	private static final int C_BOX = 0x22FFFFFF;
	private static final int C_SEL = 0x2EFFFFFF;
	private static final int C_HOVER = 0x16FFFFFF;
	private static final int C_CARD = 0x0DFFFFFF;
	private static final int C_CARD_HOVER = 0x1AFFFFFF;
	private static final int C_BORDER = 0x30FFFFFF;
	private static final int C_PILL_OFF = 0x40FFFFFF;
	private static final int C_KNOB = 0xFFEDEDED;
	private static final int C_TEXT = 0xFFE6E6E6;
	private static final int C_SOFT = 0xFFC8C8C8;
	private static final int C_MUTED = 0xFF9A9A9A;
	private int cAccent, cAccentText;

	private enum Tab {
		ALL("All", "All", null),
		COMBAT("Combat", "Combat", Category.COMBAT),
		PLAYER("Player", "Player", Category.PLAYER),
		VISUALS("Visuals", "Visuals", Category.VISUALS),
		UTILS("Utils", "Utils", Category.UTILS),
		RADAR("Radar", "Radar", null),
		SETTINGS("Ustawienia", "Settings", null),
		FRIENDS("Znajomi", "Friends", null);

		final String pl, en;
		final Category category;

		Tab(String pl, String en, Category category) {
			this.pl = pl;
			this.en = en;
			this.category = category;
		}

		String label() {
			return GuiSettings.lang == 0 ? pl : en;
		}
	}

	private enum Kind { TAB, SEARCH, CARD, BIND, CARD_TOGGLE, CARD_VALUE, WP, WP_DEL, BUTTON, SETTING, LABEL, SCROLLBAR }

	private record Row(int x, int y, int w, int h, Kind kind, Module module, Setting setting,
					   String label, String label2, boolean flag, Runnable left, Runnable right) {
		boolean contains(double mx, double my) {
			return mx >= x && mx < x + w && my >= y && my < y + h;
		}
	}

	private List<Row> rows = new ArrayList<>();
	private Tab tab = Tab.ALL;
	private int scroll = 0;
	private int contentHeight = 0;
	private String search = "";
	private boolean searchFocused = false;
	private Module openModule = null;
	private Module binding = null;
	private Row dragRow = null;

	public ClickGuiScreen() {
		super(Text.literal("Kazuma Client"));
		applyTheme();
	}

	private String t(String pl, String en) {
		return GuiSettings.lang == 0 ? pl : en;
	}

	// ---------- motyw (kolor akcentu) ----------
	private static int rot(int rgb, float deg) {
		float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
		float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
		float h;
		if (d == 0) h = 0;
		else if (max == r) h = ((g - b) / d) % 6;
		else if (max == g) h = (b - r) / d + 2;
		else h = (r - g) / d + 4;
		h *= 60;
		if (h < 0) h += 360;
		float s = max == 0 ? 0 : d / max, v = max;
		h = (h + deg) % 360;
		if (h < 0) h += 360;
		float c = v * s, x = c * (1 - Math.abs((h / 60f) % 2 - 1)), m = v - c;
		float rr, gg, bb;
		switch ((int) (h / 60f) % 6) {
			case 0 -> { rr = c; gg = x; bb = 0; }
			case 1 -> { rr = x; gg = c; bb = 0; }
			case 2 -> { rr = 0; gg = c; bb = x; }
			case 3 -> { rr = 0; gg = x; bb = c; }
			case 4 -> { rr = x; gg = 0; bb = c; }
			default -> { rr = c; gg = 0; bb = x; }
		}
		return 0xFF000000 | ((int) ((rr + m) * 255 + 0.5f) << 16) | ((int) ((gg + m) * 255 + 0.5f) << 8)
				| (int) ((bb + m) * 255 + 0.5f);
	}

	private void applyTheme() {
		if (GuiSettings.theme < 0 || GuiSettings.theme >= HUE.length) GuiSettings.theme = 0;
		float d = HUE[GuiSettings.theme];
		cAccent = rot(0xFFFF4D4F, d);
		cAccentText = rot(0xFFFF5A5F, d);
	}

	// ---------- rysowanie ----------
	private static int inset(int r, int i) {
		double dy = r - i - 0.5;
		return (int) Math.round(r - Math.sqrt(r * r - dy * dy));
	}

	private static void rfill(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
		r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
		if (r <= 0) {
			c.fill(x1, y1, x2, y2, color);
			return;
		}
		for (int i = 0; i < r; i++) {
			int in = inset(r, i);
			c.fill(x1 + in, y1 + i, x2 - in, y1 + i + 1, color);
			c.fill(x1 + in, y2 - i - 1, x2 - in, y2 - i, color);
		}
		c.fill(x1, y1 + r, x2, y2 - r, color);
	}

	/** Cienka ramka (1 px) wzdluz zaokraglonego prostokata - bez podwojnego mieszania przezroczystosci. */
	private static void rring(DrawContext c, int x1, int y1, int x2, int y2, int r, int color) {
		r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
		int h = y2 - y1;
		int ir = Math.max(0, r - 1);
		int ih = h - 2;
		for (int y = 0; y < h; y++) {
			int oi = y < r ? inset(r, y) : (y >= h - r ? inset(r, h - 1 - y) : 0);
			int ox1 = x1 + oi, ox2 = x2 - oi;
			if (y == 0 || y == h - 1) {
				c.fill(ox1, y1 + y, ox2, y1 + y + 1, color);
				continue;
			}
			int iy = y - 1;
			int ii = 0;
			if (ir > 0) {
				if (iy < ir) ii = inset(ir, iy);
				else if (iy >= ih - ir) ii = inset(ir, ih - 1 - iy);
			}
			int ix1 = Math.max(x1 + 1 + ii, ox1 + 1);
			int ix2 = Math.min(x2 - 1 - ii, ox2 - 1);
			c.fill(ox1, y1 + y, ix1, y1 + y + 1, color);
			c.fill(ix2, y1 + y, ox2, y1 + y + 1, color);
		}
	}

	private Text styled(String s) {
		Text base = Text.literal(s);
		return GuiSettings.bold ? Text.literal(s).formatted(Formatting.BOLD) : base;
	}

	private void txtScaled(DrawContext c, String s, int x, int y, int color, float sc) {
		MatrixStack m = c.getMatrices();
		m.push();
		m.translate((float) x, (float) y, 0f);
		m.scale(sc, sc, 1f);
		c.drawText(this.textRenderer, styled(s), 0, 0, color, GuiSettings.shadow);
		m.pop();
	}

	private int twScaled(String s, float sc) {
		return (int) (this.textRenderer.getWidth(styled(s)) * sc);
	}

	private void txt(DrawContext c, String s, int x, int y, int color) {
		txtScaled(c, s, x, y, color, TS);
	}

	private int tw(String s) {
		return twScaled(s, TS);
	}

	private int ty(int boxY, int boxH) {
		return boxY + (boxH - 7) / 2;
	}

	private String fitScaled(String s, int maxPx, float sc) {
		if (twScaled(s, sc) <= maxPx) return s;
		while (s.length() > 1 && twScaled(s + "..", sc) > maxPx) s = s.substring(0, s.length() - 1);
		return s + "..";
	}

	private String fit(String s, int maxPx) {
		return fitScaled(s, maxPx, TS);
	}

	private void toggle(DrawContext c, int x, int y, boolean on) {
		rfill(c, x, y, x + 17, y + 10, 5, on ? cAccent : C_PILL_OFF);
		int kx = on ? x + 17 - 1 - 8 : x + 1;
		rfill(c, kx, y + 1, kx + 8, y + 9, 4, on ? 0xFFFFFFFF : C_KNOB);
	}

	private void card(DrawContext c, int x, int y, int w, int h, boolean hover) {
		rfill(c, x + 1, y + 1, x + w - 1, y + h - 1, 3, hover ? C_CARD_HOVER : C_CARD);
		rring(c, x, y, x + w, y + h, 4, C_BORDER);
	}

	// ---------- geometria ----------
	// Cale menu jest rysowane w "wspolrzednych projektu" (rozmiar 1.0) i skalowane macierza o K().
	private float K() { return Math.max(0.5f, Math.min(1.5f, GuiSettings.scale)); }
	private int W() { return Math.min(DEF_W, (int) ((this.width - 16) / K())); }
	private int H() { return Math.min(DEF_H, (int) ((this.height - 16) / K())); }
	private int wx() { return 0; }
	private int wy() { return 0; }
	private float ox() { return (this.width - W() * K()) / 2f; }
	private float oy() { return (this.height - H() * K()) / 2f; }
	private double toDX(double x) { return (x - ox()) / K(); }
	private double toDY(double y) { return (y - oy()) / K(); }
	private int clipTop() { return wy() + TOP_H + 1; }
	private int clipBottom() { return wy() + H() - FOOT_H - 1; }
	private int viewH() { return clipBottom() - (wy() + 44); }

	private boolean inClip(double my) {
		return my >= clipTop() && my < clipBottom();
	}

	private static boolean isContent(Kind k) {
		return k != Kind.TAB && k != Kind.SEARCH && k != Kind.SCROLLBAR;
	}

	private int maxScroll() {
		return Math.max(0, contentHeight - viewH());
	}

	private int thumbH(int trackH) {
		if (contentHeight <= viewH()) return trackH;
		return Math.max(14, (int) ((long) trackH * viewH() / contentHeight));
	}

	private int thumbY(int trackY, int trackH, int th) {
		int ms = maxScroll();
		if (ms == 0) return trackY;
		return trackY + (int) Math.round((trackH - th) * (double) scroll / ms);
	}

	private boolean clampScroll() {
		int old = scroll;
		scroll = Math.max(0, Math.min(maxScroll(), scroll));
		return old != scroll;
	}

	private void add(Kind k, int x, int y, int w, int h, Module m, Setting s, String label, String label2,
					 boolean flag, Runnable left, Runnable right) {
		rows.add(new Row(x, y, w, h, k, m, s, label, label2, flag, left, right));
	}

	private String keyName(int key) {
		if (key < 0) return "-";
		try {
			return InputUtil.fromKeyCode(key, 0).getLocalizedText().getString();
		} catch (Exception e) {
			return "#" + key;
		}
	}

	private String plural(int n, String one, String few, String many) {
		int m10 = n % 10, m100 = n % 100;
		if (n == 1) return one;
		if (m10 >= 2 && m10 <= 4 && !(m100 >= 12 && m100 <= 14)) return few;
		return many;
	}

	// ---------- budowa listy elementow ----------
	private void buildRows() {
		rows = new ArrayList<>();
		int W = W(), wx = wx(), wy = wy();

		// kategorie po lewej
		Tab[] tabs = Tab.values();
		for (int i = 0; i < tabs.length; i++) {
			final Tab tb = tabs[i];
			int y = i < 5 ? wy + 46 + i * 20 : wy + 157 + (i - 5) * 20;
			add(Kind.TAB, wx + 4, y, 91, 19, null, null, tb.label(), null, tab == tb, () -> {
				tab = tb;
				scroll = 0;
				openModule = null;
				binding = null;
			}, null);
		}

		// pole szukania (srodek gornego paska)
		add(Kind.SEARCH, wx + (W - 113) / 2, wy + 10, 113, 17, null, null, "", null, false, () -> searchFocused = true, null);

		int cx = wx + 107, cw = W - 114, colW = (cw - 3) / 2;
		int y0 = wy + 44 - scroll;
		int bottom = y0;
		String q = search.toLowerCase(Locale.ROOT);

		if (openModule != null) {
			add(Kind.BUTTON, cx, y0, cw, CARD_H, null, null, "< " + t("Wstecz", "Back") + " (" + openModule.name + ")",
					null, false, () -> openModule = null, null);
			bottom = y0 + CARD_H;
			int i = 0;
			for (Setting st : openModule.getSettings()) {
				int y = y0 + 32 + (i * 59) / 2;
				add(Kind.SETTING, cx, y, cw, CARD_H, openModule, st, st.name, null, false, null, null);
				bottom = y + CARD_H;
				i++;
			}
		} else if (tab == Tab.RADAR) {
			List<Waypoints.Point> pts = new ArrayList<>();
			for (Waypoints.Point p : Waypoints.all()) {
				if (q.isEmpty() || p.name().toLowerCase(Locale.ROOT).contains(q)) pts.add(p);
			}
			add(Kind.BUTTON, cx, y0, cw, CARD_H, null, null,
					"+ " + t("Dodaj znacznik (Twoja pozycja)", "Add waypoint (your position)"), null, false, () -> {
						MinecraftClient mc = MinecraftClient.getInstance();
						if (mc.player == null) return;
						int n = Waypoints.all().size() + 1;
						Waypoints.add(new Waypoints.Point("Punkt " + n, (int) Math.floor(mc.player.getX()),
								(int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())));
						Config.save();
					}, null);
			bottom = y0 + CARD_H;
			MinecraftClient mc = MinecraftClient.getInstance();
			for (int i = 0; i < pts.size(); i++) {
				Waypoints.Point p = pts.get(i);
				int y = y0 + ((i + 1) * 59) / 2;
				String dist = "";
				if (mc.player != null) {
					double dx = p.x() + 0.5 - mc.player.getX(), dy = p.y() - mc.player.getY(), dz = p.z() + 0.5 - mc.player.getZ();
					dist = (int) Math.sqrt(dx * dx + dy * dy + dz * dz) + " m";
				}
				add(Kind.WP, cx, y, cw, CARD_H, null, null, p.name() + "  (" + p.x() + ", " + p.y() + ", " + p.z() + ")",
						dist, false, null, null);
				add(Kind.WP_DEL, cx + cw - 24, y + (CARD_H - 10) / 2, 16, 10, null, null, "\u00d7", null, false, () -> {
					Waypoints.remove(p);
					Config.save();
				}, null);
				bottom = y + CARD_H;
			}
			if (pts.isEmpty()) {
				add(Kind.LABEL, cx + 2, y0 + 34, cw, 10, null, null, t("Brak znacznik\u00f3w.", "No waypoints."), null, false, null, null);
				bottom = y0 + 44;
			}
		} else if (tab == Tab.SETTINGS) {
			String themeName = GuiSettings.lang == 0 ? THEME_PL[GuiSettings.theme] : THEME_EN[GuiSettings.theme];
			add(Kind.CARD_TOGGLE, cx, y0, colW, CARD_H, null, null, t("Gruba czcionka", "Bold font"), null,
					GuiSettings.bold, () -> {
						GuiSettings.bold = !GuiSettings.bold;
						Config.save();
					}, null);
			add(Kind.CARD_VALUE, cx + colW + 3, y0, colW, CARD_H, null, null, t("J\u0119zyk", "Language"),
					GuiSettings.lang == 0 ? "Polski" : "English", false, () -> {
						GuiSettings.lang = 1 - GuiSettings.lang;
						Config.save();
					}, null);
			add(Kind.CARD_VALUE, cx, y0 + 29, colW, CARD_H, null, null, t("Kolor akcentu", "Accent color"), themeName,
					false, () -> {
						GuiSettings.theme = (GuiSettings.theme + 1) % HUE.length;
						applyTheme();
						Config.save();
					}, null);
			add(Kind.CARD_VALUE, cx + colW + 3, y0 + 29, colW, CARD_H, null, null, t("Rozmiar menu", "Menu size"),
					String.format(Locale.US, "%.2f", GuiSettings.scale), false, () -> {
						GuiSettings.scale = Math.min(1.5f, Math.round((GuiSettings.scale + 0.05f) * 100) / 100f);
						Config.save();
					}, () -> {
						GuiSettings.scale = Math.max(0.5f, Math.round((GuiSettings.scale - 0.05f) * 100) / 100f);
						Config.save();
					});
			add(Kind.LABEL, cx + 2, y0 + 62, cw, 10, null, null,
					t("Rozmiar: LPM - wi\u0119kszy, PPM - mniejszy", "Size: LMB - bigger, RMB - smaller"), null, false, null, null);
			bottom = y0 + 72;
		} else if (tab == Tab.FRIENDS) {
			add(Kind.LABEL, cx + 2, y0 + 2, cw, 10, null, null, t("Brak znajomych.", "No friends yet."), null, false, null, null);
			bottom = y0 + 12;
		} else {
			List<Module> mods = new ArrayList<>();
			for (Module m : ModuleManager.all()) {
				if (tab.category != null && m.category != tab.category) continue;
				if (!q.isEmpty() && !m.name.toLowerCase(Locale.ROOT).contains(q)) continue;
				mods.add(m);
			}
			mods.sort((a, b) -> a.name.compareToIgnoreCase(b.name)); // kolejnosc alfabetyczna
			for (int i = 0; i < mods.size(); i++) {
				Module m = mods.get(i);
				int x = cx + (i % 2) * (colW + 3), y = y0 + ((i / 2) * 59) / 2;
				add(Kind.CARD, x, y, colW, CARD_H, m, null, m.name, null, false, m::toggle,
						m.getSettings().isEmpty() ? null : () -> openModule = m);
				// pole klawisza (kreska = brak)
				add(Kind.BIND, x + colW - 47, y + (CARD_H - 10) / 2, 16, 10, m, null, "", null, binding == m,
						() -> binding = m, () -> {
							m.key = -1;
							Config.save();
						});
				bottom = Math.max(bottom, y + CARD_H);
			}
			if (mods.isEmpty()) {
				add(Kind.LABEL, cx + 2, y0 + 2, cw, 10, null, null, t("Brak modu\u0142\u00f3w.", "No modules."), null, false, null, null);
				bottom = y0 + 12;
			}
		}
		contentHeight = bottom - y0;

		// cienki suwak po prawej
		add(Kind.SCROLLBAR, wx + W - 5, clipTop(), 3, clipBottom() - clipTop(), null, null, "", null, false, null, null);
	}

	// ---------- render ----------
	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		buildRows();
		if (clampScroll()) buildRows();

		int W = W(), H = H(), wx = wx(), wy = wy();
		int cx = wx + 107, cw = W - 114;
		int dmx = (int) Math.floor(toDX(mouseX)), dmy = (int) Math.floor(toDY(mouseY));

		MatrixStack ms = ctx.getMatrices();
		ms.push();
		ms.translate(ox(), oy(), 0f);
		ms.scale(K(), K(), 1f);

		// okno
		rfill(ctx, wx, wy, wx + W, wy + H, 6, C_WIN);
		rring(ctx, wx, wy, wx + W, wy + H, 6, 0x28FFFFFF);
		ctx.fill(wx + 1, wy + TOP_H, wx + W - 1, wy + TOP_H + 1, C_LINE);
		ctx.fill(wx + 1, wy + H - FOOT_H, wx + W - 1, wy + H - FOOT_H + 1, C_LINE);
		ctx.fill(wx + SIDE_W, wy + TOP_H + 1, wx + SIDE_W + 1, wy + H - FOOT_H, C_LINE);
		ctx.fill(wx + 7, wy + 151, wx + 92, wy + 152, C_LINE);

		// tytul (lewy gorny rog) i napis w prawym gornym rogu
		String title = openModule != null ? openModule.name : tab.label();
		txt(ctx, title, wx + 8, ty(wy, TOP_H), 0xFFFFFFFF);
		txt(ctx, VERSION_LABEL, wx + W - 10 - tw(VERSION_LABEL), ty(wy, TOP_H), cAccentText);

		// stopka: licznik
		int active = 0;
		for (Module m : ModuleManager.all()) if (m.isEnabled()) active++;
		int total = ModuleManager.all().size();
		String footer = GuiSettings.lang == 0
				? active + " " + plural(active, "aktywny", "aktywne", "aktywnych") + " \u00b7 " + total + " "
				+ plural(total, "modu\u0142", "modu\u0142y", "modu\u0142\u00f3w")
				: active + " active \u00b7 " + total + " modules";
		txt(ctx, footer, wx + W - 8 - tw(footer), ty(wy + H - FOOT_H + 1, FOOT_H - 1), C_MUTED);

		for (int pass = 0; pass < 2; pass++) {
			if (pass == 1) {
				// obszar przycinania ustawiamy poza skala (we wspolrzednych ekranu menu)
				ms.pop();
				int sx1 = (int) Math.floor(ox() + (cx - 1) * K());
				int sy1 = (int) Math.floor(oy() + clipTop() * K());
				int sx2 = (int) Math.ceil(ox() + (wx + W - 6) * K());
				int sy2 = (int) Math.ceil(oy() + clipBottom() * K());
				ctx.enableScissor(sx1, sy1, sx2, sy2);
				ms.push();
				ms.translate(ox(), oy(), 0f);
				ms.scale(K(), K(), 1f);
			}
			for (Row row : rows) {
				if (isContent(row.kind()) != (pass == 1)) continue;
				boolean hover = row.contains(dmx, dmy) && (pass == 0 || inClip(dmy));
				int x = row.x(), y = row.y(), w = row.w(), h = row.h();

				switch (row.kind()) {
					case TAB -> {
						if (row.flag()) {
							rfill(ctx, x, y, x + w, y + h, 4, C_SEL);
							ctx.fill(x, y + 4, x + 2, y + h - 4, 0xFFFFFFFF);
						} else if (hover) {
							rfill(ctx, x, y, x + w, y + h, 4, C_HOVER);
						}
						txt(ctx, row.label(), x + 8, ty(y, h), row.flag() ? 0xFFFFFFFF : C_SOFT);
					}
					case SEARCH -> {
						rfill(ctx, x + 1, y + 1, x + w - 1, y + h - 1, 3, C_BOX);
						rring(ctx, x, y, x + w, y + h, 4, 0x18FFFFFF);
						if (!search.isEmpty() || searchFocused) {
							String shown = search;
							if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) shown += "_";
							txt(ctx, fit(shown, w - 10), x + 5, ty(y, h), C_TEXT);
						}
					}
					case CARD -> {
						card(ctx, x, y, w, h, hover);
						txt(ctx, fit(row.label(), w - 70), x + 7, ty(y, h), C_TEXT);
						toggle(ctx, x + w - 8 - 17, y + (h - 10) / 2, row.module().isEnabled());
					}
					case BIND -> {
						rfill(ctx, x + 1, y + 1, x + w - 1, y + h - 1, 2, hover ? C_CARD_HOVER : 0x14FFFFFF);
						rring(ctx, x, y, x + w, y + h, 3, 0x44FFFFFF);
						String k = row.flag() ? "..." : fitScaled(keyName(row.module().key), w - 4, 0.65f);
						int kw = twScaled(k, 0.65f);
						txtScaled(ctx, k, x + (w - kw) / 2, y + (h - 6) / 2, row.flag() ? cAccentText : C_MUTED, 0.65f);
					}
					case CARD_TOGGLE -> {
						card(ctx, x, y, w, h, hover);
						txt(ctx, fit(row.label(), w - 40), x + 7, ty(y, h), C_TEXT);
						toggle(ctx, x + w - 8 - 17, y + (h - 10) / 2, row.flag());
					}
					case CARD_VALUE -> {
						card(ctx, x, y, w, h, hover);
						txt(ctx, row.label(), x + 7, ty(y, h), C_TEXT);
						txt(ctx, row.label2(), x + w - 8 - tw(row.label2()), ty(y, h), cAccentText);
					}
					case WP -> {
						card(ctx, x, y, w, h, false);
						txt(ctx, fit(row.label(), w - 120), x + 7, ty(y, h), C_TEXT);
						if (row.label2() != null && !row.label2().isEmpty()) {
							txt(ctx, row.label2(), x + w - 32 - tw(row.label2()), ty(y, h), C_MUTED);
						}
					}
					case WP_DEL -> {
						rfill(ctx, x + 1, y + 1, x + w - 1, y + h - 1, 2, hover ? cAccent : 0x14FFFFFF);
						rring(ctx, x, y, x + w, y + h, 3, 0x44FFFFFF);
						int dw = tw(row.label());
						txt(ctx, row.label(), x + (w - dw) / 2, ty(y, h), C_TEXT);
					}
					case BUTTON -> {
						card(ctx, x, y, w, h, hover);
						txt(ctx, row.label(), x + (w - tw(row.label())) / 2, ty(y, h), C_TEXT);
					}
					case LABEL -> txt(ctx, row.label(), x, y, C_MUTED);
					case SETTING -> {
						if (row.setting() instanceof Setting.Bool b) {
							card(ctx, x, y, w, h, hover);
							txt(ctx, b.name, x + 7, ty(y, h), C_TEXT);
							toggle(ctx, x + w - 8 - 17, y + (h - 10) / 2, b.value);
						} else if (row.setting() instanceof Setting.Num n) {
							card(ctx, x, y, w, h, false);
							double frac = (n.value - n.min) / (n.max - n.min);
							int fw = (int) ((w - 2) * frac);
							if (fw > 0) rfill(ctx, x + 1, y + 1, x + 1 + fw, y + h - 1, 3, (cAccent & 0x00FFFFFF) | 0x66000000);
							String val = n.step >= 1 ? String.valueOf((int) n.value) : String.format(Locale.US, "%.2f", n.value);
							txt(ctx, n.name, x + 7, ty(y, h), C_TEXT);
							txt(ctx, val, x + w - 8 - tw(val), ty(y, h), C_TEXT);
						}
					}
					case SCROLLBAR -> {
						int th = thumbH(h);
						int thY = thumbY(y, h, th);
						boolean can = contentHeight > viewH();
						rfill(ctx, x, thY, x + w, thY + th, 1, can ? 0x78FFFFFF : 0x24FFFFFF);
					}
				}
			}
			if (pass == 1) ctx.disableScissor();
		}
		ms.pop();
	}

	// ---------- mysz i klawiatura ----------
	private void applySlider(Row row, double mouseX, double mouseY) {
		if (row.kind() == Kind.SCROLLBAR) {
			int ms = maxScroll();
			if (ms > 0) {
				int th = thumbH(row.h());
				double f = (mouseY - row.y() - th / 2.0) / (row.h() - th);
				f = Math.max(0, Math.min(1, f));
				scroll = (int) Math.round(f * ms);
			}
			return;
		}
		if (row.setting() instanceof Setting.Num n) {
			double frac = (mouseX - row.x()) / row.w();
			frac = Math.max(0, Math.min(1, frac));
			n.set(n.min + frac * (n.max - n.min));
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		mouseX = toDX(mouseX);
		mouseY = toDY(mouseY);
		buildRows();
		searchFocused = false;
		binding = null;
		for (int i = rows.size() - 1; i >= 0; i--) {
			Row row = rows.get(i);
			if (!row.contains(mouseX, mouseY)) continue;
			if (isContent(row.kind()) && !inClip(mouseY)) continue;

			if (row.kind() == Kind.SCROLLBAR) {
				if (button == 0) {
					dragRow = row;
					applySlider(row, mouseX, mouseY);
				}
				return true;
			}
			if (row.kind() == Kind.SETTING) {
				if (button == 0) {
					if (row.setting() instanceof Setting.Bool b) {
						b.value = !b.value;
						Config.save();
					} else if (row.setting() instanceof Setting.Num) {
						dragRow = row;
						applySlider(row, mouseX, mouseY);
					}
				}
				return true;
			}
			Runnable action = button == 0 ? row.left() : (button == 1 ? row.right() : null);
			if (action != null) {
				action.run();
				if (row.kind() == Kind.CARD && button == 0) Config.save();
			}
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
		if (dragRow != null) {
			applySlider(dragRow, toDX(mouseX), toDY(mouseY));
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
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scroll -= (int) Math.round(verticalAmount * 14);
		clampScroll();
		return true;
	}

	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (searchFocused && chr >= 32 && chr != 127 && search.length() < 24) {
			search += chr;
			scroll = 0;
			openModule = null;
			return true;
		}
		return super.charTyped(chr, modifiers);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (binding != null) {
			if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_BACKSPACE) binding.key = -1;
			else binding.key = keyCode;
			binding = null;
			Config.save();
			return true;
		}
		if (searchFocused) {
			if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
				if (!search.isEmpty()) search = search.substring(0, search.length() - 1);
				scroll = 0;
				return true;
			}
			if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
				searchFocused = false;
				return true;
			}
		} else if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
			close();
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
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
