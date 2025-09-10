package quilt.internal.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.jetbrains.annotations.NotNull;
import quilt.internal.task.DiffQMapTask;
import quilt.internal.task.SetupQMapState;
import quilt.internal.task.SetupQMapTask;
import quilt.internal.util.ProviderAware;
import quilt.internal.util.GitFetchService;

public abstract class QMapDiffPlugin implements Plugin<Project>, ProviderAware {
	public static final String QMAP_FETCH_SERVICE_NAME = "fetch-qmap";

	@Override
	public void apply(@NotNull Project project) {
		final ProviderFactory providers = this.getProviders();

		final TaskContainer tasks = project.getTasks();
		final PluginContainer plugins = project.getPlugins();
		final DirectoryProperty buildDir = project.getLayout().getBuildDirectory();

		final var ext = project.getExtensions().create(DiffQMapExtension.NAME, DiffQMapExtension.class);
		ext.getQepRepoDir().set(project.getRootDir());
		ext.getQMapRepoDir().set(buildDir.dir("quilt-mappings"));

		final var qmapFetcher = project.getGradle().getSharedServices().registerIfAbsent(
			QMAP_FETCH_SERVICE_NAME, GitFetchService.class,
			spec -> spec.parameters(params -> {
				final DirectoryProperty repoDir = params.getRepoDir();

				repoDir.set(ext.getQMapRepoDir());
			})
		);

		plugins.apply(MavenPublishPlugin.class);

		final var setupQMap = tasks.register(SetupQMapTask.SETUP_QMAP_TASK_NAME, SetupQMapTask.class, task -> {
			task.getProjectVersion().set(ext.getProjectVersion());

			task.getQMapDest().set(ext.getQMapRepoDir());

			task.getSetupState().set(providers.of(SetupQMapState.Source.class, spec -> spec.parameters(params -> {
				params.getQepRepoDir().set(ext.getQepRepoDir());
				params.getQepBranchCache().set(task.getQepBranchCache());

				params.getQMapRepoDir().set(ext.getQMapRepoDir());
				params.getQMapFetcher().set(qmapFetcher);
			})));

			task.getQepBranchCache().set(buildDir.file("diffQMap-QEP-branch-cache.txt"));
		});

		tasks.register(DiffQMapTask.DIFF_Q_MAP_TASK_NAME, DiffQMapTask.class, task -> {
			task.dependsOn(setupQMap, tasks.named(MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME));

			task.getQMapDir().set(setupQMap.flatMap(SetupQMapTask::getQMapDest));

			task.getDest().set(buildDir.file("qmap.diff"));
		});
	}
}
