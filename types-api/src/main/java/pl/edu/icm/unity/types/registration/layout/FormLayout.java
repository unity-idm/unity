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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

	public FormLayout(List<? extends FormElement> elements)
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
		return elements;
	}
	
	
	
	public void removeParametersWithIndexLargerThen(String type, int size)
	{
		Iterator<FormElement> iterator = getElements().iterator();
		while (iterator.hasNext())
		{
			FormElement formElement = iterator.next();
			if (formElement.getType().equals(type) && 
					((FormParameterElement)formElement).getIndex() >= size)
				iterator.remove();
		}
	}

	public void removeBasicElementIfPresent(String type)
	{
		for (int i = 0; i < getElements().size(); i++)
		{
			FormElement formElement = getElements().get(i);
			if (formElement.getType().equals(type))
			{
				getElements().remove(i);
				return;
			}
		}
	}

	public Set<String> getDefinedElements()
	{
		Set<String> definedElements = new HashSet<>();
		for (FormElement element: getElements())
		{
			String id = getIdOfElement(element);
			if (id != null)
				definedElements.add(id);
		}
		return definedElements;
	}
	
	public void checkLayoutElement(String key, Set<String> definedElements)
	{
		if (!definedElements.remove(key))
			throw new IllegalStateException("Form layout does not define position of " + key);
	}

	public void addParameterIfMissing(String type, int index, Set<String> definedElements)
	{
		if (!definedElements.contains(type + "_" + index))
			getElements().add(new FormParameterElement(type, index));
	}
	
	public void addBasicElementIfMissing(String type, Set<String> definedElements)
	{
		if (!definedElements.contains(type))
			getElements().add(new BasicFormElement(type));
	}
	
	public String getIdOfElement(FormElement element)
	{
		if (!element.isFormContentsRelated())
			return null;
		if (element instanceof FormParameterElement)
			return element.getType() + "_" + ((FormParameterElement)element).getIndex();
		return element.getType();
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
