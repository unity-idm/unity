/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.registration.layout.BasicFormElement;
import pl.edu.icm.unity.types.registration.layout.FormCaptionElement;
import pl.edu.icm.unity.types.registration.layout.FormElement;
import pl.edu.icm.unity.types.registration.layout.FormLayout;
import pl.edu.icm.unity.types.registration.layout.FormLayoutElement;
import pl.edu.icm.unity.types.registration.layout.FormLocalSignupButtonElement;
import pl.edu.icm.unity.types.registration.layout.FormParameterElement;

/**
 * Configuration of a registration form. Registration form data contains:
 * <ol>
 *  <li> its identification and description,
 *  <li> visibility, which controls whether the form is publicly available for all (anonymous) clients or
 *  whether only for authorized administrators. 
 *  <li> configuration of what information is collected during registration (and in how),
 *  <li> extra information to be presented to the user
 *  <li> translation profile which can modify the data collected by the form
 * </ol>
 * <p>
 * Instances of this class can be built either from JSON or using a {@link RegistrationFormBuilder}.
 * 
 * @author K. Benedyczak
 */
public class RegistrationForm extends BaseForm
{
	public static final int MAX_CAPTCHA_LENGTH = 8;
	
	private boolean publiclyAvailable;
	private RegistrationFormNotifications notificationsConfiguration = new RegistrationFormNotifications();
	private int captchaLength;
	private String registrationCode;
	private String defaultCredentialRequirement;
	private I18nString title2ndStage = new I18nString();
	private ExternalSignupSpec externalSignupSpec = new ExternalSignupSpec();
	private ExternalSignupGridSpec externalSignupGridSpec = new ExternalSignupGridSpec();
	private RegistrationFormLayouts formLayouts = new RegistrationFormLayouts();
	private boolean showSignInLink;
	private String signInLink;
	/**
	 * @implNote: if the realm name is provided, then after the registration is
	 *            completed from standalone view, and the request meets various
	 *            criteria, newly registered user is automatically logged into
	 *            this realm.
	 */
	private String autoLoginToRealm;

	@JsonCreator
	public RegistrationForm(ObjectNode json)
	{
		super(json);
		fromJson(json);
		validateRegistration();
		validateLayouts();
	}
	
	RegistrationForm()
	{
	}
	
	public void validateLayouts()
	{
		getFormLayouts().validate(this);
	}
	
	@Override
	public RegistrationFormNotifications getNotificationsConfiguration()
	{
		return notificationsConfiguration;
	}

	void setNotificationsConfiguration(RegistrationFormNotifications notificationsConfiguration)
	{
		this.notificationsConfiguration = notificationsConfiguration;
	}

	public String getRegistrationCode()
	{
		return registrationCode;
	}

	void setRegistrationCode(String registrationCode)
	{
		this.registrationCode = registrationCode;
	}

	public boolean isPubliclyAvailable()
	{
		return publiclyAvailable;
	}

	void setPubliclyAvailable(boolean publiclyAvailable)
	{
		this.publiclyAvailable = publiclyAvailable;
	}


	public int getCaptchaLength()
	{
		return captchaLength;
	}

	void setCaptchaLength(int captchaLength)
	{
		this.captchaLength = captchaLength;
	}

	public String getDefaultCredentialRequirement()
	{
		return defaultCredentialRequirement;
	}

	public void setDefaultCredentialRequirement(String defaultCredentialRequirement)
	{
		this.defaultCredentialRequirement = defaultCredentialRequirement;
	}
	
	public ExternalSignupSpec getExternalSignupSpec()
	{
		return externalSignupSpec;
	}

	public void setExternalSignupSpec(ExternalSignupSpec externalSignupSpec)
	{
		this.externalSignupSpec = externalSignupSpec;
	}
	
	public ExternalSignupGridSpec getExternalSignupGridSpec()
	{
		return externalSignupGridSpec;
	}

	public void setExternalSignupGridSpec(ExternalSignupGridSpec externalSignupGridSpec)
	{
		this.externalSignupGridSpec = externalSignupGridSpec;
	}

	public RegistrationFormLayouts getFormLayouts()
	{
		return formLayouts;
	}

	public void setFormLayouts(RegistrationFormLayouts formLayouts)
	{
		this.formLayouts = formLayouts;
	}

	public I18nString getTitle2ndStage()
	{
		return title2ndStage;
	}

	public void setTitle2ndStage(I18nString title2ndStage)
	{
		this.title2ndStage = title2ndStage;
	}

	public boolean isShowSignInLink()
	{
		return showSignInLink;
	}

	public void setShowSignInLink(boolean showSignInLink)
	{
		this.showSignInLink = showSignInLink;
	}

	public String getSignInLink()
	{
		return signInLink;
	}

