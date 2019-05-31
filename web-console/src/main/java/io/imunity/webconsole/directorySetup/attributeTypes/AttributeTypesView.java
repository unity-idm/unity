/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.attributeTypes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.vaadin.simplefiledownloader.SimpleFileDownloader;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
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
import pl.edu.icm.unity.webui.common.ConfirmWithOptionDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.HamburgerMenu;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SidebarStyles;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.common.i18n.I18nLabel;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all attribute types
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class AttributeTypesView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "AttributeTypes";

	private UnityMessageSource msg;
	private AttributeTypeController controller;
	private GridWithActionColumn<AttributeTypeEntry> attrTypesGrid;

	@Autowired
	AttributeTypesView(UnityMessageSource msg, AttributeTypeController controller)
	{
		this.msg = msg;
		this.controller = controller;

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper
				.buildTopButtonsBar(
						StandardButtonsHelper.buildButton(
								msg.getMessage("AttributeTypesView.import"),
								Images.download,
								e -> NavigationHelper.goToView(
										ImportAttributeTypesView.VIEW_NAME)),
						StandardButtonsHelper.build4AddAction(msg, e -> NavigationHelper
								.goToView(NewAttributeTypeView.VIEW_NAME)));

		attrTypesGrid = new GridWithActionColumn<>(msg, getActionsHandlers(), false, true);
		attrTypesGrid.addShowDetailsColumn(a -> getDetailsComponent(a));
		attrTypesGrid.addComponentColumn(at -> {

			if (at.isEditable())
			{
				return StandardButtonsHelper.buildLinkButton(at.attributeType.getName(),
						e -> gotoEdit(at));
			} else
			{
				return new Label(at.attributeType.getName());
			}
		}, msg.getMessage("AttributeTypesView.nameCaption"), 10).setId("name").setSortable(true).setComparator((a1, a2) -> {
			return a1.attributeType.getName().compareTo(a2.attributeType.getName());
		});

		attrTypesGrid.addSortableColumn(at -> at.getDisplayedName(),
				msg.getMessage("AttributeTypesView.displayedNameCaption"), 10);
		attrTypesGrid.addSortableColumn(at -> at.attributeType.getValueSyntax(),
				msg.getMessage("AttributeTypesView.typeCaption"), 10);

		attrTypesGrid.addCheckboxColumn(at -> at.attributeType.isSelfModificable(),
				msg.getMessage("AttributeTypesView.selfModifiableCaption"), 10);

		attrTypesGrid.addSortableColumn(at -> at.getBoundsDesc(),
				msg.getMessage("AttributeTypesView.cardinalityCaption"), 10).setSortable(true);

		attrTypesGrid.addCheckboxColumn(at -> at.attributeType.isUniqueValues(),
				msg.getMessage("AttributeTypesView.uniqueValuesCaption"), 10).setSortable(true);

		attrTypesGrid.addHamburgerActions(getHamburgerActionsHandlers());
		attrTypesGrid.setMultiSelect(true);

		attrTypesGrid.setItems(getAttributeTypes());
		attrTypesGrid.sort("name");
		
		HamburgerMenu<AttributeTypeEntry> hamburgerMenu = new HamburgerMenu<>();
		hamburgerMenu.addStyleName(SidebarStyles.sidebar.toString());
		hamburgerMenu.addActionHandlers(getHamburgerCommonHandlers());
		attrTypesGrid.addSelectionListener(hamburgerMenu.getSelectionListener());		
		
		TextField search = FilterableGridHelper.generateSearchField(attrTypesGrid, msg);

		VerticalLayout gridWrapper = new VerticalLayout();
		gridWrapper.setMargin(false);
		gridWrapper.setSpacing(false);
		HorizontalLayout hamburgerAndSearchWrapper = new HorizontalLayout(hamburgerMenu, search);
		hamburgerAndSearchWrapper.setWidth(100, Unit.PERCENTAGE);
		hamburgerAndSearchWrapper.setComponentAlignment(hamburgerMenu, Alignment.BOTTOM_LEFT);
		hamburgerAndSearchWrapper.setComponentAlignment(search, Alignment.BOTTOM_RIGHT);
		hamburgerAndSearchWrapper.setMargin(false);
		hamburgerAndSearchWrapper.setSpacing(false);
		gridWrapper.addComponent(hamburgerAndSearchWrapper);
		gridWrapper.setExpandRatio(hamburgerAndSearchWrapper, 0);
		gridWrapper.addComponent(attrTypesGrid);
		gridWrapper.setExpandRatio(attrTypesGrid, 2);
		gridWrapper.setSizeFull();

		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(gridWrapper);
		main.setSizeFull();		
		main.setMargin(false);
		setCompositionRoot(main);
	}
	
	private FormLayout getDetailsComponent(AttributeTypeEntry a)
	{
		I18nLabel desc = new I18nLabel(msg);
		desc.setCaption(msg.getMessage("AttributeTypesView.descriptionLabelCaption"));
		desc.setValue(a.attributeType.getDescription());
		FormLayout wrapper = new FormLayout(desc);
		desc.setStyleName(Styles.wordWrap.toString());
		wrapper.setWidth(95, Unit.PERCENTAGE);
		return wrapper;
	}

	
	private Collection<AttributeTypeEntry> getAttributeTypes()
	{
		try
		{
			return controller.getAttributeTypes();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private List<SingleActionHandler<AttributeTypeEntry>> getActionsHandlers()
	{
		SingleActionHandler<AttributeTypeEntry> edit = SingleActionHandler
				.builder4Edit(msg, AttributeTypeEntry.class)
				.withDisabledPredicate(at -> (at.isEditable()))
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		return Arrays.asList(edit);
	}

	private List<SingleActionHandler<AttributeTypeEntry>> getHamburgerActionsHandlers()
	{
		SingleActionHandler<AttributeTypeEntry> copy = SingleActionHandler
				.builder4Copy(msg, AttributeTypeEntry.class)
				.withDisabledPredicate(at -> at.attributeType.isTypeImmutable())
				.withHandler(at -> goToCopy(at.iterator().next())).build();
		List<SingleActionHandler<AttributeTypeEntry>> hamburgerHandlers = new ArrayList<>();
		hamburgerHandlers.add(copy);
		hamburgerHandlers.addAll(getHamburgerCommonHandlers());
		return hamburgerHandlers;

	}

	private List<SingleActionHandler<AttributeTypeEntry>> getHamburgerCommonHandlers()
	{

		SingleActionHandler<AttributeTypeEntry> remove = SingleActionHandler
				.builder4Delete(msg, AttributeTypeEntry.class)
				.withDisabledPredicate(at -> at.attributeType.isTypeImmutable())
				.withHandler(this::tryRemove).build();

		SingleActionHandler<AttributeTypeEntry> export = SingleActionHandler.builder(AttributeTypeEntry.class)
				.multiTarget().withCaption(msg.getMessage("AttributeTypesView.export"))
				.withDisabledPredicate(at -> at.attributeType.isTypeImmutable())
				.withIcon(Images.export.getResource()).withHandler(this::export).build();

		return Arrays.asList(export, remove);

	}

	private void tryRemove(Set<AttributeTypeEntry> items)
	{
		String confirmText = MessageUtils.createConfirmFromNames(msg,
				items.stream().map(a -> a.attributeType).collect(Collectors.toSet()));
		new ConfirmWithOptionDialog(msg, msg.getMessage("AttributeTypesView.confirmDelete", confirmText),
				msg.getMessage("AttributeTypesView.withInstances"), withInstances -> {
					remove(items, withInstances);
				}).show();
	}

	private void remove(Set<AttributeTypeEntry> items, boolean withInstances)
	{
		try
		{
			controller.removeAttributeTypes(
					items.stream().map(a -> a.attributeType).collect(Collectors.toSet()),
					withInstances);
			items.forEach(a -> attrTypesGrid.removeElement(a));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void export(Set<AttributeTypeEntry> items)
	{
		SimpleFileDownloader downloader;
		try
		{
			downloader = controller.getAttributeTypesDownloader(items);

		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}
		addExtension(downloader);
		downloader.download();

	}

	private void goToCopy(AttributeTypeEntry type)
	{
		NavigationHelper.goToView(NewAttributeTypeView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ type.attributeType.getName());
	}

	private void gotoEdit(AttributeTypeEntry type)
	{
		NavigationHelper.goToView(EditAttributeTypeView.VIEW_NAME + "/" + CommonViewParam.name.toString() + "="
				+ type.attributeType.getName());
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.directorySetup.attributeTypes");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class AttributeTypesNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public AttributeTypesNavigationInfoProvider(UnityMessageSource msg,
				DirectorySetupNavigationInfoProvider parent, ObjectFactory<AttributeTypesView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.directorySetup.attributeTypes"))
					.withPosition(10).build());

		}
	}
}
