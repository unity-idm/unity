/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.credentialReq;

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
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.authentication.AuthenticationNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.base.authn.CredentialRequirements;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

/**
 * Lists all local credentials
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
class CredentialRequirementsView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "CredentialRequirements";

	private CredentialRequirementsController controller;
	private MessageSource msg;
	private GridWithActionColumn<CredentialRequirements> credList;
	private EventsBus bus;

	@Autowired
	CredentialRequirementsView(MessageSource msg, CredentialRequirementsController controller)
	{
		this.controller = controller;
		this.msg = msg;
		this.bus = WebSession.getCurrent().getEventBus();

	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		HorizontalLayout buttonsBar = StandardButtonsHelper.buildTopButtonsBar(
				StandardButtonsHelper.build4AddAction(msg, 
						e -> NavigationHelper.goToView(NewCredentialRequirementsView.VIEW_NAME)));

		credList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
		credList.addComponentColumn(c -> {

			if (!c.isReadOnly())
			{
				return StandardButtonsHelper.buildLinkButton(c.getName(), e -> gotoEdit(c));
			} else
			{
				return new Label(c.getName());
			}
		}, msg.getMessage("CredentialReqView.nameCaption"), 5).setSortable(true)
		.setComparator((cr1, cr2) -> {
			return cr1.getName().compareTo(cr2.getName());
		}).setId("name");

		credList.addSortableColumn(c -> String.join(", ", c.getRequiredCredentials()),
				msg.getMessage("CredentialReqView.credentialsCaption"), 10);

		credList.addSortableColumn(c -> c.getDescription(), msg.getMessage("CredentialReqView.descriptionCaption"), 10);

		credList.setItems(getCredentials());
		credList.sort("name");
		
		VerticalLayout main = new VerticalLayout();
		main.addComponent(buttonsBar);
		main.addComponent(credList);
		main.setWidth(100, Unit.PERCENTAGE);
		main.setMargin(false);

		setCompositionRoot(main);
	}

	private Collection<CredentialRequirements> getCredentials()
	{
		try
		{
			return controller.getCredentialRequirements();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
		return Collections.emptyList();
	}

	private List<SingleActionHandler<CredentialRequirements>> getActionsHandlers()
	{
		SingleActionHandler<CredentialRequirements> edit = SingleActionHandler
				.builder4Edit(msg, CredentialRequirements.class)
				.withHandler(r -> gotoEdit(r.iterator().next()))
				.withDisabledPredicate(r -> r.isReadOnly()).build();

		SingleActionHandler<CredentialRequirements> remove = SingleActionHandler
				.builder4Delete(msg, CredentialRequirements.class)
				.withDisabledPredicate(credReq -> credReq.isReadOnly())
				.withHandler(r -> tryRemove(r.iterator().next())).build();

		return Arrays.asList(edit, remove);
	}

	private void tryRemove(CredentialRequirements item)
	{
		Collection<CredentialRequirements> allCRs;
		try
		{
			allCRs = controller.getCredentialRequirements();
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			return;
		}
		new CredentialRequirementRemovalDialog(msg, Sets.newHashSet(item.getName()), allCRs, replacementCR -> {
			remove(item, replacementCR);
		}).show();

	}

	private void remove(CredentialRequirements toRemove, String replacementCR)
	{
		try
		{
			controller.removeCredentialRequirements(toRemove, replacementCR, bus);
			credList.removeElement(toRemove);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
		}
	}

	private void gotoEdit(CredentialRequirements cred)
	{
		 NavigationHelper.goToView(EditCredentialRequirementsView.VIEW_NAME +
				"/" + CommonViewParam.name.toString()
		 + "=" + cred.getName());
	}

	@Override
	public String getDisplayedName()
	{
		return msg.getMessage("WebConsoleMenu.authentication.credentialRequirements");
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class CredentialsRequirementsNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		public static final String ID = CredentialRequirementsView.VIEW_NAME;
		
		@Autowired
		public CredentialsRequirementsNavigationInfoProvider(MessageSource msg,
				ObjectFactory<CredentialRequirementsView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(ID, Type.View)
					.withParent(AuthenticationNavigationInfoProvider.ID).withObjectFactory(factory)
					.withCaption(msg.getMessage(
							"WebConsoleMenu.authentication.credentialRequirements"))
					.withIcon(Images.optiona_a.getResource())
					.withPosition(30).build());
		}
	}
}
