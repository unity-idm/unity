/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Test;

import eu.unicore.util.configuration.PropertyMD;

public class UnityPropertiesHelperTest
{
	private static final Logger log = Logger.getLogger(UnityPropertiesHelperTest.class);
	
	public final static Map<String, PropertyMD> META = new HashMap<String, PropertyMD>();
	static 
	{
		META.put("multi", new PropertyMD().setList(false).setCanHaveSubkeys());
	}
	
	@Test
	public void listKeysAreCached()
	{
		Properties props = new Properties();
		props.setProperty("p.multi.foo.cached.2", "v2");
		props.setProperty("p.multi.foo.cached.1", "v1");
		props.setProperty("p.multi.bar.cached.2", "v4");
		props.setProperty("p.multi.bar.cached.1", "v3");
		props.setProperty("p.multi.tar.not.2", "v6");
		props.setProperty("p.multi.tar.not.1", "v5");
		
		UnityPropertiesHelper tested = new UnityPropertiesHelper("p.", props, 
				META, log);
		tested.addCachedPrefixes("p.multi\\.[^.]+\\.cached.");
		
		Set<String> sortedStringKeys = tested.getSortedStringKeys("p.multi.foo.cached.", true);
		assertThat(sortedStringKeys, hasItems("p.multi.foo.cached.1", "p.multi.foo.cached.2"));
		Set<String> sortedStringKeys2 = tested.getSortedStringKeys("p.multi.bar.cached.", true);
		assertThat(sortedStringKeys2, hasItems("p.multi.bar.cached.1", "p.multi.bar.cached.2"));
	}

	@Test
	public void addedPropertyIsCached()
	{
		Properties props = new Properties();
		props.setProperty("p.multi.foo.cached.2", "v2");
		props.setProperty("p.multi.foo.cached.1", "v1");
		
		UnityPropertiesHelper tested = new UnityPropertiesHelper("p.", props, 
				META, log);
		tested.addCachedPrefixes("p.multi\\.[^.]+\\.cached.");
		
		tested.setProperty("multi.foo.cached.3", "v");
		
		Set<String> sortedStringKeys = tested.getSortedStringKeys("p.multi.foo.cached.", true);
		assertThat(sortedStringKeys, hasItems("p.multi.foo.cached.1", 
				"p.multi.foo.cached.2", "p.multi.foo.cached.3"));
	}

	@Test
	public void notCachedAreReturned()
	{
		Properties props = new Properties();
		props.setProperty("p.multi.foo.cached.2", "v2");
		props.setProperty("p.multi.foo.cached.1", "v1");
		props.setProperty("p.multi.bar.cached.2", "v4");
		props.setProperty("p.multi.bar.cached.1", "v3");
		props.setProperty("p.multi.tar.not.2", "v6");
		props.setProperty("p.multi.tar.not.1", "v5");
		
		UnityPropertiesHelper tested = new UnityPropertiesHelper("p.", props, 
				META, log);
		tested.addCachedPrefixes("p.multi\\.[^.]+\\.cached.");
		
		Set<String> sortedStringKeys = tested.getSortedStringKeys("p.multi.tar.not.", true);
		assertThat(sortedStringKeys, hasItems("p.multi.tar.not.1", "p.multi.tar.not.2"));
	}

	@Test
	public void emptyCachedAreReturned()
	{
		Properties props = new Properties();
		
		UnityPropertiesHelper tested = new UnityPropertiesHelper("p.", props, 
				META, log);
		tested.addCachedPrefixes("p.multi\\.[^.]+\\.cached.");
		
		Set<String> sortedStringKeys = tested.getSortedStringKeys("p.multi.foo.cached.", true);
		assertThat(sortedStringKeys.size(), is(0));
	}
}
