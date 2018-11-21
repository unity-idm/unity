/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.token.Token;

/**
 * Represents login session. Session expiration can be stored in two ways: either
 * to expire after a certain time of inactivity is reached or when an absolute point in time is reached.
 * The first case is the typical one. The latter is used when user's session should be preserved between  
 * browser shutdowns.
 * <p>
 * In the absolute termination time the maxInactivity time is also used, but only after the 
 * absolute expiration time has passed. This prevents killing such session when it is being used.
 * 
 * @author K. Benedyczak
 */
public class LoginSession
{
	private String id;
	private Date started;
	private Date expires;
	private Date lastUsed;
	private long maxInactivity;
	private long entityId;
	private String realm;
	private String outdatedCredentialId;
	private String entityLabel;
	private Set<String> authenticatedIdentities = new LinkedHashSet<>();
	private String remoteIdP;
	private RememberMeInfo rememberMeInfo;
	private AuthNInfo login1stFactor;
	private AuthNInfo login2ndFactor;
	private AuthNInfo additionalAuthn;
	
	private Map<String, String> sessionData = new HashMap<>();

	public LoginSession()
	{
	}

	/**
	 * Construct a session with absolute expiration.
	 * @param id
	 * @param started
	 * @param expires
	 * @param maxInactivity
	 * @param entityId
	 * @param realm
	 */
	public LoginSession(String id, Date started, Date expires, long maxInactivity,
			long entityId, String realm, RememberMeInfo rememberMeInfo,
			AuthNInfo login1stFactor, AuthNInfo login2ndFactor)
	{
		this.id = id;
		this.started = started;
		this.entityId = entityId;
		this.realm = realm;
		this.login1stFactor = login1stFactor;
		this.login2ndFactor = login2ndFactor;
		this.lastUsed = new Date();
		this.expires = expires;
		this.maxInactivity = maxInactivity;
		this.rememberMeInfo = rememberMeInfo;
	}

	/**
	 * Constructs a session with relative expiration
	 * @param id
	 * @param started
	 * @param maxInactivity
	 * @param entityId
	 * @param realm
	 */
	public LoginSession(String id, Date started, long maxInactivity, long entityId,
			String realm, RememberMeInfo rememberMeInfo, 
			AuthNInfo login1stFactor, AuthNInfo login2ndFactor)
	{
		this(id, started, null, maxInactivity, entityId, realm, rememberMeInfo, login1stFactor, login2ndFactor);
	}

	
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
	public Date getStarted()
	{
		return started;
	}
	public void setStarted(Date started)
	{
		this.started = started;
	}
	public Date getExpires()
	{
		return expires;
	}
	public void setExpires(Date expires)
	{
		this.expires = expires;
	}
	public long getEntityId()
	{
		return entityId;
	}
	public void setEntityId(long entityId)
	{
		this.entityId = entityId;
	}
	public String getRealm()
	{
		return realm;
	}
	public void setRealm(String realm)
	{
		this.realm = realm;
	}
	public Map<String, String> getSessionData()
	{
		return sessionData;
	}
	public void setSessionData(Map<String, String> sessionData)
	{
		this.sessionData = sessionData;
	}

	public Date getLastUsed()
	{
		return lastUsed;
	}

	public void setLastUsed(Date lastUsed)
	{
		this.lastUsed = lastUsed;
	}

	public long getMaxInactivity()
	{
		return maxInactivity;
	}

	public void setMaxInactivity(long maxInactivity)
	{
		this.maxInactivity = maxInactivity;
	}

	public String getEntityLabel()
	{
		return entityLabel;
	}

	public void setEntityLabel(String entityLabel)
	{
		this.entityLabel = entityLabel;
	}
	
	public Set<String> getAuthenticatedIdentities()
	{
		return authenticatedIdentities;
	}

	public void addAuthenticatedIdentities(Collection<String> identity)
	{
		this.authenticatedIdentities.addAll(identity);
	}
	
	public String getRemoteIdP()
	{
		return remoteIdP;
	}

	public void setRemoteIdP(String remoteIdP)
	{
		this.remoteIdP = remoteIdP;
	}
	
	public String getOutdatedCredentialId()
	{
		return outdatedCredentialId;
	}
	
	public boolean isUsedOutdatedCredential()
	{
		return outdatedCredentialId != null;
	}

	public void setOutdatedCredentialId(String outdatedCredentialId)
	{
		this.outdatedCredentialId = outdatedCredentialId;
	}
	
	public RememberMeInfo getRememberMeInfo()
	{
		return rememberMeInfo;
	}

	public void setRememberMeInfo(RememberMeInfo rememberMeInfo)
	{
		this.rememberMeInfo = rememberMeInfo;
	}

	public AuthNInfo getLogin1stFactor()
	{
		return login1stFactor;
	}

	public String getLogin1stFactorOptionId()
	{
		return login1stFactor == null ? null : login1stFactor.optionId;
	}

	public void setLogin1stFactor(AuthNInfo login1stFactor)
	{
		this.login1stFactor = login1stFactor;
	}

	public AuthNInfo getLogin2ndFactor()
	{
		return login2ndFactor;
	}

	public String getLogin2ndFactorOptionId()
	{
		return login2ndFactor == null ? null : login2ndFactor.optionId;
	}

