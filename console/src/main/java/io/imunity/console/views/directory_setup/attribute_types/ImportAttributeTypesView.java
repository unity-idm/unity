/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_types;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;

import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.OptionalParameter;
import com.vaadin.flow.router.Route;

import io.imunity.console.ConsoleMenu;
import io.imunity.console.views.ConsoleViewComponent;
import io.imunity.vaadin.elements.BreadCrumbParameter;
import io.imunity.vaadin.elements.NotificationPresenter;
import jakarta.annotation.security.PermitAll;
import pl.edu.icm.unity.base.attribute.AttributeType;
import pl.edu.icm.unity.base.message.MessageSource;
import io.imunity.vaadin.endpoint.common.exceptions.ControllerException;

/**
 * Import attribute type view
 * 
 * @author P.Piernik
 *
 */
@PermitAll
@Route(value = "/attribute-type/import", layout = ConsoleMenu.class)
class ImportAttributeTypesView extends ConsoleViewComponent
{
	public static final String VIEW_NAME = "ImportAttributeTypes";

	private final AttributeTypeController controller;
	private final NotificationPresenter notificationPresenter;
	private final MessageSource msg;
	private ImportAttributeTypeEditor editor;
	private BreadCrumbParameter breadCrumbParameter;

	@Autowired
	ImportAttributeTypesView(AttributeTypeController controller, MessageSource msg,
			NotificationPresenter notificationPresenter)
	{
		this.controller = controller;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String attributeTypeName)
	{

		breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("AttributeTypesView.import"));
		init();
	}

	public void init()
	{
		getContent().removeAll();
		try
		{
			editor = controller.getImportEditor();
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getDetails());
			UI.getCurrent().navigate(AttributeTypesView.class);
			return;
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.add(editor);

		getContent().add(
				new VerticalLayout(editor, createActionLayout(msg, msg.getMessage("ImportAttributeTypesView.import"), AttributeTypesView.class, this::onConfirm)));

	}

	private void onConfirm()
	{
		Set<AttributeType> ats;
		try
		{
			ats = editor.getAttributeTypes();
		} catch (Exception e)
		{
			notificationPresenter.showError("", e.getMessage());
			return;
		}

		try
		{
			controller.mergeAttributeTypes(ats, editor.isOverwriteMode());
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getDetails());
			return;
		}

		UI.getCurrent().navigate(AttributeTypesView.class);

	}
}
