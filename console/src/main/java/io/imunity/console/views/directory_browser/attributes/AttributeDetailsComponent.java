/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.console.views.directory_browser.attributes;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import pl.edu.icm.unity.base.attribute.AttributeExt;
import pl.edu.icm.unity.base.message.MessageSource;

import java.util.List;
import java.util.Optional;

class AttributeDetailsComponent extends VerticalLayout
{
	private static final int IMAGE_SCALE_PX = 500;

	private final MessageSource msg;

	AttributeDetailsComponent(MessageSource msg,
			WebAttributeHandler handler, AttributeExt a)
	{
		this.msg = msg;
		setSpacing(false);
		setValues(handler, a);
	}

	public void setValues(WebAttributeHandler handler, AttributeExt a)
	{
		buildInfoView(a);
		List<String> values = a.getValues();
		if (!values.isEmpty())
			buildMultiValueView(handler, values);
		else
			buildNoValueView();
	}

	private void buildInfoView(AttributeExt a)
	{
		String created = msg.getMessageNullArg("Attribute.creationDate", a.getCreationTs());
		String updated = msg.getMessageNullArg("Attribute.updatedDate", a.getUpdateTs());

		Div info = new Div();
		info.add(new Html(
				"<div>" + msg.getMessage(a.isDirect() ? "Attribute.direct" : "Attribute.fromStatement") + "</div>"));
		Optional.ofNullable(a.getRemoteIdp()).ifPresent(
				remoteIdp -> info.add(new Html("<div>" + msg.getMessage("Attribute.remoteIdp", remoteIdp) + "</div>")));
		Optional.ofNullable(a.getRemoteIdp()).ifPresent(translationProfile -> info.add(new Html(
				"<div>" + msg.getMessage("Attribute.translationProfile", translationProfile) + "</div>")));
		Span infoDate = new Span(created + " " + updated);
		VerticalLayout contents = new VerticalLayout();
		contents.setPadding(false);
		contents.setSpacing(false);
		contents.add(info);
		if (!created.isEmpty())
			contents.add(infoDate);
		add(contents);
	}

	private void buildMultiValueView(final WebAttributeHandler handler,
			List<String> values)
	{
		VerticalLayout contents = new VerticalLayout();
		contents.add(new Span(msg.getMessage("Attribute.values")));
		contents.setPadding(false);
		contents.setSpacing(true);

		for (String value : values)
			buildSingleValueView(contents, handler, value);
		add(contents);
	}

	private void buildSingleValueView(VerticalLayout contents, WebAttributeHandler handler, String value)
	{
		Component c = handler.getRepresentation(value,
				AttributeViewerContext.builder().withCustomWidth(100)
						.withCustomWidthUnit(Unit.PERCENTAGE).withImageScaleHeight(IMAGE_SCALE_PX)
						.withImageScaleWidth(IMAGE_SCALE_PX).build());
		contents.add(c);
	}

	private void buildNoValueView()
	{
		Span noValue = new Span(msg.getMessage("Attribute.noValue"));
		add(noValue);
	}

}
