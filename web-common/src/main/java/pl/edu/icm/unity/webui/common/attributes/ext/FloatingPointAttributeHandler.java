/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;


/**
 * Floating point attribute handler for the web
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class FloatingPointAttributeHandler extends TextOnlyAttributeHandler<Double> implements WebAttributeHandlerFactory
{
	private UnityMessageSource msg;
	
	@Autowired
	public FloatingPointAttributeHandler(UnityMessageSource msg)
	{
		this.msg = msg;
	}
	
	@Override
	public String getSupportedSyntaxId()
	{
		return FloatingPointAttributeSyntax.ID;
	}

	@Override
	protected Double convertFromString(String value)
	{
		return Double.parseDouble(value);
	}

	@Override
	public WebAttributeHandler<?> createInstance()
	{
		return new FloatingPointAttributeHandler(msg);
	}

	@Override
	protected List<String> getHints(AttributeValueSyntax<Double> syntaxArg)
	{
		List<String> sb = new ArrayList<String>(3);
		FloatingPointAttributeSyntax syntax = (FloatingPointAttributeSyntax) syntaxArg;
		
		if (syntax.getMin() != Double.MIN_VALUE)
			sb.add(msg.getMessage("NumericAttributeHandler.min", syntax.getMin()));
		else
			sb.add(msg.getMessage("NumericAttributeHandler.minUndef"));
		if (syntax.getMax() != Double.MAX_VALUE)
			sb.add(msg.getMessage("NumericAttributeHandler.max", syntax.getMax()));
		else
			sb.add(msg.getMessage("NumericAttributeHandler.maxUndef"));
		
		return sb;
	}
}
