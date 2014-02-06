/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.api.PreferencesManagement;
import pl.edu.icm.unity.server.authn.AuthenticatedEntity;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.types.JsonSerializable;
import pl.edu.icm.unity.types.basic.EntityParam;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * User's preferences for the IdentitiesTable .
 * 
 * @author P. Piernik
 */
public class IdentitiesTablePreferences implements JsonSerializable
{
	public static final String ID = IdentitiesTablePreferences.class.getName();
	protected final ObjectMapper mapper = Constants.MAPPER;
	private Map<String, ColumnSettings> colSettings;
	private boolean groupbyEntitiesSetting;

	public IdentitiesTablePreferences()
	{
		super();
		colSettings = new HashMap<String, IdentitiesTablePreferences.ColumnSettings>();
		groupbyEntitiesSetting = false;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		ObjectNode main = mapper.createObjectNode();
		serializeAll(main);
		try
		{
			return mapper.writeValueAsString(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}

	protected void serializeAll(ObjectNode main)
	{
		ObjectNode settingsN = main.with("colSettings");
		for (Map.Entry<String, ColumnSettings> entry : colSettings.entrySet())
			settingsN.put(entry.getKey(), serializeSingle(entry.getValue()));
		ObjectNode settingC = main.with("checkBoxSettings");
			   settingC.put("groupByEntities",groupbyEntitiesSetting);
	}

	protected ObjectNode serializeSingle(ColumnSettings what)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("width", what.width);
		main.put("order", what.order);
		main.put("collapsed", what.collapsed);
		return main;
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		if (json == null || json.equals(""))
		{
			colSettings = new HashMap<String, IdentitiesTablePreferences.ColumnSettings>();
			return;
		}
		try
		{
			ObjectNode main = mapper.readValue(json, ObjectNode.class);
			deserializeAll(main);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

	}

	protected void deserializeAll(ObjectNode main)
	{
		ObjectNode spSettingsNodeC = main.with("colSettings");
		Iterator<String> keys = spSettingsNodeC.fieldNames();
		for (String key; keys.hasNext();)
		{
			key = keys.next();
			colSettings.put(key, deserializeSingle(spSettingsNodeC.with(key)));
		}
		ObjectNode spSettingsNodeB = main.with("checkBoxSettings");;
		groupbyEntitiesSetting=spSettingsNodeB.get("groupByEntities").asBoolean();
		
	}

	protected ColumnSettings deserializeSingle(ObjectNode from)
	{
		ColumnSettings ret = new ColumnSettings();
		ret.setWidth(from.get("width").asInt());
		ret.setOrder(from.get("order").asInt());
		ret.setCollapsed(from.get("collapsed").asBoolean());

		return ret;
	}

	public static void initPreferencesGeneric(PreferencesManagement preferencesMan,
			JsonSerializable toInit, String id) throws EngineException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		EntityParam entity = new EntityParam(ae.getEntityId());
		String raw = preferencesMan.getPreference(entity, id);
		toInit.setSerializedConfiguration(raw);
	}

	public static void savePreferencesGeneric(PreferencesManagement preferencesMan,
			JsonSerializable preferences, String id) throws EngineException
	{
		AuthenticatedEntity ae = InvocationContext.getCurrent().getAuthenticatedEntity();
		EntityParam entity = new EntityParam(ae.getEntityId());
		preferencesMan.setPreference(entity, id, preferences.getSerializedConfiguration());
	}

	public static IdentitiesTablePreferences getPreferences(PreferencesManagement preferencesMan)
			throws EngineException
	{
		IdentitiesTablePreferences ret = new IdentitiesTablePreferences();
		initPreferencesGeneric(preferencesMan, ret, IdentitiesTablePreferences.ID);
		return ret;
	}

	public static void savePreferences(PreferencesManagement preferencesMan,
			IdentitiesTablePreferences preferences) throws EngineException
	{
		savePreferencesGeneric(preferencesMan, preferences, IdentitiesTablePreferences.ID);
	}

	public ColumnSettings getSingleColumnSettings(String columnName)
	{
		return colSettings.get(columnName);
	}

	public Map<String, ColumnSettings> getColumnSettings()
	{
		return colSettings;
	}

	public boolean getGroupByEntitiesSetting()
	{
		return groupbyEntitiesSetting;
	}

	public void setGroupByEntitiesSetting(boolean groupCheckboxSetting)
	{
		this.groupbyEntitiesSetting = groupCheckboxSetting;
	}

	public void addColumneSettings(String columnName, ColumnSettings settings)
	{
		colSettings.put(columnName, settings);
	}

	public static class ColumnSettings
	{
		private int width;
		private int order;
		private boolean collapsed;

		public int getWidth()
		{
			return width;
		}

		public void setWidth(int width)
		{
			this.width = width;
		}

		public int getOrder()
		{
			return order;
		}

		public void setOrder(int order)
		{
			this.order = order;
		}

		public boolean isCollapsed()
		{
			return collapsed;
		}

		public void setCollapsed(boolean collapsed)
		{
			this.collapsed = collapsed;
		}

	}

}