	public void setSignInLink(String signInLink)
	{
		this.signInLink = signInLink;
	}
	
	public String getAutoLoginToRealm()
	{
		return autoLoginToRealm;
	}

	public void setAutoLoginToRealm(String autoLoginToRealm)
	{
		this.autoLoginToRealm = autoLoginToRealm;
	}

	@Override
	public String toString()
	{
		return "RegistrationForm [name=" + name + "]";
	}

	protected void validateRegistration()
	{
		if (defaultCredentialRequirement == null)
			throw new IllegalStateException("Default credential requirement must be not-null "
					+ "in RegistrationForm");
	}
	

	public FormLayout getEffectiveSecondaryFormLayoutWithoutCredentials(MessageSource msg)
	{
		FormLayout layout = getEffectiveSecondaryFormLayout(msg);
		FormLayout layoutWithoutCreds = new FormLayout(layout.toJson());
		Iterator<FormElement> elementsIter = layoutWithoutCreds.getElements().iterator();
		while (elementsIter.hasNext())
		{
			FormElement element = elementsIter.next();
			if (element.getType() == FormLayoutElement.CREDENTIAL)
				elementsIter.remove();
		}
		return layoutWithoutCreds;
	}

	public FormLayout getEffectivePrimaryFormLayout(MessageSource msg)
	{
		if (getFormLayouts().getPrimaryLayout() == null)
			return getDefaultPrimaryFormLayout(msg);
		
		return getFormLayouts().getPrimaryLayout();
	}
	
	public FormLayout getEffectiveSecondaryFormLayout(MessageSource msg)
	{
		if (getFormLayouts().getSecondaryLayout() == null)
			return getDefaultSecondaryFormLayout(msg);
		
		return getFormLayouts().getSecondaryLayout();
	}
	
	public FormLayout getDefaultPrimaryFormLayout(MessageSource msg)
	{
		List<FormElement> elements = new ArrayList<>();
		if (getExternalSignupSpec().isEnabled())
		{
			List<FormElement> externalSignUpElements = getDefaultExternalSignupFormLayoutElements(msg);
			elements.addAll(externalSignUpElements);
		}
		
		if (isLocalSignupEnabled())
		{
			if (getFormLayouts().isLocalSignupEmbeddedAsButton())
			{
				elements.add(new FormLocalSignupButtonElement());
			} else
			{
				List<FormElement> defaultElements = FormLayoutUtils.getDefaultFormLayoutElements(this, msg);
				addRegistrationFormSpecificElements(msg, defaultElements);
				if (!defaultElements.isEmpty() && getExternalSignupSpec().isEnabled())
					elements.add(new FormCaptionElement(new I18nString("RegistrationRequest.or", msg)));
				elements.addAll(defaultElements);
			}
		}
		return new FormLayout(elements);
	}
	
	private List<FormElement> getDefaultExternalSignupFormLayoutElements(MessageSource msg)
	{

		List<AuthenticationOptionKey> remoteSignupGrid = getExternalSignupGridSpec().getSpecs();
		List<AuthenticationOptionKey> remoteSignup = getExternalSignupSpec().getSpecs();

		List<FormElement> ret = new ArrayList<>();
		for (int i = 0; i < remoteSignup.size(); i++)
		{
			if (!remoteSignupGrid.contains(remoteSignup.get(i)))
			{
				ret.add(new FormParameterElement(FormLayoutElement.REMOTE_SIGNUP, i));
			}
		}
		
		if (!remoteSignupGrid.isEmpty())
		{
			ret.add(new FormParameterElement(FormLayoutElement.REMOTE_SIGNUP_GRID, 0));
		}

		return ret;
	}

	public FormLayout getDefaultSecondaryFormLayout(MessageSource msg)
	{
		List<FormElement> elements;
		if (!isCredentialAvailableAtSecondaryFormLayout(this))
		{
			elements = FormLayoutUtils.getDefaultFormLayoutElementsWithoutCredentials(this, msg);
		} else
		{
			elements = FormLayoutUtils.getDefaultFormLayoutElements(this, msg);
		}
		addRegistrationFormSpecificElements(msg, elements);
		return new FormLayout(elements);
	}
	
	public static boolean isCredentialAvailableAtSecondaryFormLayout(RegistrationForm form)
	{
		return form.getExternalSignupSpec().isEnabled() && form.getFormLayouts().isLocalSignupEmbeddedAsButton();
	}
	
	/**
	 * Adds on the beginning the registration code if exists and capta at the
	 * end if defined.
	 */
	private void addRegistrationFormSpecificElements(MessageSource msg, List<FormElement> elements)
	{
		if (registrationCode != null)
			elements.add(0, new BasicFormElement(FormLayoutElement.REG_CODE));
		
		if (captchaLength > 0)
			elements.add(new BasicFormElement(FormLayoutElement.CAPTCHA));
	}
	
