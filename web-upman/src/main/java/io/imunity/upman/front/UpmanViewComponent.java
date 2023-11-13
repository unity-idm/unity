/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.upman.front;

import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.*;
import io.imunity.upman.front.model.ProjectGroup;
import io.imunity.vaadin.elements.UnityViewComponent;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppDisplayedName;

public abstract class UpmanViewComponent extends UnityViewComponent implements HasUrlParameter<String>, AfterNavigationObserver, HasDynamicTitle
{
	public final String pageTitle = getCurrentWebAppDisplayedName();

	public UpmanViewComponent()
	{
		if(ComponentUtil.getData(UI.getCurrent(), ProjectGroup.class) == null)
			return;
		getContent().setClassName("u-view");
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
