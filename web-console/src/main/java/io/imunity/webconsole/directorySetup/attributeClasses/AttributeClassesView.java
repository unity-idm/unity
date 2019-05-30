/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.attributeClasses;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.DirectorySetupNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.basic.AttributesClass;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all attribute classes
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class AttributeClassesView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "AttributeClasses";

	private UnityMessageSource msg;
	private AttributeClassController controller;

	private GridWithActionColumn<AttributesClass> attributeClassGrid;

	@Autowired
	AttributeClassesView(UnityMessageSource msg, AttributeClassController controller)
	{
		this.msg = msg;
		this.controller = controller;

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(StandardButtonsHelper
				.build4AddAction(msg, e -> NavigationHelper.goToView(NewAttributeClassView.VIEW_NAME)));

		attributeClassGrid = new GridWithActionColumn<>(msg, getRowActionsHandlers(), false, false);
		attributeClassGrid.addComponentColumn(
				a -> StandardButtonsHelper.buildLinkButton(a.getName(), e -> gotoEdit(a)),
				msg.getMessage("AttributeClassesView.nameCaption"), 10);
		attributeClassGrid.addColumn(a -> String.join(", ", a.getAllowed()),
				msg.getMessage("AttributeClassesView.allowedCaption"), 10);
		attributeClassGrid.addColumn(a -> String.join(", ", a.getMandatory()),
				msg.getMessage("AttributeClassesView.mandatoryCaption"), 10);
	
		attributeClassGrid.addByClickDetailsComponent(a -> {
			{
				Label desc = new Label();
				desc.setCaption(msg.getMessage("AttributeClassesView.descriptionLabelCaption"));
				desc.setValue(a.getDescription());
				FormLayout wrapper = new FormLayout(desc);
				desc.setStyleName(Styles.wordWrap.toString());
				wrapper.setWidth(95, Unit.PERCENTAGE);
				return wrapper;
			}
		});
		
		
		attributeClassGrid.setSizeFull();
		attributeClassGrid.setItems(getAttributeClasses());

		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(attributeClassGrid);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);

		setCompositionRoot(main);
	}

	private List<SingleActionHandler<AttributesClass>> getRowActionsHandlers()
	{
		SingleActionHandler<AttributesClass> edit = SingleActionHandler.builder4Edit(msg, AttributesClass.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();
		SingleActionHandler<AttributesClass> remove = SingleActionHandler
				.builder4Delete(msg, AttributesClass.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(edit, remove);
	}

	private void remove(AttributesClass attrClass)
	{
		try
		{
			controller.removeAttributeClass(attrClass);
			attributeClassGrid.removeElement(attrClass);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void tryRemove(AttributesClass attrClass)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg, Sets.newHashSet(attrClass));
		new ConfirmDialog(msg, msg.getMessage("AttributeClassesView.confirmDelete", confirmText),
				() -> remove(attrClass)).show();
	}

	private void gotoEdit(AttributesClass a)
	{
		NavigationHelper.goToView(EditAttributeClassView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ a.getName());
	}

	private Collection<AttributesClass> getAttributeClasses()
	{
		try
		{
			return controller.getAttributeClasses();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.directorySetup.attributeClasses");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class AttributeClassesNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public AttributeClassesNavigationInfoProvider(UnityMessageSource msg,
				DirectorySetupNavigationInfoProvider parent,
				ObjectFactory<AttributeClassesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.directorySetup.attributeClasses"))
					.withPosition(30).build());

		}
	}
}
