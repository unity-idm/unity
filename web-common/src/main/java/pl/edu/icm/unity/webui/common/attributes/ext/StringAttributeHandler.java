/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;


/**
 * String attribute handler for the web
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component
public class StringAttributeHandler extends TextOnlyAttributeHandler<String> implements WebAttributeHandlerFactory
{
	private UnityMessageSource msg;

	@Autowired
	public StringAttributeHandler(UnityMessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public String getSupportedSyntaxId()
	{
		return StringAttributeSyntax.ID;
	}

	@Override
	protected String convertFromString(String value)
	{
		return value;
	}
	

	@Override
	public WebAttributeHandler<?> createInstance()
	{
		return new StringAttributeHandler(msg);
	}
	
	@Override
	protected List<String> getHints(AttributeValueSyntax<String> syntaxArg)
	{
		List<String> sb = new ArrayList<String>(3);
		StringAttributeSyntax syntax = (StringAttributeSyntax) syntaxArg;
		
		sb.add(msg.getMessage("StringAttributeHandler.minLen", syntax.getMinLength()));
		if (syntax.getMaxLength() != Integer.MAX_VALUE)
			sb.add(msg.getMessage("StringAttributeHandler.maxLen", syntax.getMaxLength()));
		else
			sb.add(msg.getMessage("StringAttributeHandler.maxLenUndef"));
		if (syntax.getRegexp() != null)
			sb.add(msg.getMessage("StringAttributeHandler.regexp", syntax.getRegexp()));
		
		return sb;
	}

}
