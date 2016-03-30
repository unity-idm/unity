/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.types.registration.layout;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.registration.BaseForm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Describes a layout of a {@link BaseForm}: ordering of its elements and additional
 * metadata needed only for rendering (as section titles).
 * 
 * @author Krzysztof Benedyczak
 */
public class FormLayout
{
	public static final String IDENTITY = "IDENTITY";
	public static final String ATTRIBUTE = "ATTRIBUTE";
	public static final String GROUP = "GROUP";
	public static final String CREDENTIAL = "CREDENTIAL";
	public static final String AGREEMENT = "AGREEMENT";

	public static final String COMMENTS = "COMMENTS";

	public static final String CAPTION = "CAPTION";
	public static final String SEPARATOR = "SEPARATOR";
	
	private List<FormElement> elements;

	public FormLayout(List<FormElement> elements)
	{
		this.elements = new ArrayList<>(elements);
	}

	@JsonCreator
	private FormLayout(ObjectNode n)
	{
		this.elements = Constants.MAPPER.convertValue(n.get("elements"), 
				new TypeReference<List<FormElement>>() {});
	}

	@JsonValue
	public ObjectNode toJson()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.set("elements", Constants.MAPPER.valueToTree(elements));
		return root;
	}
	
	public List<FormElement> getElements()
	{
		return new ArrayList<>(elements);
	}
	
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((elements == null) ? 0 : elements.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FormLayout other = (FormLayout) obj;
		if (elements == null)
		{
			if (other.elements != null)
				return false;
		} else if (!elements.equals(other.elements))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "FormLayout [elements=" + elements + "]";
	}
}
