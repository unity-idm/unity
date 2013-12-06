/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.RegistrationRequestAction;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;



/**
 * Responsible for showing a given registration form dialog. Wrapper over {@link RegistrationRequestEditorDialog}
 * simplifying its instantiation.
 * 
 * @author K. Benedyczak
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RegistrationFormLauncher
{
	protected UnityMessageSource msg;
	protected RegistrationsManagement registrationsManagement;
	protected IdentityEditorRegistry identityEditorRegistry;
	protected CredentialEditorRegistry credentialEditorRegistry;
	protected AttributeHandlerRegistry attributeHandlerRegistry;
	protected AttributesManagement attrsMan;
	protected AuthenticationManagement authnMan;

	protected boolean addAutoAccept;
	protected EventsBus bus;
	
	@Autowired
	public RegistrationFormLauncher(UnityMessageSource msg,
			RegistrationsManagement registrationsManagement,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributesManagement attrsMan, AuthenticationManagement authnMan)
	{
		super();
		this.msg = msg;
		this.registrationsManagement = registrationsManagement;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.attrsMan = attrsMan;
		this.authnMan = authnMan;
		this.bus = WebSession.getCurrent().getEventBus();
	}

	public void setAddAutoAccept(boolean addAutoAccept)
	{
		this.addAutoAccept = addAutoAccept;
	}

	protected boolean addRequest(RegistrationRequest request, boolean andAccept)
	{
		try
		{
			String id = registrationsManagement.submitRegistrationRequest(request);
			if (andAccept && addAutoAccept)
				registrationsManagement.processRegistrationRequest(id, request, 
						RegistrationRequestAction.accept, null, 
						msg.getMessage("RegistrationFormsChooserComponent.autoAccept"));
			else
				ErrorPopup.showNotice(msg, msg.getMessage("RegistrationFormsChooserComponent.requestSubmitted"), 
						msg.getMessage("RegistrationFormsChooserComponent.requestSubmittedInfo"));
			bus.fireEvent(new RegistrationRequestChangedEvent(id));
			return true;
		} catch (EngineException e)
		{
			ErrorPopup.showError(msg, msg.getMessage(
					"RegistrationFormsChooserComponent.errorRequestSubmit"), e);
			return false;
		}
	}
	
	public RegistrationRequestEditorDialog getDialog(String formName, RemotelyAuthenticatedContext remoteContext) 
			throws EngineException
	{
		List<RegistrationForm> forms = registrationsManagement.getForms();
		for (RegistrationForm form: forms)
		{
			if (formName.equals(form.getName()))
				return getDialog(form, remoteContext);
		}
		throw new WrongArgumentException("There is no registration form " + formName);
	}
	
	public RegistrationRequestEditorDialog getDialog(RegistrationForm form, 
			RemotelyAuthenticatedContext remoteContext) throws EngineException
	{
			RegistrationRequestEditor editor = new RegistrationRequestEditor(msg, form, 
					remoteContext, identityEditorRegistry, 
					credentialEditorRegistry, 
					attributeHandlerRegistry, attrsMan, authnMan);
			RegistrationRequestEditorDialog dialog = new RegistrationRequestEditorDialog(msg, 
					msg.getMessage("RegistrationFormsChooserComponent.dialogCaption"), 
					editor, addAutoAccept, new RegistrationRequestEditorDialog.Callback()
					{
						@Override
						public boolean newRequest(RegistrationRequest request, boolean autoAccept)
						{
							return addRequest(request, autoAccept);
						}
					});
			return dialog;
	}
}
