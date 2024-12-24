package org.quiltmc.enigma_plugin.proposal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.PackageIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.tinylog.Logger;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class MojmapNameProposer extends NameProposer {
	public static final String ID = "mojmap";

	private final String mojmapPath;
	private final String packageNameOverridesPath;
	// must be static for now. nasty hack to make sure we don't read mojmaps twice
	// we can guarantee that this is nonnull for the other proposer because jar proposal blocks dynamic proposal
	public static EntryTree<EntryMapping> mojmaps;

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public MojmapNameProposer(Optional<String> mojmapPath, Optional<String> packageNameOverridesPath) {
		super(ID);

		if (mojmapPath.isPresent()) {
			this.mojmapPath = mojmapPath.get();
		} else {
			Logger.error("no mojmap path provided, disabling " + this.getSourcePluginId());
			this.mojmapPath = null;
		}

		if (packageNameOverridesPath.isPresent()) {
			this.packageNameOverridesPath = packageNameOverridesPath.get();
		} else {
			Logger.warn("no package name overrides path provided!");
			this.packageNameOverridesPath = null;
		}
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		Path path = Path.of(mojmapPath);
		enigma.getReadWriteService(path).ifPresent((service) -> {
			try {
				mojmaps = service.read(path);
			} catch (Exception e) {
				Logger.error(e, "could not read mojmaps!");
			}
		});

		if (mojmaps != null) {
			mojmaps.getRootNodes().forEach((node) -> this.proposeNodeAndChildren(mappings, node));
		}
	}

	private void proposeNodeAndChildren(Map<Entry<?>, EntryMapping> mappings, EntryTreeNode<EntryMapping> node) {
		if (node.getValue() != null && node.getValue().targetName() != null) {
			this.insertProposal(mappings, node.getEntry(), node.getValue().targetName());
		}

		node.getChildNodes().forEach((child) -> proposeNodeAndChildren(mappings, child));
	}

	@Nullable
	public static List<PackageEntry> readPackageJson(Gson gson, String path) {
		try {
			if (path != null) {
				Reader jsonReader = new FileReader(path);
				List<PackageEntry> entries = gson.fromJson(jsonReader, PackageEntryList.class);
				for (PackageEntry entry : entries) {
					setupInheritance(entry);
				}

				return entries;
			}
		} catch (FileNotFoundException e) {
			Logger.warn("could not find old package definitions file");
		}

		return null;
	}

	private static void setupInheritance(PackageEntry entry) {
		for (PackageEntry child : entry.children) {
			child.parent = entry;
			setupInheritance(child);
		}
	}

	public static List<PackageEntry> readPackageJson(String path) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return readPackageJson(gson, path);
	}

	public static List<PackageEntry> updatePackageJson(List<PackageEntry> oldJson, EntryTree<EntryMapping> mappings) {
		List<PackageEntry> newJson = createPackageJson(mappings);

		for (PackageEntry rootEntry : oldJson) {
			rootEntry.forEach(oldEntry -> {
				if (oldEntry.deobf != null) {
					PackageEntry newEntry = null;
					String oldEntryString = oldEntry.toPackageString();

					for (PackageEntry root : newJson) {
						newEntry = root.findEntry(oldEntryString);
						if (newEntry != null) {
							break;
						}
					}

					if (newEntry != null) {
						newEntry.deobf = oldEntry.deobf;
					}
				}
			});
		}

		return newJson;
	}

	public static List<PackageEntry> createPackageJson(EntryTree<EntryMapping> mappings) {
		MappingsIndex index = new MappingsIndex(new PackageIndex());
		index.indexMappings(mappings, ProgressListener.createEmpty());

		var packageNames = index.getIndex(PackageIndex.class).getPackageNames();
		List<PackageEntry> rootPackages = new ArrayList<>();

		Map<Integer, List<String>> packageNamesByDepth = new HashMap<>();

		for (String packageName : new ArrayList<>(packageNames)) {
			int slashes;

			if (!packageName.contains("/")) {
				slashes = 0;
			} else {
				slashes = packageName.split("/").length - 1;
			}

			packageNamesByDepth.computeIfAbsent(slashes, k -> new ArrayList<>()).add(packageName);
		}

		for (int i = 0; i < packageNamesByDepth.keySet().stream().mapToInt(num -> num).max().getAsInt(); i++) {
			List<String> names = packageNamesByDepth.get(i);

			if (i == 0) {
				names.forEach(name -> rootPackages.add(new PackageEntry(name, "")));
			} else {
				for (String name : names) {
					String rootName = name.split("/")[0];

					PackageEntry rootPackage = rootPackages.stream().filter(entry -> entry.obf.equals(rootName)).findFirst().orElse(null);
					if (rootPackage != null) {
						String packageUp = name.substring(0, name.lastIndexOf('/'));

						PackageEntry parent = rootPackage.findEntry(packageUp);
						if (parent != null) {
							String finalPackage = name.substring(name.lastIndexOf('/') + 1);
							PackageEntry child = new PackageEntry(finalPackage, "");
							child.parent = parent;
							parent.children.add(child);
						}
					}
				}
			}
		}

		return rootPackages;
	}

	public static void writePackageJson(Path path, List<PackageEntry> entries) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		try {
			Files.write(path, gson.toJson(entries).getBytes());
		} catch (IOException e) {
			Logger.error(e, "could not write updated package name overrides");
		}
	}

	public static class PackageEntryList extends ArrayList<PackageEntry> {
	}

	public static class PackageEntry {
		public String obf;
		public String deobf;
		public List<PackageEntry> children;
		private transient PackageEntry parent;

		public PackageEntry(String obf, String deobf) {
			this.obf = obf;
			this.deobf = deobf;
			this.children = new ArrayList<>();
		}

		public void forEach(Consumer<PackageEntry> consumer) {
			consumer.accept(this);

			for (PackageEntry child : this.children) {
				consumer.accept(child);
				child.forEach(consumer);
			}
		}

		public PackageEntry findEntry(String obf) {
			if (this.equals(obf)) {
				return this;
			}

			for (PackageEntry entry : this.children) {
				var found = entry.findEntry(obf);
				if (found != null) {
					return found;
				}
			}

			return null;
		}

		public String toPackageString() {
			List<String> packages = new ArrayList<>();
			PackageEntry entry = this;
			while (entry != null) {
				packages.add(entry.obf);
				entry = entry.parent;
			}

			Collections.reverse(packages);
			return String.join("/", packages.toArray(new String[0]));
		}

		private boolean equals(String obfPackage) {
			return this.toPackageString().equals(obfPackage);
		}
	}
}
