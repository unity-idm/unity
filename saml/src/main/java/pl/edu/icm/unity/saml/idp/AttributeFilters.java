/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.saml.idp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

import pl.edu.icm.unity.types.basic.Attribute;

/**
 * Filters given attributes set using a configured filter.
 * @author K. Benedyczak
 */
public class AttributeFilters
{
	private AttributeFilter defaultFilter;
	private List<AttributeFilter> filters;
	private Map<String, AttributeFilter> cache;
	
	public AttributeFilters(SamlProperties config)
	{
		cache = new WeakHashMap<String, AttributeFilters.AttributeFilter>();
		Set<String> keys = config.getStructuredListKeys(SamlProperties.ATTRIBUTE_FILTER);
		filters = new ArrayList<AttributeFilters.AttributeFilter>(keys.size());
		for (String key: keys)
		{
			String target = config.getValue(key+SamlProperties.ATTRIBUTE_FILTER_TARGET);
			List<String> excludes = config.getListOfValues(key+SamlProperties.ATTRIBUTE_FILTER_EXCLUDE);
			List<String> includes = config.getListOfValues(key+SamlProperties.ATTRIBUTE_FILTER_INCLUDE);
			AttributeFilter filter = new AttributeFilter(target, includes, excludes);
			if (target == null)
			{
				defaultFilter = filter;
			} else
			{
				filters.add(filter);
			}
		}
		if (defaultFilter == null)
			defaultFilter = new AttributeFilter(null, new ArrayList<String>(0), 
					new ArrayList<String>(0));
	}
	
	public void filter(Collection<? extends Attribute<?>> attributes, String requester)
	{
		AttributeFilter filter = findFilter(requester);

		Set<String> included = filter.getIncluded();
		if (!included.isEmpty())
		{
			Iterator<? extends Attribute<?>> it = attributes.iterator();
			while(it.hasNext())
			{
				Attribute<?> a = it.next();
				if (!included.contains(a.getName()))
					it.remove();
			}
		}
			
		Set<String> excluded = filter.getExcluded();
		Iterator<? extends Attribute<?>> it = attributes.iterator();
		while(it.hasNext())
		{
			Attribute<?> a = it.next();
			if (excluded.contains(a.getName()))
				it.remove();
		}
	}
	
	private AttributeFilter findFilter(String requester)
	{
		AttributeFilter ret = cache.get(requester);
		if (ret != null)
			return ret;
		for (AttributeFilter filter: filters)
		{
			if (filter.matches(requester))
			{
				cache.put(requester, filter);
				return filter;
			}
		}
		cache.put(requester, defaultFilter);
		return defaultFilter;
	}
	
	
	private static class AttributeFilter
	{
		private Pattern clientPattern;
		private Set<String> included;
		private Set<String> excluded;
		
		public AttributeFilter(String clientPattern, List<String> included,
				List<String> excluded)
		{
			super();
			this.clientPattern = clientPattern == null ? null : Pattern.compile(clientPattern);
			this.included = new HashSet<String>();
			this.included.addAll(included);
			this.excluded = new HashSet<String>();
			this.excluded.addAll(excluded);
		}
		
		public boolean matches(String what)
		{
			if (clientPattern == null)
				return true;
			return clientPattern.matcher(what).matches();
		}

		public Set<String> getIncluded()
		{
			return included;
		}

		public Set<String> getExcluded()
		{
			return excluded;
		}
	}
}
