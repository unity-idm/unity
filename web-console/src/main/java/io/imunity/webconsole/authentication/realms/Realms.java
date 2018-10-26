/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

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

		VerticalLayout content = new VerticalLayout();

		for (AuthenticationRealm realm : realmsMan.getRealms())
		{
			HorizontalLayout line = new HorizontalLayout();
			Button editRealm = new Button();
			editRealm.setCaption(msg.getMessage("edit"));
			editRealm.addClickListener(e -> {
				getUI().getNavigator().navigateTo(EditRealm.class.getSimpleName()
						+ "/" + realm.getName());
			});
			line.addComponents(new Label(realm.getName()), editRealm);

			content.addComponent(line);

		}

		addComponent(buttonsBar);
		addComponent(content);
	}
}
