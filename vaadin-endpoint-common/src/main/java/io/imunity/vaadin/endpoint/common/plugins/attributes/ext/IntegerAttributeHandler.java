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
import io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors.LongBoundEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.TextOnlyAttributeHandler;
import org.springframework.beans.factory.annotation.Autowired;

import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


class IntegerAttributeHandler extends TextOnlyAttributeHandler
{
	
	public IntegerAttributeHandler(MessageSource msg, AttributeValueSyntax<?> syntax)
	{
		super(msg, syntax);
	}

	@Override
	protected List<String> getHints()
	{
		List<String> sb = new ArrayList<>(2);
		IntegerAttributeSyntax syntax = (IntegerAttributeSyntax) this.syntax;
		
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
	
	private static class IntegerSyntaxEditor implements AttributeSyntaxEditor<Long>
	{
		private final IntegerAttributeSyntax initial;
		private final MessageSource msg;
		private Binder<LongSyntaxBindingValue> binder;
		
		public IntegerSyntaxEditor(IntegerAttributeSyntax initial, MessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Optional<Component>  getEditor()
		{
			FormLayout fl = new FormLayout();
			fl.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

			LongBoundEditor min = new LongBoundEditor(msg, msg.getMessage("NumericAttributeHandler.minUndef"),
					Optional.empty(), Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
			LongBoundEditor max = new LongBoundEditor(msg, msg.getMessage("NumericAttributeHandler.maxUndef"),
					Optional.empty(), Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);

			binder = new Binder<>(LongSyntaxBindingValue.class);
			max.configureBinding(binder, "max");
			min.configureBinding(binder, "min");

			LongSyntaxBindingValue value = new LongSyntaxBindingValue();
			if (initial != null)
			{
				value.setMax(initial.getMax());
				value.setMin(initial.getMin());

			} else
			{
				value.setMax(Long.MAX_VALUE);
				value.setMin(0L);
			}
			binder.setBean(value);
			fl.addFormItem(min, msg.getMessage("NumericAttributeHandler.minE"));
			fl.addFormItem(max, msg.getMessage("NumericAttributeHandler.maxE"));

			return Optional.of(fl);
		}
	
		@Override
		public AttributeValueSyntax<Long> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			try
			{
				if (!binder.isValid())
				{	
					binder.validate();
					throw new IllegalAttributeTypeException("");
				}
				IntegerAttributeSyntax ret = new IntegerAttributeSyntax();
				LongSyntaxBindingValue value = binder.getBean();
				ret.setMax(value.getMax());
				ret.setMin(value.getMin());
				return ret;
			} catch (Exception e)
			{
				throw new IllegalAttributeTypeException(e.getMessage(), e);
			}
		}
		
		private static class LongSyntaxBindingValue extends MinMaxBindingValue<Long>
		{}
	}

	
	@org.springframework.stereotype.Component
	static class IntegerAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private final MessageSource msg;

		@Autowired
		IntegerAttributeHandlerFactory(MessageSource msg)
		{
			this.msg = msg;
		}
		

		@Override
		public String getSupportedSyntaxId()
		{
			return IntegerAttributeSyntax.ID;
		}
		
		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new IntegerAttributeHandler(msg, syntax);
		}
		
		@Override
		public AttributeSyntaxEditor<Long> getSyntaxEditorComponent(
				AttributeValueSyntax<?> initialValue)
		{
			return new IntegerSyntaxEditor((IntegerAttributeSyntax) initialValue, msg);
		}
	}
	
}
