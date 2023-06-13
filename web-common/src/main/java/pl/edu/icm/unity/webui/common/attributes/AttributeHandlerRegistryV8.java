/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import com.vaadin.server.Sizeable.Unit;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.attribute.Attribute;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.webui.common.Styles;

import java.util.*;

/**
 * Gives access to web attribute handlers for given syntax types. Additionally a
 * methods are provided to easily get a simplified attribute representation for
 * the given attribute.
 * 
 * @author K. Benedyczak
 */
@Component
public class AttributeHandlerRegistryV8
{
	private static final int SHORT_TEXT_VALUE_LENGHT = 100;
	private static final int PREVIEW_IMAGE_SIZE = 29;
		
	private MessageSource msg;
	private AttributeTypeSupport aTypeSupport;

	private Map<String, WebAttributeHandlerFactory> factoriesByType = new HashMap<>();

	@Autowired
	public AttributeHandlerRegistryV8(List<WebAttributeHandlerFactory> factories, MessageSource msg,
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

	public com.vaadin.ui.Component getRepresentation(Attribute attribute, AttributeViewerContext context)
	{
		VerticalLayout vl = new VerticalLayout();
		vl.addStyleName(Styles.smallSpacing.toString());
		vl.setMargin(false);
		vl.setSpacing(false);
		AttributeValueSyntax<?> syntax = getSyntaxWithStringFallback(attribute);
		StringBuilder main = new StringBuilder(attribute.getName());
		if (attribute.getRemoteIdp() != null)
		{
			String idpInfo = msg.getMessage("IdentityFormatter.remoteInfo", attribute.getRemoteIdp());
			main.append(" [").append(idpInfo).append("]");
		}
		vl.addComponent(new Label(main.toString()));
		VerticalLayout indentedValues = new VerticalLayout();
		indentedValues.setMargin(new MarginInfo(false, false, false, true));
		indentedValues.addStyleName(Styles.smallSpacing.toString());
		indentedValues.setSpacing(true);
		WebAttributeHandler handler = getHandler(syntax);
		for (String value : attribute.getValues())
		{
			com.vaadin.ui.Component valueRep = handler.getRepresentation(value,
					context);
			//valueRep.setWidth(100, Unit.PERCENTAGE);
			indentedValues.addComponent(valueRep);
		}
		vl.addComponent(indentedValues);
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
	public com.vaadin.ui.Component getSimplifiedShortValuesRepresentation(Attribute attribute)
	{
		WebAttributeHandler handler = getHandlerWithStringFallback(attribute);

		if (attribute.getValues() != null && !attribute.getValues().isEmpty())
		{
			com.vaadin.ui.Component rep = handler.getRepresentation(attribute.getValues().get(0),
					AttributeViewerContext.builder().withCustomWidth(PREVIEW_IMAGE_SIZE)
							.withCustomWidthUnit(Unit.PIXELS).withCustomHeight(PREVIEW_IMAGE_SIZE)
							.withCustomHeightUnit(Unit.PIXELS).withMaxTextSize(SHORT_TEXT_VALUE_LENGHT)
							.withShowAsLabel(true).build());
			if (attribute.getValues().size() > 1)
			{
				HorizontalLayout wrapper = new HorizontalLayout();
				wrapper.setMargin(false);
				wrapper.setSpacing(false);
				wrapper.addComponent(rep);
				wrapper.addComponent(new Label(msg.getMessage("MessageUtils.andMore",
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
