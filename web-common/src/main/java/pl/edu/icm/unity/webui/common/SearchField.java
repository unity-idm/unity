/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.base.message.MessageSource;

/**
 * Text field with clear button
 * 
 * @author P.Piernik
 *
 */
public class SearchField extends CustomComponent
{
	private TextField searchText;

	public SearchField(MessageSource msg)
	{

		HorizontalLayout wrapper = new HorizontalLayout();
		wrapper.setMargin(false);
		wrapper.setSpacing(false);

		Button clear = new Button();
		clear.addStyleName(Styles.vButtonLink.toString());
		clear.addStyleName("u-clearTextField");
		clear.setIcon(Images.close_small.getResource());
		clear.addClickListener(e -> searchText.clear());
		clear.setVisible(false);
		
		searchText = new TextField();
		searchText.addStyleName(Styles.vSmall.toString());
		searchText.setWidth(14, Unit.EM);
		searchText.setPlaceholder(msg.getMessage("search"));
		searchText.addValueChangeListener(e -> {
			clear.setVisible(e.getValue() != null && !e.getValue().isEmpty());
		});
		wrapper.addComponent(searchText);
		wrapper.addComponent(clear);

		setCompositionRoot(wrapper);
		setWidthUndefined();
	}

	public void addValueChangeListener(ValueChangeListener<String> listener)
	{
		searchText.addValueChangeListener(listener);
	}
	
	public void clear()
	{
		searchText.setValue("");
	}
	
	public void setSearchTextWidth(float width, Unit unit)
	{
		searchText.setWidth(width, unit);
	}
}
