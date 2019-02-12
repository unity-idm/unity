/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.attributes.image;

import com.vaadin.data.Binder;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.stdext.attr.ImageAttributeSyntax;
import pl.edu.icm.unity.stdext.utils.UnityImage;
import pl.edu.icm.unity.webui.common.CompactFormLayout;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;

/**
 * Editor for Image type details definition.
 *
 * @author R. Ledzinski
 */
class ImageSyntaxEditor implements AttributeSyntaxEditor<UnityImage>
{
	private ImageAttributeSyntax initial;
	private IntegerBoundEditor maxHeight, maxSize;
	private IntegerBoundEditor maxWidth;
	private UnityMessageSource msg;
	private Binder<ImageSyntaxBindingValue> binder;

	ImageSyntaxEditor(ImageAttributeSyntax initial, UnityMessageSource msg)
	{
		this.initial = initial;
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
			value.setMaxWidth(initial.getMaxWidth());
			value.setMaxHeight(initial.getMaxHeight());
			value.setMaxSize(initial.getMaxSize());
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
	public AttributeValueSyntax<UnityImage> getCurrentValue()
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
			ImageAttributeSyntax ret = new ImageAttributeSyntax();
			ret.setMaxHeight(value.getMaxHeight());
			ret.setMaxWidth(value.getMaxWidth());
			ret.setMaxSize(value.getMaxSize());
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
