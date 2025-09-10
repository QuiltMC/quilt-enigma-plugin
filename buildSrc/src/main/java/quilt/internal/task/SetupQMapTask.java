package quilt.internal.task;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecSpec;
import org.jetbrains.annotations.Nullable;
import quilt.internal.util.ExecAware;
import quilt.internal.util.Util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static quilt.internal.util.Util.replaceRegion;

public abstract class SetupQMapTask extends DefaultTask implements ExecAware {
	public static final String SETUP_Q_MAP_TASK_NAME = "setupQMap";

	private static final String Q_MAP_REMOTE = "https://github.com/QuiltMC/quilt-mappings";
	private static final String LIBS_PATH = "gradle/libs.versions.toml";

	private static final Pattern DEFAULT_BRANCH_IN_SHOW_OUTPUT_PATTERN = Pattern.compile("(?m)^\\s*HEAD branch: (\\w+)");

	private static final String ENIGMA_PLUGIN = "enigma_plugin";
	private static final Pattern ENIGMA_VERSION_PATTERN = Pattern.compile("(?<=" + ENIGMA_PLUGIN + " = \").*(?=\")");

	@Input
	public abstract Property<String> getProjectVersion();

	// string because we just use it as a working dir, we don't want to take the entire project as input
	@Input
	public abstract Property<String> getQepRepoRootName();

	@OutputFile
	public abstract RegularFileProperty getQepBranchCache();

	@OutputDirectory
	public abstract DirectoryProperty getQMapDest();

	@TaskAction
	public void setup() throws IOException {
		final Path qMapDest = this.getQMapDest().get().getAsFile().toPath();
		final Path qepBranchCache = this.getQepBranchCache().get().getAsFile().toPath();

		Files.createDirectories(qMapDest);

		@Nullable
		final String priorQepBranch = Files.isRegularFile(qepBranchCache) ? Files.readString(qepBranchCache) : null;
		final String currentQepBranch = this.getCurrentGitBranch(Path.of(this.getQepRepoRootName().get()));

		final boolean sameQepBranch = currentQepBranch.equals(priorQepBranch);
		if (!sameQepBranch) {
			Files.write(qepBranchCache, currentQepBranch.getBytes());
		}

		if (Files.isDirectory(qMapDest.resolve(".git"))) {
			this.execGit(qMapDest,"fetch");

			final String currentQMapBranch = this.getCurrentGitBranch(qMapDest);

			final String showQmapOrigin = this.execGitOutput(qMapDest, "remote", "show", "origin");

			final Matcher defaultQmapBranchMatcher = DEFAULT_BRANCH_IN_SHOW_OUTPUT_PATTERN.matcher(showQmapOrigin);
			if (!defaultQmapBranchMatcher.find()) {
				throw new GradleException("Could not find default quilt-mappings branch!");
			}

			final String defaultQmapBranch = defaultQmapBranchMatcher.group(1);

			if (!currentQMapBranch.equals(defaultQmapBranch)) {
				final boolean stashed;
				if (sameQepBranch) {
					stashed = this.tryGitStash(qMapDest);
				} else {
					stashed = false;
					this.execGit(qMapDest, "reset", "--hard", "HEAD");
				}

				this.execGit(qMapDest, "fetch", "origin", defaultQmapBranch);
				this.execGit(qMapDest, "checkout", defaultQmapBranch);
				this.gitResetQMap(qMapDest, defaultQmapBranch);

				if (stashed) {
					this.gitStashPop(qMapDest);
				}
			} else if (!sameQepBranch || this.qmapNeedsUpdate(qMapDest, defaultQmapBranch)) {
				final boolean stashed = sameQepBranch && this.tryGitStash(qMapDest);

				this.gitResetQMap(qMapDest, defaultQmapBranch);

				if (stashed) {
					this.gitStashPop(qMapDest);
				}
			}
		} else {
			// no qmap clone yet, clone it

			// makes sure qMapDest is writable to avoid errors during deletion
			this.getExecutor().exec(spec -> {
				spec.executable("chmod");
				spec.args("-Rf", "+w", qMapDest);
			});


			Util.clearDirectory(qMapDest);

			this.execGit(qMapDest, "clone", Q_MAP_REMOTE, qMapDest);
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

	private void gitResetQMap(Path qMapDest, String defaultQmapBranch) {
		this.execGit(qMapDest, "reset", "--hard", "origin/" + defaultQmapBranch);
	}

	private String getCurrentGitBranch(Path workingDir) {
		return this.execGitOutput(workingDir, "rev-parse", "--abbrev-ref", "HEAD").trim();
	}

	private void execGit(Path workingDir, @Nullable Action<ExecSpec> action, Object... args) {
		this.getExecutor().exec (spec -> {
			spec.workingDir(workingDir);
			spec.executable("git");
			if (action != null) {
				action.execute(spec);
			}
			spec.args(args);
		});
	}

	private void execGit(Path workingDir, Object... args) {
		this.execGit(workingDir, null, args);
	}

	private String execGitOutput(Path workingDir, @Nullable Action<ExecSpec> action, Object... args) {
		final var output = new ByteArrayOutputStream();
		this.execGit(
			workingDir,
			spec -> {
				spec.setStandardOutput(output);
				if (action != null) {
					action.execute(spec);
				}
			},
			args);
		return output.toString();
	}

	private String execGitOutput(Path workingDir, Object... args) {
		return this.execGitOutput(workingDir, null, args);
	}

	private boolean tryGitStash(Path workingDir) {
		// stash everything excepts libs so when stash is applied it doesn't conflict with or override altered QEP version
		return !this
			.execGitOutput(workingDir, "stash", "-m", "QEP auto-stash", "--", ":!:" + LIBS_PATH)
			.contains("No local changes to save");
	}

	private void gitStashPop(Path workingDir) {
		this.execGit(workingDir, "stash", "pop");
	}

	private boolean qmapNeedsUpdate(Path qMapDest, String defaultBranch) {
		if (!Files.isRegularFile(qMapDest.resolve(LIBS_PATH))) {
			return true;
		}

		final String localHash = this.execGitOutput(qMapDest, "rev-parse", "HEAD");

		final String originHash = this.execGitOutput(qMapDest, "rev-parse", "origin/" + defaultBranch);

		return !originHash.equals(localHash);
	}
}
