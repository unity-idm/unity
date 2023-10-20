/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_classes;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.server.Sizeable;
import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.console.views.authentication.facilities.AuthenticationFlowEditView;
import io.imunity.vaadin.elements.Breadcrumb;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.attribute.AttributesClass;
import pl.edu.icm.unity.base.describedObject.DescribedObjectROImpl;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;

import java.util.Comparator;

import static io.imunity.vaadin.elements.VaadinClassNames.GRID_DETAILS_FORM;
import static io.imunity.vaadin.elements.VaadinClassNames.GRID_DETAILS_FORM_ITEM;

@PermitAll
@Breadcrumb(key = "WebConsoleMenu.directorySetup.attributeClasses")
@Route(value = "/attribute-classes/edit", layout = ConsoleMenu.class)
public class AttributeClassesEditView extends ConsoleViewComponent
{
	private final MessageSource msg;
	private final AttributeClassController controller;
	private Grid<AttributesClass> attributesClassGrid;

	AttributeClassesEditView(MessageSource msg, AttributeClassController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}
}
