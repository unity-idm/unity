/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.preferences;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.registries.AttributeSyntaxFactoriesRegistry;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.EntityParam;
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

	protected AttributeSyntaxFactoriesRegistry syntaxReg;
	private Map<String, SPSettings> spSettings = new HashMap<String, SamlPreferences.SPSettings>();
	
	public SamlPreferences(AttributeSyntaxFactoriesRegistry syntaxReg)
	{
		this.syntaxReg = syntaxReg;
	}

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
		ArrayNode hN = main.withArray("attrHidden");
		SimpleAttributeSerializer serializer = new SimpleAttributeSerializer(syntaxReg);
		for (Entry<String, Attribute<?>> entry : what.hiddenAttribtues.entrySet())
		{
			ObjectNode aEntry = hN.addObject();
			aEntry.put("name", entry.getKey());
			if (entry.getValue() != null)
			{
				ObjectNode jsonAttr = serializer.toJson(entry.getValue());
				aEntry.set("attribute", jsonAttr);
			}
		}
		
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
		
		
		if (from.has("hidden"))
		{
			handleLegacyHidden((ArrayNode) from.get("hidden"), ret);
		} else
		{
			Map<String, Attribute<?>> attributes = new HashMap<>();
			SimpleAttributeSerializer serializer = new SimpleAttributeSerializer(syntaxReg);
			ArrayNode attrsNode = (ArrayNode) from.get("attrHidden");
			for (int i=0; i<attrsNode.size(); i++)
			{
				ObjectNode attrNode = (ObjectNode) attrsNode.get(i);
				String name = attrNode.get("name").asText();
				if (attrNode.has("attribute"))
				{
					try
					{
						Attribute<Object> readA = serializer.fromJson(
								(ObjectNode) attrNode.get("attribute"));
						attributes.put(name, readA);
					} catch (IllegalTypeException e)
					{
						//ok, type is somehow missing, ignore this preference
					}
				} else
				{
					attributes.put(name, null);
				}
			}
			ret.setHiddenAttribtues(attributes);
		}
		
		
		if (from.has("selectedIdentity"))
			ret.setSelectedIdentity(from.get("selectedIdentity").asText());
		return ret;
	}

	protected void handleLegacyHidden(ArrayNode hiddenA, SPSettings ret)
	{
		Map<String, Attribute<?>> hidden = new HashMap<>();
		for (int i=0; i<hiddenA.size(); i++)
			hidden.put(hiddenA.get(i).asText(), null);
		ret.setHiddenAttribtues(hidden);
	}
	
	public static SamlPreferences getPreferences(PreferencesManagement preferencesMan,
			AttributeSyntaxFactoriesRegistry syntaxReg) throws EngineException
	{
		SamlPreferences ret = new SamlPreferences(syntaxReg);
		initPreferencesGeneric(preferencesMan, ret, SamlPreferences.ID);
		return ret;
	}

	public static SamlPreferences getPreferences(PreferencesManagement preferencesMan,
			AttributeSyntaxFactoriesRegistry syntaxReg, EntityParam entity) throws EngineException
	{
		SamlPreferences ret = new SamlPreferences(syntaxReg);
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
		private Map<String, Attribute<?>> hiddenAttribtues = new HashMap<>();
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
		public Map<String, Attribute<?>> getHiddenAttribtues()
		{
			Map<String, Attribute<?>> ret = new HashMap<>();
			ret.putAll(hiddenAttribtues);
			return ret;
		}
		public void setHiddenAttribtues(Map<String, Attribute<?>> attribtues)
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
	}
}
