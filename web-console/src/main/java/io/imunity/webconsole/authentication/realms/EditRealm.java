/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.ui.UI;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * View for edit realm
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class EditRealm extends AbstractEditRealm
{
	
	@Autowired
	public EditRealm(UnityMessageSource msg, RealmController controller)
	{
		super(msg, controller);
	}

	@Override
	protected void onConfirm()
	{
		if (binder.validate().hasErrors())
		{
			return;
		}
			
		try
		{
			if (!controller.updateRealm(binder.getBean()))
				return;
		} catch (Exception e)
		{
			//TODO
			NotificationPopup.showError(msg, "IVALID REALM", e);
			return;
		}
			
			
			
		UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());
		
	}

	@Override
	protected void onCancel()
	{
		UI.getCurrent().getNavigator().navigateTo(Realms.class.getSimpleName());
		
	}


	@Override
	protected void init(Map<String, String> parameters) throws Exception
	{
		if (parameters.isEmpty())
				throw new Exception("");
		
		
		
		for (AuthenticationRealm realm : controller.getRealms())
		{
			if (realm.getName().equals(parameters.keySet().iterator().next()))
			{
				binder.setBean(realm);
			}
		}
		name.setReadOnly(true);
		
		
	}
}
