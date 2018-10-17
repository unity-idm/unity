/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static pl.edu.icm.unity.saml.SAMLResponseValidatorUtil.AUTHN_CONTEXT_CLASS_REF_ATTR;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import eu.unicore.samly2.SAMLUtils;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.samly2.validators.SSOAuthnResponseValidator;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteAttribute;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import xmlbeans.org.oasis.saml2.assertion.AssertionDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

public class TestSAMLResponseValidatorUtil
{
	@Test
	public void shouldReturnAuthnContextFromAssertionAsAttribute() throws Exception
	{
		ReplayAttackChecker rac = new ReplayAttackChecker();
		SAMLSPProperties samlProperties = mock(SAMLSPProperties.class);
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
				respDoc, validator, null, "cfgKey");
		
		
		RemoteAttribute authnCtxAttr = authnInput.getAttributes().get(AUTHN_CONTEXT_CLASS_REF_ATTR);
		assertThat(authnCtxAttr, is(notNullValue()));
		assertThat(authnCtxAttr.getValues().isEmpty(), is(false));
		assertThat(authnInput.getAttributes().get(AUTHN_CONTEXT_CLASS_REF_ATTR).getValues().get(0), 
				is("urn:oasis:names:tc:SAML:2.0:ac:classes:Password"));
		
	}
}
