/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.api.internal.IdPLoginController;
import pl.edu.icm.unity.server.api.registration.RegistrationRedirectURLBuilder;
import pl.edu.icm.unity.server.api.registration.RegistrationRedirectURLBuilder.Status;
import pl.edu.icm.unity.server.translation.form.BaseRegistrationTranslationProfile;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.registration.BaseForm;
import pl.edu.icm.unity.types.registration.BaseRegistrationInput;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.EnquiryResponseState;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.server.Page;

/**
 * Performs UI-related actions after registration request/enquiry response submission or cancellation.
 *  
 * @author K. Benedyczak
 */
public class PostFormFillingHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, PostFormFillingHandler.class);
	private BaseForm form;
	private UnityMessageSource msg;
	private boolean doRedirect;
	private IdPLoginController loginController;
	private BaseRegistrationTranslationProfile translationProfile;
	
	public PostFormFillingHandler(IdPLoginController loginController, 
			BaseForm form, UnityMessageSource msg, BaseRegistrationTranslationProfile profile)
	{
		this(loginController, form, msg, profile, true);
	}

	public PostFormFillingHandler(IdPLoginController loginController, 
			BaseForm form, UnityMessageSource msg, BaseRegistrationTranslationProfile profile, 
			boolean doRedirect)
	{
		this.loginController = loginController;
		this.form = form;
		this.msg = msg;
		this.translationProfile = profile;
		this.doRedirect = doRedirect;
	}

	/**
	 * Invokes proper redirection or shows an information message depending on request status and form settings.
	 * @param form
	 * @param requestId
	 * @param msg
	 * @param registrationsManagement
	 * @throws EngineException
	 */
	public void submittedRegistrationRequest(String requestId, RegistrationsManagement registrationsManagement, 
			RegistrationRequest request, RegistrationContext context)
	{
		boolean autoAccepted;
		try
		{
			autoAccepted = isRequestAutoAccepted(requestId, registrationsManagement);
		} catch (EngineException e)
		{
			log.error("Shouldn't happen: can't get request status to check if it was auto accepted", e);
			autoAccepted = false;
		}
		submittedGeneric(requestId, request, context, autoAccepted);
	}
	
	private boolean isRequestAutoAccepted(String requestId, RegistrationsManagement registrationsManagement) 
			throws EngineException
	{
		for (RegistrationRequestState r : registrationsManagement.getRegistrationRequests())
		{
			if (r.getRequestId().equals(requestId)
					&& r.getStatus() == RegistrationRequestStatus.accepted)
				return true;
		}
		return false;
	}

	private boolean isResponseAutoAccepted(String requestId, EnquiryManagement registrationsManagement) 
			throws EngineException
	{
		for (EnquiryResponseState r : registrationsManagement.getEnquiryResponses())
		{
			if (r.getRequestId().equals(requestId)
					&& r.getStatus() == RegistrationRequestStatus.accepted)
				return true;
		}
		return false;
	}

	/**
	 * Invokes proper redirection or shows an information message depending on response status and form settings.
	 */
	public void submittedEnquiryResponse(String requestId, EnquiryManagement enquiryManagement, 
			EnquiryResponse request, RegistrationContext context)
	{
		boolean autoAccepted;
		try
		{
			autoAccepted = isResponseAutoAccepted(requestId, enquiryManagement);
		} catch (EngineException e)
		{
			log.error("Shouldn't happen: can't get request status to check if it was auto accepted", e);
			autoAccepted = false;
		}
		submittedGeneric(requestId, request, context, autoAccepted);
	}

	private void submittedGeneric(String requestId, BaseRegistrationInput request, 
			RegistrationContext context, boolean autoAccepted)
	{
		String redirect = translationProfile.getPostSubmitRedirectURL(form, request, context, requestId);
		I18nMessage message = translationProfile.getPostSubmitMessage(form, request, context, requestId);
		if (redirect != null)
		{
			String finalRedirect = new RegistrationRedirectURLBuilder(redirect, form.getName(), requestId, 
					autoAccepted ? Status.submittedAccepted : Status.submitted).build();
			redirectOrInform(finalRedirect);
		} else
		{
			if (message != null)
			{
				NotificationPopup.showNotice(msg,
						message.getSubject().getValue(msg),
						message.getBody().getValue(msg));
			} else if (autoAccepted)
			{
				NotificationPopup.showNotice(msg,
						msg.getMessage("RegistrationFormsChooserComponent.requestSubmitted"),
						msg.getMessage("RegistrationFormsChooserComponent.requestSubmittedInfoWithAccept"));
	
			} else
			{
				NotificationPopup.showNotice(msg, msg.getMessage("RegistrationFormsChooserComponent.requestSubmitted"),
						msg.getMessage("RegistrationFormsChooserComponent.requestSubmittedInfoNoAccept"));
			}
		}
	}

	
	/**
	 * Performs action appropriate after request filling cancellation. Can redirect if handler is configured so
	 * and form has redirect URL defined. Otherwise can show a cancellation message or not. 
	 * @param showCancelMessage used when there is no redirect URL and defines if popup info should be shown.
	 */
	public void cancelled(boolean showCancelMessage, RegistrationContext context)
	{
		String redirect = translationProfile.getPostCancelledRedirectURL(form, context);
		if (redirect != null)
		{
			String redirectUpdated = new RegistrationRedirectURLBuilder(redirect, form.getName(), 
					null, Status.cancelled).build();
			redirectOrInform(redirectUpdated);
		} else
		{
			if (showCancelMessage)
				NotificationPopup.showNotice(msg, msg.getMessage("notice"), 
					msg.getMessage("StandalonePublicFormView.cancel"));			
		}
	}
	
	public void submissionError(Exception e, RegistrationContext context)
	{
		if (e instanceof FormValidationException || e instanceof WrongArgumentException)
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
		} else
		{
			String redirect = translationProfile.getPostCancelledRedirectURL(form, context);
			if (redirect != null)
			{
				String redirectUpdated = new RegistrationRedirectURLBuilder(redirect, 
						form.getName(), null, 
						Status.submittedWithError).setErrorCode(e.toString()).build();
				log.warn("Form submission finished with error, redirecting to " + 
						redirectUpdated, e);
				redirectOrInform(redirectUpdated);
			} else
			{
				NotificationPopup.showError(msg,
					msg.getMessage("RegistrationFormsChooserComponent.errorRequestSubmit"), e);
			}
		}
		
	}
	
	private void redirectOrInform(String redirectUrl)
	{
		if (doRedirect)
		{
			loginController.breakLogin();
			Page.getCurrent().open(redirectUrl, null);
		} else
		{
			NotificationPopup.showNotice(msg,
					msg.getMessage("RegistrationFormsChooserComponent.requestProcessed"), 
					msg.getMessage("RegistrationFormsChooserComponent.redirectInfo", redirectUrl));
		}
	}
}
