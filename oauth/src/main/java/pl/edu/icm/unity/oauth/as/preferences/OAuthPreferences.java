/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.webui.idpcommon.IdPPreferences;

/**
 * User's preferences for the OAuth endpoints.
 * 
 * @author K. Benedyczak
 */
public class OAuthPreferences extends IdPPreferences
{
	public static final String ID = OAuthPreferences.class.getName();
	protected final ObjectMapper mapper = Constants.MAPPER;

	private Map<String, OAuthClientSettings> spSettings = new HashMap<>();

	@Override
	protected void serializeAll(ObjectNode main)
	{
		ObjectNode settingsN = main.with("spSettings");
		for (Map.Entry<String, OAuthClientSettings> entry : spSettings.entrySet())
			settingsN.set(entry.getKey(), entry.getValue().serialize());
	}

	@Override
	protected void deserializeAll(ObjectNode main)
	{
		ObjectNode spSettingsNode = main.with("spSettings");
		Iterator<String> keys = spSettingsNode.fieldNames();
		for (String key; keys.hasNext();)
		{
			key = keys.next();
			spSettings.put(key, new OAuthClientSettings(spSettingsNode.with(key)));
		}
	}

	public static OAuthPreferences getPreferences(PreferencesManagement preferencesMan)
	{
		OAuthPreferences ret = new OAuthPreferences();
		initPreferencesGeneric(preferencesMan, ret, OAuthPreferences.ID);
		return ret;
	}

	public static void savePreferences(PreferencesManagement preferencesMan, OAuthPreferences preferences)
			throws EngineException
	{
		savePreferencesGeneric(preferencesMan, preferences, OAuthPreferences.ID);
	}

	public OAuthClientSettings getSPSettings(String sp)
	{
		OAuthClientSettings ret = spSettings.get(sp);
		if (ret == null)
			ret = spSettings.get("");
		if (ret == null)
			ret = new OAuthClientSettings();
		return ret;
	}

	public Set<String> getKeys()
	{
		return spSettings.keySet();
	}

	public void setSPSettings(String sp, OAuthClientSettings settings)
	{
		spSettings.put(sp, settings);
	}

	public void removeSPSettings(String sp)
	{
		spSettings.remove(sp);
	}

	public static class OAuthClientSettings
	{
		private boolean doNotAsk = false;
		private boolean defaultAccept = true;
		private String selectedIdentity;
		private Set<String> effectiveRequestedScopes = new HashSet<>();
		private Set<String> audience = new HashSet<>();
		private Instant timestamp;

		public OAuthClientSettings()
		{
		}

		protected OAuthClientSettings(ObjectNode from)
		{
			setDefaultAccept(from.get("defaultAccept").asBoolean());
			setDoNotAsk(from.get("doNotAsk").asBoolean());

			if (from.has("effectiveRequestedScopes"))
			{
				ArrayNode jsonAcs = (ArrayNode) from.get("effectiveRequestedScopes");
				Set<String> scopes = new HashSet<>();
				for (JsonNode e : jsonAcs)
					scopes.add(e.asText());
				getEffectiveRequestedScopes().addAll(scopes);
			}

			if (from.has("audience"))
			{
				ArrayNode jsonAcs = (ArrayNode) from.get("audience");
				Set<String> audience = new HashSet<>();
				for (JsonNode e : jsonAcs)
					audience.add(e.asText());
				getAudience().addAll(audience);
			}
			if (from.has("timestamp"))
			{
				timestamp = Instant.ofEpochMilli(from.get("timestamp").asLong());
			}
		}
		
		protected ObjectNode serialize()
		{
			ObjectNode main = Constants.MAPPER.createObjectNode();
			main.put("doNotAsk", doNotAsk);
			main.put("defaultAccept", defaultAccept);
			if (selectedIdentity != null)
				main.put("selectedIdentity", selectedIdentity);
			ArrayNode values = main.putArray("effectiveRequestedScopes");
			for (String value : effectiveRequestedScopes)
				values.add(value);
			ArrayNode audienceNode = main.putArray("audience");
			for (String value : audience)
				audienceNode.add(value);
			if (timestamp != null)
			{
				main.put("timestamp", timestamp.toEpochMilli());
			}

			return main;
		}

		public boolean isDoNotAsk()
		{
			return doNotAsk;
		}

		public void setDoNotAsk(boolean doNotAsk)
		{
			this.doNotAsk = doNotAsk;
		}

		public boolean isDefaultAccept()
		{
			return defaultAccept;
		}

		public void setDefaultAccept(boolean defaultAccept)
		{
			this.defaultAccept = defaultAccept;
		}

		public String getSelectedIdentity()
		{
			return selectedIdentity;
		}

		public void setSelectedIdentity(String selectedIdentity)
		{
			this.selectedIdentity = selectedIdentity;
		}

		public Set<String> getEffectiveRequestedScopes()
		{
			return effectiveRequestedScopes;
		}

		public void setEffectiveRequestedScopes(Set<String> effectiveRequestedScopes)
		{
			this.effectiveRequestedScopes = effectiveRequestedScopes;
		}

		public Set<String> getAudience()
		{
			return audience;
		}

		public void setAudience(Set<String> audience)
		{
			this.audience = audience;
		}

		public Instant getTimestamp()
		{
			return timestamp;
		}

		public void setTimestamp(Instant timestamp)
		{
			this.timestamp = timestamp;
		}
	}
}
