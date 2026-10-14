package pl.myclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
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

	public static void save() {
		JsonObject root = new JsonObject();
		for (Module m : ModuleManager.all()) {
			JsonObject o = new JsonObject();
			o.addProperty("enabled", m.isEnabled());
			for (Setting s : m.getSettings()) {
				if (s instanceof Setting.Bool b) o.addProperty(s.name, b.value);
				else if (s instanceof Setting.Num n) o.addProperty(s.name, n.value);
			}
			root.add(m.name, o);
		}
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
				for (Setting s : m.getSettings()) {
					if (!o.has(s.name)) continue;
					if (s instanceof Setting.Bool b) b.value = o.get(s.name).getAsBoolean();
					else if (s instanceof Setting.Num n) n.set(o.get(s.name).getAsDouble());
				}
			}
		} catch (Exception e) {
			MyClient.LOGGER.error("Nie udalo sie wczytac configu", e);
		}
	}
}
