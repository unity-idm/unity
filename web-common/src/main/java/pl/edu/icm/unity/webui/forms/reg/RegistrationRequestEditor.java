/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.Layout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.CredentialManagement;
import pl.edu.icm.unity.engine.api.GroupsManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedContext;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.registration.FormLayoutUtils;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.types.registration.RegistrationRequest;
import pl.edu.icm.unity.types.registration.invite.InvitationWithCode;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.identities.IdentityEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlConfigurableLabel;
import pl.edu.icm.unity.webui.common.safehtml.HtmlTag;
import pl.edu.icm.unity.webui.forms.BaseRequestEditor;
import pl.edu.icm.unity.webui.forms.RegistrationLayoutsContainer;

/**
 * Generates a UI based on a given registration form. User can fill the form and a request is returned.
 * The class verifies if the data obtained from an upstream IdP is complete wrt requirements of the form.
 * <p>
 * Objects of this class should be typically created using {@link RequestEditorCreator}, so that the
 * registration code is collected appropriately.
 * @author K. Benedyczak
 */
public class RegistrationRequestEditor extends BaseRequestEditor<RegistrationRequest>
{
	enum Stage {FIRST, SECOND}
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, RegistrationRequestEditor.class);
	private RegistrationForm form;
	
	private TextField registrationCode;
	private CaptchaComponent captcha;
	private String regCodeProvided;
	private InvitationWithCode invitation;
	private AuthenticatorSupportService authnSupport;
	private SignUpAuthNController signUpAuthNController;
	private Map<AuthenticationOptionKey, SignUpAuthNOption> signupOptions;
	private Runnable onLocalSignupHandler;
	private FormLayout effectiveLayout;
	private Stage stage;
	private RegistrationLayoutsContainer layoutContainer;

	/**
	 * Note - the two managers must be insecure, if the form is used in not-authenticated context, 
	 * what is possible for registration form.
	 */
	public RegistrationRequestEditor(UnityMessageSource msg, RegistrationForm form,
			RemotelyAuthenticatedContext remotelyAuthenticated,
			IdentityEditorRegistry identityEditorRegistry,
			CredentialEditorRegistry credentialEditorRegistry,
			AttributeHandlerRegistry attributeHandlerRegistry,
			AttributeTypeManagement aTypeMan, CredentialManagement credMan,
			GroupsManagement groupsMan, 
			String registrationCode, InvitationWithCode invitation, 
			AuthenticatorSupportService authnSupport, 
			SignUpAuthNController signUpAuthNController) throws AuthenticationException
	{
		super(msg, form, remotelyAuthenticated, identityEditorRegistry, credentialEditorRegistry, 
				attributeHandlerRegistry, aTypeMan, credMan, groupsMan);
		this.form = form;
		this.regCodeProvided = registrationCode;
		this.invitation = invitation;
		this.signUpAuthNController = signUpAuthNController;
		this.authnSupport = authnSupport;
	}
	
	public void showFirstStage(Runnable onLocalSignupHandler)
	{
		this.effectiveLayout = form.getEffectivePrimaryFormLayout(msg);
		this.onLocalSignupHandler = onLocalSignupHandler;
		this.stage = Stage.FIRST;
		initUI();
	}
	
	public void showSecondStage(boolean withCredentials)
	{
		this.effectiveLayout = withCredentials ? form.getEffectiveSecondaryFormLayout(msg) 
				: form.getEffectiveSecondaryFormLayoutWithoutCredentials(msg);
		this.stage = Stage.SECOND;
		initUI();
	}
	
	@Override
	public RegistrationRequest getRequest(boolean withCredentials) throws FormValidationException
	{
		//defensive check: if we have local signup button then submission makes no sense - 
		//we need to go to 2nd stage first. 
		if (FormLayoutUtils.hasLocalSignupButton(effectiveLayout))
			throw new FormValidationException(msg.getMessage("RegistrationRequest.continueRegistration"));
		
		RegistrationRequest ret = new RegistrationRequest();
		FormErrorStatus status = new FormErrorStatus();

		super.fillRequest(ret, status, withCredentials);
		
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
			throw new FormValidationException(status.errorMsg);
		
		return ret;
	}
	
	/**
	 * @return true if the editor can be submitted without the subsequent stage
	 */
	public boolean isSubmissionPossible()
	{
		return (stage == Stage.FIRST && !FormLayoutUtils.hasLocalSignupButton(effectiveLayout)) 
				|| stage == Stage.SECOND;
	}

	Stage getStage()
	{
		return stage;
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
	

	void focusFirst()
	{
		focusFirst(layoutContainer.registrationFormLayout);
	}
	
	private void initUI()
	{
		layoutContainer = createLayouts();
		
		resolveRemoteSignupOptions();
		
		createControls(layoutContainer, effectiveLayout, invitation);
	}
	
	@Override
	protected RegistrationLayoutsContainer createLayouts()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(false);
		main.setWidth(100, Unit.PERCENTAGE);
		setCompositionRoot(main);
		
		String logoURL = form.getLayoutSettings().getLogoURL();
		if (logoURL != null && !logoURL.isEmpty())
		{
			Resource logoResource = ImageUtils.getConfiguredImageResource(logoURL);
			Image image = new Image(null, logoResource);
			image.addStyleName("u-signup-logo");
			main.addComponent(image);
			main.setComponentAlignment(image, Alignment.TOP_CENTER);
		}
		
		I18nString title = stage == Stage.FIRST ? form.getDisplayedName() : form.getTitle2ndStage();
		Label formName = new Label(title.getValue(msg));
		formName.addStyleName(Styles.vLabelH1.toString());
		formName.addStyleName("u-reg-title");
		main.addComponent(formName);
		main.setComponentAlignment(formName, Alignment.MIDDLE_CENTER);
		
		if (stage == Stage.FIRST)
		{
			String info = form.getFormInformation() == null ? null : form.getFormInformation().getValue(msg);
			if (info != null)
			{
				HtmlConfigurableLabel formInformation = new HtmlConfigurableLabel(info);
				formInformation.addStyleName("u-reg-info");
				main.addComponent(formInformation);
				main.setComponentAlignment(formInformation, Alignment.MIDDLE_CENTER);
			}
		}
		
		RegistrationLayoutsContainer container = new RegistrationLayoutsContainer(formWidth(), formWidthUnit());
		container.addFormLayoutToRootLayout(main);
		return container;
	}
	
	private void resolveRemoteSignupOptions()
	{
		if (!form.getExternalSignupSpec().isEnabled())
			return;
		
		signupOptions = Maps.newHashMap();
		Set<String> authnOptions = form.getExternalSignupSpec().getSpecs().stream()
			.map(AuthenticationOptionKey::getAuthenticatorKey)
			.collect(Collectors.toSet());
		List<AuthenticationFlow> flows = authnSupport.resolveAuthenticationFlows(Lists.newArrayList(authnOptions),
				VaadinAuthentication.NAME);
		Set<AuthenticationOptionKey> formSignupSpec = form.getExternalSignupSpec().getSpecs().stream().collect(Collectors.toSet());
		for (AuthenticationFlow flow : flows)
		{
			for (AuthenticatorInstance authenticator : flow.getFirstFactorAuthenticators())
			{
				VaadinAuthentication vaadinAuthenticator = (VaadinAuthentication) authenticator.getRetrieval();
				String authenticatorKey = vaadinAuthenticator.getAuthenticatorId();
				Collection<VaadinAuthenticationUI> optionUIInstances = vaadinAuthenticator.createUIInstance(Context.REGISTRATION);
				for (VaadinAuthenticationUI vaadinAuthenticationUI : optionUIInstances)
				{
					String optionKey = vaadinAuthenticationUI.getId();
					AuthenticationOptionKey authnOption = new AuthenticationOptionKey(authenticatorKey, optionKey);
					if (formSignupSpec.contains(authnOption))
					{
						SignUpAuthNOption signupAuthNOption = new SignUpAuthNOption(flow, vaadinAuthenticationUI);
						setupExpectedIdentity(vaadinAuthenticationUI);
						signupOptions.put(authnOption, signupAuthNOption);
					}
				}
			}
		}
	}

	private void setupExpectedIdentity(VaadinAuthenticationUI vaadinAuthenticationUI)
	{
		if (invitation != null && invitation.getExpectedIdentity() != null)
			vaadinAuthenticationUI.setExpectedIdentity(invitation.getExpectedIdentity());
	}
	
	@Override
	protected boolean createControlFor(RegistrationLayoutsContainer layoutContainer, FormElement element, 
			FormElement previousAdded, FormElement next, InvitationWithCode invitation)
	{
		switch (element.getType())
		{
		case CAPTCHA:
			return createCaptchaControl(layoutContainer.registrationFormLayout, (BasicFormElement) element);
		case REG_CODE:
			return createRegistrationCodeControl(layoutContainer.registrationFormLayout, (BasicFormElement) element);
		case REMOTE_SIGNUP:
			return createRemoteSignupButton(layoutContainer.registrationFormLayout, (FormParameterElement) element);
		case LOCAL_SIGNUP:
			return createLocalSignupButton(layoutContainer.registrationFormLayout, (FormLocalSignupButtonElement) element);
		default:
			return super.createControlFor(layoutContainer, element, previousAdded, next, invitation);
		}
	}

	private boolean createRemoteSignupButton(AbstractOrderedLayout layout, FormParameterElement element)
	{
		int index = element.getIndex();
		AuthenticationOptionKey spec =  form.getExternalSignupSpec().getSpecs().get(index);
		SignUpAuthNOption option = signupOptions.get(spec);
		if (option == null)
		{
			log.debug("Ignoring not available remote sign up option {}", spec.toGlobalKey());
			return false;
		}
		
		Component signupOptionComponent = option.authenticatorUI.getComponent();
		signupOptionComponent.setWidth(formWidth(), formWidthUnit()); 
		layout.addComponent(signupOptionComponent);
		layout.setComponentAlignment(signupOptionComponent, Alignment.MIDDLE_CENTER);

		if (signUpAuthNController == null)
		{
			signupOptionComponent.setEnabled(false); //for some UIs (admin) we can't really trigger external authN
		} else
		{
			option.authenticatorUI.setAuthenticationCallback(signUpAuthNController.buildCallback(option));
		}
		
		return true;
	}
	
	private boolean createLocalSignupButton(AbstractOrderedLayout layout, FormLocalSignupButtonElement element)
	{
		Button localSignup = new Button(msg.getMessage("RegistrationRequest.localSignup"));
		localSignup.addStyleName("u-localSignUpButton");
		localSignup.addClickListener(event -> onLocalSignupHandler.run());
		localSignup.setWidth(formWidth(), formWidthUnit());
		layout.addComponent(localSignup);
		layout.setComponentAlignment(localSignup, Alignment.MIDDLE_CENTER);
		return true;
	}
	
	private boolean createCaptchaControl(Layout layout, BasicFormElement element)
	{
		captcha = new CaptchaComponent(msg, form.getCaptchaLength(), form.getLayoutSettings().isCompactInputs());
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
	
	RegistrationForm getForm()
	{
		return form;
	}
	
	RemotelyAuthenticatedContext getRemoteAuthnContext()
	{
		return remotelyAuthenticated;
	}
}


