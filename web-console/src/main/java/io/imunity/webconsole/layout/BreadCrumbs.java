/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.layout;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

import io.imunity.webconsole.leftmenu.components.MenuButton;
import pl.edu.icm.unity.webui.common.Images;

/**
 * Breadcrumbs component
 * @author P.Piernik
 *
 */
public class BreadCrumbs extends HorizontalLayout implements MenuComponent<BreadCrumbs> {
	
	public static final String STYLE_NAME = "breadcrumbs";
	public static final String BREADCRUMB_SEPARATOR = Images.rightArrow.getHtml();
	
	private MenuButton root = null;
	
	public BreadCrumbs() {
		setStyleName(STYLE_NAME);
		setWidth(100, Unit.PERCENTAGE);
		setMargin(false);
		setSpacing(true);
	}
	
	public BreadCrumbs setRoot(MenuButton root) {
		this.root = MenuButton.get().withCaption(root.getCaption()).withStyleName("clickable").withClickListener(e -> root.click());
		add(this.root);
		return this;
	}
	
	public MenuButton getRoot() {
		return this.root;
	}
	
	public BreadCrumbs clear() {
		for (MenuComponent<?> menuComponent : getList()) {
			if (!menuComponent.equals(root)) {
				remove(menuComponent);
			}
		}
		return this;
	}

	public <C extends MenuComponent<?>> C add(C c) {
		addComponent(c);
		return c;
	}
	
	@Override
	public <C extends MenuComponent<?>> C addAsFirst(C c) {
		addComponentAsFirst(c);
		return c;
	}

	@Override
	public <C extends MenuComponent<?>> C addAt(C c, int index) {
		addComponent(c, index);
		return c;
	}

	@Override
	public int count() {
		return getList().size();
	}

	@Override
	public <C extends MenuComponent<?>> BreadCrumbs remove(C c) {
		removeComponent(c);
		return this;
	}

	@Override
	public List<MenuComponent<?>> getList() {
		List<MenuComponent<?>> menuComponentList = new ArrayList<MenuComponent<?>>();
		for (int i = 0; i < getComponentCount(); i++) {
			Component component = getComponent(i);
			if (component instanceof MenuComponent<?>) {
				menuComponentList.add((MenuComponent<?>) component);
			}
		}
		return menuComponentList;
	}

	@Override
	public String getRootStyle() {
		return null;
	}
}