/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.console;

import com.nimbusds.oauth2.sdk.client.ClientType;
import io.imunity.vaadin.endpoint.common.file.LocalOrRemoteResource;
import pl.edu.icm.unity.oauth.as.OAuthSystemAttributesProvider.GrantFlow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represent single OAuth client. When client is edited or deleted the
 * corresponding indicator should be set
 * 
 * @author P.Piernik
 *
 */
public class OAuthClient
{
	private Long entity;
	private String name;
	private String id;
	private String secret;
	private List<String> flows;
	private boolean allowAnyScopes;
	private boolean canReceivePatternScopes;
	private List<String> scopes;
	private String type;
	private List<String> redirectURIs;
	private LocalOrRemoteResource logo;
	private String title;
	private String group;
	private boolean toRemove;
	private boolean updated;

	public OAuthClient()
	{
		this.type = ClientType.CONFIDENTIAL.toString();
		this.allowAnyScopes = true;
		this.flows = new ArrayList<>();
		this.scopes = new ArrayList<>();
		this.redirectURIs = new ArrayList<>();
	}

	public OAuthClient(String id, String secret)
	{
		this();
		this.id = id;
		this.secret = secret;
		this.flows = new ArrayList<>(Collections.singletonList(GrantFlow.authorizationCode.toString()));
		this.scopes = new ArrayList<>();
		this.redirectURIs = new ArrayList<>();
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getType()
	{
		return type;
	}

	public void setType(String type)
	{
		this.type = type;
	}

	public List<String> getFlows()
	{
		return flows;
	}

	public void setFlows(List<String> flows)
	{
		this.flows = flows;
	}

	public String getGroup()
	{
		return group;
	}

	public void setGroup(String group)
	{
		this.group = group;
	}

	public Long getEntity()
	{
		return entity;
	}

	public void setEntity(Long entity)
	{
		this.entity = entity;
	}

	public List<String> getRedirectURIs()
	{
		return redirectURIs;
	}

	public void setRedirectURIs(List<String> redirectURIs)
	{
		this.redirectURIs = redirectURIs;
	}

	public String getSecret()
	{
		return secret;
	}

	public void setSecret(String secret)
	{
		this.secret = secret;
	}

	public String getId()
	{
		return id;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public boolean isToRemove()
	{
		return toRemove;
	}

	public void setToRemove(boolean toRemove)
	{
		this.toRemove = toRemove;
	}

	public boolean isUpdated()
	{
		return updated;
	}

	public void setUpdated(boolean updated)
	{
		this.updated = updated;
	}

	public LocalOrRemoteResource getLogo()
	{
		return logo;
	}

	public void setLogo(LocalOrRemoteResource logo)
	{
		this.logo = logo;
	}

	public List<String> getScopes()
	{
		return scopes;
	}

	public void setScopes(List<String> scopes)
	{
		this.scopes = scopes;
	}
	
	public boolean isAllowAnyScopes()
	{
		return allowAnyScopes;
	}

	public void setAllowAnyScopes(boolean allowAnyScopes)
	{
		this.allowAnyScopes = allowAnyScopes;
	}
	
	public boolean isCanReceivePatternScopes()
	{
		return canReceivePatternScopes;
	}

	public void setCanReceivePatternScopes(boolean canReceivePatternScopes)
	{
		this.canReceivePatternScopes = canReceivePatternScopes;
	}
	
	public OAuthClient clone()
	{
		OAuthClient clone = new OAuthClient();
		if (this.getFlows() != null)
		{
			clone.setFlows(this.getFlows().stream().map(s -> new String(s)).collect(Collectors.toList()));
		}
		
		clone.setAllowAnyScopes(isAllowAnyScopes());
		
		if (this.getScopes() != null)
		{
			clone.setScopes(this.getScopes().stream().map(s -> new String(s)).collect(Collectors.toList()));
		}

		if (this.getRedirectURIs() != null)
		{
			clone.setRedirectURIs(this.getRedirectURIs().stream().map(s -> new String(s))
					.collect(Collectors.toList()));
		}

		clone.setName(this.getName());
		clone.setId(this.getId());
		clone.setGroup(this.getGroup());
		clone.setEntity(this.getEntity());
		clone.setSecret(this.getSecret());
		clone.setTitle(this.getTitle());
		clone.setToRemove(this.isToRemove());
		clone.setType(this.getType());
		clone.setUpdated(this.isUpdated());
		clone.setLogo(this.getLogo() != null ? this.getLogo().clone() : null);
		clone.setCanReceivePatternScopes(this.isCanReceivePatternScopes());
		return clone;
	}

	public static class OAuthClientsBean
	{
		private List<OAuthClient> clients;

		public OAuthClientsBean()
		{
			clients = new ArrayList<>();
		}

		public List<OAuthClient> getClients()
		{
			return clients;
		}

		public void setClients(List<OAuthClient> clients)
		{
			this.clients = clients;
		}
	}

}
