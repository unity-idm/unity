/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.attribute_types;

import static io.imunity.console.views.EditViewActionLayoutFactory.createActionLayout;

import java.util.Optional;

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
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Edit attribute type view
 * 
 * @author P.Piernik
 *
 */
@PermitAll
@Route(value = "/attribute-type/edit", layout = ConsoleMenu.class)
class EditAttributeTypeView extends ConsoleViewComponent
{
	private final AttributeTypeController controller;
	private final MessageSource msg;
	private final NotificationPresenter notificationPresenter;
	
	private BreadCrumbParameter breadCrumbParameter;
	private AttributeTypeEditor editor;


	@Autowired
	EditAttributeTypeView(AttributeTypeController controller, MessageSource msg,
			NotificationPresenter notificationPresenter)
	{
		this.controller = controller;
		this.msg = msg;
		this.notificationPresenter = notificationPresenter;
	}

	@Override
	public void setParameter(BeforeEvent event, @OptionalParameter String attributeTypeName)
	{
		getContent().removeAll();

		AttributeType at;
		try
		{
			at = controller.getAttributeType(attributeTypeName);
			breadCrumbParameter = new BreadCrumbParameter(attributeTypeName, attributeTypeName);
			
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause()
					.getMessage());
			return;
		}

		initUI(at);
	}
	
	@Override
	public Optional<BreadCrumbParameter> getDynamicParameter()
	{
		return Optional.ofNullable(breadCrumbParameter);
	}
	
	private void initUI(AttributeType at)
	{

		editor = controller.getEditor(at);

		getContent().add(new VerticalLayout(editor.getComponent(),
				createActionLayout(msg, true, AttributeTypesView.class, this::onConfirm)));
	}

	private void onConfirm()
	{
		AttributeType at;
		try
		{
			at = editor.getAttributeType();
		} catch (Exception e)
		{
			return;
		}

		try
		{
			controller.updateAttributeType(at);
		} catch (ControllerException e)
		{
			notificationPresenter.showError(e.getCaption(), e.getCause().getMessage());
			return;
		}

		UI.getCurrent()
				.navigate(AttributeTypesView.class);

	}
}
