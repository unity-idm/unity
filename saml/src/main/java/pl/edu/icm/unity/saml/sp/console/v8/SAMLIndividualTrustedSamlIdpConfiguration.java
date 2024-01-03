/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.console.v8;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.engine.api.translation.TranslationProfileGenerator;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.binding.ToggleWithDefault;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

/**
 * SAML Individual trusted idp configuration
 * 
 * @author P.Piernik
 *
 */
public class SAMLIndividualTrustedSamlIdpConfiguration
{
	private String name;
	private TranslationProfile translationProfile;
	private String id;
	private I18nString displayedName;
	private LocalOrRemoteResource logo;
	private String address;
	private Binding binding;
	private List<String> certificates;
	private String registrationForm;
	private ToggleWithDefault accountAssociation;
	private boolean signRequest;
	private List<String> requestedNameFormats;
	private String postLogoutEndpoint;
	private String postLogoutResponseEndpoint;
	private String redirectLogoutEndpoint;
	private String redirectLogoutResponseEndpoint;
	private String soapLogoutEndpoint;
	private String groupMembershipAttribute;

	public SAMLIndividualTrustedSamlIdpConfiguration()
	{
		setBinding(SAMLSPProperties.DEFAULT_IDP_BINDING);
		accountAssociation = ToggleWithDefault.bydefault;
		setTranslationProfile(TranslationProfileGenerator.generateIncludeInputProfile(SAMLSPProperties.DEFAULT_TRANSLATION_PROFILE));
	}

	public void fromProperties(MessageSource msg, ImageAccessService imageAccessService, SAMLSPProperties source,
			String name)
	{

		setName(name);
		String prefix = SAMLSPProperties.IDP_PREFIX + name + ".";
		setId(source.getValue(prefix + SAMLSPProperties.IDP_ID));
		setDisplayedName(source.getLocalizedStringWithoutFallbackToDefault(msg,
				prefix + SAMLSPProperties.IDP_NAME));

		if (source.isSet(prefix + SAMLSPProperties.IDP_LOGO))
		{
			String logoUri = source.getValue(prefix + SAMLSPProperties.IDP_LOGO);
			setLogo(imageAccessService.getEditableImageResourceFromUriWithUnknownTheme(logoUri).orElse(null));
		}

		if (source.isSet(prefix + SAMLSPProperties.IDP_BINDING))
		{
			setBinding(source.getEnumValue(prefix + SAMLSPProperties.IDP_BINDING, Binding.class));
		}

		setAddress(source.getValue(prefix + SAMLSPProperties.IDP_ADDRESS));
		certificates = new ArrayList<>();
		if (source.isSet(prefix + SAMLSPProperties.IDP_CERTIFICATE))
		{
			certificates.add(source.getValue(prefix + SAMLSPProperties.IDP_CERTIFICATE));
		}

		List<String> certs = source.getListOfValues(prefix + SAMLSPProperties.IDP_CERTIFICATES);
		certs.forEach(

				c -> {
					certificates.add(c);
				});
		setRegistrationForm(source.getValue(prefix + CommonWebAuthnProperties.REGISTRATION_FORM));
		if (source.isSet(prefix + CommonWebAuthnProperties.ENABLE_ASSOCIATION))
		{
			accountAssociation = source.getBooleanValue(prefix + CommonWebAuthnProperties.ENABLE_ASSOCIATION)
					? ToggleWithDefault.enable
					: ToggleWithDefault.disable;
		}
		if (source.isSet(prefix + SAMLSPProperties.IDP_SIGN_REQUEST))
		{
			setSignRequest(source.getBooleanValue(prefix + SAMLSPProperties.IDP_SIGN_REQUEST));
		}

		String reqNameFormat = source.getValue(prefix + SAMLSPProperties.IDP_REQUESTED_NAME_FORMAT);
		setRequestedNameFormats(reqNameFormat != null ? Arrays.asList(reqNameFormat) : null);

		setPostLogoutEndpoint(source.getValue(prefix + SamlProperties.POST_LOGOUT_URL));
		setPostLogoutResponseEndpoint(source.getValue(prefix + SamlProperties.POST_LOGOUT_RET_URL));
		setRedirectLogoutEndpoint(source.getValue(prefix + SamlProperties.REDIRECT_LOGOUT_URL));
		setRedirectLogoutResponseEndpoint(source.getValue(prefix + SamlProperties.REDIRECT_LOGOUT_RET_URL));
		setSoapLogoutEndpoint(source.getValue(prefix + SamlProperties.SOAP_LOGOUT_URL));

		if (source.isSet(prefix + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE))
		{
			setTranslationProfile(TranslationProfileGenerator.getProfileFromString(source
					.getValue(prefix + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE)));

		} else if (source.isSet(prefix + CommonWebAuthnProperties.TRANSLATION_PROFILE))
		{
			setTranslationProfile(TranslationProfileGenerator.generateIncludeInputProfile(
					source.getValue(prefix + CommonWebAuthnProperties.TRANSLATION_PROFILE)));
		}
		
		groupMembershipAttribute = source.getValue(prefix + SAMLSPProperties.IDP_GROUP_MEMBERSHIP_ATTRIBUTE);

	}

