package pl.myclient.module;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import pl.myclient.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
	public final String name;
	public final String description;
	private boolean enabled;
	private final List<Setting> settings = new ArrayList<>();

	protected Module(String name, String description) {
		this.name = name;
		this.description = description;
	}

	protected <T extends Setting> T add(T setting) {
		settings.add(setting);
		return setting;
	}

	public List<Setting> getSettings() {
		return settings;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void toggle() {
		setEnabled(!enabled);
	}

	public void setEnabled(boolean value) {
		if (this.enabled == value) return;
		this.enabled = value;
		if (value) onEnable(); else onDisable();
	}

	/** Uzywane przy wczytywaniu configu (bez wywolywania hookow). */
	public void setEnabledSilently(boolean value) {
		this.enabled = value;
	}

	public void onEnable() {}
	public void onDisable() {}
	public void onTick(MinecraftClient mc) {}
	public void onWorldRender(WorldRenderContext ctx, MinecraftClient mc) {}
}
