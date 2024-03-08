/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.sub_view_switcher;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.imunity.vaadin.elements.AfterSubNavigationEvent;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.UnityViewComponent;
import io.imunity.vaadin.endpoint.common.api.SubViewSwitcher;
import io.imunity.vaadin.endpoint.common.api.UnitySubView;

import java.util.function.Consumer;

public class DefaultSubViewSwitcher implements SubViewSwitcher
{
	private final UnityViewComponent viewComponent;
	private final VerticalLayout mainView;
	private final VerticalLayout unsavedInfoBanner;
	private final BreadCrumbParameter original;
	private final Consumer<BreadCrumbParameter> breadCrumbParameterSetter;

	private UnitySubView lastSubView;
	private Component currentSubView;
	private boolean bannerInfoVisibility;

	public DefaultSubViewSwitcher(UnityViewComponent viewComponent, VerticalLayout mainView,
			VerticalLayout unsavedInfoBanner, BreadCrumbParameter original,
			Consumer<BreadCrumbParameter> breadCrumbParameterSetter)
	{
		this.viewComponent = viewComponent;
		this.mainView = mainView;
		this.unsavedInfoBanner = unsavedInfoBanner;
		this.original = original;
		this.breadCrumbParameterSetter = breadCrumbParameterSetter;
	}

	@Override
	public void exitSubView()
	{
		viewComponent.getContent().remove(currentSubView);
		unsavedInfoBanner.setVisible(bannerInfoVisibility);
		if(lastSubView != null)
		{
			viewComponent.getContent().add((Component) lastSubView);
			setBreadcrumb(lastSubView);
			currentSubView = (Component) lastSubView;
			lastSubView = null;
		}
		else
		{
			mainView.setVisible(true);
			breadCrumbParameterSetter.accept(original);
			currentSubView = null;
		}
		ComponentUtil.fireEvent(UI.getCurrent(), new AfterSubNavigationEvent(viewComponent, false));
	}

	@Override
	public void exitSubViewAndShowUpdateInfo()
	{
		exitSubView();
		bannerInfoVisibility = true;
		unsavedInfoBanner.setVisible(bannerInfoVisibility);
	}

	@Override
	public void goToSubView(UnitySubView subview)
	{
		if(currentSubView != null)
		{
			viewComponent.getContent().remove(currentSubView);
			lastSubView = (UnitySubView) currentSubView;
		}
		currentSubView = (Component)subview;
		unsavedInfoBanner.setVisible(false);
		mainView.setVisible(false);
		viewComponent.getContent().add(currentSubView);
		setBreadcrumb(subview);
		ComponentUtil.fireEvent(UI.getCurrent(), new AfterSubNavigationEvent(viewComponent, false));
	}

	private void setBreadcrumb(UnitySubView subview)
	{
		if(subview.getBreadcrumbs().size() == 1)
			breadCrumbParameterSetter.accept(
					new BreadCrumbParameter(subview.getBreadcrumbs().get(0), subview.getBreadcrumbs().get(0), null, true));
		else
			breadCrumbParameterSetter.accept(new BreadCrumbParameter(subview.getBreadcrumbs().get(0), subview.getBreadcrumbs().get(0), subview.getBreadcrumbs().get(1), true));
	}
}
