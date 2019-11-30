/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.configtester;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import org.apache.logging.log4j.Logger;

import eu.unicore.util.configuration.PropertyMD;
import eu.unicore.util.configuration.PropertyMD.Type;
import pl.edu.icm.unity.base.utils.Log;

public class ConfigurationGenerator
{
	static final Logger log = Log.getLogger(Log.U_SERVER, ConfigurationGenerator.class);
	
	public static PropertyTuner generateMinimalDefault(String pfx, Map<String, PropertyMD> propertiesMD)
	{
		Predicate<PropertyMD> exclude = meta -> 
			meta.isDeprecated() || meta.getDefault() != null || !meta.isMandatory() || meta.isHidden();
		Properties props = generateFiltering(pfx, propertiesMD, exclude, ConfigurationGenerator::generateValue);
		return PropertyTuner.newTuner(pfx, props);
	}

	public static PropertyTuner generateMinimalWithDefaults(String pfx, Map<String, PropertyMD> propertiesMD)
	{
		Predicate<PropertyMD> exclude = meta -> meta.isDeprecated() || 
				(!meta.isMandatory() && meta.getDefault() == null);
		Properties props = generateFiltering(pfx, propertiesMD, exclude, ConfigurationGenerator::generateWithDefault);
		return PropertyTuner.newTuner(pfx, props);
	}

	public static PropertyTuner generateCompleteWithNonDefaults(String pfx, Map<String, PropertyMD> propertiesMD)
	{
		Predicate<PropertyMD> exclude = meta -> meta.isDeprecated();
		Properties props = generateFiltering(pfx, propertiesMD, exclude, ConfigurationGenerator::generateWithNonDefault);
		return PropertyTuner.newTuner(pfx, props);
	}
	
	private static Properties generateFiltering(String pfx, Map<String, PropertyMD> propertiesMD, 
			Predicate<PropertyMD> filter, BiFunction<String, PropertyMD, Tuple> generator)
	{
		Properties props = new Properties();
		for (Entry<String, PropertyMD> entry : propertiesMD.entrySet())
		{
			if (filter.test(entry.getValue()))
				continue;
			Tuple generated = generator.apply(entry.getKey(), entry.getValue());
			if (generated != null)
				props.setProperty(pfx + generated.key, generated.value);
		}
		return props;
	}
	
	private static Tuple generateWithDefault(String key, PropertyMD metadata)
	{
		return metadata.getDefault() != null ? 
				generateDefaultValue(key, metadata) : 
				generateValue(key, metadata);
	}

	private static Tuple generateWithNonDefault(String key, PropertyMD metadata)
	{
		return metadata.getDefault() != null ? 
				generateNonDefaultValue(key, metadata) : 
				generateValue(key, metadata);
	}
	
	private static Tuple generateDefaultValue(String key, PropertyMD metadata)
	{
		if (metadata.isStructuredListEntry())
			key = metadata.getStructuredListEntryId() + "1." + key;
		return new Tuple(key, metadata.getDefault());
	}

	private static Tuple generateNonDefaultValue(String key, PropertyMD metadata)
	{
		Type type = metadata.getType();
		
		if (metadata.isStructuredListEntry())
			key = metadata.getStructuredListEntryId() + "1." + key;
		String def = metadata.getDefault();
		
		switch (type)
		{
		case BOOLEAN:
			return new Tuple(key, def.equals("false") ? "true" : "false");
		case CLASS:
			return new Tuple(key, def + "Foo");
		case ENUM:
			return new Tuple(key, metadata.getEnumTypeInstance().name());
		case FLOAT:
			return new Tuple(key, Double.toString(Double.parseDouble(def)+1));
		case INT:
			return new Tuple(key, Integer.toString(Integer.parseInt(def)+1));
		case LONG:
			return new Tuple(key, Long.toString(Long.parseLong(def)+1));
		case PATH:
			return new Tuple(key, def + "/tmp");
		case STRING:
			return new Tuple(key, def + "foo");
		case LIST:
			return null;
		case STRUCTURED_LIST:
			return null;
		}
		throw new IllegalStateException("Unknown entry type " + type);
	}
	
	private static Tuple generateValue(String key, PropertyMD metadata)
	{
		Type type = metadata.getType();
		
		if (metadata.isStructuredListEntry())
			key = metadata.getStructuredListEntryId() + "1." + key;
		
		switch (type)
		{
		case BOOLEAN:
			return new Tuple(key, "false");
		case CLASS:
			return new Tuple(key, metadata.getBaseClass().toString());
		case ENUM:
			return new Tuple(key, metadata.getEnumTypeInstance().name());
		case FLOAT:
			return new Tuple(key, Double.toString(metadata.getMinFloat()));
		case INT:
			return new Tuple(key, Integer.toString((int)metadata.getMin()));
		case LONG:
			return new Tuple(key, Long.toString(metadata.getMin()));
		case PATH:
			return new Tuple(key, "/tmp");
		case STRING:
			return new Tuple(key, "foo");
		case LIST:
			return new Tuple(key + "1", "value");
		case STRUCTURED_LIST:
			return null;
		}
		throw new IllegalStateException("Unknown entry type " + type);
	}
}
