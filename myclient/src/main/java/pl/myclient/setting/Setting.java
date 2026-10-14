package pl.myclient.setting;

public abstract class Setting {
	public final String name;

	protected Setting(String name) {
		this.name = name;
	}

	public static class Bool extends Setting {
		public boolean value;

		public Bool(String name, boolean value) {
			super(name);
			this.value = value;
		}
	}

	public static class Num extends Setting {
		public double value;
		public final double min, max, step;

		public Num(String name, double value, double min, double max, double step) {
			super(name);
			this.value = value;
			this.min = min;
			this.max = max;
			this.step = step;
		}

		public void set(double v) {
			v = Math.max(min, Math.min(max, v));
			v = Math.round(v / step) * step;
			this.value = Math.max(min, Math.min(max, v));
		}

		public float f() {
			return (float) value;
		}
	}
}
