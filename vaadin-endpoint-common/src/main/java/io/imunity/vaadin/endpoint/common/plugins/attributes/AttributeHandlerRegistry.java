/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;

import java.util.*;


@Component
public class AttributeHandlerRegistry
{
	private final MessageSource msg;
	private final AttributeTypeSupport aTypeSupport;

	private final Map<String, WebAttributeHandlerFactory> factoriesByType = new HashMap<>();

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
	 * @return attribute handler
	 */
	public WebAttributeHandler getHandlerWithStringFallback(AttributeType attributeType)
	{
		AttributeValueSyntax<?> syntax = getSyntaxWithStringFallback(attributeType);
		return getHandler(syntax);
	}

	/**
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
		vl.add(new Span(main.toString()));
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

	public AttributeTypeSupport getaTypeSupport()
	{
		return aTypeSupport;
	}
	
	public Set<String> getSupportedSyntaxes()
	{
		return new HashSet<>(factoriesByType.keySet());
	}
	
	public AttributeSyntaxEditor<?> getSyntaxEditor(String syntaxId, AttributeValueSyntax<?> syntax)
	{
		WebAttributeHandlerFactory factory = factoriesByType.get(syntaxId);
		if (factory == null)
			throw new IllegalArgumentException("Syntax " + syntaxId + " has no handler factory registered");
		return factory.getSyntaxEditorComponent(syntax);
	}

}
