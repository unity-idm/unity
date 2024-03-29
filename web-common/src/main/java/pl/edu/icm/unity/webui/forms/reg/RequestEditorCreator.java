/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.Optional;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import com.vaadin.server.Page;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.invite.EnquiryInvitationParam;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.policyAgreement.PolicyAgreementRepresentationBuilder;
import pl.edu.icm.unity.webui.forms.InvitationResolver;
import pl.edu.icm.unity.webui.forms.RegCodeException;
import pl.edu.icm.unity.webui.forms.RegCodeException.ErrorCause;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;
import pl.edu.icm.unity.webui.forms.URLQueryPrefillCreator;

/**
 * Creates instances of {@link RegistrationRequestEditor}. May ask for a registration/invitation code if needed first
 * and handles loading of related invitation if needed.
 * 
 * @author Krzysztof Benedyczak
 */
@PrototypeComponent
public class RequestEditorCreator
{
	private final MessageSource msg;
	private final ImageAccessService imageAccessService;
	private final IdentityEditorRegistry identityEditorRegistry;
	private final CredentialEditorRegistry credentialEditorRegistry;
	private final AttributeHandlerRegistry attributeHandlerRegistry;
	private final AttributeTypeManagement aTypeMan;
	private final GroupsManagement groupsMan;
	private final CredentialManagement credMan;
	private final AuthenticatorSupportService authnSupport;
	private final InvitationResolver invitationResolver;
	private final URLQueryPrefillCreator urlQueryPrefillCreator;
	private final PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder;
	private final PublicRegistrationURLSupport publicRegistrationURLSupport;
	private final SwitchToEnquiryComponentProvider toEnquirySwitchLabelProvider;
	
	private RegistrationForm form;
	private RemotelyAuthenticatedPrincipal remotelyAuthenticated;
	private String registrationCode;
	private boolean enableRemoteSignup;
	private AuthenticationOptionKey authenticationOptionKey;

	@Autowired
	public RequestEditorCreator(MessageSource msg, ImageAccessService imageAccessService,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			@Qualifier("insecure") AttributeTypeManagement aTypeMan,
			@Qualifier("insecure") GroupsManagement groupsMan, 
			@Qualifier("insecure") CredentialManagement credMan,
			AuthenticatorSupportService authnSupport,
			URLQueryPrefillCreator urlQueryPrefillCreator,
			PolicyAgreementRepresentationBuilder policyAgreementsRepresentationBuilder,
			PublicRegistrationURLSupport publicRegistrationURLSupport,
			InvitationResolver invitationResolver,
			SwitchToEnquiryComponentProvider toEnquirySwitchLabelProvider
			)
	{
		this.msg = msg;
		this.identityEditorRegistry = identityEditorRegistry;
		this.credentialEditorRegistry = credentialEditorRegistry;
		this.attributeHandlerRegistry = attributeHandlerRegistry;
		this.aTypeMan = aTypeMan;
		this.groupsMan = groupsMan;
		this.credMan = credMan;
		this.urlQueryPrefillCreator = urlQueryPrefillCreator;
		this.invitationResolver = invitationResolver;
		this.authnSupport = authnSupport;
		this.imageAccessService = imageAccessService;
		this.policyAgreementsRepresentationBuilder = policyAgreementsRepresentationBuilder;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
		this.toEnquirySwitchLabelProvider = toEnquirySwitchLabelProvider;
	}
	

	public RequestEditorCreator init(RegistrationForm form, boolean enableRemoteSignup,
			RemotelyAuthenticatedPrincipal context, String presetRegistrationCode,
			AuthenticationOptionKey authenticationOptionKey)
	{
		this.form = form;
		this.enableRemoteSignup = enableRemoteSignup;
		this.remotelyAuthenticated = context;
		this.registrationCode = presetRegistrationCode;
		this.authenticationOptionKey = authenticationOptionKey;
		return this;
	}
	
	public RequestEditorCreator init(RegistrationForm form, RemotelyAuthenticatedPrincipal context,
			AuthenticationOptionKey authenticationOptionKey)
	{
		return init(form, false, context, null, authenticationOptionKey);
	}

