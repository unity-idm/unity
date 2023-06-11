/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.policyDocuments;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.Orientation;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.settings.SettingsNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.MessageUtils;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.ComponentWithToolbar;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SearchField;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.common.Toolbar;
import pl.edu.icm.unity.webui.common.grid.FilterableGridHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all policy documents
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class PolicyDocumentsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "PolicyDocuments";

	private MessageSource msg;
	private PolicyDocumentsController controller;
	private GridWithActionColumn<PolicyDocumentEntry> policyDocsGrid;

	@Autowired
	PolicyDocumentsView(MessageSource msg, PolicyDocumentsController controller)
	{
		this.msg = msg;
		this.controller = controller;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(StandardButtonsHelper
				.build4AddAction(msg, e -> NavigationHelper.goToView(NewPolicyDocumentView.VIEW_NAME)));

		policyDocsGrid = new GridWithActionColumn<>(msg, getActionsHandlers(), false, false);

		policyDocsGrid.addComponentColumn(
				e -> StandardButtonsHelper.buildLinkButton(e.doc.name, ev -> gotoEdit(e)),
				msg.getMessage("PolicyDocumentsView.name"), 10).setSortable(true)
				.setComparator((e1, e2) -> {
					return e1.doc.name.compareTo(e2.doc.name);
				}).setId("name");

		policyDocsGrid.addSortableColumn(e -> String.valueOf(e.doc.revision),
				msg.getMessage("PolicyDocumentsView.version"), 10);

		policyDocsGrid.addSortableColumn(
				e -> msg.getMessage("PolicyDocumentType." + e.doc.contentType.toString()),
				msg.getMessage("PolicyDocumentsView.type"), 10);

		policyDocsGrid.setMultiSelect(false);
		policyDocsGrid.setItems(getPolicyDocuments());
		policyDocsGrid.sort("name");

		SearchField search = FilterableGridHelper.generateSearchField(policyDocsGrid, msg);

		Toolbar<PolicyDocumentEntry> toolbar = new Toolbar<>(Orientation.HORIZONTAL);
		toolbar.setWidth(100, Unit.PERCENTAGE);
		toolbar.addSearch(search, Alignment.MIDDLE_RIGHT);
		ComponentWithToolbar attrTypeGridWithToolbar = new ComponentWithToolbar(policyDocsGrid, toolbar,
				Alignment.BOTTOM_LEFT);
		attrTypeGridWithToolbar.setSpacing(false);
		attrTypeGridWithToolbar.setSizeFull();

		VerticalLayout gridWrapper = new VerticalLayout();
		gridWrapper.setMargin(false);
		gridWrapper.setSpacing(true);
		gridWrapper.addComponent(buttonsBar);
		gridWrapper.setExpandRatio(buttonsBar, 0);
		gridWrapper.addComponent(attrTypeGridWithToolbar);
		gridWrapper.setExpandRatio(attrTypeGridWithToolbar, 2);
		gridWrapper.setSizeFull();

		setCompositionRoot(gridWrapper);
		setSizeFull();
	}

	private Collection<PolicyDocumentEntry> getPolicyDocuments()
	{
		try
		{
			return controller.getPolicyDocuments();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private List<SingleActionHandler<PolicyDocumentEntry>> getActionsHandlers()
	{
		SingleActionHandler<PolicyDocumentEntry> edit = SingleActionHandler
				.builder4Edit(msg, PolicyDocumentEntry.class)
				.withHandler(r -> gotoEdit(r.iterator().next())).build();

		SingleActionHandler<PolicyDocumentEntry> remove = SingleActionHandler
				.builder4Delete(msg, PolicyDocumentEntry.class)
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(edit, remove);

	}

	private void tryRemove(PolicyDocumentEntry e)
	{

		String confirmText = MessageUtils.createConfirmFromStrings(msg, Sets.newHashSet(e.doc.name));
		new ConfirmDialog(msg, msg.getMessage("PolicyDocumentsView.confirmDelete", confirmText),
				() -> remove(e)).show();
	}

	private void remove(PolicyDocumentEntry e)
	{
		try
		{
			controller.removePolicyDocument(e.doc);
			policyDocsGrid.removeElement(e);
		} catch (ControllerException er)
		{
			NotificationPopup.showError(msg, er);
		}
	}

	private void gotoEdit(PolicyDocumentEntry e)
	{
		NavigationHelper.goToView(EditPolicyDocumentView.VIEW_NAME + "/" + CommonViewParam.id.toString() + "="
				+ e.doc.id);
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.settings.policyDocuments");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class PolicyDocumentsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = VIEW_NAME;
		
		@Autowired
		public PolicyDocumentsNavigationInfoProvider(MessageSource msg, ObjectFactory<PolicyDocumentsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(SettingsNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage("WebConsoleMenu.settings.policyDocuments"))
					.withIcon(Images.check_square.getResource()).withPosition(20).build());

		}
	}
}
