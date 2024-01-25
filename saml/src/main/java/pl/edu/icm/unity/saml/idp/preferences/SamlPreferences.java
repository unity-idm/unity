/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.samly2.SAMLConstants;
import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.entity.EntityParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.webui.idpcommon.IdPPreferences;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;

import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * User's preferences for the SAML endpoints.
 * @author K. Benedyczak
 */
public class SamlPreferences extends IdPPreferences
{
	public static final String ID = SamlPreferences.class.getName();
	protected final ObjectMapper mapper = Constants.MAPPER;

	private final Map<String, SPSettings> spSettings = new HashMap<>();
	
	@Override
	protected void serializeAll(ObjectNode main)
	{
		ObjectNode settingsN = main.withObjectProperty("spSettings");
		for (Map.Entry<String, SPSettings> entry: spSettings.entrySet())
			settingsN.set(entry.getKey(), serializeSingle(entry.getValue()));
	}
	
	protected ObjectNode serializeSingle(SPSettings what)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("doNotAsk", what.doNotAsk);
		main.put("defaultAccept", what.defaultAccept);
		ArrayNode hN = main.withArray("attrHidden");
		for (Entry<String, Attribute> entry : what.hiddenAttribtues.entrySet())
		{
			ObjectNode aEntry = hN.addObject();
			aEntry.put("name", entry.getKey());
			if (entry.getValue() != null)
			{
				aEntry.putPOJO("attribute", entry.getValue());
			}
		}
		
		if (what.selectedIdentity != null)
			main.put("selectedIdentity", what.selectedIdentity);
		
		if (what.timestamp != null)
		{
			main.put("timestamp", what.timestamp.toEpochMilli());
		}
		
		
		return main;
	}
	
	@Override
	protected void deserializeAll(ObjectNode main)
	{
		ObjectNode spSettingsNode = main.withObjectProperty("spSettings");
		Iterator<String> keys = spSettingsNode.fieldNames();
		for (String key; keys.hasNext();)
		{
			key=keys.next();
			spSettings.put(key, deserializeSingle(spSettingsNode.withObjectProperty(key)));
		}
	}
	
	protected SPSettings deserializeSingle(ObjectNode from)
	{
		SPSettings ret = new SPSettings();
		ret.setDefaultAccept(from.get("defaultAccept").asBoolean());
		ret.setDoNotAsk(from.get("doNotAsk").asBoolean());
		
		
		Map<String, Attribute> attributes = new HashMap<>();
		ArrayNode attrsNode = (ArrayNode) from.get("attrHidden");
		for (int i=0; i<attrsNode.size(); i++)
		{
			ObjectNode attrNode = (ObjectNode) attrsNode.get(i);
			String name = attrNode.get("name").asText();
			if (attrNode.has("attribute"))
			{
				Attribute readA = Constants.MAPPER.convertValue(attrNode.get("attribute"), 
						Attribute.class);
				attributes.put(name, readA);
			} else
			{
				attributes.put(name, null);
			}
		}
		ret.setHiddenAttribtues(attributes);
		
		
		if (from.has("selectedIdentity"))
			ret.setSelectedIdentity(from.get("selectedIdentity").asText());
		
		if (from.has("timestamp"))
		{
			ret.setTimestamp(Instant.ofEpochMilli(from.get("timestamp").asLong()));
		}
		
		return ret;
	}

	public static SamlPreferences getPreferences(PreferencesManagement preferencesMan)
	{
		SamlPreferences ret = new SamlPreferences();
		initPreferencesGeneric(preferencesMan, ret, SamlPreferences.ID);
		return ret;
	}

	public static SamlPreferences getPreferences(PreferencesManagement preferencesMan, EntityParam entity)
	{
		SamlPreferences ret = new SamlPreferences();
		initPreferencesGeneric(preferencesMan, ret, SamlPreferences.ID, entity);
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

	/**
	 * @param sp
	 * @return settings for the given service provider. If no preferences are defined for he given provider 
	 * then first the default as set by user preferences are returned and if those are also unset then
	 * hardcoded defaults are used. 
	 */
	public SPSettings getSPSettings(String sp)
	{
		SPSettings ret = spSettings.get(sp);
		if (ret == null)
			ret = spSettings.get("");
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
	
	/**
	 * @param sp Use empty string as a key to set default preferences
	 * @param settings
	 */
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
		private Map<String, Attribute> hiddenAttribtues = new HashMap<>();
		private String selectedIdentity;
		private Instant timestamp;


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
		public Map<String, Attribute> getHiddenAttribtues()
		{
			Map<String, Attribute> ret = new HashMap<>();
			ret.putAll(hiddenAttribtues);
			return ret;
		}
		public void setHiddenAttribtues(Map<String, Attribute> attribtues)
		{
			this.hiddenAttribtues.clear();
			this.hiddenAttribtues.putAll(attribtues);
		}
		public String getSelectedIdentity()
		{
			return selectedIdentity;
		}
		public void setSelectedIdentity(String selectedIdentity)
		{
			this.selectedIdentity = selectedIdentity;
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
