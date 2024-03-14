/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.registration;

import com.vaadin.flow.component.UI;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationSignupResolverFactory;
import io.imunity.vaadin.endpoint.common.forms.*;
import io.imunity.vaadin.endpoint.common.forms.components.GetRegistrationCodeDialog;
import io.imunity.vaadin.endpoint.common.forms.policy_agreements.PolicyAgreementRepresentationBuilder;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeHandlerRegistry;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import io.imunity.vaadin.endpoint.common.plugins.identities.IdentityEditorRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.base.registration.invitation.EnquiryInvitationParam;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedPrincipal;
import pl.edu.icm.unity.engine.api.registration.PublicRegistrationURLSupport;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;


@PrototypeComponent
public class RequestEditorCreator
{
	private final MessageSource msg;
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
	private final NotificationPresenter notificationPresenter;
	private final VaadinLogoImageLoader logoImageLoader;
	private final RemoteRegistrationSignupResolverFactory remoteRegistrationSignupResolverFactory;

	private RegistrationForm form;
	private RemotelyAuthenticatedPrincipal remotelyAuthenticated;
	private String registrationCode;
	private boolean enableRemoteSignup;
	private AuthenticationOptionKey authenticationOptionKey;
	private Map<String, List<String>> parameters;

	RequestEditorCreator(MessageSource msg,
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
	                            SwitchToEnquiryComponentProvider toEnquirySwitchLabelProvider,
	                            NotificationPresenter notificationPresenter,
	                            VaadinLogoImageLoader logoImageLoader,
	                            RemoteRegistrationSignupResolverFactory remoteRegistrationSignupResolverFactory)
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
		this.policyAgreementsRepresentationBuilder = policyAgreementsRepresentationBuilder;
		this.publicRegistrationURLSupport = publicRegistrationURLSupport;
		this.toEnquirySwitchLabelProvider = toEnquirySwitchLabelProvider;
		this.notificationPresenter = notificationPresenter;
		this.logoImageLoader = logoImageLoader;
		this.remoteRegistrationSignupResolverFactory = remoteRegistrationSignupResolverFactory;
	}


	public RequestEditorCreator init(RegistrationForm form, boolean enableRemoteSignup,
	                                 RemotelyAuthenticatedPrincipal context, String presetRegistrationCode,
	                                 AuthenticationOptionKey authenticationOptionKey,
									 Map<String, List<String>> parameters)
	{
		this.form = form;
		this.enableRemoteSignup = enableRemoteSignup;
		this.remotelyAuthenticated = context;
		this.registrationCode = presetRegistrationCode;
		this.authenticationOptionKey = authenticationOptionKey;
		this.parameters = parameters;
		return this;
	}

	public RequestEditorCreator init(RegistrationForm form, RemotelyAuthenticatedPrincipal context,
	                                 AuthenticationOptionKey authenticationOptionKey)
	{
		return init(form, false, context, null, authenticationOptionKey, parameters);
	}

	public void createFirstStage(RequestEditorCreatedCallback callback, InvitationCodeConsumer onLocalSignupHandler)
	{
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
			callback.onCreationError(e, RegCodeException.ErrorCause.MISCONFIGURED);
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
			callback.onCreationError(e, RegCodeException.ErrorCause.MISCONFIGURED);
		}
	}

	private boolean redirectToPublicEnquiryViewIfPossible(Optional<ResolvedInvitationParam> invitation)
	{
		if (invitation.isPresent() && invitation.get().canBeProcessedAsEnquiryWithResolvedUser())
		{
			EnquiryInvitationParam enqInv = invitation.get().getAsEnquiryInvitationParamWithAnonymousEntity();
			String url = publicRegistrationURLSupport.getPublicEnquiryLink(enqInv.getFormPrefill().getFormId(),
					registrationCode);
			UI.getCurrent().getPage().open(url, null);
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
		askForCodeDialog.open();
	}

	private RegistrationRequestEditor doCreateEditor(String registrationCode,
			ResolvedInvitationParam invitation)
			throws AuthenticationException
	{
		return new RegistrationRequestEditor(msg, form,
				remotelyAuthenticated, identityEditorRegistry,
				credentialEditorRegistry, attributeHandlerRegistry,
				aTypeMan, credMan, groupsMan, notificationPresenter,
				registrationCode, invitation, authnSupport,
				urlQueryPrefillCreator, policyAgreementsRepresentationBuilder,
				toEnquirySwitchLabelProvider,
				enableRemoteSignup,
				authenticationOptionKey, logoImageLoader, remoteRegistrationSignupResolverFactory, parameters);
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
		void onCreationError(Exception e, RegCodeException.ErrorCause cause);
		void onCancel();
	}

	interface InvitationCodeConsumer extends Consumer<String>
	{
	}
}
