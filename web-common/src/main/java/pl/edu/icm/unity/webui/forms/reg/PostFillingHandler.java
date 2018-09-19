/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import com.vaadin.server.Page;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.RegistrationRedirectURLBuilder;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.forms.FinalRegistrationConfiguration;

/**
 * Controller making decisions on what to do/show after completed registration.
 * 
 * @author K. Benedyczak
 */
public class PostFillingHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, PostFillingHandler.class);
	private UnityMessageSource msg;
	private RegistrationsManagement registrationsManagement;
	private String formId;
	private Consumer<String> redirector;
	private List<RegistrationWrapUpConfig> wrapUpConfigs;
	
	public PostFillingHandler(IdPLoginController loginController, 
			RegistrationForm form, UnityMessageSource msg, 
			RegistrationsManagement registrationsManagement)
	{
		this(url -> redirect(url, loginController), 
				form.getName(), 
				form.getWrapUpConfig(), 
				msg, registrationsManagement);
	}

	PostFillingHandler(Consumer<String> redirector,
			String formId, 
			List<RegistrationWrapUpConfig> wrapUpConfigs,
			UnityMessageSource msg, 
			RegistrationsManagement registrationsManagement)
	{
		this.redirector = redirector;
		this.formId = formId;
		this.wrapUpConfigs = wrapUpConfigs;
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
	}
	
	public Optional<FinalRegistrationConfiguration> getFinalRegistrationConfigurationPostSubmit(String requestId)
	{
		RegistrationRequestStatus status = getRequestStatus(requestId, registrationsManagement);
		TriggeringState state = requestStatusToState(status);
		return getFinalRegistrationConfigurationGeneric(state, requestId);
	}

	public Optional<FinalRegistrationConfiguration> getFinalRegistrationConfigurationOnError(
			RegistrationWrapUpConfig.TriggeringState state)
	{
		return getFinalRegistrationConfigurationGeneric(state, null);
	}

	private Optional<FinalRegistrationConfiguration> getFinalRegistrationConfigurationGeneric(
			RegistrationWrapUpConfig.TriggeringState state, String requestId)
	{
		RegistrationWrapUpConfig config = getWrapUpConfigForState(state);
		String title = getTitle(state, config);
		String info = getInfo(state, config);
		String finalRedirectURL = buildFinalRedirectURL(config, requestId, state); 
		
		if (config.isAutomatic() && Strings.isNotEmpty(finalRedirectURL))
		{
			redirector.accept(finalRedirectURL);
			return Optional.empty();
		}
		
		String redirectCaption = config.getRedirectCaption() == null ? 
				msg.getMessage("RegistrationFormsChooserComponent.defaultRedirectCaption") 
				: config.getRedirectCaption().getValue(msg);
		
		return Optional.of(new FinalRegistrationConfiguration(title, info,
				finalRedirectURL == null ? null : () -> redirector.accept(finalRedirectURL), 
				redirectCaption));
	}
	
	private String buildFinalRedirectURL(RegistrationWrapUpConfig config, String requestId, TriggeringState state)
	{
		return config.getRedirectURL() == null ? null : 
			new RegistrationRedirectURLBuilder(config.getRedirectURL(), 
						formId, requestId, state).build();
	}
	
	private TriggeringState requestStatusToState(RegistrationRequestStatus status)
	{
		switch (status)
		{
		case accepted:
			return TriggeringState.AUTO_ACCEPTED;
		case pending:
			return TriggeringState.SUBMITTED;
		case rejected:
			return TriggeringState.AUTO_REJECTED;
		default:
			throw new IllegalStateException("Unknown status: " + status);
		}
	}
	
	private RegistrationRequestStatus getRequestStatus(String requestId, RegistrationsManagement registrationsManagement) 
	{
		try
		{
			for (RegistrationRequestState r : registrationsManagement.getRegistrationRequests())
			{
				if (r.getRequestId().equals(requestId))
					return r.getStatus();
			}
		} catch (EngineException e)
		{
			log.error("Shouldn't happen: can't get request status, assuming rejested", e);
		}
		return RegistrationRequestStatus.rejected;
	}

	private RegistrationWrapUpConfig getWrapUpConfigForState(RegistrationWrapUpConfig.TriggeringState state)
	{
		RegistrationWrapUpConfig defaultCofnig = null;
		for (RegistrationWrapUpConfig config: wrapUpConfigs)
		{
			if (config.getState() == state)
				return config;
			if (config.getState() == TriggeringState.DEFAULT)
				defaultCofnig = config;
		}
		return defaultCofnig == null ? new RegistrationWrapUpConfig(state) : defaultCofnig;
	}
	
	private String getTitle(RegistrationWrapUpConfig.TriggeringState state, RegistrationWrapUpConfig config)
	{
		return config.getTitle() == null ? getDefaultTitle(state) : config.getTitle().getValue(msg);
	}

	private String getInfo(RegistrationWrapUpConfig.TriggeringState state, RegistrationWrapUpConfig config)
	{
		return config.getInfo() == null ? getDefaultInfo(state) : config.getInfo().getValue(msg);
	}
	
	private String getDefaultTitle(RegistrationWrapUpConfig.TriggeringState state)
	{
		String msgKey;
		switch (state)
		{
		case AUTO_ACCEPTED:
			msgKey = "RegistrationWrupUp.requestAcceptedTitle"; 
			break;
		case AUTO_REJECTED:
			msgKey = "RegistrationWrupUp.requestRejectedTitle";
			break;
		case CANCELLED:
			msgKey = "RegistrationWrupUp.registrationCancelledTitle";
			break;
		case GENERAL_ERROR:
			msgKey = "RegistrationWrupUp.genericRegistrationErrorTitle";
			break;
		case INVITATION_CONSUMED:
			msgKey = "RegistrationWrupUp.invitationAlreadyConsumedTitle";
			break;
		case INVITATION_EXPIRED:
			msgKey = "RegistrationWrupUp.invitationExpiredTitle";
			break;
		case INVITATION_MISSING:
			msgKey = "RegistrationWrupUp.invitationUnknownTitle";
			break;
		case PRESET_USER_EXISTS:
			msgKey = "RegistrationWrupUp.userExistsTitle";
			break;
		case SUBMITTED:
			msgKey = "RegistrationWrupUp.requestSubmittedTitle";
			break;
		case EMAIL_CONFIRMATION_FAILED:
			msgKey = "RegistrationWrupUp.confirmationFailedTitle";
			break;
		case EMAIL_CONFIRMED:
			msgKey = "RegistrationWrupUp.emailConfirmedTitle";
			break;
		case DEFAULT:
		default:
			msgKey = "RegistrationWrupUp.genericRegistrtionFinishTitle";
		}
		return new I18nString(msgKey, msg).getValue(msg);
	}

	private String getDefaultInfo(RegistrationWrapUpConfig.TriggeringState state)
	{
		return null;
	}
	
	private static void redirect(String redirectUrl, IdPLoginController loginController)
	{
		loginController.breakLogin();
		Page.getCurrent().open(redirectUrl, null);
	}
}
