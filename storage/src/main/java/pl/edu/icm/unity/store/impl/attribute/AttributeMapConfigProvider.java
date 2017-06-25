/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.impl.attribute;

import org.springframework.stereotype.Component;

import com.hazelcast.config.MapAttributeConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapIndexConfig;
import com.hazelcast.query.extractor.ValueCollector;
import com.hazelcast.query.extractor.ValueExtractor;

import pl.edu.icm.unity.store.hz.MapConfigProvider;
import pl.edu.icm.unity.store.types.StoredAttribute;


/**
 * Configures attributes map: indices on entity, group and name. Adds custom extractor of those attributes
 * from StoredAttribute.
 * @author K. Benedyczak
 */
@Component
public class AttributeMapConfigProvider implements MapConfigProvider
{
	@Override
	public MapConfig getMapConfig()
	{
		MapConfig mapConfig = new MapConfig();
		mapConfig.setName(AttributeHzStore.STORE_ID);

		configureIndexedAttribute(mapConfig, NameExtractor.class.getName(), "name");
		configureIndexedAttribute(mapConfig, GroupExtractor.class.getName(), "group");

		MapIndexConfig indexCfg = new MapIndexConfig("entityId", false);
		mapConfig.addMapIndexConfig(indexCfg);
		return mapConfig;
	}
	
	private void configureIndexedAttribute(MapConfig mapConfig, String extractor, String name)
	{
		MapAttributeConfig attributeCfg = new MapAttributeConfig(name, extractor);
		mapConfig.addMapAttributeConfig(attributeCfg);
		MapIndexConfig indexCfg = new MapIndexConfig(name, false);
		mapConfig.addMapIndexConfig(indexCfg);
	}
	
	
	public static class GroupExtractor extends ValueExtractor<StoredAttribute, Void>
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public void extract(StoredAttribute target, Void argument, ValueCollector collector)
		{
			collector.addObject(target.getAttribute().getGroupPath());
		}
	}

	public static class NameExtractor extends ValueExtractor<StoredAttribute, Void>
	{
		@SuppressWarnings({ "rawtypes", "unchecked" })
		@Override
		public void extract(StoredAttribute target, Void argument, ValueCollector collector)
		{
			collector.addObject(target.getAttribute().getName());
		}
	}
}
