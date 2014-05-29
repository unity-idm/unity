/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.types.basic.AttributeVisibility;

/**
 * AttributeExt wrapper, preparing it for JSON serialization. Type details are removed, 
 * values are properly serialized.
 * @author K. Benedyczak
 */
public class AttributeRepresentation
{
	private List<Object> values;
	private boolean direct;
	private String name;
	private String groupPath;
	private AttributeVisibility visibility;
	private String syntax;
	
	@SuppressWarnings("unchecked")
	public AttributeRepresentation(AttributeExt<?> orig)
	{
		@SuppressWarnings("rawtypes")
		AttributeValueSyntax syntax = orig.getAttributeSyntax();
		values = new ArrayList<Object>(orig.getValues().size());
		for (Object value: orig.getValues())
		{
			values.add(syntax.serializeSimple(value));
		}
		
		this.direct = orig.isDirect();
		this.name = orig.getName();
		this.groupPath = orig.getGroupPath();
		this.visibility = orig.getVisibility();
		this.syntax = orig.getAttributeSyntax().getValueSyntaxId();
	}

	public String getSyntax()
	{
		return syntax;
	}

	public List<Object> getValues()
	{
		return values;
	}

	public boolean isDirect()
	{
		return direct;
	}

	public String getName()
	{
		return name;
	}

	public String getGroupPath()
	{
		return groupPath;
	}

	public AttributeVisibility getVisibility()
	{
		return visibility;
	}
}