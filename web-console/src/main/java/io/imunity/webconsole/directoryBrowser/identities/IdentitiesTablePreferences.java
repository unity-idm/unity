/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.identities;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.Constants;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.engine.api.PreferencesManagement;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;

/**
 * User's preferences for the IdentitiesTable .
 * 
 * @author P. Piernik
 */
class IdentitiesTablePreferences
{
	static final String ID = IdentitiesTablePreferences.class.getName();
	protected final ObjectMapper mapper = Constants.MAPPER;
	private Map<String, ColumnSettings> colSettings = new HashMap<>();
	private boolean groupbyEntitiesSetting = true;
	private boolean showTargetedSetting = false;

	IdentitiesTablePreferences()
	{
	}

	@JsonCreator
	IdentitiesTablePreferences(ObjectNode main) throws InternalException
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
	
	static IdentitiesTablePreferences getPreferences(PreferencesManagement preferencesMan)
			throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam entity = new EntityParam(ae.getEntityId());
		String raw = preferencesMan.getPreference(entity, ID);
		return raw == null ? new IdentitiesTablePreferences() : 
			new IdentitiesTablePreferences(JsonUtil.parse(raw));
	}
	
	@JsonValue
	ObjectNode serializeToJson()
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

	void savePreferences(PreferencesManagement preferencesMan) throws EngineException
	{
		LoginSession ae = InvocationContext.getCurrent().getLoginSession();
		EntityParam entity = new EntityParam(ae.getEntityId());
		preferencesMan.setPreference(entity, ID, JsonUtil.toJsonString(serializeToJson()));
	}

	ColumnSettings getSingleColumnSettings(String columnName)
	{
		return colSettings.get(columnName);
	}

	Map<String, ColumnSettings> getColumnSettings()
	{
		return colSettings;
	}

	boolean getGroupByEntitiesSetting()
	{
		return groupbyEntitiesSetting;
	}

	void setGroupByEntitiesSetting(boolean groupCheckboxSetting)
	{
		this.groupbyEntitiesSetting = groupCheckboxSetting;
	}
	
	boolean getShowTargetedSetting()
	{
		return showTargetedSetting;
	}

	void setShowTargetedSetting(boolean showTargetedSetting)
	{
		this.showTargetedSetting = showTargetedSetting;
	}


	void addColumneSettings(String columnName, ColumnSettings settings)
	{
		colSettings.put(columnName, settings);
	}

	static class ColumnSettings
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