	public void toProperties(Properties raw, MessageSource msg, FileStorageService fileService,
			String authName)
	{
		String prefix = SAMLSPProperties.P + SAMLSPProperties.IDP_PREFIX + getName() + ".";

		raw.put(prefix + SAMLSPProperties.IDP_ID, getId());
		if (getDisplayedName() != null && !getDisplayedName().isEmpty())
		{
			getDisplayedName().toProperties(raw, prefix + SAMLSPProperties.IDP_NAME, msg);
		}

		if (getLogo() != null)
		{
			FileFieldUtils.saveInProperties(getLogo(), prefix + SAMLSPProperties.IDP_LOGO, raw, fileService,
					StandardOwner.AUTHENTICATOR.toString(), authName + "." + getId());
		}

		if (getBinding() != null)
		{
			raw.put(prefix + SAMLSPProperties.IDP_BINDING, getBinding().toString());
		}

		if (getAddress() != null)
		{
			raw.put(prefix + SAMLSPProperties.IDP_ADDRESS, getAddress());
		}

		if (requestedNameFormats != null)
		{
			requestedNameFormats.stream()
					.forEach(f -> raw.put(prefix + SAMLSPProperties.IDP_REQUESTED_NAME_FORMAT, f));
		}

		if (certificates != null && !certificates.isEmpty())
		{
			certificates.forEach(c -> raw.put(
					prefix + SAMLSPProperties.IDP_CERTIFICATES + (certificates.indexOf(c) + 1), c));
		}

		if (getRegistrationForm() != null)
		{
			raw.put(prefix + CommonWebAuthnProperties.REGISTRATION_FORM, getRegistrationForm());
		}

		if (getAccountAssociation() != ToggleWithDefault.bydefault)
		{
			raw.put(prefix + CommonWebAuthnProperties.ENABLE_ASSOCIATION,
					accountAssociation == ToggleWithDefault.enable ? "true" : "false");
		}	
		
		if (getGroupMembershipAttribute() != null)
		{
			raw.put(prefix + SAMLSPProperties.IDP_GROUP_MEMBERSHIP_ATTRIBUTE, getGroupMembershipAttribute());
		}
		
		raw.put(prefix + SAMLSPProperties.IDP_SIGN_REQUEST, String.valueOf(isSignRequest()));
		if (getPostLogoutEndpoint() != null)
		{
			raw.put(prefix + SamlProperties.POST_LOGOUT_URL, getPostLogoutEndpoint());
		}
		if (getPostLogoutResponseEndpoint() != null)
		{
			raw.put(prefix + SamlProperties.POST_LOGOUT_RET_URL, getPostLogoutResponseEndpoint());
		}
		if (getRedirectLogoutEndpoint() != null)
		{
			raw.put(prefix + SamlProperties.REDIRECT_LOGOUT_URL, getRedirectLogoutEndpoint());
		}
		if (getRedirectLogoutResponseEndpoint() != null)
		{
			raw.put(prefix + SamlProperties.REDIRECT_LOGOUT_RET_URL, getRedirectLogoutResponseEndpoint());
		}
		if (getSoapLogoutEndpoint() != null)
		{
			raw.put(prefix + SamlProperties.SOAP_LOGOUT_URL, getSoapLogoutEndpoint());
		}
		
		if (getTranslationProfile() != null)
		{
			try
			{
				raw.put(prefix + CommonWebAuthnProperties.EMBEDDED_TRANSLATION_PROFILE, Constants.MAPPER
						.writeValueAsString(getTranslationProfile().toJsonObject()));
			} catch (Exception e)
			{
				throw new InternalException("Can't serialize provider's translation profile to JSON",
						e);
			}
		}

	}

