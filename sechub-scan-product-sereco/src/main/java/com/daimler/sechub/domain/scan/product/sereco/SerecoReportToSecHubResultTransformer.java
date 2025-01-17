// SPDX-License-Identifier: MIT
package com.daimler.sechub.domain.scan.product.sereco;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.daimler.sechub.domain.scan.SecHubFinding;
import com.daimler.sechub.domain.scan.SecHubResult;
import com.daimler.sechub.domain.scan.Severity;
import com.daimler.sechub.domain.scan.product.ProductIdentifier;
import com.daimler.sechub.domain.scan.report.ScanReportToSecHubResultTransformer;
import com.daimler.sechub.sereco.metadata.MetaData;
import com.daimler.sechub.sereco.metadata.Vulnerability;
import com.daimler.sechub.sharedkernel.execution.SecHubExecutionException;
import com.daimler.sechub.sharedkernel.util.JSONConverter;

@Component
public class SerecoReportToSecHubResultTransformer implements ScanReportToSecHubResultTransformer {

	private static final Logger LOG = LoggerFactory.getLogger(SerecoReportToSecHubResultTransformer.class);

	@Override
	public SecHubResult transform(String origin) throws SecHubExecutionException {
		MetaData data = JSONConverter.get().fromJSON(MetaData.class, origin);
		SecHubResult result = new SecHubResult();

		List<SecHubFinding> findings = result.getFindings();
		int id = 1;
		for (Vulnerability v : data.getVulnerabilities()) {
			SecHubFinding finding = new SecHubFinding();
			finding.setDescription(v.getDescription());
			finding.setName(v.getType());
			finding.setId(id++);
			finding.setSeverity(transformSeverity(v.getSeverity()));
			findings.add(finding);
		}

		return result;
	}

	private Severity transformSeverity(com.daimler.sechub.sereco.metadata.Severity metaSeverity) {
		if (metaSeverity==null) {
			LOG.error("Missing Sereco Severity cannot transformed {} to sechub result! So returning unclassified!",metaSeverity);
			return Severity.UNCLASSIFIED;
		}
		for (Severity severity : Severity.values()) {
			if (severity.name().equals(metaSeverity.name())) {
				return severity;
			}
		}
		LOG.error("Was not able to tranform Sereco Severity:{} to sechub result! So returning unclassified!",metaSeverity);
		return Severity.UNCLASSIFIED;
	}

	@Override
	public boolean canTransform(ProductIdentifier productIdentifier) {
		return ProductIdentifier.SERECO.equals(productIdentifier);
	}

}
