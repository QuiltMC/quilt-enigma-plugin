package quilt.internal.task;

import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jetbrains.annotations.Nullable;

import quilt.internal.util.ExecAware;
import quilt.internal.task.SetupQMapState.Source.Params;
import quilt.internal.util.GitFetchService;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SetupQMapState(
	String qepBranch,
	boolean qepBranchChanged,
	@Nullable String defaultQMapBranch,
	boolean qMapOnDefault,
	boolean qMapMatchesDefault
) implements Serializable {
	public interface Source extends ValueSource<SetupQMapState, Params>, ExecAware {
		Pattern DEFAULT_BRANCH_IN_SHOW_OUTPUT_PATTERN = Pattern.compile("(?m)^\\s*HEAD branch: (\\w+)");

		@Override
		default SetupQMapState obtain() {
			final Params params = this.getParameters();

			final String qepBranch = this.getGitBranch(params.getQepRepoDir().get().getAsFile().toPath());
			@Nullable
			final String cachedQepBranch;
			try {
				final Path cachePath = params.getQepBranchCache().get().getAsFile().toPath();
				cachedQepBranch = Files.isRegularFile(cachePath) ? Files.readString(cachePath) : null;
			} catch (IOException e) {
				throw new GradleException("Failed to read qep branch cache!", e);
			}

			final boolean qepBranchChanged = !qepBranch.equals(cachedQepBranch);

			final Path qMapRepoDir = params.getQMapRepoDir().get().getAsFile().toPath();
			if (Files.isDirectory(qMapRepoDir)) {
				params.getQMapFetcher().get().fetch();

				final String localQMapBranch = this.getGitBranch(qMapRepoDir);

				final String showQmapOrigin = this.execGitOutput(qMapRepoDir, "remote", "show", "origin");

				final Matcher defaultQmapBranchMatcher = DEFAULT_BRANCH_IN_SHOW_OUTPUT_PATTERN.matcher(showQmapOrigin);
				if (!defaultQmapBranchMatcher.find()) {
					throw new GradleException("Could not find default branch!");
				}

				final String defaultQMapBranch = defaultQmapBranchMatcher.group(1);

				if (localQMapBranch.equals(defaultQMapBranch)) {
					final String localQMapHash = this.execGitOutput(qMapRepoDir, "rev-parse", "HEAD");
					final String defaultQMapHash = this.execGitOutput(qMapRepoDir, "rev-parse", "origin/" + defaultQMapBranch);

					return new SetupQMapState(
						qepBranch, qepBranchChanged,
						defaultQMapBranch, true,
						localQMapHash.equals(defaultQMapHash)
					);
				} else {
					return new SetupQMapState(
						qepBranch, qepBranchChanged,
						defaultQMapBranch, false,
						false
					);
				}
			} else {
				return new SetupQMapState(qepBranch, qepBranchChanged, null, false, false);
			}
		}

		interface Params extends ValueSourceParameters {
			DirectoryProperty getQepRepoDir();
			RegularFileProperty getQepBranchCache();

			DirectoryProperty getQMapRepoDir();
			Property<GitFetchService> getQMapFetcher();
		}
	}
}
