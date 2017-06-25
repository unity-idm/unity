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

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;


/**
 * String attribute handler for the web
 * @author K. Benedyczak
 */
public class StringAttributeHandler extends TextOnlyAttributeHandler
{
	private UnityMessageSource msg;

	public StringAttributeHandler(UnityMessageSource msg, AttributeValueSyntax<?> syntax)
	{
		super(syntax);
		this.msg = msg;
	}

	@Override
	protected List<String> getHints()
	{
		List<String> sb = new ArrayList<String>(3);
		StringAttributeSyntax syntax = (StringAttributeSyntax) this.syntax;
		
		sb.add(msg.getMessage("StringAttributeHandler.minLen", syntax.getMinLength()));
		if (syntax.getMaxLength() != Integer.MAX_VALUE)
			sb.add(msg.getMessage("StringAttributeHandler.maxLen", syntax.getMaxLength()));
		else
			sb.add(msg.getMessage("StringAttributeHandler.maxLenUndef"));
		if (syntax.getRegexp() != null && !syntax.getRegexp().equals(""))
			sb.add(msg.getMessage("StringAttributeHandler.regexp", syntax.getRegexp()));
		
		return sb;
	}
	
	private static class StringSyntaxEditor implements AttributeSyntaxEditor<String>
	{
		private StringAttributeSyntax initial;
		private IntegerBoundEditor max;
		private TextField min;
		private TextField regexp;
		private UnityMessageSource msg;
		
		
		public StringSyntaxEditor(StringAttributeSyntax initial, UnityMessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new CompactFormLayout();
			min = new RequiredTextField(msg);
			min.setCaption(msg.getMessage("StringAttributeHandler.minLenE"));
			min.addValidator(new IntegerRangeValidator(msg.getMessage("StringAttributeHandler.wrongMin"), 
					0, Integer.MAX_VALUE));
			min.setConverter(Integer.class);
			min.setValue("0");
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
			} else
			{
				min.setValue("0");
				max.setValue(200);
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
	
	
	@org.springframework.stereotype.Component
	public static class StringAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;

		@Autowired
		public StringAttributeHandlerFactory(UnityMessageSource msg)
		{
			this.msg = msg;
		}
		
		@Override
		public String getSupportedSyntaxId()
		{
			return StringAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new StringAttributeHandler(msg, syntax);
		}
		
		
		@Override
		public AttributeSyntaxEditor<String> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new StringSyntaxEditor((StringAttributeSyntax) initialValue, msg);
		}
	}
}
