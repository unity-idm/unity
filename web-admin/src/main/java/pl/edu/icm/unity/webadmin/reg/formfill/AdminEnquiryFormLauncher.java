/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formfill;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.EnquiryManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.internal.IdPLoginController;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.EnquiryForm;
import pl.edu.icm.unity.types.registration.EnquiryResponse;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.webui.AsyncErrorHandler;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.forms.PostFormFillingHandler;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseChangedEvent;
import pl.edu.icm.unity.webui.forms.enquiry.EnquiryResponseEditor;
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
	private UnityMessageSource msg;
	private EnquiryManagement enquiryManagement;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributesManagement attrsMan;
	private AuthenticationManagement authnMan;
	private GroupsManagement groupsMan;
	
	private EventsBus bus;
	private IdPLoginController idpLoginController;
	
	@Autowired
	public AdminEnquiryFormLauncher(UnityMessageSource msg,
			EnquiryManagement enquiryManagement,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan, IdPLoginController idpLoginController)
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
		this.bus = WebSession.getCurrent().getEventBus();
	}

	protected boolean addRequest(EnquiryResponse response, boolean andAccept, EnquiryForm form, 
			TriggeringMode mode)
	{
		RegistrationContext context = new RegistrationContext(!andAccept, 
				idpLoginController.isLoginInProgress(), mode);
		String id;
		try
		{
			id = enquiryManagement.submitEnquiryResponse(response, context);
			bus.fireEvent(new EnquiryResponseChangedEvent(id));
		} catch (EngineException e)
		{
			new PostFormFillingHandler(idpLoginController, form, msg, 
					enquiryManagement.getProfileInstance(form)).submissionError(e, context);
			return false;
		}

		try
		{							
			if (andAccept)
			{
				enquiryManagement.processEnquiryResponse(id, response, 
						RegistrationRequestAction.accept, null, 
						msg.getMessage("AdminFormLauncher.autoAccept"));
				bus.fireEvent(new RegistrationRequestChangedEvent(id));
			}	
			new PostFormFillingHandler(idpLoginController, form, msg, 
					enquiryManagement.getProfileInstance(form), false).
				submittedEnquiryResponse(id, enquiryManagement, response, context);
			
			return true;
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"AdminFormLauncher.errorRequestAutoAccept"), e);
			return true;
		}
	}
	
	public void showDialog(final EnquiryForm form, 
			RemotelyAuthenticatedContext remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler)
	{
		EnquiryResponseEditor editor;
		try
		{
			editor = new EnquiryResponseEditor(msg, form, 
					remoteContext, identityEditorRegistry, 
					credentialEditorRegistry, attributeHandlerRegistry, 
					attrsMan, authnMan, groupsMan);
		} catch (Exception e)
		{
			errorHandler.onError(e);
			return;
		}
		UserFormFillDialog<EnquiryResponse> dialog = new UserFormFillDialog<>(msg, 
				msg.getMessage("AdminEnquiryFormLauncher.dialogCaption"), 
				editor, new UserFormFillDialog.Callback<EnquiryResponse>()
				{
					@Override
					public boolean newRequest(EnquiryResponse request, boolean autoAccept)
					{
						return addRequest(request, autoAccept, form, mode);
					}

					@Override
					public void cancelled()
					{
						RegistrationContext context = new RegistrationContext(false, 
								idpLoginController.isLoginInProgress(), mode);
						new PostFormFillingHandler(idpLoginController, form, msg, 
								enquiryManagement.getProfileInstance(form)).
							cancelled(false, context);
					}
				});
		dialog.show();
	}
}
