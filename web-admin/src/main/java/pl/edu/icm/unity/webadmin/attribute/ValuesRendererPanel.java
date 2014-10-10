/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attribute;

import java.util.List;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.AttributeExt;
import pl.edu.icm.unity.types.basic.AttributeValueSyntax;
import pl.edu.icm.unity.webui.common.CompositeSplitPanel;
import pl.edu.icm.unity.webui.common.HtmlLabel;
import pl.edu.icm.unity.webui.common.attributes.WebAttributeHandler;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

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
	
	public ValuesRendererPanel(UnityMessageSource msg)
	{
		this.msg = msg;
		setMargin(new MarginInfo(false, false, false, true));
		setSizeFull();
		setSpacing(true);
	}
	
	public void removeValues()
	{
		removeAllComponents();
	}
	
	public void setValues(WebAttributeHandler<?> handler, AttributeExt<?> a)
	{
		removeValues();
		buildInfoView(a);
		List<?> values = a.getValues();
		if (values.size() > 1)
			buildMultiValueView(handler, a.getAttributeSyntax(), values);
		else if (values.size() == 1)
			buildSingleValueView(handler, a.getAttributeSyntax(), values.get(0));
		else
			buildNoValueView();
	}
	
	private void buildInfoView(AttributeExt<?> a)
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
		Panel infoPanel = new Panel(msg.getMessage("Attribute.info"));
		infoPanel.addStyleName(Reindeer.PANEL_LIGHT);
		infoPanel.setContent(contents);
		addComponent(infoPanel);
	}
	
	@SuppressWarnings("rawtypes")
	private void buildMultiValueView(final WebAttributeHandler handler, final AttributeValueSyntax<?> syntax, 
			List<?> values)
	{
		final CompositeSplitPanel main = new CompositeSplitPanel(true, false, 33);
		
		final ValuesTable valuesTable = new ValuesTable(msg);
		valuesTable.setValues(values, syntax, handler);
		main.setFirstComponent(valuesTable);
		
		valuesTable.setImmediate(true);
		valuesTable.addValueChangeListener(new ValueChangeListener()
		{
			@SuppressWarnings("unchecked")
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				Object selectedId = valuesTable.getValue();
				Object value = valuesTable.getItemById(selectedId);
				Component c = handler.getRepresentation(value, syntax);
				c.setSizeUndefined();
				
				Panel valuePanel = new Panel(msg.getMessage("Attribute.selectedValue"));
				valuePanel.addStyleName(Reindeer.PANEL_LIGHT);
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
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void buildSingleValueView(WebAttributeHandler handler, AttributeValueSyntax<?> syntax, 
			Object value)
	{
		Component c = handler.getRepresentation(value, syntax);
		c.setSizeUndefined();
		Panel valuePanel = new Panel(msg.getMessage("Attribute.value"));
		valuePanel.addStyleName(Reindeer.PANEL_LIGHT);
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
