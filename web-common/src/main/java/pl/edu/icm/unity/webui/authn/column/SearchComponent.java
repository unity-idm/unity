/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

import java.util.function.Consumer;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Styles;

public class SearchComponent extends CustomComponent
{
	public SearchComponent(UnityMessageSource msg, Consumer<String> filterChangedCallback)
	{
		TextField search = new TextField();
		search.setPlaceholder(msg.getMessage("IdpSelectorComponent.filter"));
		search.addStyleName(Styles.vSmall.toString());
		search.addValueChangeListener(event -> filterChangedCallback.accept(search.getValue()));
		search.addStyleName("u-authn-search");
		setCompositionRoot(search);
		setWidthUndefined();
	}
}
