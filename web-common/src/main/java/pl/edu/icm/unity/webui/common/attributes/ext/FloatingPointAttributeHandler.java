/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.boundededitors.DoubleBoundEditor;


/**
 * Floating point attribute handler for the web
 * @author K. Benedyczak
 */
public class FloatingPointAttributeHandler extends TextOnlyAttributeHandler
{	
	
	public FloatingPointAttributeHandler(UnityMessageSource msg, AttributeValueSyntax<?> syntax)
	{
		super(msg, syntax);
	}

	@Override
	protected List<String> getHints()
	{
		List<String> sb = new ArrayList<String>(3);
		FloatingPointAttributeSyntax syntax = (FloatingPointAttributeSyntax) this.syntax;
		
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
	
	private static class FloatingPointSyntaxEditor implements AttributeSyntaxEditor<Double>
	{
		private FloatingPointAttributeSyntax initial;
		private DoubleBoundEditor max;
		private DoubleBoundEditor min;
		private UnityMessageSource msg;
		private Binder<DoubleSyntaxBindingValue> binder;
		
		public FloatingPointSyntaxEditor(FloatingPointAttributeSyntax initial, UnityMessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new CompactFormLayout();
			min = new DoubleBoundEditor(msg,
					msg.getMessage("NumericAttributeHandler.minUndef"),
					msg.getMessage("NumericAttributeHandler.minE"),
					Double.MIN_VALUE, Double.MIN_VALUE, Double.MAX_VALUE);
			max = new DoubleBoundEditor(msg,
					msg.getMessage("NumericAttributeHandler.maxUndef"),
					msg.getMessage("NumericAttributeHandler.maxE"),
					Double.MAX_VALUE, Double.MIN_VALUE, Double.MAX_VALUE);

			binder = new Binder<>(DoubleSyntaxBindingValue.class);
			max.configureBinding(binder, "max");
			min.configureBinding(binder, "min");

			DoubleSyntaxBindingValue value = new DoubleSyntaxBindingValue();
			if (initial != null)
			{
				value.setMax(initial.getMax());
				value.setMin(initial.getMin());

			} else
			{
				value.setMax(Double.MAX_VALUE);
				value.setMin(0d);
			}
			binder.setBean(value);

			fl.addComponents(min, max);
			return fl;
		}

		@Override
		public AttributeValueSyntax<Double> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			try
			{
				if (!binder.isValid())
				{	
					binder.validate();
					throw new IllegalAttributeTypeException("");
				}
				
				FloatingPointAttributeSyntax ret = new FloatingPointAttributeSyntax();
				DoubleSyntaxBindingValue value = binder.getBean();
				ret.setMax(value.getMax());
				ret.setMin(value.getMin());
				return ret;
			} catch (Exception e)
			{
				throw new IllegalAttributeTypeException(e.getMessage(), e);
			}
		}
		
		public class DoubleSyntaxBindingValue extends MinMaxBindingValue<Double>{}	
	}
	
	
	@org.springframework.stereotype.Component
	public static class FloatingPointAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;

		@Autowired
		public FloatingPointAttributeHandlerFactory(UnityMessageSource msg)
		{
			this.msg = msg;
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return FloatingPointAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new FloatingPointAttributeHandler(msg, syntax);
		}
		
		@Override
		public AttributeSyntaxEditor<Double> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new FloatingPointSyntaxEditor(
					(FloatingPointAttributeSyntax) initialValue, msg);
		}
	}
}
