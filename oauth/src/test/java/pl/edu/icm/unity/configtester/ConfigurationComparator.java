/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.configtester;

import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import eu.unicore.util.configuration.PropertyMD;
import pl.edu.icm.unity.exceptions.InternalException;

public class ConfigurationComparator
{
	private String pfx;
	private Map<String, PropertyMD> propertiesMD;
	private Set<String> ignoredSuperflous = new HashSet<>();
	private Set<String> ignoredMissing = new HashSet<>();
	
	public ConfigurationComparator(String pfx, Map<String, PropertyMD> propertiesMD)
	{
		this.pfx = pfx;
		this.propertiesMD = new HashMap<>(propertiesMD);
	}

	public static ConfigurationComparator createComparator(String pfx, Map<String, PropertyMD> propertiesMD)
	{
		return new ConfigurationComparator(pfx, propertiesMD);
	}

	public ConfigurationComparator addStructuredEntryMeta(String entryPfx, Map<String, PropertyMD> propertiesMD)
	{
		propertiesMD.forEach((key, val) -> this.propertiesMD.put(entryPfx + key, val));
		return this;
	}

	
	public ConfigurationComparator ignoringSuperflous(String... keys)
	{
		for (String key: keys)
			ignoredSuperflous.add(pfx + key);
		return this;
	}

	public ConfigurationComparator ignoringMissing(String... keys)
	{
		for (String key: keys)
			ignoredMissing.add(pfx + key);
		return this;
	}
	
	public void checkMatching(Properties actual, Properties expected)
	{
		List<Tuple> superflous = new ArrayList<>();
		actual.entrySet().forEach(e -> 
		{
			if (ignoredSuperflous.contains(e.getKey()))
				return;
			if (!expected.containsKey(e.getKey()) && isNotDefault((String)e.getKey(), (String)e.getValue()))
				superflous.add(new Tuple((String)e.getKey(), (String)e.getValue()));
		});
		List<Tuple> missing = new ArrayList<>();
		expected.entrySet().forEach(e -> 
		{
			if (ignoredMissing.contains(e.getKey()))
				return;
			if (!actual.containsKey(e.getKey())&& isNotDefault((String)e.getKey(), (String)e.getValue()))
				missing.add(new Tuple((String)e.getKey(), (String)e.getValue()));
		});

		List<Tuple> wrongValue = new ArrayList<>();
		expected.entrySet().forEach(e -> 
		{
			if (actual.containsKey(e.getKey()) && ! actual.get(e.getKey()).equals(e.getValue()))
				wrongValue.add(new Tuple((String)e.getKey(), actual.get(e.getKey()) + " should be " + (String)e.getValue()));
		});
		
		if (!superflous.isEmpty() || !missing.isEmpty() || !wrongValue.isEmpty())
			fail(getDiff(superflous, missing, wrongValue));
		
	}
	
	private boolean isNotDefault(String key, String value)
	{
		PropertyMD meta = propertiesMD.get(key.substring(pfx.length()));
		return meta == null || !value.equals(meta.getDefault());
	}

	private static String getDiff(List<Tuple> superflous, List<Tuple> missing, List<Tuple> wrongValue)
	{
		return "\n******* Superflous:\n" + superflous.stream().map(t -> t.toString()).collect(Collectors.joining("\n")) +
			"\n****** Missing:\n" + missing.stream().map(t -> t.toString()).collect(Collectors.joining("\n")) + 
			"\n****** Wrong Value:\n" + wrongValue.stream().map(t -> t.toString()).collect(Collectors.joining("\n"));
	}
	
	
	
	
	public static String getAsString(Properties properties)
	{
		StringWriter writer = new StringWriter();
		try
		{
			properties.store(writer, "");
		} catch (IOException e)
		{
			throw new InternalException("Can not save properties to string");
		}
		return writer.getBuffer().toString();
	}
	
	public static PropertyTuner fromString(String properties, String pfx)
	{
		Properties props = new Properties();
		try
		{
			props.load(new StringReader(properties));
			return PropertyTuner.newTuner(pfx, props);
		} catch (IOException e)
		{
			throw new InternalException("Can not load properties from string");
		}
	}
}
