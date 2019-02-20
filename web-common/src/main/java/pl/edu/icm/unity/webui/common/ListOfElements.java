/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common;

import java.util.List;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.webui.common.ListOfElementsWithActions.LabelConverter;

/**
 * Simple list of elements.
 * 
 * @author P.Piernik
 *
 */
public class ListOfElements<T> extends CustomComponent
{
	public ListOfElements(List<T> values, LabelConverter<T> converter)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		values.stream().forEach(e -> main.addComponent(converter.toLabel(e)));
		setCompositionRoot(main);
	}
}