	public void createFirstStage(RequestEditorCreatedCallback callback, InvitationCodeConsumer onLocalSignupHandler)
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

	private void doCreateFirstStage(RequestEditorCreatedCallback callback, InvitationCodeConsumer onLocalSignupHandler)
	{
		Optional<ResolvedInvitationParam> invitation = Optional.empty();
		try
		{
			invitation = getInvitationByCode(registrationCode);
		} catch (RegCodeException e1)
		{
			callback.onCreationError(e1, e1.cause);
			return;
		}
		
		if (redirectToPublicEnquiryViewIfPossible(invitation))
		{
			return;
		}
		
		try
		{
			RegistrationRequestEditor editor = doCreateEditor(registrationCode,  invitation.orElse(null));
			editor.showFirstStage(onLocalSignupHandler);
			callback.onCreated(editor);
		} catch (AuthenticationException e)
		{
			callback.onCreationError(e, ErrorCause.MISCONFIGURED);
		}
	}

	
	private void doCreateSecondStage(RequestEditorCreatedCallback callback, boolean withCredentials)
	{
		Optional<ResolvedInvitationParam> invitation = Optional.empty();
		try
		{
			invitation = getInvitationByCode(registrationCode);
		} catch (RegCodeException e1)
		{
			callback.onCreationError(e1, e1.cause);
			return;
		}
		
		if (redirectToPublicEnquiryViewIfPossible(invitation))
		{
			return;
		}
		
		try
		{
			RegistrationRequestEditor editor = doCreateEditor(registrationCode, invitation.orElse(null));
			editor.showSecondStage(withCredentials);
			callback.onCreated(editor);
		} catch (AuthenticationException e)
		{
			callback.onCreationError(e, ErrorCause.MISCONFIGURED);
		}
	}
	
	private boolean redirectToPublicEnquiryViewIfPossible(Optional<ResolvedInvitationParam> invitation)
	{
		if (invitation.isPresent() && invitation.get().canBeProcessedAsEnquiryWithResolvedUser())
		{
			EnquiryInvitationParam enqInv = invitation.get().getAsEnquiryInvitationParamWithAnonymousEntity();
			String url = publicRegistrationURLSupport.getPublicEnquiryLink(enqInv.getFormPrefill().getFormId(),
					registrationCode);
			Page.getCurrent().open(url, null);
			return true;
		}
		return false;
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
		}, msg.getMessage("GetRegistrationCodeDialog.title"),
		msg.getMessage("GetRegistrationCodeDialog.information"),
		msg.getMessage("GetRegistrationCodeDialog.code"));
		askForCodeDialog.show();
	}

	private RegistrationRequestEditor doCreateEditor(String registrationCode, 
			ResolvedInvitationParam invitation) 
			throws AuthenticationException
	{
		return new RegistrationRequestEditor(msg, form, 
				remotelyAuthenticated, identityEditorRegistry, 
				credentialEditorRegistry, attributeHandlerRegistry, 
				aTypeMan, credMan, groupsMan, imageAccessService,
				registrationCode, invitation, authnSupport,  
				urlQueryPrefillCreator, policyAgreementsRepresentationBuilder, 
				toEnquirySwitchLabelProvider, 
				enableRemoteSignup,
				authenticationOptionKey);
	}
	
	private Optional<ResolvedInvitationParam> getInvitationByCode(String registrationCode) throws RegCodeException
	{
		ResolvedInvitationParam invitation;
		try
		{
			invitation = invitationResolver.getInvitationByCode(registrationCode);
		} catch (RegCodeException e)
		{
			if (form.isByInvitationOnly())
			{
				throw e;
			}
			else {
				return Optional.empty();
			}
		}
		
		invitation.assertMatchToForm(form);	
		return Optional.of(invitation);
	}
	
	public interface RequestEditorCreatedCallback
	{
		void onCreated(RegistrationRequestEditor editor);
		void onCreationError(Exception e, ErrorCause cause);
		void onCancel();
	}
	
	interface InvitationCodeConsumer extends Consumer<String>
	{
	}
}
