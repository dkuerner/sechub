// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.product;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import com.daimler.sechub.domain.scan.InstallSetup;
import com.daimler.sechub.sharedkernel.configuration.SecHubConfiguration;
import com.daimler.sechub.sharedkernel.configuration.SecHubInfrastructureScanConfiguration;

public abstract class AbstractInfrastructureScanProductExecutor<S extends InstallSetup> extends AbstractInstallSetupProductExecutor<S> implements InfrastructureScanProductExecutor {

	@Override
	protected List<URI> resolveURIsForTarget(SecHubConfiguration config) {
		/* assert WEBSCAN configuration available */
		Optional<SecHubInfrastructureScanConfiguration> infraScan = config.getInfraScan();
		if (!infraScan.isPresent()) {
			throw new IllegalStateException("At this state there must be a infrascan setup!");
		}
		/* Fetch URI */
		SecHubInfrastructureScanConfiguration infraScanConfiguration = infraScan.get();
		List<URI> urls = infraScanConfiguration.getUris();
		if (urls == null) {
			throw new IllegalStateException("At this state the URI must be set - validation failed!");
		}
		return urls;
	}

}
