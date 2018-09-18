/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.reg.formman;

import com.vaadin.ui.CheckBox;
import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.registration.RedirectConfig;
import pl.edu.icm.unity.webui.common.LayoutEmbeddable;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;

public class RedirectConfigEditor extends LayoutEmbeddable
{
	private TextField redirectURL;
	private CheckBox automatic;
	private I18nTextField caption;
	
	public RedirectConfigEditor(UnityMessageSource msg, String urlCaption, String captionCaption)
	{
		redirectURL = new TextField(urlCaption);
		caption = new I18nTextField(msg, captionCaption);
		automatic = new CheckBox(msg.getMessage("RegistrationFormEditor.automaticRedirect"));
		automatic.addValueChangeListener(e -> caption.setEnabled(!automatic.getValue()));
		addComponents(redirectURL, automatic, caption);
	}
	
	public void setValue(RedirectConfig toEdit)
	{
		if (toEdit == null)
			return;
		if (toEdit.getRedirectCaption() != null)
			caption.setValue(toEdit.getRedirectCaption());
		if (toEdit.getRedirectURL() != null)
			redirectURL.setValue(toEdit.getRedirectURL());
		automatic.setValue(toEdit.isAutomatic());
	}
	
	public RedirectConfig getValue()
	{
		return new RedirectConfig(caption.getValue(), redirectURL.getValue(), automatic.getValue());
	}
}
