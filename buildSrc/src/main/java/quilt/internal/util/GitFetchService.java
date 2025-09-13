package quilt.internal.util;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.services.BuildService;

import org.gradle.api.services.BuildServiceParameters;
import quilt.internal.util.GitFetchService.Params;

public abstract class GitFetchService implements BuildService<Params>, ExecAware {
	private boolean fetched = false;

	public void fetch() {
		if (!this.fetched) {
			this.execGit(this.getParameters().getRepoDir().get().getAsFile().toPath(), "fetch");

			this.fetched = true;
		}
	}

	public interface Params extends BuildServiceParameters {
		DirectoryProperty getRepoDir();
	}
}
