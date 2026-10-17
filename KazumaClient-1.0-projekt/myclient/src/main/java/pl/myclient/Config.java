package pl.myclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import pl.myclient.gui.GuiSettings;
import pl.myclient.module.Module;
import pl.myclient.module.ModuleManager;
import pl.myclient.setting.Setting;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("myclient.json");
	private static final String GUI_KEY = "_gui";
	private static final String WP_KEY = "_waypoints";

	private static void writeSetting(JsonObject o, Setting s) {
		if (s instanceof Setting.Bool b) o.addProperty(s.name, b.value);
		else if (s instanceof Setting.Num n) o.addProperty(s.name, n.value);
	}

	private static void readSetting(JsonObject o, Setting s) {
		if (!o.has(s.name)) return;
		if (s instanceof Setting.Bool b) b.value = o.get(s.name).getAsBoolean();
		else if (s instanceof Setting.Num n) n.set(o.get(s.name).getAsDouble());
	}

	public static void save() {
		JsonObject root = new JsonObject();
		for (Module m : ModuleManager.all()) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", m.isEnabled());
			o.addProperty("key", m.key);
			for (Setting s : m.getSettings()) writeSetting(o, s);
			root.add(m.name, o);
		}

		JsonObject gui = new JsonObject();
		gui.addProperty("theme", GuiSettings.theme);
		gui.addProperty("lang", GuiSettings.lang);
		gui.addProperty("shadow", GuiSettings.shadow);
		gui.addProperty("bold", GuiSettings.bold);
		gui.addProperty("scale", GuiSettings.scale);
		root.add(GUI_KEY, gui);

		JsonArray wps = new JsonArray();
		for (Waypoints.Point p : Waypoints.all()) {
			JsonObject o = new JsonObject();
			o.addProperty("name", p.name());
			o.addProperty("x", p.x());
			o.addProperty("y", p.y());
			o.addProperty("z", p.z());
			wps.add(o);
		}
		root.add(WP_KEY, wps);

		try (Writer w = Files.newBufferedWriter(FILE)) {
			GSON.toJson(root, w);
		} catch (Exception e) {
			MyClient.LOGGER.error("Nie udalo sie zapisac configu", e);
		}
	}

	public static void load() {
		if (!Files.exists(FILE)) return;
		try (Reader r = Files.newBufferedReader(FILE)) {
			JsonObject root = JsonParser.parseReader(r).getAsJsonObject();
			for (Module m : ModuleManager.all()) {
				if (!root.has(m.name)) continue;
				JsonObject o = root.getAsJsonObject(m.name);
				if (o.has("enabled")) m.setEnabledSilently(o.get("enabled").getAsBoolean());
				if (o.has("key")) m.key = o.get("key").getAsInt();
				for (Setting s : m.getSettings()) readSetting(o, s);
			}
			if (root.has(GUI_KEY)) {
				JsonObject gui = root.getAsJsonObject(GUI_KEY);
				if (gui.has("theme")) GuiSettings.theme = Math.max(0, gui.get("theme").getAsInt());
				if (gui.has("lang")) GuiSettings.lang = gui.get("lang").getAsInt() == 1 ? 1 : 0;
				if (gui.has("shadow")) GuiSettings.shadow = gui.get("shadow").getAsBoolean();
				if (gui.has("bold")) GuiSettings.bold = gui.get("bold").getAsBoolean();
				if (gui.has("scale")) GuiSettings.scale = Math.max(0.5f, Math.min(1.5f, gui.get("scale").getAsFloat()));
			}
			if (root.has(WP_KEY)) {
				Waypoints.clear();
				for (JsonElement e : root.getAsJsonArray(WP_KEY)) {
					JsonObject o = e.getAsJsonObject();
					Waypoints.add(new Waypoints.Point(o.get("name").getAsString(),
							o.get("x").getAsInt(), o.get("y").getAsInt(), o.get("z").getAsInt()));
				}
			}
		} catch (Exception e) {
			MyClient.LOGGER.error("Nie udalo sie wczytac configu", e);
		}
	}
}
