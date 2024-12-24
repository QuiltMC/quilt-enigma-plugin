package org.quiltmc.enigma_plugin;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.EnigmaProfile;
import org.quiltmc.enigma.api.EnigmaProject;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.class_provider.ClasspathClassProvider;
import org.quiltmc.enigma.api.source.TokenType;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.serde.MappingParseException;
import org.quiltmc.enigma.api.translation.representation.MethodDescriptor;
import org.quiltmc.enigma.api.translation.representation.TypeDescriptor;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.enigma.api.translation.representation.entry.FieldEntry;
import org.quiltmc.enigma.api.translation.representation.entry.MethodEntry;
import org.quiltmc.enigma.util.validation.PrintNotifier;
import org.quiltmc.enigma.util.validation.ValidationContext;
import org.quiltmc.enigma_plugin.proposal.MojmapNameProposer;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

// todo evaluate whether this obsoletes any proposers (non-hashed proposer is def on the chopping block)
// todo make sure of priority

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
	void setupEnigma() throws IOException {
		var profile = EnigmaProfile.parse(new StringReader("""
			{
					"services": {
						"name_proposal": [
							{
								"id": "quiltmc:name_proposal/fallback",
								"args": {
									"mojmap_path": "./build/mojmap_cache/server-mappings.txt",
									"package_name_overrides_path": "./src/test/resources/mojmap_test/package_name_overrides.json"
								}
							},
							{
								"id": "quiltmc:name_proposal/unchecked"
							}
						]
					}
			}
			"""));


		var enigma = Enigma.builder().setProfile(profile).build();
		project = enigma.openJar(cachedServerJar.toPath(), new ClasspathClassProvider(), ProgressListener.createEmpty());
	}

	@Test
	void testStaticProposal() {
		// assert that all types propose properly
		// note that mojmaps do not contain params

		ClassEntry a = new ClassEntry("a"); // Axis
		assertMapping(a, "com/mojang/math/Axis", TokenType.JAR_PROPOSED);

		ClassEntry b = new ClassEntry("b");
		FieldEntry pi = new FieldEntry(b, "a", new TypeDescriptor("F"));
		assertMapping(pi, "PI", TokenType.JAR_PROPOSED);

		ClassEntry dnt = new ClassEntry("dnt");
		MethodEntry getExplosionResistance = new MethodEntry(dnt, "e", new MethodDescriptor("()F"));
		assertMapping(getExplosionResistance, "getExplosionResistance", TokenType.JAR_PROPOSED);
	}

	@Test
	void testDynamicProposal() {
		ClassEntry a = new ClassEntry("a");
		assertMapping(a, "com/mojang/math/Axis", TokenType.JAR_PROPOSED);

		ValidationContext vc = new ValidationContext(PrintNotifier.INSTANCE);
		project.getRemapper().putMapping(vc, a, new EntryMapping("gaming/Gaming"));

		assertMapping(a, "com/mojang/math/Gaming", TokenType.DEOBFUSCATED);
	}

	@SuppressWarnings("OptionalGetWithoutIsPresent")
	@Test
	void testOverrideGeneration() throws IOException, MappingParseException {
		Path tempFile = Files.createTempFile("temp_package_overrides", "json");
		Path mappingPath = Path.of("./src/test/resources/mojmap_test/example_mappings.mapping");
		var mappings = project.getEnigma().readMappings(mappingPath).get();

		var entries = MojmapNameProposer.createPackageJson(mappings);
		MojmapNameProposer.writePackageJson(tempFile, entries);

		String expected = Files.readString(Path.of("./src/test/resources/mojmap_test/example_mappings_empty.json"));
		String actual = Files.readString(tempFile);

		Assertions.assertEquals(expected, actual);
	}

	@Test
	void testPackageNameOverrideGenerationMojmap() throws IOException {
		Path tempFile = Files.createTempFile("temp_package_overrides_moj", "json");

		var entries = MojmapNameProposer.createPackageJson(MojmapNameProposer.mojmaps);
		MojmapNameProposer.writePackageJson(tempFile, entries);

		String expected = Files.readString(Path.of("./src/test/resources/mojmap_test/expected.json"));
		String actual = Files.readString(tempFile);

		Assertions.assertEquals(expected, actual);
	}

	@Test
	void testOverrideUpdating() throws IOException, MappingParseException {
		Path tempFile = Files.createTempFile("temp_package_overrides_update", "json");
		Path mappingPath = Path.of("./src/test/resources/mojmap_test/example_mappings.mapping");

		Path oldOverrides = Path.of("./src/test/resources/mojmap_test/update_test/example_mappings.json");
		Path newOverrides = Path.of("./src/test/resources/mojmap_test/update_test/example_mappings_expected.json");

		var mappings = project.getEnigma().readMappings(mappingPath).get();

		var oldPackageJson = MojmapNameProposer.readPackageJson(oldOverrides.toString());
		var updated = MojmapNameProposer.updatePackageJson(oldPackageJson, mappings);
		MojmapNameProposer.writePackageJson(tempFile, updated);

		String expected = Files.readString(newOverrides);
		String actual = Files.readString(tempFile);

		Assertions.assertEquals(expected, actual);
	}

	private void assertMapping(Entry<?> entry, String name, TokenType type) {
		var mapping = project.getRemapper().getMapping(entry);
		assertEquals(entry, name, mapping.targetName());
		assertEquals(entry, type, mapping.tokenType());
	}

	private void assertEquals(Entry<?> entry, Object left, Object right) {
		Assertions.assertEquals(left, right, "mismatch for " + entry);
	}
}
