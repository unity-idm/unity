/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.api.internal.IdPLoginController;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.webui.AsyncErrorHandler;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.registration.RequestEditorCreator.RequestEditorCreatedCallback;



/**
 * Responsible for showing a given registration form dialog. Wrapper over {@link RegistrationRequestEditorDialog}
 * simplifying its instantiation.
 * <p> This version is intended for use in AdminUI where automatic request acceptance is possible.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class AdminRegistrationFormLauncher implements RegistrationFormDialogProvider
{
	protected UnityMessageSource msg;
	protected RegistrationsManagement registrationsManagement;
	protected IdentityEditorRegistry identityEditorRegistry;
	protected CredentialEditorRegistry credentialEditorRegistry;
	protected AttributeHandlerRegistry attributeHandlerRegistry;
	protected AttributesManagement attrsMan;
	protected AuthenticationManagement authnMan;
	protected GroupsManagement groupsMan;
	
	protected EventsBus bus;
	private IdPLoginController idpLoginController;
	
	@Autowired
	public AdminRegistrationFormLauncher(UnityMessageSource msg,
			RegistrationsManagement registrationsManagement,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, AuthenticationManagement authnMan,
			GroupsManagement groupsMan, IdPLoginController idpLoginController)
	{
		super();
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
		this.groupsMan = groupsMan;
		this.idpLoginController = idpLoginController;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	protected boolean addRequest(RegistrationRequest request, boolean andAccept, RegistrationForm form, 
			TriggeringMode mode)
	{
		RegistrationContext context = new RegistrationContext(!andAccept, 
				idpLoginController.isLoginInProgress(), mode);
		String id;
		try
		{
			id = registrationsManagement.submitRegistrationRequest(request, context);
			bus.fireEvent(new RegistrationRequestChangedEvent(id));
		} catch (EngineException e)
		{
			new PostRegistrationHandler(idpLoginController, form, msg, 
					registrationsManagement.getProfileInstance(form)).submissionError(e, context);
			return false;
		}

		try
		{							
			if (andAccept)
			{
				registrationsManagement.processRegistrationRequest(id, request, 
						RegistrationRequestAction.accept, null, 
						msg.getMessage("RegistrationFormsChooserComponent.autoAccept"));
				bus.fireEvent(new RegistrationRequestChangedEvent(id));
			}	
			new PostRegistrationHandler(idpLoginController, form, msg, 
					registrationsManagement.getProfileInstance(form), false).
				submitted(id, registrationsManagement, request, context);
			
			return true;
		} catch (EngineException e)
		{
			NotificationPopup.showError(msg, msg.getMessage(
					"RegistrationFormsChooserComponent.errorRequestAutoAccept"), e);
			return true;
		}
	}
	
	@Override
	public void showRegistrationDialog(final RegistrationForm form, 
			RemotelyAuthenticatedContext remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler)
	{
			RequestEditorCreator editorCreator = new RequestEditorCreator(msg, form, 
					remoteContext, identityEditorRegistry, credentialEditorRegistry, 
					attributeHandlerRegistry, registrationsManagement, attrsMan, groupsMan, authnMan);
			editorCreator.invoke(new RequestEditorCreatedCallback()
			{
				@Override
				public void onCreationError(Exception e)
				{
					errorHandler.onError(e);
				}
				
				@Override
				public void onCreated(RegistrationRequestEditor editor)
				{
					showDialog(form, editor, mode);
				}

				@Override
				public void onCancel()
				{
					//nop
				}
			});
	}
	
	private void showDialog(RegistrationForm form, RegistrationRequestEditor editor, TriggeringMode mode)
	{
		AdminRegistrationRequestEditorDialog dialog = new AdminRegistrationRequestEditorDialog(msg, 
				msg.getMessage("RegistrationFormsChooserComponent.dialogCaption"), 
				editor, new AdminRegistrationRequestEditorDialog.Callback()
				{
					@Override
					public boolean newRequest(RegistrationRequest request, boolean autoAccept)
					{
						return addRequest(request, autoAccept, form, mode);
					}

					@Override
					public void cancelled()
					{
						RegistrationContext context = new RegistrationContext(false, 
								idpLoginController.isLoginInProgress(), mode);
						new PostRegistrationHandler(idpLoginController, form, msg, 
								registrationsManagement.getProfileInstance(form)).
							cancelled(false, context);
					}
				});
		dialog.show();
	}
}
