// SPDX-License-Identifier: MIT
package com.daimler.sechub.sharedkernel.configuration;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.verification.VerificationMode;
import org.springframework.validation.Errors;

import com.daimler.sechub.sharedkernel.validation.ApiVersionValidation;
import com.daimler.sechub.sharedkernel.validation.ValidationResult;

public class SecHubConfigurationValidatorTest {

	private SecHubConfigurationValidator validatorToTest;
	private Errors errors;
	private SecHubConfiguration target;
	private ApiVersionValidation apiValidation;
	private ValidationResult okResult;
	private ValidationResult failedResult;

	@Before
	public void before() throws Exception {
		okResult= mock(ValidationResult.class);
		when(okResult.isValid()).thenReturn(true);

		failedResult= mock(ValidationResult.class);
		when(failedResult.isValid()).thenReturn(false);

		apiValidation=mock(ApiVersionValidation.class);
		when(apiValidation.validate(any())).thenReturn(okResult);
		validatorToTest = new SecHubConfigurationValidator();
		validatorToTest.apiValidation=apiValidation;
		errors = mock(Errors.class);
		target = mock(SecHubConfiguration.class);

		/* prepare defaults */
		when(target.getApiVersion()).thenReturn("1.0");
		when(target.getWebScan()).thenReturn(Optional.empty());
	}

	@Test
	public void illegal_api_is_rejected() throws Exception {

		/* prepare */
		when(target.getApiVersion()).thenReturn("illegal");
		when(apiValidation.validate(eq("illegal"))).thenReturn(failedResult);
		/* execute */
		validatorToTest.validate(target, errors);

		/* test */
		verify(errors).rejectValue(eq(SecHubConfiguration.PROPERTY_API_VERSION),eq("api.error.unsupported.version"),any());
	}

	@Test
	public void api_1_0_is_NOT_rejected() throws Exception {

		/* execute */
		validatorToTest.validate(target, errors);

		/* test */
		verify(errors,never()).rejectValue(any(),any(),any());
	}


	@Test
	public void webconfig_with_uri_as_http_is_NOT_rejected() throws Exception {

		/* prepare */
		SecHubWebScanConfiguration webscan = mock(SecHubWebScanConfiguration.class);
		List<URI> list = new ArrayList<>();
		list.add(URI.create("http://www.example.com"));
		when(webscan.getUris()).thenReturn(list);
		when(target.getWebScan()).thenReturn(Optional.of(webscan));

		/* execute */
		validatorToTest.validate(target, errors);

		assertNoIllegalSchemaError();
	}

	@Test
	public void webconfig_with_uri_as_https_is_NOT_rejected() throws Exception {

		/* prepare */
		SecHubWebScanConfiguration webscan = mock(SecHubWebScanConfiguration.class);
		List<URI> list = new ArrayList<>();
		list.add(URI.create("https://www.example.com"));
		when(webscan.getUris()).thenReturn(list);
		when(target.getWebScan()).thenReturn(Optional.of(webscan));

		/* execute */
		validatorToTest.validate(target, errors);

		assertNoIllegalSchemaError();

	}

	@Test
	public void webconfig_with_uri_as_ftp_is_rejected() throws Exception {

		/* prepare */
		SecHubWebScanConfiguration webscan = mock(SecHubWebScanConfiguration.class);
		List<URI> list = new ArrayList<>();
		list.add(URI.create("ftp://www.example.com"));
		when(webscan.getUris()).thenReturn(list);
		when(target.getWebScan()).thenReturn(Optional.of(webscan));

		/* execute */
		validatorToTest.validate(target, errors);

		/* test */
		assertIllegalSchemaError();

	}


	@Test
	public void infraconfig_with_uri_as_ftp_is_NOT_rejected() throws Exception {

		/* prepare */
		SecHubInfrastructureScanConfiguration webscan = mock(SecHubInfrastructureScanConfiguration.class);
		List<URI> list = new ArrayList<>();
		list.add(URI.create("http://www.example.com"));
		when(webscan.getUris()).thenReturn(list);
		when(target.getInfraScan()).thenReturn(Optional.of(webscan));

		/* execute */
		validatorToTest.validate(target, errors);

		/* test */
		assertNoIllegalSchemaError();
	}


	@Test
	public void when_configuration_has_no_scan_config_at__missingScanDefinitionError_occurs() {
		/* prepare */
		when(target.getCodeScan()).thenReturn(Optional.empty());
		when(target.getInfraScan()).thenReturn(Optional.empty());
		when(target.getWebScan()).thenReturn(Optional.empty());

		/* execute */
		validatorToTest.validate(target, errors);

		/* test */
		assertMissingScanDefinitionError();
	}

	@Test
	public void when_configuration_has_infra_scan_config_at__missingScanDefinitionError_occurs_NOT() {
		/* prepare */
		when(target.getCodeScan()).thenReturn(Optional.empty());
		when(target.getInfraScan()).thenReturn(Optional.of(Mockito.mock(SecHubInfrastructureScanConfiguration.class)));
		when(target.getWebScan()).thenReturn(Optional.empty());

		/* execute */
		validatorToTest.validate(target, errors);

		/* test */
		assertNoMissingScanDefinitionError();
	}

	@Test
	public void when_configuration_has_code_scan_config_at__missingScanDefinitionError_occurs_NOT() {
		/* prepare */
		when(target.getCodeScan()).thenReturn(Optional.of(Mockito.mock(SecHubCodeScanConfiguration.class)));
		when(target.getInfraScan()).thenReturn(Optional.empty());
		when(target.getWebScan()).thenReturn(Optional.empty());

		/* execute */
		validatorToTest.validate(target, errors);

		/* test */
		assertNoMissingScanDefinitionError();
	}

	@Test
	public void when_configuration_has_web_scan_config_at__missingScanDefinitionError_occurs_NOT() {
		/* prepare */
		when(target.getWebScan()).thenReturn(Optional.of(Mockito.mock(SecHubWebScanConfiguration.class)));
		when(target.getInfraScan()).thenReturn(Optional.empty());
		when(target.getCodeScan()).thenReturn(Optional.empty());

		/* execute */
		validatorToTest.validate(target, errors);

		/* test */
		assertNoMissingScanDefinitionError();
	}

	private void assertMissingScanDefinitionError() {
		_assertMissingScanDefinitionError(Mockito.times(1));
	}

	private void assertNoMissingScanDefinitionError() {
		_assertMissingScanDefinitionError(Mockito.never());
	}

	private void _assertMissingScanDefinitionError(VerificationMode mode) {
		verify(errors,mode).reject(eq("api.error.config.noscan.defined"), any());
	}

	private void assertIllegalSchemaError() {
		_assertIllegalSchemaError(Mockito.times(1));
	}
	private void assertNoIllegalSchemaError() {
		_assertIllegalSchemaError(Mockito.never());
	}

	private void _assertIllegalSchemaError(VerificationMode mode) {
		verify(errors,mode).reject(eq("api.error.webscan.uri.illegalschema"), any(), any());
	}


}
