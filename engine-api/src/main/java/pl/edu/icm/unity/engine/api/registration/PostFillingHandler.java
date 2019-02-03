/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.registration;

import java.util.List;

import org.apache.logging.log4j.util.Strings;

import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;

/**
 * Controller making decisions on what to do/show after completed registration.
 * 
 * @author K. Benedyczak
 */
public class PostFillingHandler
{
	private UnityMessageSource msg;
	private String formId;
	private List<RegistrationWrapUpConfig> wrapUpConfigs;
	private String pageTitle;
	private String logoURL;
	private String msgPrefix;
	
	public PostFillingHandler(String formId, List<RegistrationWrapUpConfig> wrapUpConfigs, UnityMessageSource msg,
			String pageTitle, String logoURL, boolean registration)
	{
		this.formId = formId;
		this.wrapUpConfigs = wrapUpConfigs;
		this.msg = msg;
		this.pageTitle = pageTitle;
		this.logoURL = logoURL;
		this.msgPrefix = registration ? "RegistrationWrupUp." : "EnquiryWrupUp.";
	}
	
	public WorkflowFinalizationConfiguration getFinalRegistrationConfigurationPostSubmit(String requestId,
			RegistrationRequestStatus status)
	{
		TriggeringState state = requestStatusToState(status);
		boolean success = status != RegistrationRequestStatus.rejected;
		return getFinalRegistrationConfigurationGeneric(success, state, requestId);
	}

	public WorkflowFinalizationConfiguration getFinalRegistrationConfigurationNonSubmit(boolean successful,
			String requestId, RegistrationWrapUpConfig.TriggeringState state)
	{
		return getFinalRegistrationConfigurationGeneric(successful, state, requestId);
	}
	
	public WorkflowFinalizationConfiguration getFinalRegistrationConfigurationOnError(
			RegistrationWrapUpConfig.TriggeringState state)
	{
		return getFinalRegistrationConfigurationGeneric(false, state, null);
	}

	private WorkflowFinalizationConfiguration getFinalRegistrationConfigurationGeneric(
			boolean successful,
			RegistrationWrapUpConfig.TriggeringState state, String requestId)
	{
		RegistrationWrapUpConfig config = getWrapUpConfigForState(state);
		String title = getTitle(state, config);
		String info = getInfo(state, config);
		String finalRedirectURL = buildFinalRedirectURL(config, requestId, state); 
		
		if (config.isAutomatic() && Strings.isNotEmpty(finalRedirectURL))
			return WorkflowFinalizationConfiguration.autoRedirect(finalRedirectURL);
		
		String redirectCaption = config.getRedirectCaption() == null ? 
				msg.getMessage("RegistrationFormsChooserComponent.defaultRedirectCaption") 
				: config.getRedirectCaption().getValue(msg);
		
		return WorkflowFinalizationConfiguration.builder()
				.setSuccess(successful)
				.setAutoRedirect(false)
				.setRedirectURL(finalRedirectURL)
				.setRedirectButtonText(redirectCaption)
				.setMainInformation(title)
				.setExtraInformation(info)
				.setPageTitle(pageTitle)
				.setLogoURL(logoURL)
				.build();
	}
	
	private String buildFinalRedirectURL(RegistrationWrapUpConfig config, String requestId, TriggeringState state)
	{
		return Strings.isEmpty(config.getRedirectURL()) ? null : 
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
		return config.getTitle() == null || config.getTitle().isEmpty() ? getDefaultTitle(state) : config.getTitle().getValue(msg);
	}

	private String getInfo(RegistrationWrapUpConfig.TriggeringState state, RegistrationWrapUpConfig config)
	{
		return config.getInfo() == null || config.getInfo().isEmpty() ? getDefaultInfo(state) : config.getInfo().getValue(msg);
	}
	
	private String getDefaultTitle(RegistrationWrapUpConfig.TriggeringState state)
	{
		String msgKey;
		switch (state)
		{
		case AUTO_ACCEPTED:
			msgKey = "requestAcceptedTitle"; 
			break;
		case AUTO_REJECTED:
			msgKey = "requestRejectedTitle";
			break;
		case CANCELLED:
			msgKey = "registrationCancelledTitle";
			break;
		case GENERAL_ERROR:
			msgKey = "genericRegistrationErrorTitle";
			break;
		case INVITATION_CONSUMED:
			msgKey = "invitationAlreadyConsumedTitle";
			break;
		case INVITATION_EXPIRED:
			msgKey = "invitationExpiredTitle";
			break;
		case INVITATION_MISSING:
			msgKey = "invitationUnknownTitle";
			break;
		case PRESET_USER_EXISTS:
			msgKey = "userExistsTitle";
			break;
		case SUBMITTED:
			msgKey = "requestSubmittedTitle";
			break;
		case EMAIL_CONFIRMATION_FAILED:
			msgKey = "confirmationFailedTitle";
			break;
		case EMAIL_CONFIRMED:
			msgKey = "emailConfirmedTitle";
			break;
		case IGNORED_ENQUIRY:
			msgKey = "ignoredEnquiry";
			break;	
		case NOT_APPLICABLE_ENQUIRY:
			msgKey = "notApplicableEnquiry";
			break;
		case DEFAULT:
		default:
			msgKey = "genericRegistrtionFinishTitle";
		}
		return new I18nString(msgPrefix + msgKey, msg).getValue(msg);
	}

	private String getDefaultInfo(RegistrationWrapUpConfig.TriggeringState state)
	{
		return null;
	}

	public boolean hasConfiguredFinalizationFor(TriggeringState toCheck)
	{
		return wrapUpConfigs.stream()
				.map(RegistrationWrapUpConfig::getState)
				.anyMatch(state -> state.equals(toCheck));
	}
}
