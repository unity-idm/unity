/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.registration;

import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.AuthenticationManagement;
import pl.edu.icm.unity.server.api.GroupsManagement;
import pl.edu.icm.unity.server.api.RegistrationsManagement;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Creates instances of {@link RegistrationRequestEditor}. May ask for a registration code if needed first.
 * @author Krzysztof Benedyczak
 */
public class RequestEditorCreator
{
	private UnityMessageSource msg;
	private RegistrationForm form;
	private RemotelyAuthenticatedContext remotelyAuthenticated;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private RegistrationsManagement registrationsMan;
	private AttributesManagement attrsMan;
	private GroupsManagement groupsMan;
	private AuthenticationManagement authnMan;

	public RequestEditorCreator(UnityMessageSource msg, RegistrationForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			RegistrationsManagement registrationsMan, AttributesManagement attrsMan,
			GroupsManagement groupsMan, AuthenticationManagement authnMan)
	{
		super();
		this.msg = msg;
		this.form = form;
		this.remotelyAuthenticated = remotelyAuthenticated;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.registrationsMan = registrationsMan;
		this.attrsMan = attrsMan;
		this.groupsMan = groupsMan;
		this.authnMan = authnMan;
	}
	
	public void invoke(RequestEditorCreatedCallback callback)
	{
		String registrationCode = RegistrationFormDialogProvider.getCodeFromURL();
		if (registrationCode == null && form.isByInvitationOnly())
		{
			askForCode(callback);
		} else
		{
			doCreateEditor(registrationCode, callback);
		}
	}
	
	private void askForCode(RequestEditorCreatedCallback callback)
	{
		GetRegistrationCodeDialog askForCodeDialog = new GetRegistrationCodeDialog(msg, 
				new GetRegistrationCodeDialog.Callback()
		{
			@Override
			public void onCodeGiven(String code)
			{
				doCreateEditor(code, callback);
			}
			
			@Override
			public void onCancel()
			{
				callback.onCancel();
			}
		});
		askForCodeDialog.show();
	}

	private void doCreateEditor(String registrationCode, RequestEditorCreatedCallback callback)
	{
		try
		{
			RegistrationRequestEditor editor = new RegistrationRequestEditor(msg, form, 
					remotelyAuthenticated, identityEditorRegistry, 
					credentialEditorRegistry, attributeHandlerRegistry, 
					attrsMan, authnMan, groupsMan, 
					registrationsMan, registrationCode);
			callback.onCreated(editor);
		} catch (Exception e)
		{
			callback.onCreationError(e);
		}
	}
	
	public interface RequestEditorCreatedCallback
	{
		void onCreated(RegistrationRequestEditor editor);
		void onCreationError(Exception e);
		void onCancel();
	}
}
