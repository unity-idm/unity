/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.signup;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.forms.reg.RegistrationFormDialogProvider;

@Deprecated
@PrototypeComponent
public class SignUpWithAutoRegistrationRequestEditorFactory
{
	private UnityMessageSource msg;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private InvitationManagement invitationMan;
	private AttributeTypeManagement aTypeMan;
	private GroupsManagement groupsMan;
	private CredentialManagement credMan;
	private AuthenticatorSupportManagement authenticatorSupport;

	@Autowired
	public SignUpWithAutoRegistrationRequestEditorFactory(UnityMessageSource msg, 
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") InvitationManagement invitationMan, 
			@Qualifier("insecure") AttributeTypeManagement aTypeMan,
			@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") CredentialManagement credMan,
			AuthenticatorSupportManagement authenticatorSupport)
	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.invitationMan = invitationMan;
		this.aTypeMan = aTypeMan;
		this.groupsMan = groupsMan;
		this.credMan = credMan;
		this.authenticatorSupport = authenticatorSupport;
	}
	
	public SignUpWithAutoRegistrationRequestEditor create(RegistrationForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated, SignUpAuthNController signUpAuthNController) throws Exception
	{
		String registrationCode = RegistrationFormDialogProvider.getCodeFromURL();
		SignUpWithAutoRegistrationRequestEditor editor = new SignUpWithAutoRegistrationRequestEditor(msg, form, 
				remotelyAuthenticated, identityEditorRegistry, 
				credentialEditorRegistry, attributeHandlerRegistry, 
				aTypeMan, credMan, groupsMan, 
				registrationCode, invitationMan, authenticatorSupport,
				signUpAuthNController);
		return editor;
	}
}
