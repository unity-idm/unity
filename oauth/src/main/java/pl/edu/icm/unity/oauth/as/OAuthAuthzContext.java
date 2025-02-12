/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.nimbusds.oauth2.sdk.AuthorizationRequest;
import com.nimbusds.oauth2.sdk.client.ClientType;
import com.nimbusds.openid.connect.sdk.OIDCScopeValue;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.translation.TranslationProfile;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;
import pl.edu.icm.unity.oauth.as.webauthz.ClaimsInTokenAttribute;

/**
 * Context stored in HTTP session maintaining authorization token.
 * 
 * @author K. Benedyczak
 */
public class OAuthAuthzContext
{
	public enum Prompt { NONE, LOGIN, CONSENT}
	
	public static final long AUTHN_TIMEOUT = 900000;
	private AuthorizationRequest request;
	private OAuthASProperties config;
	private Date timestamp;

	private URI returnURI;
	private String clientName;
	private String clientUsername;
	private long clientEntityId;
	private Attribute clientLogo;
	private TranslationProfile translationProfile;
	private String usersGroup;
	private Set<OAuthScope> effectiveRequestedScopes = new HashSet<>();
	private Set<String> requestedScopes = new HashSet<>();
	private Set<String> effectiveRequestedAttrs = new HashSet<>();
	private Set<Prompt> prompts= new HashSet<>();
	private List<String> additionalAudience = new ArrayList<>();
	private GrantFlow flow;
	private ClientType clientType;
	private boolean openIdMode;
	private Optional<ClaimsInTokenAttribute> claimsInTokenAttribute = Optional.empty();
	private List<AttributeValueFilter> claimValueFilters;
	
	
	public OAuthAuthzContext(AuthorizationRequest request, OAuthASProperties properties)
	{
		this.config = properties;
		this.timestamp = new Date();
		this.request = request;
	}

	public OAuthASProperties getConfig()
	{
		return config;
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

	public String getClientUsername()
	{
		return clientUsername;
	}

	public void setClientUsername(String clientUsername)
	{
		this.clientUsername = clientUsername;
	}

	public Attribute getClientLogo()
	{
		return clientLogo;
	}

	public void setClientLogo(Attribute clientLogo)
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

	public TranslationProfile getTranslationProfile()
	{
		return translationProfile;
	}

	public void setTranslationProfile(TranslationProfile translationProfile)
	{
		this.translationProfile = translationProfile;
	}
	
	public void addEffectiveScopeInfo(OAuthScope scopeInfo)
	{
		effectiveRequestedScopes.add(scopeInfo);
		effectiveRequestedAttrs.addAll(scopeInfo.attributes);
	}
	
	public Set<String> getEffectiveRequestedAttrs()
	{
		return effectiveRequestedAttrs;
	}

	public Set<OAuthScope> getEffectiveRequestedScopes()
	{
		return effectiveRequestedScopes;
	}

	public String[] getEffectiveRequestedScopesList()
	{
		String[] ret = new String[effectiveRequestedScopes.size()];
		Iterator<OAuthScope> sIt = effectiveRequestedScopes.iterator();
		for (int i=0; i<ret.length; i++)
			ret[i] = sIt.next().name;
		return ret;
	}

	public Set<Prompt> getPrompts()
	{
		return prompts;
	}
	
	public void addPrompt(Prompt prompt)
	{
		prompts.add(prompt);
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

	public boolean hasOfflineAccessScope()
	{
		return !getEffectiveRequestedScopes().stream()
				.filter(a -> a.name.equals(OIDCScopeValue.OFFLINE_ACCESS.getValue())).findAny().isEmpty();
	}
	
	public long getClientEntityId()
	{
		return clientEntityId;
	}

	public void setClientEntityId(long clientEntityId)
	{
		this.clientEntityId = clientEntityId;
	}

	public static long getAuthnTimeout()
	{
		return AUTHN_TIMEOUT;
	}

	public Set<String> getRequestedScopes()
	{
		return requestedScopes;
	}

	public void setRequestedScopes(Set<String> requestedScopes)
	{
		this.requestedScopes = requestedScopes;
	}
	
	public void addRequestedScope(String scope)
	{
		requestedScopes.add(scope);
	}

	public ClientType getClientType()
	{
		return clientType;
	}

	public void setClientType(ClientType clientType)
	{
		this.clientType = clientType;
	}

	public List<String> getAdditionalAudience()
	{
		return additionalAudience;
	}

	public void setAdditionalAudience(List<String> additionalAudience)
	{
		this.additionalAudience = additionalAudience;
	}

	public Optional<ClaimsInTokenAttribute> getClaimsInTokenAttribute()
	{
		return claimsInTokenAttribute;
	}

	public void setClaimsInTokenAttribute(Optional<ClaimsInTokenAttribute> claimsInTokenAttribute)
	{
		this.claimsInTokenAttribute = claimsInTokenAttribute;
	}
	
	public boolean requestsAttributesInIdToken()
	{
		if (claimsInTokenAttribute.isEmpty())
			return false;
		
		return claimsInTokenAttribute.get().values.contains(ClaimsInTokenAttribute.Value.id_token);	
	}

	public List<AttributeValueFilter> getClaimValueFilters()
	{
		return claimValueFilters;
	}

	public void setClaimValueFilters(List<AttributeValueFilter> claimValueFilters)
	{
		this.claimValueFilters = claimValueFilters;
	}
}
