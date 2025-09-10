package quilt.internal.task;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.options.Option;
import quilt.internal.util.ExecAware;
import quilt.internal.util.ProviderAware;
import quilt.internal.util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static quilt.internal.util.Util.replaceRegion;

public abstract class SetupQMapTask extends DefaultTask implements ExecAware, ProviderAware {
	public static final String SETUP_QMAP_TASK_NAME = "setupQMap";

	private static final String QMAP_REMOTE = "https://github.com/QuiltMC/quilt-mappings";
	private static final String LIBS_PATH = "gradle/libs.versions.toml";

	private static final String ENIGMA_PLUGIN = "enigma_plugin";
	private static final Pattern ENIGMA_VERSION_PATTERN = Pattern.compile("(?<=" + ENIGMA_PLUGIN + " = \").*(?=\")");

	@Input
	public abstract Property<String> getProjectVersion();

	@Input
	public abstract Property<SetupQMapState> getSetupState();

	@Input
	@Optional
	@Option(
		option = "discard",
		description = "Whether to discard quilt-mappings git changes even when they could be preserved; defaults to false."
	)
	public abstract Property<Boolean> getDiscardQMapChanges();

	@OutputFile
	public abstract RegularFileProperty getQepBranchCache();

	@OutputDirectory
	public abstract DirectoryProperty getQMapDest();

	public SetupQMapTask() {
		this.getOutputs().upToDateWhen(ignored -> {
			final SetupQMapState state = this.getSetupState().get();

			return !state.qepBranchChanged()
				&& state.qMapOnDefault()
				&& state.qMapMatchesDefault();
		});
	}

	@TaskAction
	public void setup() throws IOException {
		final Path qMapDest = this.getQMapDest().get().getAsFile().toPath();
		final SetupQMapState state = this.getSetupState().get();

		Files.createDirectories(qMapDest);

		if (state.qepBranchChanged()) {
			Files.write(this.getQepBranchCache().get().getAsFile().toPath(), state.qepBranch().getBytes());
		}

		final String defaultQMapBranch = state.defaultQMapBranch();
		if (defaultQMapBranch == null) {
			// no qmap repo yet: clone it

			// make sure qMapDest is writable to avoid errors during deletion
			this.getExecutor().exec(spec -> {
				spec.executable("chmod");
				spec.args("-Rf", "+w", qMapDest);
			});

			Util.clearDirectory(qMapDest);

			this.execGit(qMapDest, "clone", QMAP_REMOTE, qMapDest);
		} else {
			final boolean discard = this.getDiscardQMapChanges().getOrElse(false);

			if (!state.qMapOnDefault()) {
				final boolean stashed = !discard && !state.qepBranchChanged() && this.tryGitStash(qMapDest);

				this.execGit(qMapDest, "reset", "--hard", "HEAD");
				this.execGit(qMapDest, "fetch", "origin", defaultQMapBranch);
				this.execGit(qMapDest, "checkout", defaultQMapBranch);
				this.gitResetToDefault(qMapDest, defaultQMapBranch);

				if (stashed) {
					this.gitStashPop(qMapDest);
				}
			} else if (discard || state.qepBranchChanged() || !Files.isRegularFile(qMapDest.resolve(LIBS_PATH))) {
				this.gitResetToDefault(qMapDest, defaultQMapBranch);
			} else if (!state.qMapMatchesDefault()) {
				final boolean stashed = this.tryGitStash(qMapDest);

				this.gitResetToDefault(qMapDest, defaultQMapBranch);

				if (stashed) {
					this.gitStashPop(qMapDest);
				}
			}
		}

		final Path versionsPath = qMapDest.resolve(LIBS_PATH);
		final String versionContents = Files.readString(versionsPath);
		final Matcher versionMatcher = ENIGMA_VERSION_PATTERN.matcher(versionContents);
		if (versionMatcher.find()) {
			final String replaced = replaceRegion(
				versionContents, this.getProjectVersion().get(),
				versionMatcher.start(), versionMatcher.end()
			);

			Files.write(versionsPath, replaced.getBytes());
		} else {
			throw new GradleException("Cannot find $enigmaPlugin version.");
		}
	}

	private void gitResetToDefault(Path workingDir, String defaultBranch) {
		this.execGit(workingDir, "reset", "--hard", "origin/" + defaultBranch);
	}

	private boolean tryGitStash(Path workingDir) {
		// stash everything excepts libs so when stash is applied it doesn't conflict with or override altered QEP version
		final String stashOutput = this.execGitOutput(workingDir, "stash", "-m", "QEP auto-stash", "--", ":!:" + LIBS_PATH);
		return !stashOutput.contains("No local changes to save");
	}

	private void gitStashPop(Path workingDir) {
		this.execGit(workingDir, "stash", "pop");
	}
}
