/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webelements.navigation;

import java.util.LinkedList;

import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;

import io.imunity.webelements.menu.MenuButton;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

/**
 * Base for all views which contains subviews. Implements
 * {@link SubViewSwitcher} so updates breadcrumbs when subview is changed.
 * 
 * @author P.Piernik
 *
 */
public abstract class ViewWithSubViewBase extends CustomComponent implements SubViewSwitcher, UnityViewWithSubViews

{
	private LinkedList<UnitySubView> subViews;
	private Component mainView;
	private BreadcrumbsComponent breadCrumbs;
	private WarnComponent warnComponent;

	protected final MessageSource msg;
	
	public ViewWithSubViewBase(MessageSource msg)
	{
		this.msg = msg;
		subViews = new LinkedList<>();
		breadCrumbs = new BreadcrumbsComponent();
		warnComponent = new WarnComponent();
		breadCrumbs.setMargin(false);
	}

	protected void setMainView(Component mainView)
	{
		this.mainView = mainView;
		setCompositionRoot(mainView);
	}

	@Override
	public void exitSubView()
	{
		subViews.pollLast();
		if (subViews.isEmpty())
		{
			setCompositionRoot(mainView);
		} else
		{
			setCompositionRoot(subViews.getLast());
		}
		refreshBreadCrumbs();
	}

	@Override
	public void exitSubViewAndShowUpdateInfo()
	{
		warnComponent.setWarn(msg.getMessage("ViewWithSubViewBase.unsavedEdits"));
		exitSubView();
	}

	@Override
	public WarnComponent getWarnComponent()
	{
		return warnComponent;
			
	}

	@Override
	public void goToSubView(UnitySubView subview)
	{
		subViews.add(subview);
		setCompositionRoot(subview);
		refreshBreadCrumbs();
	}

	protected void refreshBreadCrumbs()
	{
		breadCrumbs.removeAllComponents();
		breadCrumbs.addComponent(MenuButton.get(getDisplayedName()).withCaption(getDisplayedName()));
		
		for (UnitySubView subView : subViews)
		{
			subView.getBredcrumbs().forEach(b -> {
				breadCrumbs.addSeparator();
				breadCrumbs.addComponent(MenuButton.get(b).withCaption(b));
			});
		}
	}
	
	protected LinkedList<UnitySubView> getSubViews()
	{
		return subViews;
	}

	@Override
	public BreadcrumbsComponent getBreadcrumbsComponent()
	{
		return breadCrumbs;
	}
	
	public abstract String getDisplayedName();


}
