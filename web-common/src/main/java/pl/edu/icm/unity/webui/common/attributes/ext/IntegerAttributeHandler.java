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

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.LongBoundEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
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
	
	@Override
	public AttributeSyntaxEditor<Long> getSyntaxEditorComponent(
			AttributeValueSyntax<Long> initialValue)
	{
		return new IntegerSyntaxEditor((IntegerAttributeSyntax) initialValue);
	}
	
	private class IntegerSyntaxEditor implements AttributeSyntaxEditor<Long>
	{
		private IntegerAttributeSyntax initial;
		private LongBoundEditor max, min;
		
		
		public IntegerSyntaxEditor(IntegerAttributeSyntax initial)
		{
			this.initial = initial;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new FormLayout();
			min = new LongBoundEditor(msg, msg.getMessage("NumericAttributeHandler.minUndef"), 
					msg.getMessage("NumericAttributeHandler.minE"), Long.MIN_VALUE);
			max = new LongBoundEditor(msg, msg.getMessage("NumericAttributeHandler.maxUndef"), 
					msg.getMessage("NumericAttributeHandler.maxE"), Long.MAX_VALUE);
			if (initial != null)
			{
				max.setValue(initial.getMax());
				min.setValue(initial.getMin());
			}
			min.addToLayout(fl);
			max.addToLayout(fl);
			return fl;
		}

		@Override
		public AttributeValueSyntax<Long> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			try
			{
				IntegerAttributeSyntax ret = new IntegerAttributeSyntax();
				ret.setMax(max.getValue());
				ret.setMin(min.getValue());
				return ret;
			} catch (IllegalStateException e)
			{
				throw new IllegalAttributeTypeException(e.getMessage(), e);
			}
		}
	}

}