	public SAMLIndividualTrustedSamlIdpConfiguration clone()
	{
		SAMLIndividualTrustedSamlIdpConfiguration clone = new SAMLIndividualTrustedSamlIdpConfiguration();
		clone.setName(this.getName());
		clone.setId(new String(this.getId()));
		clone.setLogo(this.getLogo() != null ? this.getLogo().clone() : null);
		clone.setBinding(this.getBinding() != null ? Binding.valueOf(this.getBinding().toString()) : null);
		clone.setTranslationProfile(
				this.getTranslationProfile() != null ? this.getTranslationProfile().clone() : null);
		clone.setDisplayedName(this.getDisplayedName() != null ? this.getDisplayedName().clone() : null);
		clone.setAddress(this.getAddress() != null ? new String(this.getAddress()) : null);
		clone.setCertificates(this.getCertificates() != null
				? this.getCertificates().stream().map(s -> new String(s)).collect(Collectors.toList())
				: null);

		clone.setAccountAssociation(this.getAccountAssociation());
		clone.setSignRequest(this.isSignRequest());
		clone.setRegistrationForm(
				this.getRegistrationForm() != null ? new String(this.getRegistrationForm()) : null);
		clone.setRequestedNameFormats(
				this.getRequestedNameFormats() != null
						? this.getRequestedNameFormats().stream().map(f -> new String(f))
								.collect(Collectors.toList())
						: null);
		clone.setPostLogoutEndpoint(
				this.getPostLogoutEndpoint() != null ? new String(this.getPostLogoutEndpoint()) : null);
		clone.setPostLogoutResponseEndpoint(this.getPostLogoutResponseEndpoint() != null
				? new String(this.getPostLogoutResponseEndpoint())
				: null);
		clone.setRedirectLogoutEndpoint(
				this.getRedirectLogoutEndpoint() != null ? new String(this.getRedirectLogoutEndpoint())
						: null);
		clone.setRedirectLogoutResponseEndpoint(this.getRedirectLogoutResponseEndpoint() != null
				? new String(this.getRedirectLogoutResponseEndpoint())
				: null);
		clone.setSoapLogoutEndpoint(
				this.getSoapLogoutEndpoint() != null ? new String(this.getSoapLogoutEndpoint()) : null);
		clone.setGroupMembershipAttribute(
				this.getGroupMembershipAttribute() != null ? new String(this.getGroupMembershipAttribute()) : null);
		return clone;

	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public TranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(TranslationProfile translationProfile)
	{
		this.translationProfile = translationProfile;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public I18nString getDisplayedName()
	{
		return displayedName;
	}

	public void setDisplayedName(I18nString displayedName)
	{
		this.displayedName = displayedName;
	}

	public LocalOrRemoteResource getLogo()
	{
		return logo;
	}

	public void setLogo(LocalOrRemoteResource logo)
	{
		this.logo = logo;
	}

	public String getAddress()
	{
		return address;
	}

	public void setAddress(String address)
	{
		this.address = address;
	}

	public Binding getBinding()
	{
		return binding;
	}

	public void setBinding(Binding binding)
	{
		this.binding = binding;
	}

	public List<String> getCertificates()
	{
		return certificates;
	}

	public void setCertificates(List<String> certificates)
	{
		this.certificates = certificates;
	}

	public String getRegistrationForm()
	{
		return registrationForm;
	}

	public void setRegistrationForm(String registrationForm)
	{
		this.registrationForm = registrationForm;
	}

	public ToggleWithDefault getAccountAssociation()
	{
		return accountAssociation;
	}

	public void setAccountAssociation(ToggleWithDefault accountAssociation)
	{
		this.accountAssociation = accountAssociation;
	}

	public boolean isSignRequest()
	{
		return signRequest;
	}

	public void setSignRequest(boolean signRequest)
	{
		this.signRequest = signRequest;
	}

	public List<String> getRequestedNameFormats()
	{
		return requestedNameFormats;
	}

	public void setRequestedNameFormats(List<String> requestedNameFormats)
	{
		this.requestedNameFormats = requestedNameFormats;
	}

	public String getPostLogoutEndpoint()
	{
		return postLogoutEndpoint;
	}

	public void setPostLogoutEndpoint(String postLogoutEndpoint)
	{
		this.postLogoutEndpoint = postLogoutEndpoint;
	}

	public String getPostLogoutResponseEndpoint()
	{
		return postLogoutResponseEndpoint;
	}

	public void setPostLogoutResponseEndpoint(String postLogoutResponseEndpoint)
	{
		this.postLogoutResponseEndpoint = postLogoutResponseEndpoint;
	}

	public String getRedirectLogoutEndpoint()
	{
		return redirectLogoutEndpoint;
	}

	public void setRedirectLogoutEndpoint(String redirectLogoutEndpoint)
	{
		this.redirectLogoutEndpoint = redirectLogoutEndpoint;
	}

	public String getRedirectLogoutResponseEndpoint()
	{
		return redirectLogoutResponseEndpoint;
	}

	public void setRedirectLogoutResponseEndpoint(String redirectLogoutResponseEndpoint)
	{
		this.redirectLogoutResponseEndpoint = redirectLogoutResponseEndpoint;
	}

	public String getSoapLogoutEndpoint()
	{
		return soapLogoutEndpoint;
	}

	public void setSoapLogoutEndpoint(String soapLogoutEndpoint)
	{
		this.soapLogoutEndpoint = soapLogoutEndpoint;
	}

	public String getGroupMembershipAttribute()
	{
		return groupMembershipAttribute;
	}

	public void setGroupMembershipAttribute(String groupMemberShipAttribute)
	{
		this.groupMembershipAttribute = groupMemberShipAttribute;
	}
}
