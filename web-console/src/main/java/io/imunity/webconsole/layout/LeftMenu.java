/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.layout;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.server.VaadinSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

/**
 * Toggleable left menu
 * 
 * @author P.Piernik
 *
 */
public class LeftMenu extends VerticalLayout implements MenuComponent<VerticalLayout> {
	
	public static final String STYLE_NAME = "leftMenu";
	
	public LeftMenu() {
		super();
		setWidth(250, Unit.EM);
		setHeight(100, Unit.PERCENTAGE);
		setStyleName(STYLE_NAME);
		setMargin(false);
		setSpacing(false);
	}
	
	public LeftMenu toggleSize() {
		if (getToggleMode().equals(ToggleMode.NORMAL)) {
			setToggleMode(ToggleMode.MINIMAL);
		} else {
			setToggleMode(ToggleMode.NORMAL);
		}
		return this;
	}
	
	public LeftMenu setToggleMode(ToggleMode toggleMode) {
		if (toggleMode != null) {
			switch (toggleMode) {
				case MINIMAL:
					setWidth(50, Unit.EM);
					getParent().addStyleName(ToggleMode.MINIMAL.name().toLowerCase());
					break;
				case NORMAL:
					setWidth(250, Unit.EM);
					getParent().removeStyleName(ToggleMode.MINIMAL.name().toLowerCase());
					break;
			}
			VaadinSession session = VaadinSession.getCurrent();
			if (session != null) {
				session.setAttribute(ToggleMode.class, toggleMode);
			}
		}
		return this;
	}
	
	public ToggleMode getToggleMode() {
		VaadinSession session = VaadinSession.getCurrent();
		if (session != null) {
			ToggleMode toggleMode = session.getAttribute(ToggleMode.class);
			if (toggleMode != null) {
				return toggleMode;
			}
		}
		return ToggleMode.NORMAL;
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
	public <C extends MenuComponent<?>> LeftMenu remove(C c) {
		removeComponent(c);
		return this;
	}
	
	/*
	public HMButton addToFooter(HMButton c) {
		if (footer == null) {
			footer = new HorizontalLayout();
			footer.setMargin(false);
			footer.setSpacing(true);
			footer.setStyleName("footer");
			footer.setWidth(100, Unit.PERCENTAGE);
			addComponent(footer);
		}
		footer.addComponent(c);
		return c;
	}
	*/
	
	public LeftMenu remove(Component component) {
		removeComponent(component);
		return this;
	}
	
	/*
	public LeftMenu removeFromFooter(HMButton component) {
		if (footer != null) {
			footer.removeComponent(component);
		}
		return this;
	}
	
	public List<Component> getFooterComponents() {
		List<Component> componentsList = new ArrayList<Component>();
		if (footer != null) {
			for (int i = 0; i < footer.getComponentCount(); i++) {
				componentsList.add(footer.getComponent(i));
			}
		}
		return componentsList;
	}
	*/
	
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
		return super.getPrimaryStyleName();
	}
}