/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import com.vaadin.server.Page;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.engine.api.registration.RegistrationRedirectURLBuilder;
import pl.edu.icm.unity.engine.api.registration.RegistrationRedirectURLBuilder.Status;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.RedirectConfig;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.forms.FinalRegistrationConfiguration;

/**
 * Controller making decisions on what to do/show after completed registration.
 * 
 * @author K. Benedyczak
 */
public class PostFillingHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, PostFillingHandler.class);
	private RegistrationForm form;
	private UnityMessageSource msg;
	private IdPLoginController loginController;
	private FormAutomationSupport formSupport;
	private RegistrationsManagement registrationsManagement;
	
	public PostFillingHandler(IdPLoginController loginController, 
			RegistrationForm form, UnityMessageSource msg, RegistrationsManagement registrationsManagement)
	{
		this.loginController = loginController;
		this.form = form;
		this.msg = msg;
		this.formSupport = registrationsManagement.getFormAutomationSupport(form);
		this.registrationsManagement = registrationsManagement;
	}

	/**
	 * Invokes proper redirection or shows an information message depending on request status and form settings.
	 */
	public Optional<FinalRegistrationConfiguration> submittedRegistrationRequest(
			String requestId, RegistrationRequest request, RegistrationContext context)
	{
		boolean autoAccepted = isRequestAutoAccepted(requestId, registrationsManagement);
		return submittedGeneric(requestId, request, context, autoAccepted);
	}

	public Optional<FinalRegistrationConfiguration> userExistsError()
	{
		RedirectConfig redirectCfg = form.getUserExistsRedirect() == null ? 
				new RedirectConfig(null, null, false) : form.getUserExistsRedirect();
		String finalRedirectURL = redirectCfg.getRedirectURL() == null ? null : 
			new RegistrationRedirectURLBuilder(redirectCfg.getRedirectURL(), 
						form.getName(), null, Status.userExists).build();
		if (redirectCfg.isAutomatic())
		{
			redirect(finalRedirectURL);
			return Optional.empty();
		}

		return Optional.of(new FinalRegistrationConfiguration( 
				msg.getMessage("StandalonePublicFormView.userExistsError"), 
				null, 
				finalRedirectURL == null ? null : () -> redirect(finalRedirectURL), 
				redirectCfg.getRedirectCaption().getValue(msg)));
	}
	
	/**
	 * Performs action appropriate after request filling cancellation. Can redirect if handler is configured so
	 * and form has redirect URL defined. Otherwise can show a cancellation message
	 */
	public Optional<FinalRegistrationConfiguration> cancelled(RegistrationContext context)
	{
		String redirectURL = formSupport.getPostCancelledRedirectURL(context);
		
		if (redirectURL != null)
		{
			String finalRedirectURL = new RegistrationRedirectURLBuilder(redirectURL, 
					form.getName(), null, Status.cancelled).build();
			redirect(finalRedirectURL);
			return Optional.empty();
		} else
		{
			log.info("Showing dead-end screen after cancelled registration. "
					+ "Either redirect URL after cancel should be defined or "
					+ "registration cancelling disabled.");
			return Optional.of(new FinalRegistrationConfiguration( 
					msg.getMessage("StandalonePublicFormView.cancel"), 
					null, 
					null, 
					null));
		}
	}
	
	public Optional<FinalRegistrationConfiguration> genericFatalError(Exception e, RegistrationContext context)
	{
		String redirectURL = formSupport.getPostCancelledRedirectURL(context);
		if (redirectURL != null)
		{
			String redirectUpdated = new RegistrationRedirectURLBuilder(redirectURL, 
					form.getName(), null, 
					Status.submittedWithError).setErrorCode(e.toString()).build();
			log.warn("Form submission finished with error, redirecting to " + 
					redirectUpdated, e);
			redirect(redirectUpdated);
			return Optional.empty();
		} else
		{
			log.warn("Form submission finished with error, showing dead-end screen ", e);
			return Optional.of(new FinalRegistrationConfiguration( 
					msg.getMessage("StandalonePublicFormView.submissionFailed"), 
					null, 
					null, 
					null));
		}
	}
	
	
	private boolean isRequestAutoAccepted(String requestId, RegistrationsManagement registrationsManagement) 
	{
		try
		{
			for (RegistrationRequestState r : registrationsManagement.getRegistrationRequests())
			{
				if (r.getRequestId().equals(requestId)
					&& r.getStatus() == RegistrationRequestStatus.accepted)
					return true;
			}
		} catch (EngineException e)
		{
			log.error("Shouldn't happen: can't get request status to check if it was auto accepted", e);
		}
		return false;
	}

	private Optional<FinalRegistrationConfiguration> submittedGeneric(String requestId, 
			BaseRegistrationInput request, RegistrationContext context, boolean autoAccepted)
	{
		I18nMessage message = getPostSubmitMessage(requestId, request, context, autoAccepted);
		RedirectConfig redirectCfg = getPostSubmitRedirectConfig(form, requestId, request, context, autoAccepted);
		if (redirectCfg.isAutomatic())
		{
			redirect(redirectCfg.getRedirectURL());
			return Optional.empty();
		}
		return Optional.of(new FinalRegistrationConfiguration( 
				message.getSubject().getValue(msg), 
				message.getBody().getValue(msg), 
				redirectCfg.getRedirectURL() == null ? 
						null : () -> redirect(redirectCfg.getRedirectURL()), 
				redirectCfg.getRedirectCaption() == null ? 
						null : redirectCfg.getRedirectCaption().getValue(msg)));
	}

	private RedirectConfig getPostSubmitRedirectConfig(RegistrationForm form, 
			String requestId, BaseRegistrationInput request, 
			RegistrationContext context, boolean autoAccepted)
	{
		String redirectURL = formSupport.getPostSubmitRedirectURL(request, context, requestId);
		if (redirectURL != null && form.getSuccessRedirect() != null && 
				!Strings.isEmpty(form.getSuccessRedirect().getRedirectURL()))
			log.warn("Post submit redirect URL for form {} is configured directly in form config ({}) "
					+ "and was obtained from its automation profile. The later will be used: {}",
					form.getName(),
					form.getSuccessRedirect().getRedirectURL(),
					redirectURL);

		if (redirectURL == null && form.getSuccessRedirect() != null)
			redirectURL = form.getSuccessRedirect().getRedirectURL();
		if (Strings.isEmpty(redirectURL))
			return new RedirectConfig(null, null, false);
		
		String finalRedirect = new RegistrationRedirectURLBuilder(redirectURL, form.getName(), requestId, 
				autoAccepted ? Status.submittedAccepted : Status.submitted).build();
		
		boolean automatic = form.getSuccessRedirect() != null ? 
				form.getSuccessRedirect().isAutomatic() : true;
		I18nString defRedirectCaption = new I18nString("RegistrationFormsChooserComponent.defaultRedirectCaption", msg);
		I18nString redirectCaption = form.getSuccessRedirect() != null ? 
				getCaptionWithDefault(form.getSuccessRedirect().getRedirectCaption(), defRedirectCaption) : 
				new I18nString(""); //not needed as if there is no config then we have automatic redirect
		return new RedirectConfig(redirectCaption, finalRedirect, automatic);
	}
	
	private I18nString getCaptionWithDefault(I18nString value, I18nString defaultValue)
	{
		return value.isEmpty() ? defaultValue : value;
	}
	
	private I18nMessage getPostSubmitMessage(String requestId, BaseRegistrationInput request, 
			RegistrationContext context, boolean autoAccepted)
	{
		I18nMessage message = formSupport.getPostSubmitMessage(request, context, requestId);
		if (message != null)
			return message;
		
		I18nString title = new I18nString(autoAccepted ? 
				"RegistrationFormsChooserComponent.requestAccepted" : 
				"RegistrationFormsChooserComponent.requestSubmitted", msg);
		
		I18nString info = new I18nString(autoAccepted ? 
				"RegistrationFormsChooserComponent.requestSubmittedInfoWithAccept" : 
				"RegistrationFormsChooserComponent.requestSubmittedInfoNoAccept", 
				msg);
		return new I18nMessage(title, info);
	}
	
	private void redirect(String redirectUrl)
	{
		loginController.breakLogin();
		Page.getCurrent().open(redirectUrl, null);
	}
}
