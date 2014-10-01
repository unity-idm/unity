/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.webui.common.provider.IdPPreferences;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;


/**
 * User's preferences for the SAML endpoints.
 * @author K. Benedyczak
 */
public class SamlPreferences extends IdPPreferences
{
	public static final String ID = SamlPreferences.class.getName();
	protected final ObjectMapper mapper = Constants.MAPPER;

	private Map<String, SPSettings> spSettings = new HashMap<String, SamlPreferences.SPSettings>();
	
	@Override
	protected void serializeAll(ObjectNode main)
	{
		ObjectNode settingsN = main.with("spSettings");
		for (Map.Entry<String, SPSettings> entry: spSettings.entrySet())
			settingsN.set(entry.getKey(), serializeSingle(entry.getValue()));
	}
	
	protected ObjectNode serializeSingle(SPSettings what)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("doNotAsk", what.doNotAsk);
		main.put("defaultAccept", what.defaultAccept);
		ArrayNode hN = main.withArray("hidden");
		for (String h: what.hiddenAttribtues)
			hN.add(h);
		if (what.selectedIdentity != null)
			main.put("selectedIdentity", what.selectedIdentity);
		return main;
	}
	
	@Override
	protected void deserializeAll(ObjectNode main)
	{
		ObjectNode spSettingsNode = main.with("spSettings");
		Iterator<String> keys = spSettingsNode.fieldNames();
		for (String key; keys.hasNext();)
		{
			key=keys.next();
			spSettings.put(key, deserializeSingle(spSettingsNode.with(key)));
		}
	}
	
	protected SPSettings deserializeSingle(ObjectNode from)
	{
		SPSettings ret = new SPSettings();
		ret.setDefaultAccept(from.get("defaultAccept").asBoolean());
		ret.setDoNotAsk(from.get("doNotAsk").asBoolean());
		Set<String> hidden = new HashSet<String>();
		ArrayNode hiddenA = from.withArray("hidden");
		for (int i=0; i<hiddenA.size(); i++)
			hidden.add(hiddenA.get(i).asText());
		ret.setHiddenAttribtues(hidden);
		if (from.has("selectedIdentity"))
			ret.setSelectedIdentity(from.get("selectedIdentity").asText());
		return ret;
	}

	public static SamlPreferences getPreferences(PreferencesManagement preferencesMan) throws EngineException
	{
		SamlPreferences ret = new SamlPreferences();
		initPreferencesGeneric(preferencesMan, ret, SamlPreferences.ID);
		return ret;
	}
	
	public static void savePreferences(PreferencesManagement preferencesMan, SamlPreferences preferences) 
			throws EngineException
	{
		savePreferencesGeneric(preferencesMan, preferences, SamlPreferences.ID);
	}
	
	/**
	 * @param sp
	 * @return settings for the given Service provider. Never null - if there are no preferences, then 
	 * the default settings are returned.
	 */
	public SPSettings getSPSettings(NameIDType spName)
	{
		String sp = getSPKey(spName);
		return getSPSettings(sp);
	}

	public SPSettings getSPSettings(String sp)
	{
		SPSettings ret = spSettings.get(sp);
		if (ret == null)
			ret = new SPSettings();
		return ret;
	}
	
	protected String getSPKey(NameIDType spName)
	{
		return SAMLConstants.NFORMAT_DN.equals(spName.getFormat()) ? 
				X500NameUtils.getComparableForm(spName.getStringValue()) : spName.getStringValue();
	}
	
	public Set<String> getKeys()
	{
		return spSettings.keySet();
	}
	
	public void setSPSettings(NameIDType spName, SPSettings settings)
	{
		setSPSettings(getSPKey(spName), settings);
	}
	
	public void setSPSettings(String sp, SPSettings settings)
	{
		spSettings.put(sp, settings);
	}
	
	public void removeSPSettings(NameIDType spName)
	{
		spSettings.remove(getSPKey(spName));
	}

	public void removeSPSettings(String sp)
	{
		spSettings.remove(sp);
	}
	
	public static class SPSettings
	{
		private boolean doNotAsk=false;
		private boolean defaultAccept=true;
		private Set<String> hiddenAttribtues = new HashSet<String>();
		private String selectedIdentity;

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
		public Set<String> getHiddenAttribtues()
		{
			Set<String> ret = new HashSet<String>();
			ret.addAll(hiddenAttribtues);
			return ret;
		}
		public void setHiddenAttribtues(Set<String> hiddenAttribtues)
		{
			this.hiddenAttribtues.clear();
			this.hiddenAttribtues.addAll(hiddenAttribtues);
		}
		public String getSelectedIdentity()
		{
			return selectedIdentity;
		}
		public void setSelectedIdentity(String selectedIdentity)
		{
			this.selectedIdentity = selectedIdentity;
		}
	}
}
