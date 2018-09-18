/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Creates instances of {@link RegistrationRequestEditor}. May ask for a registration code if needed first.
 * @author Krzysztof Benedyczak
 */
@PrototypeComponent
public class RequestEditorCreator
{
	private UnityMessageSource msg;
	private RegistrationForm form;
	private RemotelyAuthenticatedContext remotelyAuthenticated;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private InvitationManagement invitationMan;
	private AttributeTypeManagement aTypeMan;
	private GroupsManagement groupsMan;
	private CredentialManagement credMan;
	private SignUpAuthNController signUpAuthNController;
	private AuthenticatorSupportManagement authnSupport;
	private String registrationCode;

	@Autowired
	public RequestEditorCreator(UnityMessageSource msg, 
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") InvitationManagement invitationMan, 
			@Qualifier("insecure") AttributeTypeManagement aTypeMan,
			@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") CredentialManagement credMan,
			AuthenticatorSupportManagement authnSupport)
	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.invitationMan = invitationMan;
		this.aTypeMan = aTypeMan;
		this.groupsMan = groupsMan;
		this.credMan = credMan;
		this.authnSupport = authnSupport;
	}
	

	public RequestEditorCreator init(RegistrationForm form, SignUpAuthNController signUpAuthNController,
			RemotelyAuthenticatedContext context)
	{
		this.form = form;
		this.remotelyAuthenticated = context;
		this.signUpAuthNController = signUpAuthNController;
		return this;
	}
	
	public RequestEditorCreator init(RegistrationForm form, RemotelyAuthenticatedContext context)
	{
		return init(form, null, context);
	}

	public void createFirstStage(RequestEditorCreatedCallback callback, Runnable onLocalSignupHandler)
	{
		if (registrationCode == null)
			registrationCode = RegistrationFormDialogProvider.getCodeFromURL();
		
		if (registrationCode == null && form.isByInvitationOnly())
		{
			askForCode(callback, () -> doCreateFirstStage(callback, onLocalSignupHandler));
		} else
		{
			doCreateFirstStage(callback, onLocalSignupHandler);
		}
	}
	
	public void createSecondStage(RequestEditorCreatedCallback callback, boolean withCredentials)
	{
		if (registrationCode == null && form.isByInvitationOnly())
		{
			askForCode(callback, () -> doCreateSecondStage(callback, withCredentials));
		} else
		{
			doCreateSecondStage(callback, withCredentials);
		}
	}

	private void doCreateFirstStage(RequestEditorCreatedCallback callback, Runnable onLocalSignupHandler)
	{
		try
		{
			RegistrationRequestEditor editor = doCreateEditor(registrationCode);
			editor.showFirstStage(onLocalSignupHandler);
			callback.onCreated(editor);
		} catch (AuthenticationException e)
		{
			callback.onCreationError(e);
		}
	}

	
	private void doCreateSecondStage(RequestEditorCreatedCallback callback, boolean withCredentials)
	{
		try
		{
			RegistrationRequestEditor editor = doCreateEditor(registrationCode);
			editor.showSecondStage(withCredentials);
			callback.onCreated(editor);
		} catch (AuthenticationException e)
		{
			callback.onCreationError(e);
		}
	}
	
	private void askForCode(RequestEditorCreatedCallback callback, Runnable uiCreator)
	{
		GetRegistrationCodeDialog askForCodeDialog = new GetRegistrationCodeDialog(msg, 
				new GetRegistrationCodeDialog.Callback()
		{
			@Override
			public void onCodeGiven(String code)
			{
				registrationCode = code;
				uiCreator.run();
			}
			
			@Override
			public void onCancel()
			{
				callback.onCancel();
			}
		});
		askForCodeDialog.show();
	}

	private RegistrationRequestEditor doCreateEditor(String registrationCode) 
			throws AuthenticationException
	{
		return new RegistrationRequestEditor(msg, form, 
				remotelyAuthenticated, identityEditorRegistry, 
				credentialEditorRegistry, attributeHandlerRegistry, 
				aTypeMan, credMan, groupsMan, 
				registrationCode, invitationMan, authnSupport, signUpAuthNController);
	}
	
	public interface RequestEditorCreatedCallback
	{
		void onCreated(RegistrationRequestEditor editor);
		void onCreationError(Exception e);
		void onCancel();
	}
}
