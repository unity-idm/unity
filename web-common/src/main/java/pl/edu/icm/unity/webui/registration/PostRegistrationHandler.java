/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.api.registration.RegistrationRedirectURLBuilder;
import pl.edu.icm.unity.server.api.registration.RegistrationRedirectURLBuilder.Status;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import com.vaadin.server.Page;

/**
 * Performs UI-related actions after registration request submission or form cancellation. 
 * @author K. Benedyczak
 */
public class PostRegistrationHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, PostRegistrationHandler.class);
	private RegistrationForm form;
	private UnityMessageSource msg;
	private boolean doRedirect;
	
	public PostRegistrationHandler(RegistrationForm form, UnityMessageSource msg)
	{
		this(form, msg, true);
	}

	public PostRegistrationHandler(RegistrationForm form, UnityMessageSource msg, boolean doRedirect)
	{
		this.form = form;
		this.msg = msg;
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
	public void submitted(String requestId, RegistrationsManagement registrationsManagement)
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

		String redirect = form.getRedirectAfterSubmit();
		if (redirect != null)
		{
			String finalRedirect = new RegistrationRedirectURLBuilder(form, requestId, 
					autoAccepted ? Status.submittedAccepted : Status.submitted).build();
			redirectOrInform(finalRedirect);
		} else
		{
			if (autoAccepted)
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

	
	/**
	 * Performs action appropriate after request filling cancellation. Can redirect if handler is configured so
	 * and form has redirect URL defined. Otherwise can show a cancellation message or not. 
	 * @param showCancelMessage used when there is no redirect URL and defines if popup info should be shown.
	 */
	public void cancelled(boolean showCancelMessage)
	{
		if (form.getRedirectAfterSubmit() != null)
		{
			String redirect = new RegistrationRedirectURLBuilder(form, null, Status.cancelled).build();
			redirectOrInform(redirect);
		} else
		{
			if (showCancelMessage)
				NotificationPopup.showNotice(msg, msg.getMessage("notice"), 
					msg.getMessage("StandalonePublicFormView.cancel"));			
		}
	}
	
	public void submissionError(Exception e)
	{
		if (e instanceof FormValidationException || e instanceof WrongArgumentException)
		{
			NotificationPopup.showError(msg, msg.getMessage("Generic.formError"), e);
		} else
		{
			if (form.getRedirectAfterSubmit() != null)
			{
				String redirect = new RegistrationRedirectURLBuilder(form, null, 
						Status.submittedWithError).setErrorCode(e.toString()).toString();
				log.warn("Form submission finished with error", e);
				redirectOrInform(redirect);
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
			Page.getCurrent().open(redirectUrl, null);
		else
			NotificationPopup.showNotice(msg,
					msg.getMessage("RegistrationFormsChooserComponent.requestProcessed"), 
					msg.getMessage("RegistrationFormsChooserComponent.redirectInfo", redirectUrl));
	}
}