	@Override
	@JsonValue
	public ObjectNode toJson()
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		ObjectNode root = super.toJson();
		root.put("DefaultCredentialRequirement", getDefaultCredentialRequirement());
		root.set("NotificationsConfiguration", jsonMapper.valueToTree(getNotificationsConfiguration()));
		root.put("PubliclyAvailable", isPubliclyAvailable());
		root.put("RegistrationCode", getRegistrationCode());
		root.put("CaptchaLength", getCaptchaLength());
		root.set("ExternalSignupSpec", jsonMapper.valueToTree(getExternalSignupSpec()));
		root.set("ExternalSignupGridSpec", jsonMapper.valueToTree(getExternalSignupGridSpec()));
		root.set("RegistrationFormLayouts", jsonMapper.valueToTree(getFormLayouts()));
		root.set("Title2ndStage", I18nStringJsonUtil.toJson(title2ndStage));
		root.put("ShowSignInLink", showSignInLink);
		root.put("SignInLink", signInLink);
		root.put("AutoLoginToRealm", autoLoginToRealm);
		return root;
	}

	private void fromJson(ObjectNode root)
	{
		ObjectMapper jsonMapper = Constants.MAPPER;
		try
		{
			JsonNode n = root.get("DefaultCredentialRequirement");
			setDefaultCredentialRequirement(n == null ? null : n.asText());
			
			n = root.get("NotificationsConfiguration");
			if (n != null)
				setNotificationsConfiguration(jsonMapper.convertValue(
						n, new TypeReference<RegistrationFormNotifications>(){}));

			n = root.get("PubliclyAvailable");
			setPubliclyAvailable(n.asBoolean());
			n = root.get("RegistrationCode");
			setRegistrationCode((n == null || n.isNull()) ? null : n.asText());
			
			if (root.has("CaptchaLength"))
			{
				n = root.get("CaptchaLength");
				setCaptchaLength(n.asInt());
			} else
			{
				setCaptchaLength(0);
			}

			n = root.get("ExternalSignupSpec");
			if (n != null)
				setExternalSignupSpec(jsonMapper.convertValue(n, new TypeReference<ExternalSignupSpec>(){}));
			
			n = root.get("ExternalSignupGridSpec");
			if (n != null)
				setExternalSignupGridSpec(jsonMapper.convertValue(n, new TypeReference<ExternalSignupGridSpec>(){}));
				
			n = root.get("RegistrationFormLayouts");
			if (n != null)
				setFormLayouts(jsonMapper.convertValue(n, new TypeReference<RegistrationFormLayouts>(){}));
			
			n = root.get("Title2ndStage");
			if (n != null && !n.isNull())
				setTitle2ndStage(I18nStringJsonUtil.fromJson(n));
			
			n = root.get("ShowSignInLink");
			if (n != null && !n.isNull())
				setShowSignInLink(n.asBoolean());
			
			n = root.get("SignInLink");
			if (n != null && !n.isNull())
				setSignInLink(n.asText());
			
			n = root.get("AutoLoginToRealm");
			if (n != null && !n.isNull())
				setAutoLoginToRealm(n.asText());
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize registration form from JSON", e);
		}
	}

	@Override
	public boolean equals(final Object other)
	{
		if (!(other instanceof RegistrationForm))
			return false;
		if (!super.equals(other))
			return false;
		RegistrationForm castOther = (RegistrationForm) other;
		return Objects.equals(name, castOther.name) && Objects.equals(description, castOther.description)
				&& Objects.equals(publiclyAvailable, castOther.publiclyAvailable)
				&& Objects.equals(notificationsConfiguration, castOther.notificationsConfiguration)
				&& Objects.equals(captchaLength, castOther.captchaLength)
				&& Objects.equals(registrationCode, castOther.registrationCode)
				&& Objects.equals(defaultCredentialRequirement, castOther.defaultCredentialRequirement)
				&& Objects.equals(title2ndStage, castOther.title2ndStage)
				&& Objects.equals(externalSignupSpec, castOther.externalSignupSpec)
				&& Objects.equals(formLayouts, castOther.formLayouts)
				&& Objects.equals(showSignInLink, castOther.showSignInLink)
				&& Objects.equals(autoLoginToRealm, castOther.autoLoginToRealm)
				&& Objects.equals(signInLink, castOther.signInLink);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(super.hashCode(), name, description, publiclyAvailable, notificationsConfiguration,
				captchaLength, registrationCode, defaultCredentialRequirement,
				title2ndStage, externalSignupSpec, formLayouts, showSignInLink, signInLink, 
				autoLoginToRealm);
	}
}
