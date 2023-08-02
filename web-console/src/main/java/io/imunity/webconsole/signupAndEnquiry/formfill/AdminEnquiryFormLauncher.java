/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.signupAndEnquiry.formfill;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.IdentityExistsException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.EnquiryForm;
import pl.edu.icm.unity.base.registration.EnquiryResponse;
import pl.edu.icm.unity.base.registration.RegistrationContext;
import pl.edu.icm.unity.base.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.base.registration.RegistrationRequestAction;
import pl.edu.icm.unity.base.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.webui.AsyncErrorHandler;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseEditor;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseEditorControllerV8;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponsesChangedEvent;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestsChangedEvent;



/**
 * Responsible for showing a given enquiry form dialog. 
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AdminEnquiryFormLauncher
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, AdminEnquiryFormLauncher.class);
	private MessageSource msg;
	private EnquiryManagement enquiryManagement;
	private EnquiryResponseEditorControllerV8 responseController;
	
	private EventsBus bus;
	private IdPLoginController idpLoginController;
	
	@Autowired
	public AdminEnquiryFormLauncher(MessageSource msg,
			EnquiryManagement enquiryManagement,
			IdPLoginController idpLoginController,
			EnquiryResponseEditorControllerV8 responseController)
	{
		this.msg = msg;
		this.enquiryManagement = enquiryManagement;
		this.idpLoginController = idpLoginController;
		this.responseController = responseController;
		
		this.bus = WebSession.getCurrent().getEventBus();
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
				bus.fireEvent(new RegistrationRequestsChangedEvent());
				status = getRequestStatus(id);
			}	
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"AdminFormLauncher.errorRequestAutoAccept"), e);
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
			NotificationPopup.showError(config.mainInformation, 
					config.extraInformation == null ? "" : config.extraInformation);
		} catch (WrongArgumentException e)
		{
			throw e;
		} catch (Exception e)
		{
			log.warn("Registration request submision failed", e);
			WorkflowFinalizationConfiguration config =  getFinalizationHandler(form).getFinalRegistrationConfigurationOnError(
					TriggeringState.GENERAL_ERROR);
			NotificationPopup.showError(config.mainInformation, 
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
			AsyncErrorHandler errorHandler)
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
		AdminFormFillDialog<EnquiryResponse> dialog = new AdminFormFillDialog<>(msg, 
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
		dialog.show();
	}
}
