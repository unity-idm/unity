/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.signup_and_enquiry.formfill;

import io.imunity.console.views.signup_and_enquiry.EnquiryResponsesChangedEvent;
import io.imunity.console.views.signup_and_enquiry.RegistrationRequestsChangedEvent;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.WebSession;
import io.imunity.vaadin.endpoint.common.api.RegistrationFormDialogProvider;
import io.imunity.vaadin.enquiry.EnquiryResponseEditor;
import io.imunity.vaadin.enquiry.EnquiryResponseEditorController;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IdentityExistsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.*;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;



/**
 * Responsible for showing a given enquiry form dialog. 
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class AdminEnquiryFormLauncher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdminEnquiryFormLauncher.class);
	private final MessageSource msg;
	private final EnquiryManagement enquiryManagement;
	private final EnquiryResponseEditorController responseController;
	private final NotificationPresenter notificationPresenter;

	
	private final IdPLoginController idpLoginController;
	
	@Autowired
	public AdminEnquiryFormLauncher(MessageSource msg,
			EnquiryManagement enquiryManagement,
			IdPLoginController idpLoginController,
			EnquiryResponseEditorController responseController, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.enquiryManagement = enquiryManagement;
		this.idpLoginController = idpLoginController;
		this.responseController = responseController;
		this.notificationPresenter = notificationPresenter;
		
		
	}

	private void addRequest(EnquiryResponse response, boolean andAccept, EnquiryForm form) throws WrongArgumentException
	{
		String id = submitRequestCore(response, form);
		if (id == null)
			return;
		RegistrationRequestStatus status = getRequestStatus(id);
		
		try
		{							
			if (status == RegistrationRequestStatus.pending && andAccept)
			{
				enquiryManagement.processEnquiryResponse(id, response, 
						RegistrationRequestAction.accept, null, 
						msg.getMessage("AdminFormLauncher.autoAccept"));
				WebSession.getCurrent().getEventBus().fireEvent(new RegistrationRequestsChangedEvent());
				status = getRequestStatus(id);
			}	
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage(
					"AdminFormLauncher.errorRequestAutoAccept"), e.getMessage());
		}
	}
	
	private String submitRequestCore(EnquiryResponse response, EnquiryForm form) throws WrongArgumentException
	{
		RegistrationContext context = new RegistrationContext(
				idpLoginController.isLoginInProgress(), TriggeringMode.manualAdmin);
		try
		{
			String requestId = enquiryManagement.submitEnquiryResponse(response, context);
			WebSession.getCurrent().getEventBus().fireEvent(new EnquiryResponsesChangedEvent());
			return requestId;
		} catch (IdentityExistsException e)
		{
			WorkflowFinalizationConfiguration config = getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.PRESET_USER_EXISTS);
			notificationPresenter.showError(config.mainInformation,
					config.extraInformation == null ? "" : config.extraInformation);
		} catch (WrongArgumentException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.warn("Registration request submision failed", e);
			WorkflowFinalizationConfiguration config =  getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.GENERAL_ERROR);
			notificationPresenter.showError(config.mainInformation,
					config.extraInformation == null ? "" : config.extraInformation);
		}
		return null;
	}
	
	private RegistrationRequestStatus getRequestStatus(String requestId) 
	{
		try
		{
			return enquiryManagement.getEnquiryResponse(requestId).getStatus();
		} catch (Exception e)
		{
			log.error("Shouldn't happen: can't get request status, assuming rejected", e);
			return RegistrationRequestStatus.rejected;
		}
	}
	
	private PostFillingHandler getFinalizationHandler(EnquiryForm form)
	{
		String pageTitle = form.getPageTitle() == null ? null : form.getPageTitle().getValue(msg);
		return new PostFillingHandler(form.getName(), form.getWrapUpConfig(), msg,
				pageTitle, form.getLayoutSettings().getLogoURL(), false);
	}
	
	public void showDialog(final EnquiryForm form, 
			RemotelyAuthenticatedPrincipal remoteContext, 
			RegistrationFormDialogProvider.AsyncErrorHandler errorHandler)
	{
		EnquiryResponseEditor editor;
		try
		{
			editor = responseController.getEditorInstanceForAuthenticatedUser(form, remoteContext);
		} catch (Exception e)
		{
			errorHandler.onError(e);
			return;
		}
		AdminFormFillDialog<EnquiryResponse> dialog = new AdminFormFillDialog<>(msg, notificationPresenter,
				msg.getMessage("AdminEnquiryFormLauncher.dialogCaption"), 
				editor, new AdminFormFillDialog.Callback<EnquiryResponse>()
				{
					@Override
					public void newRequest(EnquiryResponse request, boolean autoAccept) throws WrongArgumentException
					{
						addRequest(request, autoAccept, form);
					}

					@Override
					public void cancelled()
					{
					}
				});
		dialog.open();
	}
}
