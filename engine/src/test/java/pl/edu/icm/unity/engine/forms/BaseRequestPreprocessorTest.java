/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import pl.edu.icm.unity.base.entity.Identity;
import pl.edu.icm.unity.base.entity.IdentityParam;
import pl.edu.icm.unity.base.exceptions.IllegalFormContentsException;
import pl.edu.icm.unity.base.registration.ParameterRetrievalSettings;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.RegistrationFormBuilder;
import pl.edu.icm.unity.base.registration.RegistrationRequest;
import pl.edu.icm.unity.base.registration.RegistrationRequestBuilder;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.identity.EntityResolver;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;
import pl.edu.icm.unity.engine.attribute.AttributeTypeHelper;
import pl.edu.icm.unity.engine.attribute.AttributesHelper;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.engine.server.EngineInitialization;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.store.api.AttributeTypeDAO;
import pl.edu.icm.unity.store.api.GroupDAO;
import pl.edu.icm.unity.store.api.generic.InvitationDB;

public class BaseRequestPreprocessorTest
{
	@Test
	public void shouldDenyRequestUsingOccupiedIdentityWhenCheckingEnabled()
	{
		RegistrationForm form = buildForm(true);
		IdentityParam idParam = new IdentityParam(UsernameIdentity.ID, "test-user");

		EntityResolver entityResolver = mock(EntityResolver.class);
		when(entityResolver.getFullIdentity(eq(idParam))).thenReturn(new Identity(idParam, 1, "foo"));
		BaseRequestPreprocessor preprocessor = buildPreprocessor(entityResolver);
		
		RegistrationRequest request = buildRequest(idParam);
		
		Throwable error = catchThrowable(() -> preprocessor.validateSubmittedRequest(form, request, false, false));
		
		assertThat(error).isInstanceOf(IllegalFormContentsException.OccupiedIdentityUsedInRequest.class);
	}

	@Test
	public void shouldPermitRequestUsingNotOccupiedIdentityWhenCheckingEnabled()
	{
		RegistrationForm form = buildForm(true);
		IdentityParam idParam = new IdentityParam(UsernameIdentity.ID, "test-user");
		
		EntityResolver entityResolver = mock(EntityResolver.class);
		when(entityResolver.getFullIdentity(eq(idParam))).thenThrow(new UnknownIdentityException("Entity is unknown"));
		BaseRequestPreprocessor preprocessor = buildPreprocessor(entityResolver);
		
		RegistrationRequest request = buildRequest(idParam);
		
		Throwable error = catchThrowable(() -> preprocessor.validateSubmittedRequest(form, request, false, false));
		
		assertThat(error).isNull();
	}
	
	@Test
	public void shouldPermitRequestUsingOccupiedIdentityWhenCheckingDisabled()
	{
		RegistrationForm form = buildForm(false);
		IdentityParam idParam = new IdentityParam(UsernameIdentity.ID, "test-user");
		
		EntityResolver entityResolver = mock(EntityResolver.class);
		when(entityResolver.getFullIdentity(eq(idParam))).thenReturn(new Identity(idParam, 1, "foo"));
		BaseRequestPreprocessor preprocessor = buildPreprocessor(entityResolver);
			
		RegistrationRequest request = buildRequest(idParam);
			
		Throwable error = catchThrowable(() -> preprocessor.validateSubmittedRequest(form, request, false, false));
		
		assertThat(error).isNull();
	}
	
	private BaseRequestPreprocessor buildPreprocessor(EntityResolver entityResolver)
	{
		IdentityTypesRegistry idTypesReg = mock(IdentityTypesRegistry.class);
		when(idTypesReg.getByName(eq(UsernameIdentity.ID))).thenReturn(new UsernameIdentity());
		return new BaseRequestPreprocessor(
				mock(CredentialRepository.class), 
				mock(AttributeTypeDAO.class),
				mock(GroupDAO.class), 
				mock(AttributesHelper.class), 
				mock(AttributeTypeHelper.class),
				entityResolver, 
				idTypesReg,
				mock(LocalCredentialsRegistry.class), 
				mock(InvitationDB.class));
	}
	
	private RegistrationForm buildForm(boolean enableChecking)
	{
		return new RegistrationFormBuilder()
				.withCheckIdentityOnSubmit(enableChecking)
				.withName("f1")
				.withDefaultCredentialRequirement(
						EngineInitialization.DEFAULT_CREDENTIAL_REQUIREMENT)
				.withAddedIdentityParam()
					.withIdentityType(UsernameIdentity.ID)
					.withRetrievalSettings(ParameterRetrievalSettings.interactive)
				.endIdentityParam()
				.build();
	}
	
	private RegistrationRequest buildRequest(IdentityParam idParam)
	{
		return new RegistrationRequestBuilder()
				.withFormId("f1")
				.withAddedIdentity(idParam)
				.build();
	}
}
