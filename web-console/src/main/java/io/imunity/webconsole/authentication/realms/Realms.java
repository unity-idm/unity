/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.common.ControllerException;
import io.imunity.webconsole.common.ListOfElementWithActions;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;

/**
 * Lists all realms
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class Realms extends VerticalLayout implements View
{
	private RealmController realmsMan;

	private UnityMessageSource msg;

	private ListOfElementWithActions<AuthenticationRealm> realmsList;

	@Autowired
	public Realms(UnityMessageSource msg, RealmController realmsMan)
	{
		this.realmsMan = realmsMan;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{

		HorizontalLayout buttonsBar = new HorizontalLayout();
		Button newRealm = new Button();
		newRealm.setCaption(msg.getMessage("add"));
		newRealm.addClickListener(e -> {
			getUI().getNavigator().navigateTo(NewRealm.class.getSimpleName());
		});
		buttonsBar.addComponent(newRealm);
		buttonsBar.setComponentAlignment(newRealm, Alignment.MIDDLE_RIGHT);
		buttonsBar.setWidth(100, Unit.PERCENTAGE);

		realmsList = new ListOfElementWithActions<>(r -> new Label(r.getName()));

		SingleActionHandler<AuthenticationRealm> edit = SingleActionHandler
				.builder4Edit(msg, AuthenticationRealm.class)
				.withHandler(r -> getUI().getNavigator()
						.navigateTo(EditRealm.class.getSimpleName() + "/"
								+ r.iterator().next().getName()))
				.build();
		SingleActionHandler<AuthenticationRealm> view = SingleActionHandler
				.builder4ShowDetails(msg, AuthenticationRealm.class)
				.withHandler(r -> getUI().getNavigator()
						.navigateTo(ViewRealm.class.getSimpleName() + "/"
								+ r.iterator().next().getName()))
				.build();

		SingleActionHandler<AuthenticationRealm> remove = SingleActionHandler
				.builder4Delete(msg, AuthenticationRealm.class).withHandler(r -> {

					remove(r.iterator().next());

				}

				).build();

		realmsList.addActionHandler(view);
		realmsList.addActionHandler(edit);
		realmsList.addActionHandler(remove);
		realmsList.setAddSeparatorLine(true);
		realmsList.addHeader(msg.getMessage("Realms.nameTitle"), msg.getMessage("actions"));

		for (AuthenticationRealm realm : getRealms())
		{
			realmsList.addEntry(realm);
		}

		addComponent(buttonsBar);
		addComponent(realmsList);
		
		setWidth(50, Unit.PERCENTAGE);
	}

	private Collection<AuthenticationRealm> getRealms()
	{
		try
		{
			return realmsMan.getRealms();
		} catch (ControllerException e)
		{
			showError(e);
		}
		return Collections.emptyList();
	}

	private void showError(ControllerException e)
	{
		NotificationPopup.showError(e.getErrorCaption(), e.getErrorDetails());
	}

	private void remove(AuthenticationRealm realm)
	{
		try
		{
			if (realmsMan.removeRealm(realm))
				realmsList.removeEntry(realm);
		} catch (ControllerException e)
		{
			showError(e);
		}
	}

}
