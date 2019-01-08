/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.List;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.AttributeViewerContext;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Renders attribute values panel.
 * At the top always a small information is printed with attribute creation & update date, information if it is direct
 * or effective,  and source IdP and profile if available.
 * 
 * @author K. Benedyczak
 */
public class ValuesRendererPanel extends VerticalLayout
{
	private UnityMessageSource msg;
	private AttributeTypeSupport atSupport;
	
	public ValuesRendererPanel(UnityMessageSource msg, AttributeTypeSupport atSupport)
	{
		this.msg = msg;
		this.atSupport = atSupport;
		setMargin(new MarginInfo(false, false, false, true));
		setSizeFull();
		setSpacing(true);
	}
	
	public void removeValues()
	{
		removeAllComponents();
	}
	
	public void setValues(WebAttributeHandler handler, AttributeExt a)
	{
		removeValues();
		buildInfoView(a);
		List<String> values = a.getValues();
		AttributeValueSyntax<?> syntax = atSupport.getSyntax(a);
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
		SafePanel infoPanel = new SafePanel(msg.getMessage("Attribute.info"));
		infoPanel.addStyleName(Styles.vPanelLight.toString());
		infoPanel.setContent(contents);
		addComponent(infoPanel);
	}
	
	private void buildMultiValueView(final WebAttributeHandler handler, final AttributeValueSyntax<?> syntax, 
			List<String> values)
	{
		SafePanel valuePanel = new SafePanel(msg.getMessage("Attribute.values"));
		valuePanel.addStyleName(Styles.vPanelLight.toString());
		valuePanel.setSizeFull();
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);
		contents.setSpacing(false);
		contents.setSpacing(true);
		valuePanel.setContent(contents);
		for (String value: values)
			buildSingleValueView(contents, handler, syntax, value);
		addComponent(valuePanel);
		setExpandRatio(valuePanel, 1);
	}
	
	private <T> void buildSingleValueView(VerticalLayout contents, 
			WebAttributeHandler handler, AttributeValueSyntax<T> syntax, 
			String value)
	{
		Component c = handler.getRepresentation(value, AttributeViewerContext.EMPTY);
		c.setSizeUndefined();
		c.setWidth(100, Unit.PERCENTAGE);
		contents.addComponent(c);
	}
	
	private void buildNoValueView()
	{
		Label noValue = new Label(msg.getMessage("Attribute.noValue"));
		addComponent(noValue);
	}

}
