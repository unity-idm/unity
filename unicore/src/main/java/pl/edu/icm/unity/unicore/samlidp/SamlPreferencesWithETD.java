/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.unicore.samlidp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.samlidp.preferences.SamlPreferences;
import pl.edu.icm.unity.server.api.PreferencesManagement;


/**
 * User's preferences for the SAML endpoints. This extends {@link SamlPreferences} with ETD generation related settings.
 * @author K. Benedyczak
 */
public class SamlPreferencesWithETD extends SamlPreferences
{
	public static final String ID = SamlPreferencesWithETD.class.getName();
	private Map<String, SPETDSettings> spEtdSettings = new HashMap<String, SPETDSettings>();

	@Override
	protected void deserializeAll(ObjectNode main)
	{
		super.deserializeAll(main);
		ObjectNode spSettingsNode = main.with("spEtdSettings");
		Iterator<String> keys = spSettingsNode.fieldNames();
		for (String key; keys.hasNext();)
		{
			key=keys.next();
			spEtdSettings.put(key, deserializeSingleETD(spSettingsNode.with(key)));
		}
	}
	
	protected SPETDSettings deserializeSingleETD(ObjectNode from)
	{
		SPETDSettings ret = new SPETDSettings();
		ret.setGenerateETD(from.get("generateETD").asBoolean());
		ret.setEtdValidity(from.get("etdValidity").asLong());
		ret.setMaxProxyCount(from.get("maxProxyCount").asInt());
		return ret;
	}

	@Override
	protected void serializeAll(ObjectNode main)
	{
		super.serializeAll(main);
		
		ObjectNode settingsN = main.with("spEtdSettings");
		for (Map.Entry<String, SPETDSettings> entry: spEtdSettings.entrySet())
			settingsN.put(entry.getKey(), serializeSingleETD(entry.getValue()));
	}
	
	protected ObjectNode serializeSingleETD(SPETDSettings what)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("generateETD", what.generateETD);
		main.put("etdValidity", what.etdValidity);
		main.put("maxProxyCount", what.maxProxyCount);
		return main;
	}

	public SPETDSettings getSPETDSettings(String sp)
	{
		SPETDSettings ret = spEtdSettings.get(sp); 
		if (ret == null)
			ret = new SPETDSettings();
		return ret;
	}
	
	public void setSPETDSettings(String sp, SPETDSettings settings)
	{
		spEtdSettings.put(sp, settings);
	}

	public static SamlPreferencesWithETD getPreferences(PreferencesManagement preferencesMan) throws EngineException
	{
		SamlPreferencesWithETD ret = new SamlPreferencesWithETD();
		initPreferencesGeneric(preferencesMan, ret, SamlPreferencesWithETD.ID);
		return ret;
	}
	
	public static void savePreferences(PreferencesManagement preferencesMan, SamlPreferencesWithETD preferences) 
			throws EngineException
	{
		savePreferencesGeneric(preferencesMan, preferences, SamlPreferencesWithETD.ID);
	}
	
	public static class SPETDSettings
	{
		private boolean generateETD = true;
		private long etdValidity = 1000*3600*24*14;
		private int maxProxyCount = -1;
		
		public boolean isGenerateETD()
		{
			return generateETD;
		}
		public void setGenerateETD(boolean generateETD)
		{
			this.generateETD = generateETD;
		}
		public long getEtdValidity()
		{
			return etdValidity;
		}
		public void setEtdValidity(long etdValidity)
		{
			this.etdValidity = etdValidity;
		}
		public int getMaxProxyCount()
		{
			return maxProxyCount;
		}
		public void setMaxProxyCount(int maxProxyCount)
		{
			this.maxProxyCount = maxProxyCount;
		}
	}
}
