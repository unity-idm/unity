/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.List;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler.RepresentationSize;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Renders attribute values panel.
 * At the top always a small information is printed with attribute creation & update date, information if it is direct
 * or effective,  and source IdP and profile if available.
 * 
 * When the attribute has only a single value then a simple panel is displayed with the only value.
 * When the attribute has more then one value, then a table with values is displayed in the top part,
 * and the actually selected value in the bottom part.
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
		if (values.size() > 1)
			buildMultiValueView(handler, syntax, values);
		else if (values.size() == 1)
			buildSingleValueView(handler, syntax, values.get(0));
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
		final CompositeSplitPanel main = new CompositeSplitPanel(true, false, 33);
		
		final ValuesTable valuesTable = new ValuesTable(msg);
		valuesTable.setValues(values, handler);
		main.setFirstComponent(valuesTable);
		
		valuesTable.setImmediate(true);
		valuesTable.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Object selectedId = valuesTable.getValue();
				String value = valuesTable.getItemById(selectedId);
				Component c = handler.getRepresentation(value, RepresentationSize.ORIGINAL);
				c.setSizeUndefined();
				
				SafePanel valuePanel = new SafePanel(msg.getMessage("Attribute.selectedValue"));
				valuePanel.addStyleName(Styles.vPanelLight.toString());
				valuePanel.setContent(c);
				valuePanel.setSizeFull();
				main.setSecondComponent(valuePanel);
			}
		});
		Object firstId = valuesTable.getItemIds().iterator().next();
		valuesTable.select(firstId);
		addComponent(main);
		setExpandRatio(main, 1);
	}
	
	private <T> void buildSingleValueView(WebAttributeHandler handler, AttributeValueSyntax<T> syntax, 
			String value)
	{
		Component c = handler.getRepresentation(value, RepresentationSize.ORIGINAL);
		c.setSizeUndefined();
		SafePanel valuePanel = new SafePanel(msg.getMessage("Attribute.value"));
		valuePanel.addStyleName(Styles.vPanelLight.toString());
		valuePanel.setSizeFull();
		valuePanel.setContent(c);
		addComponent(valuePanel);
		setExpandRatio(valuePanel, 1);
	}
	
	private void buildNoValueView()
	{
		Label noValue = new Label(msg.getMessage("Attribute.noValue"));
		addComponent(noValue);
	}

}
