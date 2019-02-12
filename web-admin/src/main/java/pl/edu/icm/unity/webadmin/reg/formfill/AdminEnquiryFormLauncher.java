/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formfill;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.EnquiryManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.IdPLoginController;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.finalization.WorkflowFinalizationConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.registration.PostFillingHandler;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IdentityExistsException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationRequestStatus;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.webui.AsyncErrorHandler;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseChangedEvent;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseEditor;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseEditorController;
import pl.edu.icm.unity.webui.forms.reg.RegistrationRequestChangedEvent;



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
	private UnityMessageSource msg;
	private EnquiryManagement enquiryManagement;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributeTypeManagement attrsMan;
	private CredentialManagement authnMan;
	private GroupsManagement groupsMan;
	private EnquiryResponseEditorController responseController;
	
	private EventsBus bus;
	private IdPLoginController idpLoginController;
	
	@Autowired
	public AdminEnquiryFormLauncher(UnityMessageSource msg,
			EnquiryManagement enquiryManagement,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributeTypeManagement attrsMan, CredentialManagement authnMan,
			GroupsManagement groupsMan, IdPLoginController idpLoginController,
			EnquiryResponseEditorController responseController)
	{
		super();
		this.msg = msg;
		this.enquiryManagement = enquiryManagement;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
		this.groupsMan = groupsMan;
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
				bus.fireEvent(new RegistrationRequestChangedEvent(id));
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
			WebSession.getCurrent().getEventBus().fireEvent(new EnquiryResponseChangedEvent(requestId));
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
			RemotelyAuthenticatedContext remoteContext, 
			AsyncErrorHandler errorHandler)
	{
		EnquiryResponseEditor editor;
		try
		{
			editor = new EnquiryResponseEditor(msg, form, remoteContext, identityEditorRegistry,
					credentialEditorRegistry, attributeHandlerRegistry, attrsMan, authnMan,
					groupsMan, responseController.getPrefilledForSticky(form));
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
