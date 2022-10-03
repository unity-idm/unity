/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.front;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.*;
import io.imunity.upman.front.model.ProjectGroup;

import static io.imunity.vaadin23.endpoint.common.Vaadin23WebAppContext.getCurrentWebAppDisplayedName;

public abstract class UnityViewComponent extends Composite<Div> implements HasUrlParameter<String>, AfterNavigationObserver, HasDynamicTitle
{
	public final String pageTitle = getCurrentWebAppDisplayedName();

	public UnityViewComponent()
	{
		if(ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class) == null)
			return;
		getContent().setClassName("unity-view");
		getContent().setHeightFull();
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String parameter)
	{
	}

	@Override
	public void afterNavigation(AfterNavigationEvent event)
	{
		loadData();
	}

	@Override
	public String getPageTitle() {
		return pageTitle;
	}

	public abstract void loadData();
}
