/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.common.base.Strings;
import com.vaadin.server.Resource;
import com.vaadin.shared.Registration;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlEscapers;

/**
 * Shows {@link I18nString} in read only mode. Implemented as Custom field for
 * convenience.
 * 
 * The content of the message is html escaped.
 * 
 * When option with preview is used then a link is shown which opens an window
 * with full blown html preview.
 * 
 * @author Roman Krysinski
 */
public class I18nLabelWithPreview extends CustomField<I18nString>
{
	private UnityMessageSource msg;
	
	private Map<String, HPairLayout> translationTFs = new HashMap<>();
	private VerticalLayout main;
	private ContentMode previewMode;
	private I18nString value; 

	public I18nLabelWithPreview(UnityMessageSource msg, ContentMode previewMode)
	{
		this.msg = msg;
		this.previewMode = previewMode;
		initUI();
	}

	public I18nLabelWithPreview(UnityMessageSource msg, String caption, ContentMode contentMode)
	{
		this(msg, contentMode);
		setCaption(caption);
	}
	
	private void initUI()
	{
		VerticalLayout main = new VerticalLayout();
		msg.getEnabledLocales().values().stream()
			.map(Locale::toString)
			.forEach(localeKey -> 
			{
				HPairLayout pair = new HPairLayout(msg);
				Resource image = Images.getFlagForLocale(localeKey);
				if (image != null)
					pair.addImage(image);
				main.addComponent(pair);
				translationTFs.put(localeKey, pair);
			});
		main.setSpacing(false);
		this.main = main;
	}

	@Override
	protected Component initContent()
	{
		return main;
	}

	@Override
	public I18nString getValue()
	{
		return value;
	}

	@Override
	protected void doSetValue(I18nString value)
	{
		this.value = value;
		translationTFs.values().forEach(tf -> tf.setVisible(false));
		
		value.getMap().forEach((locale, message) -> 
		{
			HPairLayout tf = translationTFs.get(locale);
			if (tf != null)
			{
				tf.setTextWithPreview(message);
				tf.setVisible(true);
			}
		});
		
		String defaultLocale = msg.getDefaultLocaleCode();
		boolean hasDefaultLocale = value.getMap().keySet().contains(defaultLocale);
		
		if (!hasDefaultLocale && value.getDefaultValue() != null)
		{
			translationTFs.get(defaultLocale).setTextWithPreview(value.getDefaultValue());
			translationTFs.get(defaultLocale).setVisible(true);
		}	
	}
	
	/**
	 * Horizontal pair layout with image on the left and text on right.
	 * Has preview option which opens a window with fully blown html.
	 * 
	 * @author Roman Krysinski (roman@unity-idm.eu)
	 */
	private class HPairLayout extends HorizontalLayout
	{
		private Label label;
		private Button preview;
		private List<Registration> listeners;
		private static final String HTML_SPACE = "&nbsp";

		public HPairLayout(UnityMessageSource msg)
		{
			setSpacing(true);
			setWidth(100, Unit.PERCENTAGE);
			
			VerticalLayout vLayout = new VerticalLayout();
			
			if (previewMode != null)
			{
				preview = new Button(msg.getMessage("MessageTemplateViewer.preview"));
				preview.setStyleName(Styles.vButtonLink.toString());
				vLayout.addComponent(this.preview);
			}
			listeners = new ArrayList<>();
			
			label = new Label();
			label.setContentMode(ContentMode.HTML);
			vLayout.addComponent(this.label);
			
			addComponent(vLayout);
			setExpandRatio(vLayout, 1.0f);
			setComponentAlignment(vLayout, Alignment.TOP_LEFT);
		}

		public void addImage(Resource res)
		{
			Image img = new Image();
			img.setSource(res);
			img.addStyleName(Styles.smallMargin.toString());
			addComponentAsFirst(img);
			setComponentAlignment(img, Alignment.TOP_LEFT);
		}

		public void setTextWithPreview(String value)
		{
			setPreview(value);
			this.label.setValue(escapeHtmlAndPrepareForDisplaying(value));
		}
		
		private void setPreview(String value)
		{
			if (preview == null)
				return;
			for (Registration lisReg : listeners)
				lisReg.remove();
			listeners.clear();
			listeners.add(preview.addClickListener(event -> getUI().addWindow(
					new PreviewWindow(value, previewMode))));
		}

		private String escapeHtmlAndPrepareForDisplaying(String value)
		{
			return HtmlEscapers
					.escape(value)
					.replace("\n", "<br>")
					.replace("\t", Strings.repeat(HTML_SPACE, 4))
					.replace(" ", Strings.repeat(HTML_SPACE, 2));
		}
	}
	
	public static class PreviewWindow extends Window
	{
		public PreviewWindow(String contentToPreview, ContentMode contentMode)
		{
			Label htmlPreview = new Label();
			htmlPreview.setContentMode(contentMode);
			htmlPreview.setValue(contentToPreview);
			setContent(htmlPreview);
			setModal(true);
		}
	}
	
	public static Builder builder(UnityMessageSource msg, String caption)
	{
		return new Builder(msg, caption);
	}
	public static class Builder
	{
		private UnityMessageSource msg;
		private String caption;
		private ContentMode mode;
		
		public Builder(UnityMessageSource msg, String caption)
		{
			this.msg = msg;
			this.caption = caption;
		}
		public Builder withMode(ContentMode mode)
		{
			this.mode = mode;
			return this;
		}
		public I18nLabelWithPreview buildWithValue(I18nString content)
		{
			I18nLabelWithPreview label = new I18nLabelWithPreview(msg, caption, mode);
			label.setValue(content);
			return label;
		}
	}
}
