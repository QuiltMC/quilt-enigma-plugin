package org.quiltmc.enigma_plugin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.launchermeta.version.v1.DownloadableFile;
import org.quiltmc.launchermeta.version.v1.Version;
import org.quiltmc.launchermeta.version_manifest.VersionEntry;
import org.quiltmc.launchermeta.version_manifest.VersionManifest;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

public class MojmapTest {
	private static final Path cacheDir = Path.of("build/mojmap_cache");
	private static final File cachedServerJar = cacheDir.resolve("server.jar").toFile();
	private static final File cachedServerMappings = cacheDir.resolve("server-mappings.txt").toFile();

	private static EnigmaProject project;

	@BeforeAll
	static void downloadMojmaps() throws IOException {
		if (!cachedServerJar.exists() || !cachedServerMappings.exists()) {
			System.out.println("Could not find server jar or mappings, downloading...");
			String mcVersion = "1.21";

			InputStreamReader reader;
			URL manifestUrl = new URL("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
			InputStreamReader manifestReader = new InputStreamReader(new BufferedInputStream(manifestUrl.openStream()));
			VersionManifest manifest = VersionManifest.fromReader(manifestReader);
			Optional<VersionEntry> entry = manifest.getVersions().stream().filter(e -> e.getId().equals(mcVersion)).findAny();
			if (entry.isPresent()) {
				reader = new InputStreamReader(new BufferedInputStream(new URL(entry.get().getUrl()).openStream()));
			} else {
				throw new RuntimeException("could not download " + mcVersion + " from manifest!");
			}

			Version version = Version.fromReader(reader);
			Optional<DownloadableFile> serverJar = version.getDownloads().getServer();
			Optional<DownloadableFile> serverMappings = version.getDownloads().getServerMappings();

			if (serverJar.isEmpty() || serverMappings.isEmpty()) {
				throw new RuntimeException("could not download " + mcVersion + " from manifest!");
			}

			System.out.println("Downloading server jar...");
			download(serverJar.get().getUrl(), cachedServerJar);
			System.out.println("Downloading server mappings...");
			download(serverMappings.get().getUrl(), cachedServerMappings);
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static void download(String url, File file) throws IOException {
		file.toPath().resolve("..").toFile().mkdirs();
		file.createNewFile();

		try (BufferedInputStream in = new BufferedInputStream(new URL(url).openStream()); FileOutputStream fileOutputStream = new FileOutputStream(file)) {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = in.read(buffer, 0, 1024)) != -1) {
				fileOutputStream.write(buffer, 0, bytesRead);
			}
		}
	}

	@BeforeEach
	@SuppressWarnings("OptionalGetWithoutIsPresent")
	void setupEnigma() throws IOException, MappingParseException {
		var profile = EnigmaProfile.parse(new StringReader("""
			{
					"services": {
						"name_proposal": [
							{
								"id": "quiltmc:mojang_name_proposal"
							}
						]
					}
			}
			"""));


		var enigma = Enigma.builder().setProfile(profile).build();
		project = enigma.openJar(cachedServerJar.toPath(), new ClasspathClassProvider(), ProgressListener.createEmpty());

		// todo should be proposed instead of manually inserted!
		project.setMappings(enigma.getReadWriteService(cachedServerMappings.toPath()).get().read(cachedServerMappings.toPath()), ProgressListener.createEmpty());
	}

	@Test
	void test() {
		ClassEntry a = new ClassEntry("a");
		assert project.getRemapper().getMapping(a).targetName().equals("com/mojang/math/Axis");
	}
}
