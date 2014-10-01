/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.preferences;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.webui.common.provider.IdPPreferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;


/**
 * User's preferences for the OAuth endpoints.
 * @author K. Benedyczak
 */
public class OAuthPreferences extends IdPPreferences
{
	public static final String ID = OAuthPreferences.class.getName();
	protected final ObjectMapper mapper = Constants.MAPPER;

	private Map<String, SPSettings> spSettings = new HashMap<String, OAuthPreferences.SPSettings>();
	
	@Override
	protected void serializeAll(ObjectNode main)
	{
		ObjectNode settingsN = main.with("spSettings");
		for (Map.Entry<String, SPSettings> entry: spSettings.entrySet())
			settingsN.set(entry.getKey(), entry.getValue().serialize());
	}
	
	@Override
	protected void deserializeAll(ObjectNode main)
	{
		ObjectNode spSettingsNode = main.with("spSettings");
		Iterator<String> keys = spSettingsNode.fieldNames();
		for (String key; keys.hasNext();)
		{
			key=keys.next();
			spSettings.put(key, new SPSettings(spSettingsNode.with(key)));
		}
	}
	
	public static OAuthPreferences getPreferences(PreferencesManagement preferencesMan) throws EngineException
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
	
	public SPSettings getSPSettings(String sp)
	{
		SPSettings ret = spSettings.get(sp);
		if (ret == null)
			ret = new SPSettings();
		return ret;
	}
	
	public Set<String> getKeys()
	{
		return spSettings.keySet();
	}
	
	public void setSPSettings(String sp, SPSettings settings)
	{
		spSettings.put(sp, settings);
	}
	
	public void removeSPSettings(String sp)
	{
		spSettings.remove(sp);
	}
	
	public static class SPSettings
	{
		private boolean doNotAsk=false;
		private boolean defaultAccept=true;
		
		protected SPSettings()
		{
		}

		protected SPSettings(ObjectNode from)
		{
			setDefaultAccept(from.get("defaultAccept").asBoolean());
			setDoNotAsk(from.get("doNotAsk").asBoolean());
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
		
		protected ObjectNode serialize()
		{
			ObjectNode main = Constants.MAPPER.createObjectNode();
			main.put("doNotAsk", doNotAsk);
			main.put("defaultAccept", defaultAccept);
			return main;
		}
	}
}
