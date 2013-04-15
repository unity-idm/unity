/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;


/**
 * Integer attribute handler for the web
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class IntegerAttributeHandler extends TextOnlyAttributeHandler<Long> implements WebAttributeHandlerFactory
{
	private UnityMessageSource msg;

	@Autowired
	public IntegerAttributeHandler(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedSyntaxId()
	{
		return IntegerAttributeSyntax.ID;
	}

	@Override
	protected Long convertFromString(String value)
	{
		return Long.parseLong(value);
	}
	
	@Override
	protected List<String> getHints(AttributeValueSyntax<Long> syntaxArg)
	{
		List<String> sb = new ArrayList<String>(2);
		IntegerAttributeSyntax syntax = (IntegerAttributeSyntax) syntaxArg;
		
		if (syntax.getMin() != Long.MIN_VALUE)
			sb.add(msg.getMessage("NumericAttributeHandler.min", syntax.getMin()));
		else
			sb.add(msg.getMessage("NumericAttributeHandler.minUndef"));
		if (syntax.getMax() != Long.MAX_VALUE)
			sb.add(msg.getMessage("NumericAttributeHandler.max", syntax.getMax()));
		else
			sb.add(msg.getMessage("NumericAttributeHandler.maxUndef"));
		
		return sb;
	}

	@Override
	public WebAttributeHandler<?> createInstance()
	{
		return new IntegerAttributeHandler(msg);
	}
}
