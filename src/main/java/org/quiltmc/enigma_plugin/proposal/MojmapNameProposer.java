package org.quiltmc.enigma_plugin.proposal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.jetbrains.annotations.Nullable;
import org.quiltmc.enigma.api.Enigma;
import org.quiltmc.enigma.api.ProgressListener;
import org.quiltmc.enigma.api.analysis.index.jar.EntryIndex;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.MappingsIndex;
import org.quiltmc.enigma.api.analysis.index.mapping.PackageIndex;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.mapping.EntryRemapper;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTree;
import org.quiltmc.enigma.api.translation.mapping.tree.EntryTreeNode;
import org.quiltmc.enigma.api.translation.representation.entry.ClassEntry;
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
import java.util.function.Function;

public class MojmapNameProposer extends NameProposer {
	public static final String ID = "mojmap";

	private final String mojmapPath;
	private final String packageNameOverridesPath;
	// must be static for now. nasty hack to make sure we don't read mojmaps twice
	// we can guarantee that this is nonnull for the other proposer because jar proposal blocks dynamic proposal
	public static EntryTree<EntryMapping> mojmaps;
	private PackageEntryList packageOverrides;

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
			packageOverrides = readPackageJson(this.packageNameOverridesPath);
			mojmaps.getRootNodes().forEach((node) -> this.proposeNodeAndChildren(mappings, node));
		}
	}

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		if (packageOverrides != null) {
			if (obfEntry == null) {
				// rename all classes as per overrides
				var classes = remapper.getJarIndex().getIndex(EntryIndex.class).getClasses();
				for (ClassEntry classEntry : classes) {
					proposePackageName(classEntry, null, mappings);
				}
			} else if (obfEntry instanceof ClassEntry classEntry) {
				// rename class
				proposePackageName(classEntry, newMapping, mappings);
			}
		}
	}

	private void proposePackageName(ClassEntry entry, @Nullable EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		if (entry.isInnerClass()) {
			return;
		}

		EntryMapping mojmap = mojmaps.get(entry);
		if (mojmap == null || mojmap.targetName() == null) {
			Logger.error("no mojmap for outer class: " + entry);
			return;
		}

		// todo string manip is stupid here
		String mojTarget = mojmap.targetName();
		String target;
		String obfPackage = mojTarget.substring(0, mojTarget.lastIndexOf('/'));

		if (newMapping != null && newMapping.targetName() != null) {
			target = mojTarget.substring(0, mojTarget.lastIndexOf('/')) + newMapping.targetName().substring(newMapping.targetName().lastIndexOf('/') + 1);
		} else {
			target = mojTarget;
		}

		Optional<PackageEntry> optionalPackageEntry = packageOverrides.findEntry(obfPackage);
		optionalPackageEntry.ifPresent(packageEntry -> {
			String deobfPackageString = packageEntry.toDeobfPackageString();
			String obfPackageString = packageEntry.toObfPackageString();

			if (!deobfPackageString.equals(obfPackageString)) {
				String newTarget = target.replace(obfPackage + "/", deobfPackageString + "/");
				mappings.put(entry, new EntryMapping(newTarget));
			}
		});
	}

	private void proposeNodeAndChildren(Map<Entry<?>, EntryMapping> mappings, EntryTreeNode<EntryMapping> node) {
		if (node.getValue() != null && node.getValue().targetName() != null) {
			this.insertProposal(mappings, node.getEntry(), node.getValue().targetName());
		}

		node.getChildNodes().forEach((child) -> proposeNodeAndChildren(mappings, child));
	}

	@Nullable
	public static PackageEntryList readPackageJson(Gson gson, String path) {
		try {
			if (path != null) {
				Reader jsonReader = new FileReader(path);
				PackageEntryList entries = gson.fromJson(jsonReader, PackageEntryList.class);
				for (PackageEntry entry : entries) {
					setupInheritanceAndValidate(entry);
				}

				return entries;
			}
		} catch (FileNotFoundException e) {
			Logger.warn("could not find old package definitions file");
		}

		return null;
	}

	private static void setupInheritanceAndValidate(PackageEntry entry) {
		if (entry.deobf != null) {
			String firstChar = String.valueOf(entry.deobf.charAt(0));
			if (firstChar.matches("[0-9]")) {
				throw new InvalidOverrideException(entry, "package name cannot begin with an integer");
			}

			if (entry.deobf.contains("/")) {
				throw new InvalidOverrideException(entry, "package name cannot contain a slash");
			} else if (entry.deobf.contains("-")) {
				throw new InvalidOverrideException(entry, "package name cannot contain a dash");
			} else if (entry.deobf.contains(" ")) {
				throw new InvalidOverrideException(entry, "package name cannot contain a space");
			} else if (!entry.deobf.toLowerCase().equals(entry.deobf)) {
				throw new InvalidOverrideException(entry, "package name must be lowercase");
			} else if (!entry.deobf.matches("[a-z0-9_]+")) {
				throw new InvalidOverrideException(entry, "entry must match regex '[a-z0-9_]+'");
			}
		}

		for (PackageEntry child : entry.children) {
			child.parent = entry;
			setupInheritanceAndValidate(child);
		}
	}

	public static PackageEntryList readPackageJson(String path) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return readPackageJson(gson, path);
	}

	public static PackageEntryList updatePackageJson(List<PackageEntry> oldJson, EntryTree<EntryMapping> mappings) {
		PackageEntryList newJson = createPackageJson(mappings);

		for (PackageEntry rootEntry : oldJson) {
			rootEntry.forEach(oldEntry -> {
				if (oldEntry.deobf != null) {
					PackageEntry newEntry = null;
					String oldEntryString = oldEntry.toObfPackageString();

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

	public static PackageEntryList createPackageJson(EntryTree<EntryMapping> mappings) {
		MappingsIndex index = new MappingsIndex(new PackageIndex());
		index.indexMappings(mappings, ProgressListener.createEmpty());

		var packageNames = index.getIndex(PackageIndex.class).getPackageNames();
		PackageEntryList rootPackages = new PackageEntryList();

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

		for (int i = 0; i <= packageNamesByDepth.keySet().stream().mapToInt(num -> num).max().getAsInt(); i++) {
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
		public Optional<PackageEntry> findEntry(String obf) {
			for (PackageEntry entry : this) {
				var found = entry.findEntry(obf);
				if (found != null) {
					return Optional.of(found);
				}
			}

			return Optional.empty();
		}
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

		public String toObfPackageString() {
			return buildPackageString(entry -> entry.obf);
		}

		public String toDeobfPackageString() {
			return buildPackageString(entry -> entry.deobf != null && !entry.deobf.isEmpty() ? entry.deobf : entry.obf);
		}

		private String buildPackageString(Function<PackageEntry, String> nameGetter) {
			List<String> packages = new ArrayList<>();
			PackageEntry entry = this;
			while (entry != null) {
				packages.add(nameGetter.apply(entry));
				entry = entry.parent;
			}

			Collections.reverse(packages);
			return String.join("/", packages.toArray(new String[0]));
		}

		private boolean equals(String obfPackage) {
			return this.toObfPackageString().equals(obfPackage);
		}
	}

	public static class InvalidOverrideException extends RuntimeException {
		public InvalidOverrideException(PackageEntry entry, String message) {
			super("Invalid package override for " + entry.obf + " (" + entry.deobf + "): " + message);
		}
	}
}
