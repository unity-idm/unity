/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.Locale;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.i18n.I18nLabelWithPreview.HtmlPreviewWindow;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;

/**
 * Custom field that acts as ComboBox, explicitly filled with values that
 * corresponds to {@link MessageType}s. When {@link MessageType#HTML} is
 * selected then a preview link is enable which opens an window with full blown
 * html preview, where content is taken from given supplier.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class MessageTypeComboBox extends CustomField<MessageType>
{
	private UnityMessageSource msg;
	private I18nTextArea i18nTextProvider;
	private ComboBox bodyType;
	private Component main;
	
	public MessageTypeComboBox(UnityMessageSource msg, I18nTextArea i18nTextProvider)
	{
		this.msg = msg;
		this.i18nTextProvider = i18nTextProvider;
		setCaption(msg.getMessage("MessageTemplatesEditor.bodyType"));
		setRequired(true);
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout msgTypeWithPreviewLinks = new HorizontalLayout();
		msgTypeWithPreviewLinks.setSpacing(true);
		bodyType = new ComboBox();
		bodyType.setImmediate(true);
		bodyType.setValidationVisible(false);
		bodyType.setNullSelectionAllowed(false);
		Stream.of(MessageType.values()).forEach(bodyType::addItem);
		
		HorizontalLayout contentWithLinks = new HorizontalLayout();
		contentWithLinks.setSpacing(true);
		bodyType.addValueChangeListener(event -> {
			MessageType selected = (MessageType) bodyType.getValue();
			if (selected == MessageType.HTML)
				contentWithLinks.setVisible(true);
			else
				contentWithLinks.setVisible(false);
		});
		
		msg.getEnabledLocales().values().stream()
			.map(Locale::toString)
			.forEach(localeKey -> 
			{
				HPairLayout pair = new HPairLayout(msg, 
						() -> i18nTextProvider.getValue().getValueRaw(localeKey));
				Resource image = Images.getFlagForLocale(localeKey);
				if (image != null)
					pair.addImage(image);
				
				if (!msg.getDefaultLocaleCode().equals(localeKey))
				{
					pair.setVisible(false);
					i18nTextProvider.addShowAllListener(show -> {
						pair.setVisible(show);
					});
				}
				
				contentWithLinks.addComponent(pair);
			});
		
		msgTypeWithPreviewLinks.addComponents(bodyType, contentWithLinks);
		msgTypeWithPreviewLinks.setComponentAlignment(contentWithLinks, Alignment.MIDDLE_CENTER);
		main = msgTypeWithPreviewLinks;
	}

	@Override
	protected Component initContent()
	{
		return main;
	}
	
	@Override
	public void setValue(MessageType newFieldValue) throws ReadOnlyException, ConversionException
	{
		bodyType.setValue(newFieldValue);
	}
	
	@Override
	public MessageType getValue()
	{
		return (MessageType) bodyType.getValue();
	}

	@Override
	public Class<? extends MessageType> getType()
	{
		return MessageType.class;
	}
	
	/**
	 * Horizontal pair layout with image and preview link.
	 * 
	 * @author Roman Krysinski (roman@unity-idm.eu)
	 */
	private class HPairLayout extends HorizontalLayout
	{
		private Button preview;
		
		public HPairLayout(UnityMessageSource msg, Supplier<String> contentProvider)
		{
			preview = new Button(msg.getMessage("MessageTemplateViewer.preview"));
			preview.setStyleName(Styles.vButtonLink.toString());
			preview.addClickListener(event -> getUI()
					.addWindow(new HtmlPreviewWindow(contentProvider.get())));
			addComponent(preview);
		}

		public void addImage(Resource res)
		{
			Image img = new Image();
			img.setSource(res);
			img.addStyleName(Styles.smallMargin.toString());
			addComponentAsFirst(img);
		}
	}
}
