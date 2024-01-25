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
import pl.edu.icm.unity.base.attribute.IllegalAttributeTypeException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * New attribute type view
 * 
 * @author P.Piernik
 *
 */
@PermitAll
@Route(value = "/attribute-type/new", layout = ConsoleMenu.class)
class NewAttributeTypeView extends ConsoleViewComponent
{
	private final AttributeTypeController controller;
	private final MessageSource msg;
	private RegularAttributeTypeEditor editor;
	private final NotificationPresenter notificationPresenter;

	private BreadCrumbParameter breadCrumbParameter;

	@Autowired
	NewAttributeTypeView(AttributeTypeController controller, MessageSource msg,
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
		getContent().removeAll();

		AttributeType at = null;
		breadCrumbParameter = new BreadCrumbParameter(null, msg.getMessage("new"));
		if (attributeTypeName != null)
		{
			try
			{
				at = controller.getAttributeType(attributeTypeName);
				

			} catch (ControllerException e)
			{
				notificationPresenter.showError(e.getCaption(), e.getCause()
						.getMessage());
				return;
			}
		}

		initUI(Optional.ofNullable(at));
	}

	public void initUI(Optional<AttributeType> attributeType)
	{

		editor = controller.getRegularAttributeTypeEditor(attributeType.orElse(null));
		if (attributeType.isPresent())
		{
			editor.setCopyMode();
		}

		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.add(editor.getComponent());

		getContent().add(new VerticalLayout(editor.getComponent(),
				createActionLayout(msg, false, AttributeTypesView.class, this::onConfirm)));
	}

	private void onConfirm()
	{
		AttributeType at;
		try
		{
			at = editor.getAttributeType();
		} catch (IllegalAttributeTypeException e)
		{
			return;
		}

		try
		{
			controller.addAttributeType(at);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}

		UI.getCurrent()
				.navigate(AttributeTypesView.class);

	}

}
