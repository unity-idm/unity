/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.translation.out;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.DynamicAttribute;
import pl.edu.icm.unity.types.basic.IdentityParam;

/**
 * Result of output translation. Set of identities and attributes. This class is mutable: actions modify the contents
 * one by one.
 * @author K. Benedyczak
 */
public class TranslationResult
{
	private Collection<DynamicAttribute> attributes = new HashSet<>();
	private Collection<IdentityParam> identities = new ArrayList<>();

	private Collection<Attribute> attributesToPersist = new HashSet<>();
	private Collection<IdentityParam> identitiesToPersist = new ArrayList<>();
	
	private String redirectURL;
	
	public Collection<DynamicAttribute> getAttributes()
	{
		return attributes;
	}
	
	public Collection<IdentityParam> getIdentities()
	{
		return identities;
	}

	public Collection<Attribute> getAttributesToPersist()
	{
		return attributesToPersist;
	}

	public Collection<IdentityParam> getIdentitiesToPersist()
	{
		return identitiesToPersist;
	}

	public String getRedirectURL()
	{
		return redirectURL;
	}

	public void setRedirectURL(String redirectURL)
	{
		this.redirectURL = redirectURL;
	}

	public boolean removeAttributesByName(String name)
	{
		boolean res = false;
		Set<DynamicAttribute> copy = new HashSet<>(attributes);
		for (DynamicAttribute existing : copy)
		{
			if (existing.getAttribute().getName().equals(name))
			{
				attributes.remove(existing);
				res = true;
			}
		}
		return res;
	}

	public Set<String> removeAttributesByMatch(Pattern attrPattern)
	{
		Set<String> res = new HashSet<>();
		Set<DynamicAttribute> copy = new HashSet<>(attributes);
		for (DynamicAttribute existing : copy)
		{
			if (attrPattern.matcher(existing.getAttribute().getName()).matches())
			{
				attributes.remove(existing);
				res.add(existing.getAttribute().getName());
			}
		}
		return res;
	}
	
	public boolean removeAttributesToPersistByName(String name)
	{
		boolean res = false;
		Set<Attribute> copy = new HashSet<>(attributesToPersist);
		for (Attribute existing : copy)
		{
			if (existing.getName().equals(name))
			{
				attributesToPersist.remove(existing);
				res = true;
			}
		}
		return res;
	}

	public Set<String> removeAttributesToPersistByMatch(Pattern attrPattern)
	{
		Set<String> res = new HashSet<>();
		Set<Attribute> copy = new HashSet<>(attributesToPersist);
		for (Attribute existing : copy)
		{
			if (attrPattern.matcher(existing.getName()).matches())
			{
				attributesToPersist.remove(existing);
				res.add(existing.getName());
			}
		}
		return res;
	}

	
	public boolean removeIdentityByType(String type)
	{
		return remomveIdentityByType(identities, type);
	}

	public boolean removeIdentityToPersistByType(String type)
	{
		return remomveIdentityByType(identitiesToPersist, type);
	}

	public Set<IdentityParam> removeIdentityToPersistByTypeAndValueMatch(String type,
			Pattern idValueRegexp)
	{
		return removeIdentityByTypeAndValueMatch(identitiesToPersist, type, idValueRegexp);
	}

	public Set<IdentityParam> removeIdentityByTypeAndValueMatch(String type,
			Pattern idValueRegexp)
	{
		return removeIdentityByTypeAndValueMatch(identities, type, idValueRegexp);
	}

	private boolean remomveIdentityByType(Collection<IdentityParam> from, String type)
	{
		boolean res = false;
		Collection<IdentityParam> copy = new ArrayList<>(from);
		for (IdentityParam id : copy)
		{
			if (id.getTypeId().equals(type))
			{
				from.remove(id);
				res = true;
			}
		}
		return res;
	}

	private Set<IdentityParam> removeIdentityByTypeAndValueMatch(Collection<IdentityParam> from,
			String type, Pattern idValueRegexp)
	{
		Set<IdentityParam> res = new HashSet<>();
		Set<IdentityParam> copy = new HashSet<IdentityParam>(from);
		for (IdentityParam i : copy)
		{
			if ((type == null || i.getTypeId().equals(type)) && (idValueRegexp == null
					|| idValueRegexp.matcher(i.getValue()).matches()))
			{
				from.remove(i);
				res.add(i);
			}
		}
		return res;
	}
}
