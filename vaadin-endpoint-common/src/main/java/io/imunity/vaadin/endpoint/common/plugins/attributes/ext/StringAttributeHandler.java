/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.validator.IntegerRangeValidator;

import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeSyntaxEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandlerFactory;
import io.imunity.vaadin.endpoint.common.plugins.attributes.bounded_editors.IntegerBoundEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.TextOnlyAttributeHandler;
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.StringAttributeSyntax;


public class StringAttributeHandler extends TextOnlyAttributeHandler
{
	
	public StringAttributeHandler(MessageSource msg, AttributeValueSyntax<?> syntax)
	{
		super(msg, syntax);
	}

	@Override
	protected List<String> getHints()
	{
		List<String> sb = new ArrayList<>(3);
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
		private final StringAttributeSyntax initial;
		private final MessageSource msg;
		private Binder<StringSyntaxBindingValue> binder;
		
		public StringSyntaxEditor(StringAttributeSyntax initial, MessageSource msg)
		{
			this.initial = initial;
			this.msg = msg;
		}

		@Override
		public Component getEditor()
		{
			binder = new Binder<>(StringSyntaxBindingValue.class);

			FormLayout fl = new FormLayout();
			fl.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
		
			
			
			TextField min = new TextField();
			fl.addFormItem(min, msg.getMessage("StringAttributeHandler.minLenE"));

			IntegerBoundEditor max = new IntegerBoundEditor(msg,
					msg.getMessage("StringAttributeHandler.maxLenUndef"),
					Integer.MAX_VALUE, 1, null);

			fl.addFormItem(max, msg.getMessage("NumericAttributeHandler.maxE"));

			TextField regexp = new TextField();
			fl.addFormItem(regexp, msg.getMessage("StringAttributeHandler.regexpE"));

			Checkbox editWithTextArea = new Checkbox(msg.getMessage("StringAttributeHandler.editWithTextAreaE"));
			fl.addFormItem(editWithTextArea, msg.getMessage("StringAttributeHandler.editWithTextAreaE"));
			
			binder.forField(min).asRequired(msg.getMessage("fieldRequired"))
					.withConverter(new StringToIntegerConverter(msg.getMessage(
							"IntegerBoundEditor.notANumber")))
					.withValidator(new IntegerRangeValidator(msg.getMessage(
							"StringAttributeHandler.wrongMin"), 0,
							Integer.MAX_VALUE))
					.bind("min");
			max.configureBinding(binder, "max");
			binder.forField(regexp).bind("regexp");
			binder.forField(editWithTextArea).bind("editWithTextArea");
			
			StringSyntaxBindingValue value = new StringSyntaxBindingValue();
			if (initial != null)
			{
				value.setMax(initial.getMaxLength());
				value.setMin(initial.getMinLength());
				value.setRegexp(initial.getRegexp());
				value.setEditWithTextArea(initial.isEditWithTextArea());
			} else
			{
				
				value.setMax(200);
				value.setMin(0);
				
			}
			
			binder.setBean(value);
			
			return fl;
		}

		@Override
		public AttributeValueSyntax<String> getCurrentValue()
				throws IllegalAttributeTypeException
		{
			try
			{
				if (!binder.isValid())
				{	
					binder.validate();
					throw new IllegalAttributeTypeException("");
				}
				StringAttributeSyntax ret = new StringAttributeSyntax();
				StringSyntaxBindingValue value = binder.getBean();
				ret.setMaxLength(value.getMax());
				ret.setMinLength(value.getMin());
				ret.setEditWithTextArea(value.isEditWithTextArea());
				if (value.getRegexp() != null && !value.getRegexp().isEmpty())
					ret.setRegexp(value.getRegexp());
				return ret;
			} catch (Exception e)
			{
				throw new IllegalAttributeTypeException(e.getMessage(), e);
			}
		}
		
		public class StringSyntaxBindingValue extends MinMaxBindingValue<Integer>
		{
			private String regexp;
			private boolean editWithTextArea;
			
			public String getRegexp()
			{
				return regexp;
			}

			public void setRegexp(String regexp)
			{
				this.regexp = regexp;
			}

			public boolean isEditWithTextArea()
			{
				return editWithTextArea;
			}

			public void setEditWithTextArea(boolean editWithTextArea)
			{
				this.editWithTextArea = editWithTextArea;
			}			
		}
		
	}
	
	
	@org.springframework.stereotype.Component
	static class StringAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private final MessageSource msg;

		@Autowired
		public StringAttributeHandlerFactory(MessageSource msg)
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
