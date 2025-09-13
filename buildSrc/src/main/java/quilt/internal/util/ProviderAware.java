package quilt.internal.util;

import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;

public interface ProviderAware {
	@Inject
	ProviderFactory getProviders();
}
