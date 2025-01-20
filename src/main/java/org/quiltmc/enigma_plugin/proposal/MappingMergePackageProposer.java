/*
 * Copyright 2025 QuiltMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

/**
 * Proposes the packages from the mappings stored in {@link MappingMergeNameProposer} onto all top-level classes.
 * These package names can be changed via overrides, which are a simple set of package names keyed by the versions from the mappings in {@link MappingMergeNameProposer}.
 * This proposer must override the user-inputted mappings in order to work, and thus will generate mappings that take priority over manually input ones.
 *
 * <p>
 *     This works via stripping out the class name and replacing the package dynamically after each rename.
 *     For example, if a class is renamed {@code package/Class} and the name for the class in the mappings to merge is {@code net/minecraft/MojangClass},
 *     the manually input class name will be changed according to the mappings, resulting in {@code net/minecraft/Class}.
 * </p>
 * <p>
 *     The override format is written in JSON, with a structure mimicking a package tree and entirely composed of one object type:
 *     <pre><code>
 *         [
 *             {
 *                 "obf": "com",
 *                 "deobf": "",
 *                 "children": [
 *                     {
 *                         "obf": "mojang",
 *                         "deobf": "quiltmc",
 *                         "children": []
 *                     }
 *                 ]
 *             },
 *             {
 *                 "obf": "net",
 *                 "deobf": "",
 *                 "children": []
 *             }
 *         ]
 *     </code></pre>
 *
 *     The elements of each package object are as follows:
 *     <ul>
 *         <li>
 *             {@code obf}: the unqualified name of the package in the provided mappings
 *         </li>
 *         <li>
 *             {@code deobf}: the unqualified, overridden name of the package, or an empty string to keep the original one
 *         </li>
 *         <li>
 *             {@code children}: a list of child packages of this package.
 *             The root of each override file is identical to one of these objects.
 *         </li>
 *     </ul>
 * </p>
 */
public class MappingMergePackageProposer extends NameProposer {
	public static final String ID = "merge_packages";
	private final String packageNameOverridesPath;
	private PackageEntryList packageOverrides;

	public MappingMergePackageProposer(@Nullable String packageNameOverridesPath) {
		super(ID);
		this.packageNameOverridesPath = packageNameOverridesPath;
	}

	@Override
	public void insertProposedNames(Enigma enigma, JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		// no-op
	}

	@Override
	public void proposeDynamicNames(EntryRemapper remapper, Entry<?> obfEntry, EntryMapping oldMapping, EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings) {
		final EntryTree<EntryMapping> mergedMappings = MappingMergeNameProposer.getMergedMappings();

		if (mergedMappings != null) {
			if (this.packageOverrides == null) {
				if (this.packageNameOverridesPath != null) {
					this.packageOverrides = readPackageJson(this.packageNameOverridesPath);
				} else {
					Logger.warn("no package name overrides path provided!");
					this.packageOverrides = new PackageEntryList();
				}
			}

			if (obfEntry == null) {
				// rename all classes as per overrides
				var classes = remapper.getJarIndex().getIndex(EntryIndex.class).getClasses();
				for (ClassEntry classEntry : classes) {
					this.proposePackageName(classEntry, null, null, mappings, mergedMappings);
				}
			} else if (obfEntry instanceof ClassEntry classEntry) {
				// rename class
				this.proposePackageName(classEntry, oldMapping, newMapping, mappings, mergedMappings);
			}
		}
	}

	private void proposePackageName(ClassEntry entry, @Nullable EntryMapping oldMapping, @Nullable EntryMapping newMapping, Map<Entry<?>, EntryMapping> mappings, EntryTree<EntryMapping> mergedMappings) {
		if (entry.isInnerClass()) {
			return;
		}

		EntryMapping mappingToMerge = mergedMappings.get(entry);
		if (mappingToMerge == null || mappingToMerge.targetName() == null) {
			Logger.error("no available mapping to merge for outer class: " + entry);
			return;
		}

		String mergeTarget = mappingToMerge.targetName();
		String target;
		String obfPackage = mergeTarget.substring(0, mergeTarget.lastIndexOf('/'));

		if (newMapping != null && newMapping.targetName() != null) {
			target = mergeTarget.substring(0, mergeTarget.lastIndexOf('/'))
				+ newMapping.targetName().substring(newMapping.targetName().lastIndexOf('/'));
		} else {
			target = mergeTarget;
		}

		Optional<PackageEntry> optionalPackageEntry = this.packageOverrides.findEntry(obfPackage);
		optionalPackageEntry.ifPresentOrElse(packageEntry -> {
			String deobfPackageString = packageEntry.toDeobfPackageString();
			String obfPackageString = packageEntry.toObfPackageString();

			boolean updatedName = newMapping != null && oldMapping != null && !newMapping.targetName().equals(oldMapping.targetName());

			if (!deobfPackageString.equals(obfPackageString) || updatedName) {
				String newTarget = target.replace(obfPackage + "/", deobfPackageString + "/");
				mappings.put(entry, new EntryMapping(newTarget));
			}
		}, () -> {
			boolean updatedName = newMapping != null && oldMapping != null && !newMapping.targetName().equals(oldMapping.targetName());
			if (updatedName) {
				mappings.put(entry, new EntryMapping(target));
			}
		});
	}

	public static PackageEntryList readPackageJson(Gson gson, @Nullable String path) {
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

		return new PackageEntryList();
	}

	private static void setupInheritanceAndValidate(PackageEntry entry) {
		if (entry.deobf != null && !entry.deobf.isEmpty()) {
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

	@SuppressWarnings("OptionalGetWithoutIsPresentCheck")
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

		int maxPackageDepth = packageNamesByDepth.keySet().stream().mapToInt(num -> num).max().orElse(0);

		for (int i = 0; i <= maxPackageDepth; i++) {
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
			return this.buildPackageString(entry -> entry.obf);
		}

		public String toDeobfPackageString() {
			return this.buildPackageString(entry -> entry.deobf != null && !entry.deobf.isEmpty() ? entry.deobf : entry.obf);
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
