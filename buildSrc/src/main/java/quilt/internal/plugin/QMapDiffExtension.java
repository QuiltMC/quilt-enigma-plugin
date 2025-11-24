package quilt.internal.plugin;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;

public interface QMapDiffExtension {
	String NAME = "qMapDiff";

	Property<String> getProjectVersion();

	DirectoryProperty getQepRepoDir();

	DirectoryProperty getQMapRepoDir();
}
