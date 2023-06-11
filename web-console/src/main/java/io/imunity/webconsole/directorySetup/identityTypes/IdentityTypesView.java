/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.identityTypes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.directorySetup.DirectorySetupNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.entity.IdentityType;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all identity types
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class IdentityTypesView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "IdentityTypes";

	private MessageSource msg;
	private IdentityTypesController controller;

	private GridWithActionColumn<IdentityTypeEntry> identityTypesGrid;

	@Autowired
	IdentityTypesView(MessageSource msg, IdentityTypesController controller)
	{
		this.msg = msg;
		this.controller = controller;

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		identityTypesGrid = new GridWithActionColumn<>(msg, getRowActionsHandlers(), false, false);
		identityTypesGrid.addShowDetailsColumn(i -> getDetailsComponent(i));
		identityTypesGrid
				.addComponentColumn(
						i -> StandardButtonsHelper.buildLinkButton(i.type.getName(),
								e -> gotoEdit(i.type)),
						msg.getMessage("IdentityTypesView.nameCaption"), 10)
				.setSortable(true).setComparator((i1, i2) -> {
					return i1.type.getName().compareTo(i2.type.getName());
				}).setId("name");
		identityTypesGrid.addCheckboxColumn(i -> i.typeDefinition.isDynamic(),
				msg.getMessage("IdentityTypesView.automaticCaption"), 10);
		identityTypesGrid.addCheckboxColumn(i -> i.type.isSelfModificable(),
				msg.getMessage("IdentityTypesView.modifiableByUserCaption"), 10);
		identityTypesGrid.setSizeFull();
		
		identityTypesGrid.setItems(getIdentityTypes());
		identityTypesGrid.sort("name");

		VerticalLayout main = new VerticalLayout();
		main.addComponent(identityTypesGrid);
		main.setSizeFull();
		main.setMargin(false);
		setCompositionRoot(main);
		setSizeFull();
	}

	private FormLayout getDetailsComponent(IdentityTypeEntry i)
	{
		HtmlSimplifiedLabel desc = new HtmlSimplifiedLabel();
		desc.setCaption(msg.getMessage("IdentityTypesView.descriptionLabelCaption"));
		desc.setValue(i.type.getDescription());
		FormLayout wrapper = new FormLayout(desc);
		desc.setStyleName(Styles.wordWrap.toString());
		wrapper.setWidth(95, Unit.PERCENTAGE);
		return wrapper;
	}

	private Collection<IdentityTypeEntry> getIdentityTypes()
	{
		try
		{
			return controller.getIdentityTypes();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private List<SingleActionHandler<IdentityTypeEntry>> getRowActionsHandlers()
	{
		SingleActionHandler<IdentityTypeEntry> edit = SingleActionHandler
				.builder4Edit(msg, IdentityTypeEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next().type)).build();

		return Arrays.asList(edit);
	}

	private void gotoEdit(IdentityType idType)
	{
		NavigationHelper.goToView(EditIdentityTypeView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ idType.getName());
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.directorySetup.identityTypes");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class IdentityTypesNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;
		
		@Autowired
		public IdentityTypesNavigationInfoProvider(MessageSource msg,
				ObjectFactory<IdentityTypesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(DirectorySetupNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.directorySetup.identityTypes"))
					.withIcon(Images.clipboard_user.getResource())
					.withPosition(20).build());

		}
	}
}
