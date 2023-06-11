/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import java.util.function.Supplier;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;

import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.stdext.attr.BaseImageAttributeSyntax;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;

/**
 * Editor for Image type details definition.
 *
 * @author R. Ledzinski
 */
class BaseImageSyntaxEditor<T> implements AttributeSyntaxEditor<T>
{
	private final BaseImageAttributeSyntax<T> initial;
	private final Supplier<BaseImageAttributeSyntax<T>> ctor;
	private final MessageSource msg;
	
	private IntegerBoundEditor maxHeight, maxSize;
	private IntegerBoundEditor maxWidth;
	private Binder<ImageSyntaxBindingValue> binder;

	BaseImageSyntaxEditor(BaseImageAttributeSyntax<T> initial,
			Supplier<BaseImageAttributeSyntax<T>> ctor,
			MessageSource msg)
	{
		this.initial = initial;
		this.ctor = ctor;
		this.msg = msg;
	}

	@Override
	public Component getEditor()
	{
		FormLayout fl = new CompactFormLayout();
		maxWidth = new IntegerBoundEditor(msg,
				msg.getMessage("ImageAttributeHandler.maxWidthUnlimited"),
				msg.getMessage("ImageAttributeHandler.maxWidthE"),
				Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
		maxHeight = new IntegerBoundEditor(msg,
				msg.getMessage("ImageAttributeHandler.maxHeightUnlimited"),
				msg.getMessage("ImageAttributeHandler.maxHeightE"),
				Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
		maxSize = new IntegerBoundEditor(msg,
				msg.getMessage("ImageAttributeHandler.maxSizeUnlimited"),
				msg.getMessage("ImageAttributeHandler.maxSizeE"),
				Integer.MAX_VALUE, 100, Integer.MAX_VALUE);

		binder = new Binder<>(ImageSyntaxBindingValue.class);
		maxWidth.configureBinding(binder, "maxWidth");
		maxHeight.configureBinding(binder, "maxHeight");
		maxSize.configureBinding(binder, "maxSize");

		fl.addComponents(maxWidth, maxHeight, maxSize);

		ImageSyntaxBindingValue value = new ImageSyntaxBindingValue();
		if (initial != null)
		{
			value.setMaxWidth(initial.getConfig().getMaxWidth());
			value.setMaxHeight(initial.getConfig().getMaxHeight());
			value.setMaxSize(initial.getConfig().getMaxSize());
		} else
		{
			value.setMaxWidth(200);
			value.setMaxHeight(200);
			value.setMaxSize(1024000);
		}
		binder.setBean(value);
		return fl;
	}

	@Override
	public AttributeValueSyntax<T> getCurrentValue()
			throws IllegalAttributeTypeException
	{

		try
		{
			if (!binder.isValid())
			{
				binder.validate();
				throw new IllegalAttributeTypeException("");
			}

			ImageSyntaxBindingValue value = binder.getBean();
			BaseImageAttributeSyntax<T> ret = ctor.get();
			ret.getConfig().setMaxHeight(value.getMaxHeight());
			ret.getConfig().setMaxWidth(value.getMaxWidth());
			ret.getConfig().setMaxSize(value.getMaxSize());
			return ret;
		} catch (Exception e)
		{
			throw new IllegalAttributeTypeException(e.getMessage(), e);
		}

	}

	public static class ImageSyntaxBindingValue
	{
		private Integer maxSize;
		private Integer maxWidth;
		private Integer maxHeight;

		public ImageSyntaxBindingValue()
		{

		}

		public Integer getMaxSize()
		{
			return maxSize;
		}

		public void setMaxSize(Integer maxSize)
		{
			this.maxSize = maxSize;
		}

		public Integer getMaxWidth()
		{
			return maxWidth;
		}

		public void setMaxWidth(Integer maxWidth)
		{
			this.maxWidth = maxWidth;
		}

		public Integer getMaxHeight()
		{
			return maxHeight;
		}

		public void setMaxHeight(Integer maxHeight)
		{
			this.maxHeight = maxHeight;
		}
	}
}
