/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;

/**
 * Creates instances of {@link RegistrationRequestEditor}. May ask for a registration/invitation code if needed first
 * and handles loading of related invitation if needed.
 * 
 * @author Krzysztof Benedyczak
 */
@PrototypeComponent
public class RequestEditorCreator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RequestEditorCreator.class);
	private UnityMessageSource msg;
	private RegistrationForm form;
	private RemotelyAuthenticatedContext remotelyAuthenticated;
	private IdentityEditorRegistry identityEditorRegistry;
	private CredentialEditorRegistry credentialEditorRegistry;
	private AttributeHandlerRegistry attributeHandlerRegistry;
	private AttributeTypeManagement aTypeMan;
	private GroupsManagement groupsMan;
	private CredentialManagement credMan;
	private SignUpAuthNController signUpAuthNController;
	private AuthenticatorSupportService authnSupport;
	private String registrationCode;
	private InvitationManagement invitationMan;

	@Autowired
	public RequestEditorCreator(UnityMessageSource msg, 
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") AttributeTypeManagement aTypeMan,
			@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") CredentialManagement credMan,
			@Qualifier("insecure") InvitationManagement invitationMan,
			AuthenticatorSupportService authnSupport)
	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.aTypeMan = aTypeMan;
		this.groupsMan = groupsMan;
		this.credMan = credMan;
		this.invitationMan = invitationMan;
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
		InvitationWithCode invitation;
		try
		{
			invitation = getInvitationByCode(registrationCode);
		} catch (RegCodeException e1)
		{
			callback.onCreationError(e1, e1.cause);
			return;
		}
		
		try
		{
			RegistrationRequestEditor editor = doCreateEditor(registrationCode, invitation);
			editor.showFirstStage(onLocalSignupHandler);
			callback.onCreated(editor);
		} catch (AuthenticationException e)
		{
			callback.onCreationError(e, ErrorCause.MISCONFIGURED);
		}
	}

	
	private void doCreateSecondStage(RequestEditorCreatedCallback callback, boolean withCredentials)
	{
		InvitationWithCode invitation;
		try
		{
			invitation = getInvitationByCode(registrationCode);
		} catch (RegCodeException e1)
		{
			callback.onCreationError(e1, e1.cause);
			return;
		}
		try
		{
			RegistrationRequestEditor editor = doCreateEditor(registrationCode, invitation);
			editor.showSecondStage(withCredentials);
			callback.onCreated(editor);
		} catch (AuthenticationException e)
		{
			callback.onCreationError(e, ErrorCause.MISCONFIGURED);
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

	private RegistrationRequestEditor doCreateEditor(String registrationCode, 
			InvitationWithCode invitation) 
			throws AuthenticationException
	{
		return new RegistrationRequestEditor(msg, form, 
				remotelyAuthenticated, identityEditorRegistry, 
				credentialEditorRegistry, attributeHandlerRegistry, 
				aTypeMan, credMan, groupsMan, 
				registrationCode, invitation, authnSupport, signUpAuthNController);
	}
	
	private InvitationWithCode getInvitationByCode(String registrationCode) throws RegCodeException
	{
		if (form.isByInvitationOnly() && registrationCode == null)
			throw new RegCodeException(ErrorCause.MISSING_CODE);

		if (registrationCode == null)
			return null;
		
		InvitationWithCode invitation = getInvitationInternal(registrationCode);

		if (invitation != null && !invitation.getFormId().equals(form.getName()))
			throw new RegCodeException(ErrorCause.INVITATION_OF_OTHER_FORM);
		if (form.isByInvitationOnly() &&  invitation == null)
			throw new RegCodeException(ErrorCause.UNRESOLVED_INVITATION);
		if (form.isByInvitationOnly() &&  invitation.isExpired())
			throw new RegCodeException(ErrorCause.EXPIRED_INVITATION);
		return invitation;
	}
	
	private InvitationWithCode getInvitationInternal(String code)
	{
		try
		{
			return invitationMan.getInvitation(code);
		} catch (IllegalArgumentException e)
		{
			//ok
			return null;
		} catch (EngineException e)
		{
			log.warn("Error trying to check invitation with user provided code", e);
			return null;
		}
	}
	
	
	public enum ErrorCause 
	{
		MISSING_CODE(TriggeringState.GENERAL_ERROR), 
		INVITATION_OF_OTHER_FORM(TriggeringState.GENERAL_ERROR), 
		UNRESOLVED_INVITATION(TriggeringState.INVITATION_MISSING), 
		EXPIRED_INVITATION(TriggeringState.INVITATION_EXPIRED), 
		MISCONFIGURED(TriggeringState.GENERAL_ERROR);
		
		TriggeringState triggerState;

		private ErrorCause(TriggeringState triggerState)
		{
			this.triggerState = triggerState;
		}

		public TriggeringState getTriggerState()
		{
			return triggerState;
		}
	}
	public interface RequestEditorCreatedCallback
	{
		void onCreated(RegistrationRequestEditor editor);
		void onCreationError(Exception e, ErrorCause cause);
		void onCancel();
	}
	
	private static class RegCodeException extends Exception
	{
		final ErrorCause cause;

		public RegCodeException(ErrorCause cause)
		{
			this.cause = cause;
		}
	}
}
