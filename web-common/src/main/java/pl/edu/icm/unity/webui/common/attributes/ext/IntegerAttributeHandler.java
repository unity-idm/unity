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
import pl.edu.icm.unity.stdext.attr.IntegerAttributeSyntax;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attributes.TextOnlyAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandlerFactory;
import pl.edu.icm.unity.webui.common.boundededitors.LongBoundEditor;


/**
 * Integer attribute handler for the web
 * @author K. Benedyczak
 */
public class IntegerAttributeHandler extends TextOnlyAttributeHandler
{
	
	public IntegerAttributeHandler(UnityMessageSource msg, AttributeValueSyntax<?> syntax)
	{
		super(msg, syntax);
	}


	@Override
	protected List<String> getHints()
	{
		List<String> sb = new ArrayList<String>(2);
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
		private IntegerAttributeSyntax initial;
		private LongBoundEditor max;
		private LongBoundEditor min;
		private UnityMessageSource msg;
		private Binder<LongSyntaxBindingValue> binder;
		
		public IntegerSyntaxEditor(IntegerAttributeSyntax initial, UnityMessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Component getEditor()
		{
			FormLayout fl = new CompactFormLayout();
			min = new LongBoundEditor(msg,
					msg.getMessage("NumericAttributeHandler.minUndef"),
					msg.getMessage("NumericAttributeHandler.minE"),
					Long.MIN_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);
			max = new LongBoundEditor(msg,
					msg.getMessage("NumericAttributeHandler.maxUndef"),
					msg.getMessage("NumericAttributeHandler.maxE"),
					Long.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE);

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
				value.setMin(0l);
			}
			binder.setBean(value);
			fl.addComponents(min, max);
			return fl;
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
		
		public class LongSyntaxBindingValue extends MinMaxBindingValue<Long>{}	
	}

	
	@org.springframework.stereotype.Component
	public static class IntegerAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private UnityMessageSource msg;

		@Autowired
		public IntegerAttributeHandlerFactory(UnityMessageSource msg)
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
