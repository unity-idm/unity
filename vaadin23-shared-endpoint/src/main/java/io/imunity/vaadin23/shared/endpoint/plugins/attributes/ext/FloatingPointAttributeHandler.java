/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin23.shared.endpoint.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.AttributeSyntaxEditor;
import io.imunity.vaadin23.shared.endpoint.components.TextOnlyAttributeHandler;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.WebAttributeHandler;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.WebAttributeHandlerFactory;
import io.imunity.vaadin23.shared.endpoint.plugins.attributes.bounded_editors.DoubleBoundEditor;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;

import java.util.ArrayList;
import java.util.List;


public class FloatingPointAttributeHandler extends TextOnlyAttributeHandler
{	
	
	public FloatingPointAttributeHandler(MessageSource msg, AttributeValueSyntax<?> syntax)
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
		private MessageSource msg;
		private Binder<DoubleSyntaxBindingValue> binder;
		
		public FloatingPointSyntaxEditor(FloatingPointAttributeSyntax initial, MessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new FormLayout();
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

			fl.add(min, max);
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
		
		public class DoubleSyntaxBindingValue extends MinMaxBindingValue<Double>
		{}
	}
	
	
	@org.springframework.stereotype.Component
	public static class FloatingPointAttributeHandlerFactoryV23 implements WebAttributeHandlerFactory
	{
		private MessageSource msg;

		@Autowired
		public FloatingPointAttributeHandlerFactoryV23(MessageSource msg)
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
