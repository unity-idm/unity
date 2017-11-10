/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.Attribute;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler.RepresentationSize;

/**
 * Gives access to web attribute handlers for given syntax types.
 * Additionally a methods are provided to easily get a simplified attribute representation for the given 
 * attribute.
 * 
 * @author K. Benedyczak
 */
@Component
public class AttributeHandlerRegistry
{
	public static final int DEFAULT_MAX_LEN = 16;

	private UnityMessageSource msg;
	private AttributeTypeSupport aTypeSupport;

	private Map<String, WebAttributeHandlerFactory> factoriesByType = new HashMap<>();
	
	
	@Autowired
	public AttributeHandlerRegistry(List<WebAttributeHandlerFactory> factories, UnityMessageSource msg,
			AttributeTypeSupport aTypeSupport)
	{
		this.msg = msg;
		this.aTypeSupport = aTypeSupport;
		for (WebAttributeHandlerFactory factory: factories)
			factoriesByType.put(factory.getSupportedSyntaxId(), factory);
	}
	
	/**
	 * Returns web attribute handler for the given attribute, falling back to string 
	 * attribute handler if there is no system defined attribute for the argument attribute 
	 * name (can happen for dynamic attributes created by output profiles).
	 * @param at
	 * @return attribute handler
	 */
	public WebAttributeHandler getHandlerWithStringFallback(Attribute at)
	{
		AttributeValueSyntax<?> syntax = getSyntaxWithStringFallback(at);
		return getHandler(syntax);
	}

	/**
	 * @param at
	 * @return syntax for the given attribute, or String attribute syntax,
	 * if the attribute type is not defined.
	 */
	public AttributeValueSyntax<?> getSyntaxWithStringFallback(Attribute at)
	{
		try
		{
			return aTypeSupport.getSyntax(at);
		} catch (IllegalArgumentException e)
		{
			return new StringAttributeSyntax();
		}
	}
	
	public WebAttributeHandler getHandler(AttributeValueSyntax<?> syntax)
	{
		WebAttributeHandlerFactory factory = factoriesByType.get(syntax.getValueSyntaxId());
		if (factory == null)
			throw new IllegalArgumentException("Syntax " + syntax.getValueSyntaxId() + 
					" has no handler factory registered");
		return factory.createInstance(syntax);
	}
	
	/**
	 * 
	 * @param syntaxId
	 * @param syntax syntax to be edited or null if an empty editor should be returned.
	 * @return
	 */
	public AttributeSyntaxEditor<?> getSyntaxEditor(String syntaxId, AttributeValueSyntax<?> syntax)
	{
		WebAttributeHandlerFactory factory = factoriesByType.get(syntaxId);
		if (factory == null)
			throw new IllegalArgumentException("Syntax " + syntaxId + 
					" has no handler factory registered");
		return factory.getSyntaxEditorComponent(syntax);
	}
	
	public com.vaadin.ui.Component getRepresentation(Attribute attribute, RepresentationSize size)
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
		indentedValues.setSpacing(false);
		WebAttributeHandler handler = getHandler(syntax);
		for (String value: attribute.getValues())
			indentedValues.addComponent(handler.getRepresentation(value, size));
		vl.addComponent(indentedValues);
		return vl;
	}
	
	public Set<String> getSupportedSyntaxes()
	{
		return new HashSet<>(factoriesByType.keySet());
	}
	
	public String getSimplifiedAttributeRepresentation(Attribute attribute, int maxValuesLen)
	{
		return getSimplifiedAttributeRepresentation(attribute, maxValuesLen, attribute.getName());
	}
	
	public AttributeTypeSupport getaTypeSupport()
	{
		return aTypeSupport;
	}

	/**
	 * Returns a string representing the attribute. The returned format contains the attribute name
	 * and the values. If the values can not be put in the remaining text len, then are shortened.
	 * @param attribute
	 * @param maxValuesLen max values length, not less then 16
	 * @return
	 */
	public String getSimplifiedAttributeRepresentation(Attribute attribute, int maxValuesLen, 
			String displayedName)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(displayedName);
		List<?> values = attribute.getValues();
		if (!values.isEmpty())
		{
			sb.append(": ");
			sb.append(getSimplifiedAttributeValuesRepresentation(attribute, maxValuesLen));
		}
		return sb.toString();
	}
	
	/**
	 * Returns a string representing the attributes values. The length of the values
	 * string is limited by the argument. When some of the values can not be displayed, then 
	 * ... is appended.
	 * @param attribute
	 * @return
	 */
	public String getSimplifiedAttributeValuesRepresentation(Attribute attribute, int maxValuesLen)
	{
		if (maxValuesLen < 16)
			throw new IllegalArgumentException("The max length must be lager then 16");
		StringBuilder sb = new StringBuilder();
		List<String> values = attribute.getValues();
		AttributeValueSyntax<?> syntax = getSyntaxWithStringFallback(attribute);
		WebAttributeHandler handler = getHandler(syntax);
		int remainingLen = maxValuesLen;
		final String MORE_VALS = ", ...";
		final int moreValsLen = MORE_VALS.length();
		
		for (int i=0; i<values.size(); i++)
		{
			int allowedLen = i == (values.size()-1) ? remainingLen : remainingLen-moreValsLen;
			if (allowedLen < WebAttributeHandler.MIN_VALUE_TEXT_LEN)
			{
				sb.append(", ...");
				break;
			}
			String val = handler.getValueAsString(values.get(i), allowedLen);
			remainingLen -= val.length(); 
			if (i > 0)
			{
				sb.append(", ");
				remainingLen -= 2;
			}
			sb.append(val);
		}
		return sb.toString();
	}
}



