/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
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
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;
import pl.edu.icm.unity.webui.AsyncErrorHandler;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.registration.RequestEditorCreator.RequestEditorCreatedCallback;



/**
 * Responsible for showing a given registration form dialog. This is a no-authz variation of
 * {@link AdminRegistrationFormLauncher} intended for general use.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class InsecureRegistrationFormLauncher implements RegistrationFormDialogProvider
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
	public InsecureRegistrationFormLauncher(UnityMessageSource msg,
			@Qualifier("insecure") RegistrationsManagement registrationsManagement,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") AttributesManagement attrsMan, 
			@Qualifier("insecure") AuthenticationManagement authnMan,
			@Qualifier("insecure") GroupsManagement groupsMan,
			IdPLoginController idpLoginController)
	{
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

	protected boolean addRequest(RegistrationRequest request, RegistrationForm form, RegistrationContext context)
	{
		String id;
		try
		{
			id = registrationsManagement.submitRegistrationRequest(request, context);
			bus.fireEvent(new RegistrationRequestChangedEvent(id));
		} catch (WrongArgumentException e)
		{
			new PostRegistrationHandler(idpLoginController, form, msg, 
					registrationsManagement.getProfileInstance(form)).submissionError(e, context);
			return false;
		} catch (EngineException e)
		{
			new PostRegistrationHandler(idpLoginController, form, msg, 
					registrationsManagement.getProfileInstance(form)).submissionError(e, context);
			return true;
		}

		new PostRegistrationHandler(idpLoginController, form, msg, 
				registrationsManagement.getProfileInstance(form)).submitted(id, registrationsManagement,
				request, context);
		return true;
	}
	
	public void showRegistrationDialog(String formName, RemotelyAuthenticatedContext remoteContext, 
			TriggeringMode mode, AsyncErrorHandler errorHandler) throws EngineException
	{
		List<RegistrationForm> forms = registrationsManagement.getForms();
		for (RegistrationForm form: forms)
		{
			if (formName.equals(form.getName()))
				showRegistrationDialog(form, remoteContext, mode, errorHandler);
		}
		throw new WrongArgumentException("There is no registration form " + formName);
	}
	
	@Override
	public void showRegistrationDialog(final RegistrationForm form, 
			RemotelyAuthenticatedContext remoteContext, TriggeringMode mode,
			AsyncErrorHandler errorHandler)
	{
		RegistrationContext context = new RegistrationContext(true, 
				idpLoginController.isLoginInProgress(), mode);
		RequestEditorCreator editorCreator = new RequestEditorCreator(msg, form, remoteContext, 
				identityEditorRegistry, credentialEditorRegistry, attributeHandlerRegistry, 
				registrationsManagement, attrsMan, groupsMan, authnMan);
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
				showDialog(form, context, editor);
			}

			@Override
			public void onCancel()
			{
				//nop
			}
		});
	}
	
	private void showDialog(RegistrationForm form, RegistrationContext context, RegistrationRequestEditor editor)
	{
		RegistrationRequestEditorDialog dialog = new RegistrationRequestEditorDialog(msg, 
				msg.getMessage("RegistrationFormsChooserComponent.dialogCaption"), 
				editor, new RegistrationRequestEditorDialog.Callback()
				{
					@Override
					public boolean newRequest(RegistrationRequest request)
					{
						return addRequest(request, form, context);
					}
						@Override
					public void cancelled()
					{
						new PostRegistrationHandler(idpLoginController, form, msg, 
								registrationsManagement.getProfileInstance(form)).
							cancelled(false, context);
					}
				});
		dialog.show();
	}
}
