/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

import pl.edu.icm.unity.engine.DBIntegrationTestBase;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.registration.RegistrationRedirectURLBuilder;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;

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
		shouldHandleDefaultError(TriggeringState.INVITATION_MISSING, "The invitation was already processed or was removed");
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
		List<RegistrationWrapUpConfig> configs = Lists.newArrayList();
		PostFillingHandler handler = new PostFillingHandler("formId", 
				configs, msg, "pageTitle", "logo", true);
		
		WorkflowFinalizationConfiguration ret = handler.getFinalRegistrationConfigurationPostSubmit("requestId", status);
		
		assertThat(ret.extraInformation, is(nullValue()));
		assertThat(ret.mainInformation, is(expectedTitle));
		assertThat(ret.redirectButtonText, is("Continue"));
		assertThat(ret.redirectURL, is(nullValue()));
	}

	private void shouldHandleDefaultError(TriggeringState state, String expectedTitle) throws EngineException
	{
		List<RegistrationWrapUpConfig> configs = Lists.newArrayList();
		PostFillingHandler handler = new PostFillingHandler("formId", 
				configs, msg, "pageTitle", "logo", true);
		
		WorkflowFinalizationConfiguration ret = handler.getFinalRegistrationConfigurationOnError(state);
		
		assertThat(ret.autoRedirect, is(false));
		assertThat(ret.extraInformation, is(nullValue()));
		assertThat(ret.mainInformation, is(expectedTitle));
		assertThat(ret.redirectButtonText, is("Continue"));
		assertThat(ret.redirectURL, is(nullValue()));
	}
	
	private void shouldHandleConfiguredSubmitted(RegistrationRequestStatus status, RegistrationWrapUpConfig config) throws EngineException
	{
		List<RegistrationWrapUpConfig> configs = Lists.newArrayList(config);
		PostFillingHandler handler = new PostFillingHandler("formId", 
				configs, msg, "pageTitle", "logo", true);
		
		WorkflowFinalizationConfiguration ret = handler.getFinalRegistrationConfigurationPostSubmit("requestId", status);
		
		String url = new RegistrationRedirectURLBuilder(config.getRedirectURL(), "formId", "requestId", 
				config.getState()).build();
		if (config.isAutomatic())
		{
			assertThat(ret.autoRedirect, is(true));
			assertThat(ret.redirectURL, is(url));
		} else
		{
			assertThat(ret.autoRedirect, is(false));
			assertThat(ret.extraInformation, is(config.getInfo() == null ? null : config.getInfo().getDefaultValue()));
			assertThat(ret.mainInformation, is(config.getTitle() == null ? "" : config.getTitle().getDefaultValue()));
			assertThat(ret.redirectButtonText, is(config.getRedirectCaption() == null ? 
					"Continue" : config.getRedirectCaption().getDefaultValue()));
			assertThat(ret.redirectURL, is(url));
		}
	}

	private void shouldHandleConfiguredOnError(RegistrationWrapUpConfig config) throws EngineException
	{
		List<RegistrationWrapUpConfig> configs = Lists.newArrayList(config);
		PostFillingHandler handler = new PostFillingHandler("formId", 
				configs, msg, "pageTitle", "logo", true);
		
		WorkflowFinalizationConfiguration ret = handler.getFinalRegistrationConfigurationOnError(config.getState());
		
		String url = new RegistrationRedirectURLBuilder(config.getRedirectURL(), "formId", null, 
				config.getState()).build();
		if (config.isAutomatic())
		{
			assertThat(ret.autoRedirect, is(true));
			assertThat(ret.redirectURL, is(url));
		} else
		{
			assertThat(ret.autoRedirect, is(false));
			assertThat(ret.extraInformation, is(config.getInfo() == null ? null : config.getInfo().getDefaultValue()));
			assertThat(ret.mainInformation, is(config.getTitle() == null ? "" : config.getTitle().getDefaultValue()));
			assertThat(ret.redirectButtonText, is(config.getRedirectCaption() == null ? 
					"Continue" : config.getRedirectCaption().getDefaultValue()));
			assertThat(ret.redirectURL, is(url));
		}
	}

}
