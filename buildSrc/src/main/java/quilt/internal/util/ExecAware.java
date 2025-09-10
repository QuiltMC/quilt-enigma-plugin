package quilt.internal.util;

import org.gradle.api.Action;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecSpec;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

public interface ExecAware {
	@Inject
	ExecOperations getExecutor();

	default void execGit(Path workingDir, @Nullable Action<ExecSpec> action, Object... args) {
		this.getExecutor().exec (spec -> {
			spec.workingDir(workingDir);
			spec.executable("git");
			if (action != null) {
				action.execute(spec);
			}
			spec.args(args);
		});
	}

	default void execGit(Path workingDir, Object... args) {
		this.execGit(workingDir, null, args);
	}

	default String execGitOutput(Path workingDir, @Nullable Action<ExecSpec> action, Object... args) {
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

	default String execGitOutput(Path workingDir, Object... args) {
		return this.execGitOutput(workingDir, null, args);
	}

	default String getGitBranch(Path workingDir) {
		return this.execGitOutput(workingDir, "rev-parse", "--abbrev-ref", "HEAD").trim();
	}
}
