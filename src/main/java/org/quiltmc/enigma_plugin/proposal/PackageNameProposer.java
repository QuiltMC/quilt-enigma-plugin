package org.quiltmc.enigma_plugin.proposal;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.adapter.MappingSourceNsSwitch;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import org.quiltmc.enigma.api.analysis.index.jar.JarIndex;
import org.quiltmc.enigma.api.service.EnigmaServiceContext;
import org.quiltmc.enigma.api.service.NameProposalService;
import org.quiltmc.enigma.api.translation.mapping.EntryMapping;
import org.quiltmc.enigma.api.translation.representation.entry.Entry;
import org.quiltmc.launchermeta.version.v1.Version;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class PackageNameProposer extends NameProposer {
	public static final String ID = "package_name";
	private final String hashedPath;
	private final String mojangPath;

	public PackageNameProposer(EnigmaServiceContext<NameProposalService> context) {
		super(ID);
		this.hashedPath = context.getSingleArgument("hashed_path").orElseThrow();
		this.mojangPath = context.getSingleArgument("mojang_path").orElseThrow();
	}

	@Override
	public void insertProposedNames(JarIndex index, Map<Entry<?>, EntryMapping> mappings) {
		MemoryMappingTree officialToHashed = new MemoryMappingTree();
		try {
			var path = Path.of(this.getClass().getResource("hashed-24w33a.tiny").getPath());
			MappingReader.read(path, new MappingSourceNsSwitch(officialToHashed, "official"));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		// todo launchermeta parser and mojmap downloading
		Version.fromJson()
	}
}
