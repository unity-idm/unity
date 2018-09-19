/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.RegistrationRedirectURLBuilder;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.forms.FinalRegistrationConfiguration;

public class PostFillingHandlerTest extends DBIntegrationTestBase
{
	@Autowired
	private UnityMessageSource msg;
	
	@Test
	public void testDefaultSubmitted() throws EngineException
	{
		shouldHandleDefaultSubmitted(RegistrationRequestStatus.accepted, "Sign up completed");
		shouldHandleDefaultSubmitted(RegistrationRequestStatus.pending, "Registration request submitted");
		shouldHandleDefaultSubmitted(RegistrationRequestStatus.rejected, "Registration request was rejected");
	}
	
	@Test
	public void testDefaultError() throws EngineException
	{
		shouldHandleDefaultError(TriggeringState.PRESET_USER_EXISTS, "It seems you are already registered, please sign in");
		shouldHandleDefaultError(TriggeringState.CANCELLED, "Registration cancelled");
		shouldHandleDefaultError(TriggeringState.EMAIL_CONFIRMED, "Email confirmation succeeded");
		shouldHandleDefaultError(TriggeringState.EMAIL_CONFIRMATION_FAILED, "Email confirmation failed");
		shouldHandleDefaultError(TriggeringState.GENERAL_ERROR, "Registration failed");
		shouldHandleDefaultError(TriggeringState.INVITATION_CONSUMED, "This invitation was already processed");
		shouldHandleDefaultError(TriggeringState.INVITATION_EXPIRED, "The invitation is expired");
		shouldHandleDefaultError(TriggeringState.INVITATION_MISSING, "The invitation is not recognized");
	}

	@Test
	public void shouldHandleFullConfig() throws EngineException
	{
		shouldHandleConfiguredSubmitted(RegistrationRequestStatus.accepted, 
				new RegistrationWrapUpConfig(TriggeringState.AUTO_ACCEPTED, 
						new I18nString("title"), new I18nString("info"), 
						new I18nString("redirect"), false, "url"));
	}

	@Test
	public void shouldHandleMissingExtraInfo() throws EngineException
	{
		shouldHandleConfiguredSubmitted(RegistrationRequestStatus.accepted, 
				new RegistrationWrapUpConfig(TriggeringState.AUTO_ACCEPTED, 
						new I18nString("title"), null, 
						new I18nString("redirect"), false, "url"));
	}
	
	@Test
	public void shouldHandleAutoRedirect() throws EngineException
	{
		shouldHandleConfiguredOnError(new RegistrationWrapUpConfig(TriggeringState.GENERAL_ERROR, 
						null, null, 
						null, true, "url"));
	}

	@Test
	public void shouldHandleNoButtonCaption() throws EngineException
	{
		shouldHandleConfiguredOnError(new RegistrationWrapUpConfig(TriggeringState.PRESET_USER_EXISTS, 
						new I18nString("title"), new I18nString("info"), 
						null, false, "url"));
	}

	@Test
	public void shouldHandleNoRedirect() throws EngineException
	{
		shouldHandleConfiguredOnError(new RegistrationWrapUpConfig(TriggeringState.PRESET_USER_EXISTS, 
						new I18nString("title"), new I18nString("info"), 
						null, false, null));
	}


	private void shouldHandleDefaultSubmitted(RegistrationRequestStatus status, String expectedTitle) throws EngineException
	{
		@SuppressWarnings("unchecked")
		Consumer<String> redirector = mock(Consumer.class);
		RegistrationsManagement registrationMan = mock(RegistrationsManagement.class);
		List<RegistrationWrapUpConfig> configs = Lists.newArrayList();
		PostFillingHandler handler = new PostFillingHandler(redirector, "formId", 
				configs, msg, registrationMan);
		
		RegistrationRequestState reqState = new RegistrationRequestState();
		reqState.setRequestId("requestId");
		reqState.setStatus(status);
		when(registrationMan.getRegistrationRequests()).thenReturn(Lists.newArrayList(reqState));
		
		Optional<FinalRegistrationConfiguration> ret = handler.getFinalRegistrationConfigurationPostSubmit("requestId");
		
		assertThat(ret.isPresent(), is(true));
		assertThat(ret.get().extraInformation, is(nullValue()));
		assertThat(ret.get().mainInformation, is(expectedTitle));
		assertThat(ret.get().redirectButtonText, is("Continue"));
		assertThat(ret.get().redirectHandler, is(nullValue()));
	}

