/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;


import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.identity.IdentityTaV;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.engine.InitializerCommon;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.attr.StringAttribute;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;

public class RemoteDataRegistrationParserTest
{
	@Test
	public void shouldParseIdentitites() throws AuthenticationException
	{
		RegistrationFormBuilder formBuilder = getBaseFormBuilder();
		formBuilder.withAddedIdentityParam()
			.withIdentityType(UsernameIdentity.ID)
			.withRetrievalSettings(ParameterRetrievalSettings.automatic)
			.endIdentityParam();
		RegistrationForm form = formBuilder.build();

		RemotelyAuthenticatedPrincipal remoteCtx = new RemotelyAuthenticatedPrincipal("idp", "iprof");
		remoteCtx.addIdentities(Lists.newArrayList(new IdentityTaV(UsernameIdentity.ID, "user")));

		Map<String, IdentityTaV> identities = RemoteDataRegistrationParser.parseRemoteIdentities(form, remoteCtx);
		
		assertThat(identities.size()).isEqualTo(1);
		assertThat(identities.containsKey(UsernameIdentity.ID)).isEqualTo(true);
		assertThat(identities.get(UsernameIdentity.ID)).isEqualTo(remoteCtx.getIdentities().iterator().next());
	}

	@Test
	public void shouldFailWithoutMandatoryRemoteIdentity()
	{
		RegistrationFormBuilder formBuilder = getBaseFormBuilder();
		formBuilder.withAddedIdentityParam()
			.withIdentityType(UsernameIdentity.ID)
			.withRetrievalSettings(ParameterRetrievalSettings.automatic)
			.endIdentityParam();
		RegistrationForm form = formBuilder.build();

		RemotelyAuthenticatedPrincipal remoteCtx = new RemotelyAuthenticatedPrincipal("idp", "iprof");
		Map<String, IdentityTaV> identities = RemoteDataRegistrationParser.parseRemoteIdentities(form, remoteCtx);
		Throwable error = catchThrowable(() -> 
			RemoteDataRegistrationParser.assertMandatoryRemoteIdentitiesArePresent(form, identities));
		
		assertThat(error).isNotNull().isInstanceOf(AuthenticationException.class);
	}
	
	@Test
	public void shouldParseAttributes() throws AuthenticationException
	{
		RegistrationFormBuilder formBuilder = getBaseFormBuilder();
		formBuilder.withAddedAttributeParam()
			.withAttributeType(InitializerCommon.EMAIL_ATTR)
			.withGroup("/")
			.withRetrievalSettings(ParameterRetrievalSettings.automatic)
			.endAttributeParam();
		RegistrationForm form = formBuilder.build();
		
		RemotelyAuthenticatedPrincipal remoteCtx = new RemotelyAuthenticatedPrincipal("idp", "iprof");
		remoteCtx.addAttributes(Lists.newArrayList(StringAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "remote@example.com")));
		
		
		Map<String, Attribute> attributes = RemoteDataRegistrationParser.parseRemoteAttributes(form, remoteCtx);
		
		assertThat(attributes.size()).isEqualTo(1);
		String key = RemoteDataRegistrationParser.getAttributeKey(form.getAttributeParams().get(0));
		assertThat(attributes.containsKey(key)).isEqualTo(true);
		assertThat(attributes.get(key)).isEqualTo(remoteCtx.getAttributes().iterator().next());
	}

	@Test
	public void shouldParseAttributesUsingWildcard() throws AuthenticationException
	{
		RegistrationFormBuilder formBuilder = getBaseFormBuilder();
		formBuilder.withAddedAttributeParam()
			.withAttributeType(InitializerCommon.EMAIL_ATTR)
			.withGroup("/A*/**/B")
			.withRetrievalSettings(ParameterRetrievalSettings.automatic)
			.endAttributeParam();
		RegistrationForm form = formBuilder.build();
		
		RemotelyAuthenticatedPrincipal remoteCtx = new RemotelyAuthenticatedPrincipal("idp", "iprof");
		remoteCtx.addAttributes(Lists.newArrayList(StringAttribute.of(InitializerCommon.EMAIL_ATTR, 
				"/Addd/C/Z/B", "remote@example.com")));
		
		Map<String, Attribute> attributes = RemoteDataRegistrationParser.parseRemoteAttributes(form, remoteCtx);
		
		assertThat(attributes.size()).isEqualTo(1);
		String key = RemoteDataRegistrationParser.getAttributeKey(form.getAttributeParams().get(0));
		assertThat(attributes.containsKey(key)).isEqualTo(true);
		assertThat(attributes.get(key)).isEqualTo(remoteCtx.getAttributes().iterator().next());
	}
	
	@Test
	public void shouldFailWithoutMandatoryRemoteAttribute()
	{
		RegistrationFormBuilder formBuilder = getBaseFormBuilder();
		formBuilder.withAddedAttributeParam()
			.withAttributeType(InitializerCommon.EMAIL_ATTR)
			.withGroup("/")
			.withRetrievalSettings(ParameterRetrievalSettings.automatic)
			.endAttributeParam();
		RegistrationForm form = formBuilder.build();
		
		RemotelyAuthenticatedPrincipal remoteCtx = new RemotelyAuthenticatedPrincipal("idp", "iprof");
		Map<String, Attribute> remoteAttributes = RemoteDataRegistrationParser.parseRemoteAttributes(form, remoteCtx);
		Throwable error = catchThrowable(() -> 
			RemoteDataRegistrationParser.assertMandatoryRemoteAttributesArePresent(form, remoteAttributes));

		assertThat(error).isNotNull().isInstanceOf(AuthenticationException.class);
	}

	@Test
	public void shouldFailWithoutMandatoryRemoteAttributeUsingWildcard()
	{
		RegistrationFormBuilder formBuilder = getBaseFormBuilder();
		formBuilder.withAddedAttributeParam()
			.withAttributeType(InitializerCommon.EMAIL_ATTR)
			.withGroup("/A*/**/B")
			.withRetrievalSettings(ParameterRetrievalSettings.automatic)
			.endAttributeParam();
		RegistrationForm form = formBuilder.build();
		
		RemotelyAuthenticatedPrincipal remoteCtx = new RemotelyAuthenticatedPrincipal("idp", "iprof");
		remoteCtx.addAttributes(Lists.newArrayList(StringAttribute.of(InitializerCommon.EMAIL_ATTR, 
				"/A/notMatching", "remote@example.com")));
		
		Map<String, Attribute> remoteAttributes = RemoteDataRegistrationParser.parseRemoteAttributes(form, remoteCtx);
		Throwable error = catchThrowable(() -> 
			RemoteDataRegistrationParser.assertMandatoryRemoteAttributesArePresent(form, remoteAttributes));

		assertThat(error).isNotNull().isInstanceOf(AuthenticationException.class);
	}	
	
	
	private RegistrationFormBuilder getBaseFormBuilder()
	{
		return new RegistrationFormBuilder()
				.withName("f1")
				.withPubliclyAvailable(true)
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT);
	}
}
