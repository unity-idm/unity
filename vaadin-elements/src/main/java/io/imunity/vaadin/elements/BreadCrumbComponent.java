/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.elements;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.RouterLink;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.imunity.vaadin.elements.BreadCrumbParameter.BREAD_CRUMB_SEPARATOR;

public class BreadCrumbComponent extends Composite<Div>
{
	private final Stack<BreadCrumb> bredCrumbs = new Stack<>();
	private final List<MenuComponent> menuRouts;
	private final Function<String, String> msg;

	public BreadCrumbComponent(List<MenuComponent> menuRouts, Function<String, String> msg)
	{
		getContent().setClassName("u-breadcrumb");
		getContent().setSizeFull();
		this.menuRouts = menuRouts;
		this.msg = msg;
	}

	public void update(UnityViewComponent component)
	{
		Class<? extends UnityViewComponent> componentClass = component.getClass();
		BreadCrumb route = new BreadCrumb(componentClass, component.getDynamicParameter().orElse(null));
		
		getSubview(componentClass).ifPresent(menuElement ->
		{
			if (bredCrumbs.isEmpty()) {
				// entering page directly from URL link
				bredCrumbs.push(new BreadCrumb(menuElement.component, null));
			}
		});

		if(isChangingMenuViews(componentClass))
			bredCrumbs.removeAllElements();
		
		if(!bredCrumbs.contains(route))
			bredCrumbs.push(route);
		else if (bredCrumbs.peek().isParamChanged(route))
		{
			bredCrumbs.pop();
			bredCrumbs.push(route);
		}
		else
			while (!bredCrumbs.peek().equals(route))
				bredCrumbs.pop();
		updateView();
	}

	private Optional<MenuComponent> getSubview(Class<? extends UnityViewComponent> componentClass)
	{
		for (MenuComponent menu : menuRouts)
		{
			if (menu.subViews.contains(componentClass))
				return Optional.of(menu);
		}
		return Optional.empty();
	}

	private boolean isChangingMenuViews(Class<? extends UnityViewComponent> componentClass) {
		return menuRouts.stream()
				.flatMap(menu ->
				{
					if(menu.component != null)
						return Stream.of(menu.component);
					else
						return menu.subTabs.stream().map(tab -> tab.component);
				})
				.toList()
				.contains(componentClass);
	}

	private void updateView()
	{
		List<Component> components = new ArrayList<>();
		RouterLink firstRouterLink = createRouterLink(bredCrumbs.firstElement()).findFirst()
				.orElseThrow(() -> new IllegalStateException("Expecting bread crumbs to contain at least one element: " 
						+ bredCrumbs));
		List<Component> nextComponents = bredCrumbs.stream()
			.skip(1)
			.flatMap(this::createNextRouterLink)
			.distinct()
			.toList();
		components.add(firstRouterLink);
		components.addAll(nextComponents);

		getContent().removeAll();
		getContent().add(components.toArray(Component[]::new));
	}

	private Stream<Component> createNextRouterLink(BreadCrumb route)
	{
		return createRouterLink(route)
			.map(routerLink ->
			{
				Span span = new Span(" > ");
				span.add(routerLink);
				return span;
			});
	}

	private Stream<RouterLink> createRouterLink(BreadCrumb route)
	{
		return route.getBreadCrumbParameter()
			.map(p -> getRouterLink(route.getRouteClass(), p))
			.orElseGet(() -> Stream.of(new RouterLink(getBreadcrumb(route.getRouteClass()), route.getRouteClass())));
	}

	public String getBreadcrumb(Class<? extends Component> componentClass)
	{
		Breadcrumb annotation = componentClass.getAnnotation(Breadcrumb.class);
		if(!annotation.parent().isEmpty())
			return msg.apply(annotation.parent()) + BREAD_CRUMB_SEPARATOR + msg.apply(annotation.key());
		return msg.apply(annotation.key());
	}

	private Stream<RouterLink> getRouterLink(Class<? extends UnityViewComponent> routeClass, BreadCrumbParameter p)
	{
		RouterLink basicRoute = new RouterLink(p.name, routeClass, p.id);
		if(p.parameter != null)
		{
			RouterLink routerLink = new RouterLink(p.parameter, routeClass, p.id);
			routerLink.setQueryParameters(QueryParameters.simple(Map.of("tab", p.parameter)));
			return Stream.of(basicRoute, routerLink);
		}
		return Stream.of(basicRoute);
	}

	static class BreadCrumb
	{
		private final Class<? extends UnityViewComponent> routeClass;
		private final BreadCrumbParameter breadCrumbParameter;

		BreadCrumb(Class<? extends UnityViewComponent> routeClass, BreadCrumbParameter breadCrumbParameter)
		{
			this.routeClass = routeClass;
			this.breadCrumbParameter = breadCrumbParameter;
		}

		public Class<? extends UnityViewComponent> getRouteClass()
		{
			return routeClass;
		}

		public Optional<BreadCrumbParameter> getBreadCrumbParameter()
		{
			return Optional.ofNullable(breadCrumbParameter);
		}

		public boolean isParamChanged(BreadCrumb newParameter)
		{
			return breadCrumbParameter != null && breadCrumbParameter.id != null && breadCrumbParameter.id.equals(newParameter.breadCrumbParameter.id);
		}

		@Override
		public boolean equals(Object o)
		{
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			BreadCrumb that = (BreadCrumb) o;
			return Objects.equals(routeClass, that.routeClass) &&
					Objects.equals(breadCrumbParameter, that.breadCrumbParameter);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(routeClass, breadCrumbParameter);
		}

		@Override
		public String toString()
		{
			return "BreadCrumb{" +
					"routeClass=" + routeClass +
					", breadCrumbParameter=" + breadCrumbParameter +
					'}';
		}
	}
}
