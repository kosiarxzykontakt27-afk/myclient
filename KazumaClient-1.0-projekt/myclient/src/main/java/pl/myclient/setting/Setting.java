package pl.myclient.setting;

public abstract class Setting {
	public final String name;

	protected Setting(String name) {
		this.name = name;
	}

	public abstract void reset();

	public static class Bool extends Setting {
		public boolean value;
		private final boolean def;

		public Bool(String name, boolean value) {
			super(name);
			this.value = value;
			this.def = value;
		}

		@Override
		public void reset() {
			value = def;
		}
	}

	public static class Num extends Setting {
		public double value;
		public final double min, max, step;
		private final double def;

		public Num(String name, double value, double min, double max, double step) {
			super(name);
			this.value = value;
			this.def = value;
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

		@Override
		public void reset() {
			value = def;
		}
	}
}
