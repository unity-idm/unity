/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.DoubleBoundEditor;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
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
	
	@Override
	public AttributeSyntaxEditor<Double> getSyntaxEditorComponent(
			AttributeValueSyntax<Double> initialValue)
	{
		return new FloatingPointSyntaxEditor((FloatingPointAttributeSyntax) initialValue);
	}
	
	private class FloatingPointSyntaxEditor implements AttributeSyntaxEditor<Double>
	{
		private FloatingPointAttributeSyntax initial;
		private DoubleBoundEditor max, min;
		
		
		public FloatingPointSyntaxEditor(FloatingPointAttributeSyntax initial)
		{
			this.initial = initial;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new FormLayout();
			min = new DoubleBoundEditor(msg, msg.getMessage("NumericAttributeHandler.minUndef"), 
					msg.getMessage("NumericAttributeHandler.minE"), Double.MIN_VALUE);
			max = new DoubleBoundEditor(msg, msg.getMessage("NumericAttributeHandler.maxUndef"), 
					msg.getMessage("NumericAttributeHandler.maxE"), Double.MAX_VALUE);
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
		public AttributeValueSyntax<Double> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			try
			{
				FloatingPointAttributeSyntax ret = new FloatingPointAttributeSyntax();
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
