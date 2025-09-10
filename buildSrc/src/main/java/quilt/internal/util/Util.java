package quilt.internal.util;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class Util {
	private Util() {
		throw new UnsupportedOperationException();
	}

	public static String replaceRegion(String string, String replacement, int start, int end) {
		return string.substring(0, start) + replacement + string.substring(end);
	}

	public static void clearDirectory(Path directory) {
		try (Stream<Path> contents = Files.walk(directory)) {
			contents
				.filter(content -> !content.equals(directory))
				// reverse so deepest are first
				.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
		} catch (IOException e) {
			throw new GradleException("Failed to walk directory", e);
		}
	}
}
