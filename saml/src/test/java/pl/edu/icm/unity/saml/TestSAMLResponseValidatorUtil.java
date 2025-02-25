/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.saml.SAMLResponseValidatorUtil.AUTHN_CONTEXT_CLASS_REF_ATTR;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import eu.unicore.samly2.SAMLUtils;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.samly2.validators.SSOAuthnResponseValidator;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata.Protocol;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.saml.sp.FakeTrustedIdPConfiguration;
import pl.edu.icm.unity.saml.sp.config.SAMLSPConfiguration;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

public class TestSAMLResponseValidatorUtil
{
	@Test
	public void shouldReturnAuthnContextFromAssertionAsAttribute() throws Exception
	{
		ReplayAttackChecker rac = new ReplayAttackChecker();
		SAMLSPConfiguration samlProperties = mock(SAMLSPConfiguration.class);
		SAMLResponseValidatorUtil responseValidator = new SAMLResponseValidatorUtil(
				samlProperties, rac, "");
		
		ResponseDocument respDoc = ResponseDocument.Factory.parse(
				new File("src/test/resources/responseDocSigned.xml"));
		List<AssertionDocument> authnAssertions = SAMLUtils.extractAllAssertions(
				respDoc.getResponse(), null).stream()
				.filter(a -> a.getAssertion().getAuthnStatementArray().length > 0)
				.collect(Collectors.toList());
			
		SSOAuthnResponseValidator validator = mock(SSOAuthnResponseValidator.class);
		when(validator.getAuthNAssertions()).thenReturn(authnAssertions);
		RemotelyAuthenticatedInput authnInput = responseValidator.convertAssertion(
				respDoc, validator, null, FakeTrustedIdPConfiguration.get());
		
		
		RemoteAttribute authnCtxAttr = authnInput.getAttributes().get(AUTHN_CONTEXT_CLASS_REF_ATTR);
		assertThat(authnCtxAttr).isNotNull();
		assertThat(authnCtxAttr.getValues().isEmpty()).isFalse();
		assertThat(authnInput.getAttributes().get(AUTHN_CONTEXT_CLASS_REF_ATTR).getValues().get(0)). 
				isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:Password");
		
	}
	
	@Test
	public void shouldReturnAuthnContextFromAssertionAsRemoteMetaContext() throws Exception
	{
		ReplayAttackChecker rac = new ReplayAttackChecker();
		SAMLSPConfiguration samlProperties = mock(SAMLSPConfiguration.class);
		SAMLResponseValidatorUtil responseValidator = new SAMLResponseValidatorUtil(
				samlProperties, rac, "");
		
		ResponseDocument respDoc = ResponseDocument.Factory.parse(
				new File("src/test/resources/responseDocSigned.xml"));
		List<AssertionDocument> authnAssertions = SAMLUtils.extractAllAssertions(
				respDoc.getResponse(), null).stream()
				.filter(a -> a.getAssertion().getAuthnStatementArray().length > 0)
				.collect(Collectors.toList());
			
		SSOAuthnResponseValidator validator = mock(SSOAuthnResponseValidator.class);
		when(validator.getAuthNAssertions()).thenReturn(authnAssertions);
		RemotelyAuthenticatedInput authnInput = responseValidator.convertAssertion(
				respDoc, validator, null, FakeTrustedIdPConfiguration.get());
		
		
		RemoteAuthnMetadata remoteAuthnMeta = authnInput.getRemoteAuthnMetadata();
		assertThat(remoteAuthnMeta).isNotNull();
		assertThat(remoteAuthnMeta.classReferences().isEmpty()).isFalse();
		assertThat(remoteAuthnMeta.classReferences().get(0)). 
				isEqualTo("urn:oasis:names:tc:SAML:2.0:ac:classes:Password");
		assertThat(remoteAuthnMeta.protocol()).isEqualTo(Protocol.SAML);
		assertThat(remoteAuthnMeta.remoteIdPId()).isEqualTo("http://centos6-unity1:8080/simplesaml/saml2/idp/metadata.php");

	}
	
	@Test
	public void shouldSaveAuthenticationTimeInAuthInput() throws Exception
	{
		ReplayAttackChecker rac = new ReplayAttackChecker();
		SAMLSPConfiguration samlProperties = mock(SAMLSPConfiguration.class);
		SAMLResponseValidatorUtil responseValidator = new SAMLResponseValidatorUtil(samlProperties, rac, "");

		ResponseDocument respDoc = ResponseDocument.Factory.parse(new File("src/test/resources/responseDocSigned.xml"));
		List<AssertionDocument> authnAssertions = SAMLUtils.extractAllAssertions(respDoc.getResponse(), null)
				.stream()
				.filter(a -> a.getAssertion()
						.getAuthnStatementArray().length > 0)
				.collect(Collectors.toList());

		SSOAuthnResponseValidator validator = mock(SSOAuthnResponseValidator.class);
		when(validator.getAuthNAssertions()).thenReturn(authnAssertions);
		RemotelyAuthenticatedInput authnInput = responseValidator.convertAssertion(respDoc, validator, null,
				FakeTrustedIdPConfiguration.get());

		DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
		ZonedDateTime dateTime = ZonedDateTime.parse("2014-01-29T18:25:17Z", formatter);

		assertThat(authnInput.getAuthenticationTime()).isEqualTo(dateTime.toInstant());

	}
}
