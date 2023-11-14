/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.data.binder.Binder;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeSyntaxEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandlerFactory;
import io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors.DoubleBoundEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.TextOnlyAttributeHandler;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.FloatingPointAttributeSyntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


class FloatingPointAttributeHandler extends TextOnlyAttributeHandler
{	
	
	FloatingPointAttributeHandler(MessageSource msg, AttributeValueSyntax<?> syntax)
	{
		super(msg, syntax);
	}

	@Override
	protected List<String> getHints()
	{
		List<String> sb = new ArrayList<>(3);
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
		private final FloatingPointAttributeSyntax initial;
		private final MessageSource msg;
		private Binder<DoubleSyntaxBindingValue> binder;
		
		public FloatingPointSyntaxEditor(FloatingPointAttributeSyntax initial, MessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Optional<Component>  getEditor()
		{
			FormLayout fl = new FormLayout();
			fl.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

			DoubleBoundEditor min = new DoubleBoundEditor(msg,
					msg.getMessage("NumericAttributeHandler.minUndef"),
					Optional.empty(),
					Double.MIN_VALUE, Double.MIN_VALUE, Double.MAX_VALUE);
			DoubleBoundEditor max = new DoubleBoundEditor(msg,
					msg.getMessage("NumericAttributeHandler.maxUndef"),
					Optional.empty(),
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

			fl.addFormItem(min, msg.getMessage("NumericAttributeHandler.minE"));
			fl.addFormItem(max, msg.getMessage("NumericAttributeHandler.maxE"));
			return Optional.of(fl);
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
		
		private static class DoubleSyntaxBindingValue extends MinMaxBindingValue<Double>
		{}
	}
	
	
	@org.springframework.stereotype.Component
	static class FloatingPointAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private final MessageSource msg;

		@Autowired
		FloatingPointAttributeHandlerFactory(MessageSource msg)
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
