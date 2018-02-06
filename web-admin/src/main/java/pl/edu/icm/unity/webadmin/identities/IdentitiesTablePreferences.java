/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.identities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * User's preferences for the IdentitiesTable .
 * 
 * @author P. Piernik
 */
public class IdentitiesTablePreferences
{
	public static final String ID = IdentitiesTablePreferences.class.getName();
	protected final ObjectMapper mapper = Constants.MAPPER;
	private Map<String, ColumnSettings> colSettings = new HashMap<>();
	private boolean groupbyEntitiesSetting = true;
	private boolean showTargetedSetting = false;

	public IdentitiesTablePreferences()
	{
	}

	@JsonCreator
	public IdentitiesTablePreferences(ObjectNode main) throws InternalException
	{
		ObjectNode spSettingsNodeC = main.with("colSettings");
		Iterator<String> keys = spSettingsNodeC.fieldNames();
		for (String key; keys.hasNext();)
		{
			key = keys.next();
			colSettings.put(key, deserializeSingle(spSettingsNodeC.with(key)));
		}
		ObjectNode spSettingsNodeB = main.with("checkBoxSettings");
		groupbyEntitiesSetting = spSettingsNodeB.get("groupByEntities").asBoolean();
		if (spSettingsNodeB.has("showTargeted"))
			showTargetedSetting = spSettingsNodeB.get("showTargeted").asBoolean();
		else
			showTargetedSetting = false;
	}
	
	public static IdentitiesTablePreferences getPreferences(PreferencesManagement preferencesMan)
			throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam entity = new EntityParam(ae.getEntityId());
		String raw = preferencesMan.getPreference(entity, ID);
		return raw == null ? new IdentitiesTablePreferences() : 
			new IdentitiesTablePreferences(JsonUtil.parse(raw));
	}
	
	@JsonValue
	public ObjectNode serializeToJson()
	{
		ObjectNode main = mapper.createObjectNode();
		ObjectNode settingsN = main.with("colSettings");
		for (Map.Entry<String, ColumnSettings> entry : colSettings.entrySet())
			settingsN.set(entry.getKey(), serializeSingle(entry.getValue()));
		ObjectNode settingC = main.with("checkBoxSettings");
		settingC.put("groupByEntities", groupbyEntitiesSetting);
		settingC.put("showTargeted", showTargetedSetting);
		return main;
	}

	protected ObjectNode serializeSingle(ColumnSettings what)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("width", what.width);
		main.put("order", what.order);
		main.put("collapsed", what.collapsed);
		return main;
	}

	protected ColumnSettings deserializeSingle(ObjectNode from)
	{
		ColumnSettings ret = new ColumnSettings();
		ret.setWidth(from.get("width").asDouble());
		ret.setOrder(from.get("order").asInt());
		ret.setCollapsed(from.get("collapsed").asBoolean());

		return ret;
	}

	public void savePreferences(PreferencesManagement preferencesMan) throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam entity = new EntityParam(ae.getEntityId());
		preferencesMan.setPreference(entity, ID, JsonUtil.toJsonString(serializeToJson()));
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
	
	public boolean getShowTargetedSetting()
	{
		return showTargetedSetting;
	}

	public void setShowTargetedSetting(boolean showTargetedSetting)
	{
		this.showTargetedSetting = showTargetedSetting;
	}


	public void addColumneSettings(String columnName, ColumnSettings settings)
	{
		colSettings.put(columnName, settings);
	}

	public static class ColumnSettings
	{
		private double width;
		private int order;
		private boolean collapsed;

		public double getWidth()
		{
			return width;
		}

		public void setWidth(double width)
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
