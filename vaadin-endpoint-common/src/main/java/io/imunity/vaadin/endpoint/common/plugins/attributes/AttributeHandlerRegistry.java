/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.types.basic.AttributeType;

import java.util.*;


@Component
public class AttributeHandlerRegistry
{
	private static final int SHORT_TEXT_VALUE_LENGHT = 100;
	private static final int PREVIEW_IMAGE_SIZE = 29;
		
	private MessageSource msg;
	private AttributeTypeSupport aTypeSupport;

	private Map<String, WebAttributeHandlerFactory> factoriesByType = new HashMap<>();

	@Autowired
	public AttributeHandlerRegistry(List<WebAttributeHandlerFactory> factories, MessageSource msg,
	                                AttributeTypeSupport aTypeSupport)
	{
		this.msg = msg;
		this.aTypeSupport = aTypeSupport;
		for (WebAttributeHandlerFactory factory : factories)
			factoriesByType.put(factory.getSupportedSyntaxId(), factory);
	}

	/**
	 * Returns web attribute handler for the given attribute, falling back
	 * to string attribute handler if there is no system defined attribute
	 * for the argument attribute name (can happen for dynamic attributes
	 * created by output profiles).
	 * 
	 * @param attribute
	 * @return attribute handler
	 */
	public WebAttributeHandler getHandlerWithStringFallback(Attribute attribute)
	{
		AttributeValueSyntax<?> syntax = getSyntaxWithStringFallback(attribute);
		return getHandler(syntax);
	}

	/**
	 * Returns web attribute handler for the given attribute type, falling
	 * back to string attribute handler if there is no system defined
	 * attribute type for the argument attribute type (can happen for
	 * dynamic attributes created by output profiles).
	 * 
	 * @param attributeType
	 * @return attribute handler
	 */
	public WebAttributeHandler getHandlerWithStringFallback(AttributeType attributeType)
	{
		AttributeValueSyntax<?> syntax = getSyntaxWithStringFallback(attributeType);
		return getHandler(syntax);
	}

	/**
	 * @param attribute
	 * @return syntax for the given attribute, or String attribute syntax,
	 *         if the attribute type is not defined.
	 */
	public AttributeValueSyntax<?> getSyntaxWithStringFallback(Attribute attribute)
	{
		try
		{
			return aTypeSupport.getSyntax(attribute);
		} catch (IllegalArgumentException e)
		{
			return new StringAttributeSyntax();
		}
	}

	/**
	 * @param attributeType
	 * @return syntax for the given attribute type, or String attribute
	 *         syntax, if the attribute type is not defined.
	 */
	public AttributeValueSyntax<?> getSyntaxWithStringFallback(AttributeType attributeType)
	{
		try
		{
			return aTypeSupport.getSyntax(attributeType);
		} catch (IllegalArgumentException e)
		{
			return new StringAttributeSyntax();
		}
	}

	public WebAttributeHandler getHandler(AttributeValueSyntax<?> syntax)
	{
		WebAttributeHandlerFactory factory = factoriesByType.get(syntax.getValueSyntaxId());
		if (factory == null)
			throw new IllegalArgumentException(
					"Syntax " + syntax.getValueSyntaxId() + " has no handler factory registered");
		return factory.createInstance(syntax);
	}

	/**
	 * 
	 * @param syntaxId
	 * @param syntax
	 *                syntax to be edited or null if an empty editor should
	 *                be returned.
	 * @return
	 */
	public AttributeSyntaxEditor<?> getSyntaxEditor(String syntaxId, AttributeValueSyntax<?> syntax)
	{
		WebAttributeHandlerFactory factory = factoriesByType.get(syntaxId);
		if (factory == null)
			throw new IllegalArgumentException("Syntax " + syntaxId + " has no handler factory registered");
		return factory.getSyntaxEditorComponent(syntax);
	}