	public void setLogin2ndFactor(AuthNInfo login2ndFactor)
	{
		this.login2ndFactor = login2ndFactor;
	}

	public AuthNInfo getAdditionalAuthn()
	{
		return additionalAuthn;
	}

	public void setAdditionalAuthn(AuthNInfo additionalAuthn)
	{
		this.additionalAuthn = additionalAuthn;
	}

	public boolean isExpiredAt(long timestamp)
	{
		long inactiveFor = timestamp - getLastUsed().getTime();
		return inactiveFor > getMaxInactivity();
	}
	
	public void deserialize(Token token)
	{
		ObjectNode main = JsonUtil.parse(token.getContents());
		String realm = main.get("realm").asText();
		long maxInactive = main.get("maxInactivity").asLong();
		long lastUsed = main.get("lastUsed").asLong();
		String entityLabel = main.get("entityLabel").asText();
		String credentialId = null;
		if (main.has("outdatedCredentialId"))
			credentialId = main.get("outdatedCredentialId").asText();
		if (main.has("authenticatedIdentities"))
		{
			List<String> ai = new ArrayList<>(2);
			ArrayNode an = (ArrayNode) main.get("authenticatedIdentities");
			for (int i=0; i<an.size(); i++)
				ai.add(an.get(i).asText());
			addAuthenticatedIdentities(ai);
		}

		if (main.has("remoteIdP"))
			setRemoteIdP(main.get("remoteIdP").asText());
		
		if (main.has("login1stFactor")) 
			login1stFactor = Constants.MAPPER.convertValue(main.get("login1stFactor"), AuthNInfo.class);
		
		if (main.has("login2ndFactor"))
			login2ndFactor = Constants.MAPPER.convertValue(main.get("login2ndFactor"), AuthNInfo.class);

		if (main.has("additionalAuthn"))
			additionalAuthn = Constants.MAPPER.convertValue(main.get("additionalAuthn"), AuthNInfo.class);
		
		if (main.has("rememberMeInfo"))
			rememberMeInfo = Constants.MAPPER.convertValue(main.get("rememberMeInfo"), RememberMeInfo.class);
		
		setId(token.getValue());
		setStarted(token.getCreated());
		setExpires(token.getExpires());
		setMaxInactivity(maxInactive);
		setEntityId(token.getOwner());
		setRealm(realm);
		setLastUsed(new Date(lastUsed));
		setEntityLabel(entityLabel);
		setOutdatedCredentialId(credentialId);
		
		Map<String, String> attrs = new HashMap<String, String>(); 
		ObjectNode attrsJson = (ObjectNode) main.get("attributes");
		Iterator<String> fNames = attrsJson.fieldNames();
		while (fNames.hasNext())
		{
			String attrName = fNames.next();
			attrs.put(attrName, attrsJson.get(attrName).asText());
		}
		setSessionData(attrs);
	}
	
	public byte[] getTokenContents()
	{
		ObjectNode main = Constants.MAPPER.createObjectNode();
		main.put("realm", getRealm());
		main.put("maxInactivity", getMaxInactivity());
		main.put("lastUsed", getLastUsed().getTime());
		if (isUsedOutdatedCredential())
			main.put("outdatedCredentialId", getOutdatedCredentialId());
		main.put("entityLabel", getEntityLabel());
		ArrayNode ai = main.withArray("authenticatedIdentities");
		for (String id: authenticatedIdentities)
			ai.add(id);
		if (remoteIdP != null)
			main.put("remoteIdP", remoteIdP);
		ObjectNode attrsJson = main.putObject("attributes");
		for (Map.Entry<String, String> a: getSessionData().entrySet())
			attrsJson.put(a.getKey(), a.getValue());

		if (login1stFactor != null)
			main.putPOJO("login1stFactor", login1stFactor);
		
		if (login2ndFactor != null)
			main.putPOJO("login2ndFactor", login2ndFactor);

		if (additionalAuthn != null)
			main.putPOJO("additionalAuthn", additionalAuthn);
		
		if (rememberMeInfo != null)
			main.putPOJO("rememberMeInfo", rememberMeInfo);
		
		return JsonUtil.serialize2Bytes(main);
	}
	
	@Override
	public String toString()
	{
		return id + "@" + realm + " of entity " + entityId;
	}

	public static class RememberMeInfo
	{
		public final boolean firstFactorSkipped;
		public final boolean secondFactorSkipped;
		
		@JsonCreator
		public RememberMeInfo(@JsonProperty("firstFactorSkipped") boolean firstFactorSkipped, 
				@JsonProperty("secondFactorSkipped") boolean secondFactorSkipped)
		{
			this.firstFactorSkipped = firstFactorSkipped;
			this.secondFactorSkipped = secondFactorSkipped;
		}
	}
	
	public static class AuthNInfo
	{
		public final String optionId;
		public final Date time;

		@JsonCreator
		public AuthNInfo(@JsonProperty("optionId") String optionId, 
				@JsonProperty("time") Date time)
		{
			this.optionId = optionId;
			this.time = time;
		}

		@Override
		public String toString()
		{
			return "AuthNInfo [optionId=" + optionId + ", time=" + time.getTime() + "]";
		}
	}	
}
