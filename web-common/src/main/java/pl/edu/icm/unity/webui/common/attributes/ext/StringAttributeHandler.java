/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.validator.IntegerRangeValidator;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.IntegerBoundEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
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
		if (syntax.getRegexp() != null && !syntax.getRegexp().equals(""))
			sb.add(msg.getMessage("StringAttributeHandler.regexp", syntax.getRegexp()));
		
		return sb;
	}
	
	@Override
	public AttributeSyntaxEditor<String> getSyntaxEditorComponent(
			AttributeValueSyntax<String> initialValue)
	{
		return new StringSyntaxEditor((StringAttributeSyntax) initialValue);
	}
	
	private class StringSyntaxEditor implements AttributeSyntaxEditor<String>
	{
		private StringAttributeSyntax initial;
		private IntegerBoundEditor max;
		private TextField min;
		private TextField regexp;
		
		
		public StringSyntaxEditor(StringAttributeSyntax initial)
		{
			this.initial = initial;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new FormLayout();
			min = new TextField();
			min.setCaption(msg.getMessage("StringAttributeHandler.minLenE"));
			min.addValidator(new IntegerRangeValidator(msg.getMessage("StringAttributeHandler.wrongMin"), 
					0, Integer.MAX_VALUE));
			min.setRequired(true);
			min.setRequiredError(msg.getMessage("fieldRequired"));
			min.setConverter(Integer.class);
			fl.addComponent(min);
			max = new IntegerBoundEditor(msg, msg.getMessage("StringAttributeHandler.maxLenUndef"), 
					msg.getMessage("NumericAttributeHandler.maxE"), Integer.MAX_VALUE);
			max.setMax(Integer.MAX_VALUE).setMin(1);
			fl.addComponent(max);
			regexp = new TextField(msg.getMessage("StringAttributeHandler.regexpE"));
			fl.addComponent(regexp);
			if (initial != null)
			{
				max.setValue(initial.getMaxLength());
				min.setValue(Integer.toString(initial.getMinLength()));
				regexp.setValue(initial.getRegexp());
			}
			return fl;
		}

		@Override
		public AttributeValueSyntax<String> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			try
			{
				StringAttributeSyntax ret = new StringAttributeSyntax();
				ret.setMaxLength(max.getValue());
				ret.setMinLength((Integer)min.getConvertedValue());
				ret.setRegexp(regexp.getValue());
				return ret;
			} catch (Exception e)
			{
				throw new IllegalAttributeTypeException(e.getMessage(), e);
			}
		}
	}
}
