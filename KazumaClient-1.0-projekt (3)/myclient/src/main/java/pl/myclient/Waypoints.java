package pl.myclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Waypoints {
	public record Point(String name, int x, int y, int z) {}

	private static final List<Point> LIST = new ArrayList<>();

	public static List<Point> all() {
		return Collections.unmodifiableList(LIST);
	}

	public static void add(Point p) {
		LIST.add(p);
	}

	public static void remove(Point p) {
		LIST.remove(p);
	}

	public static void clear() {
		LIST.clear();
	}
}
