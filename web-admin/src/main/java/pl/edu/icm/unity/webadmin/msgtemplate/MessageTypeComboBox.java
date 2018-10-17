/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

import com.vaadin.server.Resource;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.CustomField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.i18n.I18nLabelWithPreview.PreviewWindow;

/**
 * Custom field that acts as ComboBox, explicitly filled with values that
 * corresponds to {@link MessageType}s. Besides type selection also shows preview buttons for all
 * enabled locales.
 * TODO - move preview buttons out from this component. 
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
public class MessageTypeComboBox extends CustomField<MessageType>
{
	private UnityMessageSource msg;
	private Function<String, String> i18nTextProvider;
	private ComboBox<MessageType> bodyType;
	private Component main;
	
	public MessageTypeComboBox(UnityMessageSource msg, Function<String, String> i18nTextProvider)
	{
		this.msg = msg;
		this.i18nTextProvider = i18nTextProvider;
		setCaption(msg.getMessage("MessageTemplatesEditor.bodyType"));
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout msgTypeWithPreviewLinks = new HorizontalLayout();
		msgTypeWithPreviewLinks.setSpacing(true);
		bodyType = new ComboBox<MessageType>();
		bodyType.setEmptySelectionAllowed(false);
		bodyType.setItems(MessageType.values());
		bodyType.addValueChangeListener(e -> fireEvent(e));
		HorizontalLayout contentWithLinks = new HorizontalLayout();
		contentWithLinks.setSpacing(true);
		msg.getEnabledLocales().values().stream()
			.map(Locale::toString)
			.forEach(localeKey -> 
			{
				HPairLayout pair = new HPairLayout(msg, 
						() -> i18nTextProvider.apply(localeKey));
				Resource image = Images.getFlagForLocale(localeKey);
				if (image != null)
					pair.addImage(image);
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
	public MessageType getValue()
	{
		return bodyType.getValue();
	}
	
	@Override
	protected void doSetValue(MessageType value)
	{
		bodyType.setValue(value);	
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
					.addWindow(new PreviewWindow(contentProvider.get(), getMode())));
			addComponent(preview);
		}

		private ContentMode getMode()
		{
			return getValue() == MessageType.HTML ? 
					ContentMode.HTML : ContentMode.PREFORMATTED;		
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