	private void shouldHandleDefaultError(TriggeringState state, String expectedTitle) throws EngineException
	{
		@SuppressWarnings("unchecked")
		Consumer<String> redirector = mock(Consumer.class);
		RegistrationsManagement registrationMan = mock(RegistrationsManagement.class);
		List<RegistrationWrapUpConfig> configs = Lists.newArrayList();
		PostFillingHandler handler = new PostFillingHandler(redirector, "formId", 
				configs, msg, registrationMan);
		
		Optional<FinalRegistrationConfiguration> ret = handler.getFinalRegistrationConfigurationOnError(state);
		
		assertThat(ret.isPresent(), is(true));
		assertThat(ret.get().extraInformation, is(nullValue()));
		assertThat(ret.get().mainInformation, is(expectedTitle));
		assertThat(ret.get().redirectButtonText, is("Continue"));
		assertThat(ret.get().redirectHandler, is(nullValue()));
	}
	
	private void shouldHandleConfiguredSubmitted(RegistrationRequestStatus status, RegistrationWrapUpConfig config) throws EngineException
	{
		@SuppressWarnings("unchecked")
		Consumer<String> redirector = mock(Consumer.class);
		RegistrationsManagement registrationMan = mock(RegistrationsManagement.class);
		List<RegistrationWrapUpConfig> configs = Lists.newArrayList(config);
		PostFillingHandler handler = new PostFillingHandler(redirector, "formId", 
				configs, msg, registrationMan);
		
		RegistrationRequestState reqState = new RegistrationRequestState();
		reqState.setRequestId("requestId");
		reqState.setStatus(status);
		when(registrationMan.getRegistrationRequests()).thenReturn(Lists.newArrayList(reqState));
		
		Optional<FinalRegistrationConfiguration> ret = handler.getFinalRegistrationConfigurationPostSubmit("requestId");
		
		if (config.isAutomatic())
		{
			assertThat(ret.isPresent(), is(false));
			String url = new RegistrationRedirectURLBuilder(config.getRedirectURL(), "formId", "requestId", 
					config.getState()).build();
			verify(redirector).accept(url);
		} else
		{
			assertThat(ret.isPresent(), is(true));
			assertThat(ret.get().extraInformation, is(config.getInfo() == null ? null : config.getInfo().getDefaultValue()));
			assertThat(ret.get().mainInformation, is(config.getTitle() == null ? "" : config.getTitle().getDefaultValue()));
			assertThat(ret.get().redirectButtonText, is(config.getRedirectCaption() == null ? 
					"Continue" : config.getRedirectCaption().getDefaultValue()));
			assertThat(ret.get().redirectHandler, is(config.getRedirectURL() != null ? notNullValue() : nullValue()));
		}
	}

	private void shouldHandleConfiguredOnError(RegistrationWrapUpConfig config) throws EngineException
	{
		@SuppressWarnings("unchecked")
		Consumer<String> redirector = mock(Consumer.class);
		RegistrationsManagement registrationMan = mock(RegistrationsManagement.class);
		List<RegistrationWrapUpConfig> configs = Lists.newArrayList(config);
		PostFillingHandler handler = new PostFillingHandler(redirector, "formId", 
				configs, msg, registrationMan);
		
		Optional<FinalRegistrationConfiguration> ret = handler.getFinalRegistrationConfigurationOnError(config.getState());
		
		if (config.isAutomatic())
		{
			assertThat(ret.isPresent(), is(false));
			String url = new RegistrationRedirectURLBuilder(config.getRedirectURL(), "formId", null, 
					config.getState()).build();
			verify(redirector).accept(url);
		} else
		{
			assertThat(ret.isPresent(), is(true));
			assertThat(ret.get().extraInformation, is(config.getInfo() == null ? null : config.getInfo().getDefaultValue()));
			assertThat(ret.get().mainInformation, is(config.getTitle() == null ? "" : config.getTitle().getDefaultValue()));
			assertThat(ret.get().redirectButtonText, is(config.getRedirectCaption() == null ? 
					"Continue" : config.getRedirectCaption().getDefaultValue()));
			assertThat(ret.get().redirectHandler, is(config.getRedirectURL() != null ? notNullValue() : nullValue()));
		}
	}

}
