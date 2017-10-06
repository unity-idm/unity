/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.msgtemplate;

import java.util.function.Supplier;
import java.util.stream.Stream;

import com.vaadin.data.util.converter.Converter.ConversionException;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.i18n.I18nLabelWithPreview;

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
	private Supplier<String> bodyToShowInPreview;
	private ComboBox bodyType;
	private Component main;
	
	public MessageTypeComboBox(UnityMessageSource msg, Supplier<String> bodyToShowInPreview)
	{
		this.msg = msg;
		this.bodyToShowInPreview = bodyToShowInPreview;
		setCaption(msg.getMessage("MessageTemplatesEditor.bodyType"));
		setRequired(true);
		initUI();
	}

	private void initUI()
	{
		HorizontalLayout msgTypeWithPreviewLink = new HorizontalLayout();
		msgTypeWithPreviewLink.setSpacing(true);
		bodyType = new ComboBox();
		bodyType.setImmediate(true);
		bodyType.setValidationVisible(false);
		bodyType.setNullSelectionAllowed(false);
		Stream.of(MessageType.values()).forEach(bodyType::addItem);
		Button preview = new Button(msg.getMessage("MessageTemplateViewer.preview"));
		preview.setStyleName(Styles.vButtonLink.toString());
		bodyType.addValueChangeListener(event -> {
			MessageType selected = (MessageType) bodyType.getValue();
			if (selected == MessageType.HTML)
				preview.setVisible(true);
			else
				preview.setVisible(false);
		});
		preview.addClickListener(event -> getUI().addWindow(
				new I18nLabelWithPreview.HtmlPreviewWindow(bodyToShowInPreview.get())));
		msgTypeWithPreviewLink.addComponents(bodyType, preview);
		msgTypeWithPreviewLink.setComponentAlignment(preview, Alignment.MIDDLE_CENTER);
		main = msgTypeWithPreviewLink;
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
}
