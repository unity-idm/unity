/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.forms.reg;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;

import pl.edu.icm.unity.engine.api.registration.FormAutomationSupport;
import pl.edu.icm.unity.engine.api.registration.RequestSubmitStatus;
import pl.edu.icm.unity.engine.api.translation.form.TranslatedRegistrationRequest.AutomaticRequestAction;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestState;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.UserRequestState;

public class TestFormAutomationSupport extends RegistrationTestBase
{
	@Test
	public void autoProcessActionIsReturned() throws EngineException
	{
		RegistrationForm form = initAndCreateForm(true, "true");
		FormAutomationSupport automationSupport = registrationsMan.getFormAutomationSupport(form);
		RegistrationContext context = new RegistrationContext(true, TriggeringMode.manualAtLogin);

		RegistrationRequest rawRequest = getRequest();
		UserRequestState<RegistrationRequest> request = new RegistrationRequestState();
		request.setTimestamp(new Date(100));
		request.setRequestId("id");
		request.setRegistrationContext(context);
		request.setStatus(RegistrationRequestStatus.pending);
		request.setRequest(rawRequest);
		
		AutomaticRequestAction autoProcessAction = automationSupport.getAutoProcessAction(request, 
				RequestSubmitStatus.submitted);
		assertThat(autoProcessAction, is(AutomaticRequestAction.accept));
	}

	/* TODO test the rest
	@Test
	public void confirmedAttributeIsPresentInMVELContext() throws EngineException
	{
		RegistrationForm form = initAndCreateForm(true, "true");
		FormAutomationSupport automationSupport = registrationsMan.getFormAutomationSupport(form);
		RegistrationContext context = new RegistrationContext(true, true, TriggeringMode.manualAtLogin);

		RegistrationRequest rawRequest = getRequest();
		UserRequestState<RegistrationRequest> request = new RegistrationRequestState();
		request.setTimestamp(new Date(100));
		request.setRequestId("id");
		request.setRegistrationContext(context);
		request.setStatus(RegistrationRequestStatus.pending);
		request.setRequest(rawRequest);
		
		String postCancelledRedirectURL = automationSupport.getPostCancelledRedirectURL(context);
		assertThat(postCancelledRedirectURL, is(""));

		String postConfirmationRedirectURL = automationSupport.getPostConfirmationRedirectURL(request, 
				VerifiableEmailAttribute.of(InitializerCommon.EMAIL_ATTR, "/", "some@ex.com"), 
				"requestId");
		assertThat(postConfirmationRedirectURL, is(""));

		I18nMessage postSubmitMessage = automationSupport.getPostSubmitMessage(rawRequest, context, 
				"requestId");
		assertThat(postSubmitMessage, is(""));

		String postSubmitRedirectURL = automationSupport.getPostSubmitRedirectURL(rawRequest, context, 
				"requestId");
		assertThat(postSubmitRedirectURL, is(""));
	}
	*/
}
