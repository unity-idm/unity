/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.idp.console.v8;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.files.FileStorageService;
import pl.edu.icm.unity.engine.api.files.FileStorageService.StandardOwner;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.webui.common.binding.LocalOrRemoteResource;
import pl.edu.icm.unity.webui.common.file.FileFieldUtils;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

/**
 * SAML Individual trusted sp configuration
 * 
 * @author P.Piernik
 *
 */
public class SAMLIndividualTrustedSPConfiguration
{
	private String name;
	private String id;
	private boolean x500Name;
	private I18nString displayedName;
	private LocalOrRemoteResource logo;
	private List<String> certificates;
	private boolean encryptAssertions;
	private List<String> authorizedRedirectsUri;
	private String postLogoutEndpoint;
	private String postLogoutResponseEndpoint;
	private String redirectLogoutEndpoint;
	private String redirectLogoutResponseEndpoint;
	private String soapLogoutEndpoint;

	public SAMLIndividualTrustedSPConfiguration()
	{
		authorizedRedirectsUri = new ArrayList<>();
		x500Name = false;
	}

	public void fromProperties(MessageSource msg, ImageAccessService imageAccessService, SamlIdpProperties source,
			String name, String theme)
	{
		setName(name);
		String prefix = SamlIdpProperties.ALLOWED_SP_PREFIX + name + ".";
		
		if (source.isSet(prefix + SamlIdpProperties.ALLOWED_SP_ENTITY))
		{
			setX500Name(false);
			setId(source.getValue(prefix + SamlIdpProperties.ALLOWED_SP_ENTITY));
		}else
		{
			setX500Name(true);
			setId(source.getValue(prefix + SamlIdpProperties.ALLOWED_SP_DN));
		}
		
		setDisplayedName(source.getLocalizedStringWithoutFallbackToDefault(msg,
				prefix + SamlIdpProperties.ALLOWED_SP_NAME));

		if (source.isSet(prefix + SamlIdpProperties.ALLOWED_SP_LOGO))
		{
			String logoUri = source.getValue(prefix + SamlIdpProperties.ALLOWED_SP_LOGO);
			setLogo(imageAccessService.getEditableImageResourceFromUri(logoUri, theme).orElse(null));
		}

		certificates = new ArrayList<>();
		if (source.isSet(prefix + SamlIdpProperties.ALLOWED_SP_CERTIFICATE))
		{
			certificates.add(source.getValue(prefix + SamlIdpProperties.ALLOWED_SP_CERTIFICATE));
		}

		List<String> certs = source.getListOfValues(prefix + SamlIdpProperties.ALLOWED_SP_CERTIFICATES);
		certs.forEach(

				c -> {
					certificates.add(c);
				});

		setEncryptAssertions(source.getBooleanValue(prefix + SamlIdpProperties.ALLOWED_SP_ENCRYPT));

		authorizedRedirectsUri = new ArrayList<>();
		if (source.isSet(prefix + SamlIdpProperties.ALLOWED_SP_RETURN_URL))
		{
			authorizedRedirectsUri.add(source.getValue(prefix + SamlIdpProperties.ALLOWED_SP_RETURN_URL));
		}

		List<String> uris = source.getListOfValues(prefix + SamlIdpProperties.ALLOWED_SP_RETURN_URLS);
		uris.forEach(

				c -> {
					authorizedRedirectsUri.add(c);
				});

		setPostLogoutEndpoint(source.getValue(prefix + SamlProperties.POST_LOGOUT_URL));
		setPostLogoutResponseEndpoint(source.getValue(prefix + SamlProperties.POST_LOGOUT_RET_URL));
		setRedirectLogoutEndpoint(source.getValue(prefix + SamlProperties.REDIRECT_LOGOUT_URL));
		setRedirectLogoutResponseEndpoint(source.getValue(prefix + SamlProperties.REDIRECT_LOGOUT_RET_URL));
		setSoapLogoutEndpoint(source.getValue(prefix + SamlProperties.SOAP_LOGOUT_URL));
	}

