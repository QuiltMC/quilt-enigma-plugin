package quilt.internal.util;

import org.gradle.process.ExecOperations;

import javax.inject.Inject;

public interface ExecAware {
	@Inject
	ExecOperations getExecutor();
}
