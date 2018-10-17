/*
 * Copyright (c) 2016 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.registration.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.types.registration.BaseForm;

/**
 * Describes a layout of a {@link BaseForm}: ordering of its elements and additional
 * metadata needed only for rendering (as section titles).
 * 
 * @author Krzysztof Benedyczak
 */
public class FormLayout
{
	private List<FormElement> elements;

	public FormLayout(List<? extends FormElement> elements)
	{
		this.elements = new ArrayList<>(elements);
	}

	@JsonCreator
	public FormLayout(ObjectNode n)
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
		return elements;
	}
	
	@Override
	public String toString()
	{
		return "FormLayout [elements=" + elements + "]";
	}

	@Override
	public boolean equals(final Object other)
	{
		if (this == other)
			return true;
		if (!(other instanceof FormLayout))
			return false;
		FormLayout castOther = (FormLayout) other;
		return Objects.equals(elements, castOther.elements);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(elements);
	}
}