	public void toProperties(Properties raw, MessageSource msg, FileStorageService fileService,
			String serviceName)
	{
		String prefix = SamlIdpProperties.P + SamlIdpProperties.ALLOWED_SP_PREFIX + getName() + ".";

		if (isX500Name())
		{
			raw.put(prefix + SamlIdpProperties.ALLOWED_SP_DN, getId());
		} else
		{
			raw.put(prefix + SamlIdpProperties.ALLOWED_SP_ENTITY, getId());
		}
		
		if (getDisplayedName() != null)
		{
			getDisplayedName().toProperties(raw, prefix + SamlIdpProperties.ALLOWED_SP_NAME, msg);
		}

		if (getLogo() != null)
		{
			FileFieldUtils.saveInProperties(getLogo(), prefix + SamlIdpProperties.ALLOWED_SP_LOGO, raw,
					fileService, StandardOwner.SERVICE.toString(), serviceName + "." + getId());
		}

		if (certificates != null && !certificates.isEmpty())
		{
			certificates.forEach(c -> raw.put(prefix + SamlIdpProperties.ALLOWED_SP_CERTIFICATES
					+ (certificates.indexOf(c) + 1), c));
		}

		raw.put(prefix + SamlIdpProperties.ALLOWED_SP_ENCRYPT, String.valueOf(isEncryptAssertions()));

		
		
		
		if (authorizedRedirectsUri != null && !authorizedRedirectsUri.isEmpty())
		{
			raw.put(prefix + SamlIdpProperties.ALLOWED_SP_RETURN_URL, authorizedRedirectsUri.get(0));
			authorizedRedirectsUri.stream().skip(1).forEach(c -> raw.put(prefix + SamlIdpProperties.ALLOWED_SP_RETURN_URLS
					+ (authorizedRedirectsUri.indexOf(c) + 1), c));
		}

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

	}

	public SAMLIndividualTrustedSPConfiguration clone()
	{
		SAMLIndividualTrustedSPConfiguration clone = new SAMLIndividualTrustedSPConfiguration();
		clone.setName(this.getName());
		clone.setDisplayedName(this.getDisplayedName() != null ? this.getDisplayedName().clone() : null);
		clone.setId(new String(this.getId()));
		clone.setX500Name(this.isX500Name());
		clone.setLogo(this.getLogo() != null ? this.getLogo().clone() : null);
		clone.setCertificates(this.getCertificates() != null
				? this.getCertificates().stream().map(s -> new String(s)).collect(Collectors.toList())
				: null);
		clone.setEncryptAssertions(this.isEncryptAssertions());
		clone.setAuthorizedRedirectsUri(this.getAuthorizedRedirectsUri() != null
				? this.getAuthorizedRedirectsUri().stream().map(s -> new String(s)).collect(Collectors.toList())
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

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public LocalOrRemoteResource getLogo()
	{
		return logo;
	}

	public void setLogo(LocalOrRemoteResource logo)
	{
		this.logo = logo;
	}

	public List<String> getCertificates()
	{
		return certificates;
	}

	public void setCertificates(List<String> certificates)
	{
		this.certificates = certificates;
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

	public I18nString getDisplayedName()
	{
		return displayedName;
	}

	public void setDisplayedName(I18nString displayedName)
	{
		this.displayedName = displayedName;
	}

	public boolean isEncryptAssertions()
	{
		return encryptAssertions;
	}

	public void setEncryptAssertions(boolean encryptAssertions)
	{
		this.encryptAssertions = encryptAssertions;
	}

	public List<String> getAuthorizedRedirectsUri()
	{
		return authorizedRedirectsUri;
	}

	public void setAuthorizedRedirectsUri(List<String> authorizedRedirectsUri)
	{
		this.authorizedRedirectsUri = authorizedRedirectsUri;
	}
	
	public boolean isX500Name()
	{
		return x500Name;
	}

	public void setX500Name(boolean x500Name)
	{
		this.x500Name = x500Name;
	}
}
