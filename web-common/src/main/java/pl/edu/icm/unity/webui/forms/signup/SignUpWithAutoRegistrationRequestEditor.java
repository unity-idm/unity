/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.signup;

import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.InvitationManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.column.AuthenticationOptionsHandler;
import pl.edu.icm.unity.webui.authn.column.AuthenticationOptionsHandler.AuthNOption;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;
import pl.edu.icm.unity.webui.forms.reg.RequestEditorCreator;

/**
 * Generates a UI based on a given registration form. User can fill the form and a request is returned.
 * The class verifies if the data obtained from an upstream IdP is complete wrt requirements of the form.
 * <p>
 * Objects of this class should be typically created using {@link RequestEditorCreator}, so that the
 * registration code is collected appropriately.
 * @author K. Benedyczak
 */
class SignUpWithAutoRegistrationRequestEditor extends BaseRequestEditor<RegistrationRequest>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SignUpWithAutoRegistrationRequestEditor.class);
	private RegistrationForm form;
	
	private TextField registrationCode;
	private CaptchaComponent captcha;
	private String regCodeProvided;
	private InvitationWithCode invitation;
	private InvitationManagement invitationMan;
	private AuthenticatorSupportManagement authenticatorSupport;
	private SignUpAuthNController signUpAuthNCtrl;

	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 *  
	 * @param msg
	 * @param form
	 * @param remotelyAuthenticated
	 * @param identityEditorRegistry
	 * @param credentialEditorRegistry
	 * @param attributeHandlerRegistry
	 * @param aTypeMan
	 * @param authnMan
	 * @throws EngineException
	 */
	public SignUpWithAutoRegistrationRequestEditor(UnityMessageSource msg, RegistrationForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributeTypeManagement aTypeMan, CredentialManagement credMan,
			GroupsManagement groupsMan, 
			String registrationCode, InvitationManagement invitationMan,
			AuthenticatorSupportManagement authenticatorSupport,
			SignUpAuthNController signUpAuthNCtrl) throws Exception
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, aTypeMan, credMan, groupsMan);
		this.form = form;
		this.regCodeProvided = registrationCode;
		this.invitationMan = invitationMan;
		this.authenticatorSupport = authenticatorSupport;
		this.signUpAuthNCtrl = signUpAuthNCtrl;
		
		initUI();
	}
	
	@Override
	public RegistrationRequest getRequest() throws FormValidationException
	{
		RegistrationRequest ret = new RegistrationRequest();
		FormErrorStatus status = new FormErrorStatus();

		super.fillRequest(ret, status);
		
		setRequestCode(ret, status);
		if (captcha != null)
		{
			try
			{
				captcha.verify();
			} catch (WrongArgumentException e)
			{
				status.hasFormException = true;
			}
		}
		
		if (status.hasFormException)
			throw new FormValidationException();
		
		return ret;
	}
	
	private void setRequestCode(RegistrationRequest ret, FormErrorStatus status)
	{
		if (form.getRegistrationCode() != null && regCodeProvided == null)
		{
			ret.setRegistrationCode(registrationCode.getValue());
			if (registrationCode.getValue().isEmpty())
			{
				registrationCode.setComponentError(new UserError(msg.getMessage("fieldRequired")));
				status.hasFormException = true;
			} else
				registrationCode.setComponentError(null);
		}
		
		if (invitation != null)
			ret.setRegistrationCode(regCodeProvided);
	}
	
	private void initUI() throws EngineException
	{
		FormLayout mainFormLayout = createMainFormLayout();
		
		setupInvitationByCode();
		
		createRemoteAuthnControls(mainFormLayout);
		
		createControls(mainFormLayout, invitation);
	}
	
	private void createRemoteAuthnControls(FormLayout mainFormLayout) throws EngineException
	{
		if (!form.isAutoRegistrationEnabled())
			return;

		Set<String> specs = form.getAuthenticationFlows().getSpecs();
		List<AuthenticationFlow> flows = authenticatorSupport.resolveAndGetAuthenticationFlows(Lists.newArrayList(specs));
		
		AuthenticationOptionsHandler authnOptionsHandler = new AuthenticationOptionsHandler(flows, "registration");
		
		VerticalLayout authenticationMainLayout = new VerticalLayout();
		authenticationMainLayout.setMargin(false);
		// TODO: width should be configurable
		authenticationMainLayout.setWidth(VaadinEndpointProperties.DEFAULT_AUTHN_COLUMN_WIDTH, Unit.EM);
		
		for (String spec : specs)
		{
			List<AuthNOption> options = authnOptionsHandler.getMatchingAuthnOptions(spec);
			for (AuthNOption option : options)
			{
				option.authenticatorUI.setAuthenticationCallback(signUpAuthNCtrl.buildCallback(option));
				Component component = option.authenticatorUI.getComponent();
				authenticationMainLayout.addComponent(component);
				authenticationMainLayout.setComponentAlignment(component, Alignment.MIDDLE_CENTER);
			}
		}
		mainFormLayout.addComponent(authenticationMainLayout);
		mainFormLayout.setComponentAlignment(authenticationMainLayout, Alignment.MIDDLE_CENTER);
	}

	@Override
	protected boolean createControlFor(AbstractOrderedLayout layout, FormElement element, 
			FormElement previousAdded, InvitationWithCode invitation) throws EngineException
	{
		switch (element.getType())
		{
		case RegistrationForm.CAPTCHA:
			return createCaptchaControl(layout, (BasicFormElement) element);
		case RegistrationForm.REG_CODE:
			return createRegistrationCodeControl(layout, (BasicFormElement) element);
		default:
			return super.createControlFor(layout, element, previousAdded, invitation);
		}
	}
	
	private boolean createCaptchaControl(Layout layout, BasicFormElement element)
	{
		captcha = new CaptchaComponent(msg, form.getCaptchaLength());
		layout.addComponent(HtmlTag.br());
		layout.addComponent(captcha.getAsComponent());
		return true;
	}

	private boolean createRegistrationCodeControl(Layout layout, BasicFormElement element)
	{
		registrationCode = new TextField(msg.getMessage("RegistrationRequest.registrationCode"));
		registrationCode.setRequiredIndicatorVisible(true);
		layout.addComponent(registrationCode);
		return true;
	}
	
	private void setupInvitationByCode()
	{
		if (regCodeProvided != null)
			invitation = getInvitation(regCodeProvided);

		if (invitation != null && !invitation.getFormId().equals(form.getName()))
			throw new IllegalStateException(msg.getMessage("RegistrationRequest.errorCodeOfOtherForm"));
		if (form.isByInvitationOnly() && regCodeProvided == null)
			throw new IllegalStateException(msg.getMessage("RegistrationRequest.errorMissingCode"));
		if (form.isByInvitationOnly() &&  invitation == null)
			throw new IllegalStateException(msg.getMessage("RegistrationRequest.errorWrongCode"));
		if (form.isByInvitationOnly() &&  invitation.isExpired())
			throw new IllegalStateException(msg.getMessage("RegistrationRequest.errorExpiredCode"));
	}
	
	private InvitationWithCode getInvitation(String code)
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
	
	public void refresh(VaadinRequest request)
	{
		signUpAuthNCtrl.refresh(request);
	}
}


