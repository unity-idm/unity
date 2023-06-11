/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.webconsole.directoryBrowser.attributes;

import java.util.List;

import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Renders attribute values panel. At the top always a small information is
 * printed with attribute creation & update date, information if it is direct or
 * effective, and source IdP and profile if available.
 * 
 * @author K. Benedyczak
 */
public class AttributeDetailsComponent extends VerticalLayout
{
	private static final int IMAGE_SCALE = 500;

	private MessageSource msg;

	public AttributeDetailsComponent(MessageSource msg, AttributeValueSyntax<?> syntax,
			WebAttributeHandler handler, AttributeExt a)
	{
		this.msg = msg;
		setSpacing(false);
		setValues(syntax, handler, a);

	}

	public void setValues(AttributeValueSyntax<?> syntax, WebAttributeHandler handler, AttributeExt a)
	{
		buildInfoView(a);
		List<String> values = a.getValues();
		if (values.size() > 0)
			buildMultiValueView(handler, syntax, values);
		else
			buildNoValueView();
	}

	private void buildInfoView(AttributeExt a)
	{
		String created = msg.getMessageNullArg("Attribute.creationDate", a.getCreationTs());
		String updated = msg.getMessageNullArg("Attribute.updatedDate", a.getUpdateTs());

		HtmlLabel info = new HtmlLabel(msg);
		info.addHtmlValue(a.isDirect() ? "Attribute.direct" : "Attribute.effective");
		info.addHtmlValue("Attribute.remoteIdp", a.getRemoteIdp());
		info.addHtmlValue("Attribute.translationProfile", a.getTranslationProfile());

		Label infoDate = new Label(created + " " + updated);
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);
		contents.setSpacing(false);
		contents.addComponent(info);
		if (!created.equals(""))
			contents.addComponent(infoDate);
		addComponent(contents);
	}

	private void buildMultiValueView(final WebAttributeHandler handler, final AttributeValueSyntax<?> syntax,
			List<String> values)
	{
		VerticalLayout contents = new VerticalLayout();
		contents.addComponent(new Label(msg.getMessage("Attribute.values")));
		contents.setMargin(false);
		contents.setSpacing(true);

		for (String value : values)
			buildSingleValueView(contents, handler, syntax, value);
		addComponent(contents);
	}

	private <T> void buildSingleValueView(VerticalLayout contents, WebAttributeHandler handler,
			AttributeValueSyntax<T> syntax, String value)
	{
		Component c = handler.getRepresentation(value,
				AttributeViewerContext.builder().withCustomWidth(100)
						.withCustomWidthUnit(Unit.PERCENTAGE).withImageScaleHeight(IMAGE_SCALE)
						.withImageScaleWidth(IMAGE_SCALE).build());
		contents.addComponent(c);
	}

	private void buildNoValueView()
	{
		Label noValue = new Label(msg.getMessage("Attribute.noValue"));
		addComponent(noValue);
	}

}
