package quilt.internal.task;

import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Exec;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.OutputFile;

public abstract class DiffQMapTask extends Exec {
	public static final String DIFF_Q_MAP_TASK_NAME = "diffQMap";

	@InputDirectory
	public abstract DirectoryProperty getQMapDir();

	@OutputFile
	public abstract RegularFileProperty getDest();

	@Override
	protected void exec() {
		this.workingDir(this.getQMapDir().get().getAsFile());

		this.executable(System.getProperty("os.name", "").toLowerCase().contains("windows") ? "./gradlew.bat" : "./gradlew");

		this.args("--no-daemon", "diffTarget", "--refresh-dependencies", "--dest", this.getDest().get().getAsFile());

		super.exec();
	}
}
