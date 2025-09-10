package quilt.internal.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;
import org.jetbrains.annotations.NotNull;
import quilt.internal.task.DiffQMapTask;
import quilt.internal.task.SetupQMapTask;

public abstract class QMapDiffPlugin implements Plugin<Project> {
	@Override
	public void apply(@NotNull Project project) {
		final TaskContainer tasks = project.getTasks();
		final PluginContainer plugins = project.getPlugins();
		final DirectoryProperty buildDir = project.getLayout().getBuildDirectory();

		// add jar task
		plugins.apply(JavaPlugin.class);
		// adds publishToMavenLocal task
		plugins.apply(MavenPublishPlugin.class);

		final var setupQMap = tasks.register(SetupQMapTask.SETUP_Q_MAP_TASK_NAME, SetupQMapTask.class, task -> {
			final TaskProvider<Jar> jarTask = tasks.named(JavaPlugin.JAR_TASK_NAME, Jar.class);
			task.dependsOn(jarTask);
			// if jar did work, current QEP branch must be checked
			task.getOutputs().upToDateWhen(ignored -> !jarTask.get().getDidWork());

			task.getQepRepoRootName().set(project.getRootDir().getAbsolutePath());

			task.getQepBranchCache().set(buildDir.file("diffQMap-QEP-branch-cache.txt"));

			task.getQMapDest().set(buildDir.dir("quilt-mappings"));
		});

		tasks.register(DiffQMapTask.DIFF_Q_MAP_TASK_NAME, DiffQMapTask.class, task -> {
			task.dependsOn(setupQMap, tasks.named(MavenPublishPlugin.PUBLISH_LOCAL_LIFECYCLE_TASK_NAME));

			task.getQMapDir().set(setupQMap.flatMap(SetupQMapTask::getQMapDest));

			task.getDest().set(buildDir.file("qmap.diff"));
		});
	}
}
