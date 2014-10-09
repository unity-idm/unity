/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.webauthz;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.types.basic.Attribute;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;

/**
 * Context stored in HTTP session maintaining authorization token. 
 * @author K. Benedyczak
 */
public class OAuthAuthzContext
{
	public static final long AUTHN_TIMEOUT = 900000;
	private AuthorizationRequest request;
	private Date timestamp;
	private URI returnURI;
	private String clientName;
	private Attribute<BufferedImage> clientLogo;
	private String translationProfile;
	private String usersGroup;
	private Set<ScopeInfo> effectiveRequestedScopes = new HashSet<OAuthAuthzContext.ScopeInfo>();
	private Set<String> requestedAttrs = new HashSet<>();
	private GrantFlow flow;
	private boolean openIdMode;
	private int codeTokenValidity;
	private int accessTokenValidity;
	private int idTokenValidity;
	private String issuerName;

	public OAuthAuthzContext(AuthorizationRequest request, int accessTokenValidity, int codeTokenValidity,
			int idTokenValidity, String issuerName)
	{
		super();
		this.timestamp = new Date();
		this.request = request;
		this.codeTokenValidity = codeTokenValidity;
		this.accessTokenValidity = accessTokenValidity;
		this.idTokenValidity = idTokenValidity;
		this.issuerName = issuerName;
	}

	public AuthorizationRequest getRequest()
	{
		return request;
	}
	
	public boolean isExpired()
	{
		return System.currentTimeMillis() > AUTHN_TIMEOUT+timestamp.getTime();
	}

	public URI getReturnURI()
	{
		return returnURI;
	}

	public void setReturnURI(URI returnURI)
	{
		this.returnURI = returnURI;
	}

	public String getClientName()
	{
		return clientName;
	}

	public void setClientName(String clientName)
	{
		this.clientName = clientName;
	}

	public Attribute<BufferedImage> getClientLogo()
	{
		return clientLogo;
	}

	public void setClientLogo(Attribute<BufferedImage> clientLogo)
	{
		this.clientLogo = clientLogo;
	}

	public String getUsersGroup()
	{
		return usersGroup;
	}

	public void setUsersGroup(String usersGroup)
	{
		this.usersGroup = usersGroup;
	}

	public String getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(String translationProfile)
	{
		this.translationProfile = translationProfile;
	}
	
	public void addScopeInfo(ScopeInfo scopeInfo)
	{
		effectiveRequestedScopes.add(scopeInfo);
		requestedAttrs.addAll(scopeInfo.getAttributes());
	}
	
	public Set<String> getRequestedAttrs()
	{
		return requestedAttrs;
	}

	public Set<ScopeInfo> getEffectiveRequestedScopes()
	{
		return effectiveRequestedScopes;
	}

	public GrantFlow getFlow()
	{
		return flow;
	}

	public void setFlow(GrantFlow flow)
	{
		this.flow = flow;
	}

	public boolean isOpenIdMode()
	{
		return openIdMode;
	}

	public void setOpenIdMode(boolean openIdMode)
	{
		this.openIdMode = openIdMode;
	}

	public int getCodeTokenValidity()
	{
		return codeTokenValidity;
	}

	public int getAccessTokenValidity()
	{
		return accessTokenValidity;
	}

	public int getIdTokenValidity()
	{
		return idTokenValidity;
	}

	public String getIssuerName()
	{
		return issuerName;
	}


	public static class ScopeInfo
	{
		private String name;
		private String description;
		private Set<String> attributes;
		
		public ScopeInfo(String name, String description, Collection<String> attributes)
		{
			super();
			this.name = name;
			this.description = description;
			this.attributes = new HashSet<String>(attributes);
		}

		public String getName()
		{
			return name;
		}

		public String getDescription()
		{
			return description;
		}

		public Set<String> getAttributes()
		{
			return attributes;
		}
	}
}
