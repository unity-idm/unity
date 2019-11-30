/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.configtester;

import java.util.Properties;
import java.util.stream.Collectors;

public class PropertyTuner
{
	private String pfx;
	private Properties props;

	private PropertyTuner(String pfx, Properties props)
	{
		this.pfx = pfx;
		this.props = props;
	}

	public static PropertyTuner newTuner(String pfx, Properties props)
	{
		return new PropertyTuner(pfx, props);
	}

	public PropertyTuner rename(String from, String to)
	{
		Object removed = props.remove(pfx + from);
		props.put(pfx+to, removed);
		return this;
	}

	public PropertyTuner update(String key, String value)
	{
		props.put(pfx+key, value);
		return this;
	}
	
	public PropertyTuner remove(String key)
	{
		props.remove(pfx+key);
		return this;
	}
	
	public Properties get()
	{
		ConfigurationGenerator.log.info("Generated properties:\n{}", props.entrySet().stream()
				.sorted((e1, e2) -> e1.getKey().toString().compareTo(e2.getKey().toString()))
				.map(e -> e.toString())
				.collect(Collectors.joining("\n")));
		return props;
	}
}