	public VerticalLayout getRepresentation(Attribute attribute, AttributeViewerContext context)
	{
		VerticalLayout vl = new VerticalLayout();
		vl.setMargin(false);
		vl.setSpacing(false);
		AttributeValueSyntax<?> syntax = getSyntaxWithStringFallback(attribute);
		StringBuilder main = new StringBuilder(attribute.getName());
		if (attribute.getRemoteIdp() != null)
		{
			String idpInfo = msg.getMessage("IdentityFormatter.remoteInfo", attribute.getRemoteIdp());
			main.append(" [").append(idpInfo).append("]");
		}
		vl.add(new Label(main.toString()));
		VerticalLayout indentedValues = new VerticalLayout();
		indentedValues.setSpacing(true);
		WebAttributeHandler handler = getHandler(syntax);
		for (String value : attribute.getValues())
		{
			com.vaadin.flow.component.Component valueRep = handler.getRepresentation(value,
					context);
			indentedValues.add(valueRep);
		}
		vl.add(indentedValues);
		return vl;
	}

	public Div getSimpleRepresentation(Attribute attribute, AttributeViewerContext context)
	{
		Div vl = new Div();
		AttributeValueSyntax<?> syntax = getSyntaxWithStringFallback(attribute);
		WebAttributeHandler handler = getHandler(syntax);
		for (String value : attribute.getValues())
		{
			com.vaadin.flow.component.Component valueRep = handler.getRepresentation(value, context);
			vl.add(valueRep);
		}
		return vl;
	}

	public Set<String> getSupportedSyntaxes()
	{
		return new HashSet<>(factoriesByType.keySet());
	}

	public AttributeTypeSupport getaTypeSupport()
	{
		return aTypeSupport;
	}

	public String getSimplifiedAttributeRepresentation(Attribute attribute)
	{
		return getSimplifiedAttributeRepresentation(attribute, attribute.getName());
	}

	/**
	 * Returns a string representing the attribute. The returned format
	 * contains the attribute name and the values.
	 * 
	 * @param attribute
	 * @return
	 */
	private String getSimplifiedAttributeRepresentation(Attribute attribute, String displayedName)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(displayedName);
		List<?> values = attribute.getValues();
		if (!values.isEmpty())
		{
			sb.append(": ");
			sb.append(getSimplifiedAttributeValuesRepresentation(attribute));
		}
		return sb.toString();
	}

	/**
	 * Returns a string representing the attributes values.
	 * 
	 * @param attribute
	 * @return
	 */
	private String getSimplifiedAttributeValuesRepresentation(Attribute attribute)
	{
		WebAttributeHandler handler = getHandlerWithStringFallback(attribute);
		return getSimplifiedAttributeValuesRepresentation(attribute, handler);
	}

	public String getSimplifiedAttributeValuesRepresentation(Attribute attribute, WebAttributeHandler handler)
	{
		StringBuilder sb = new StringBuilder();
		List<String> values = attribute.getValues();
		for (int i = 0; i < values.size(); i++)
		{
			String val = handler.getValueAsString(values.get(i));
			if (i > 0)
				sb.append(", ");
			sb.append(val);
		}
		return sb.toString();
	}

	/**
	 * Returns a string representing the attributes values.
	 * 
	 * @param attribute
	 * @return
	 */
	public com.vaadin.flow.component.Component getSimplifiedShortValuesRepresentation(Attribute attribute)
	{
		WebAttributeHandler handler = getHandlerWithStringFallback(attribute);

		if (attribute.getValues() != null && !attribute.getValues().isEmpty())
		{
			com.vaadin.flow.component.Component rep = handler.getRepresentation(attribute.getValues().get(0),
					AttributeViewerContext.builder().withCustomWidth(PREVIEW_IMAGE_SIZE)
							.withCustomWidthUnit(Unit.PIXELS).withCustomHeight(PREVIEW_IMAGE_SIZE)
							.withCustomHeightUnit(Unit.PIXELS).withMaxTextSize(SHORT_TEXT_VALUE_LENGHT)
							.withShowAsLabel(true).build());
			if (attribute.getValues().size() > 1)
			{
				HorizontalLayout wrapper = new HorizontalLayout();
				wrapper.setMargin(false);
				wrapper.setSpacing(false);
				wrapper.add(rep);
				wrapper.add(new Label(msg.getMessage("MessageUtils.andMore",
						String.valueOf(attribute.getValues().size() - 1))));
				return wrapper;

			} else
			{
				return rep;
			}
		} else
		{
			return new Label();
		}
	}
}
