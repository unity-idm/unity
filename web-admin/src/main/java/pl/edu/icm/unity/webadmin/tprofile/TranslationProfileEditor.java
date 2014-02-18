/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webadmin.tprofile;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.server.authn.remote.translation.TranslationProfile;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.RuleComponent.Callback;
import pl.edu.icm.unity.webui.common.DescriptionTextArea;
import pl.edu.icm.unity.webui.common.RequiredTextField;

import com.vaadin.ui.AbstractTextField;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

/**
 * Component to edit or add translation profile
 * @author P. Piernik
 *
 */
public class TranslationProfileEditor extends VerticalLayout
{
	private UnityMessageSource msg;
	private TranslationActionsRegistry registry;
	private boolean editMode;
	private AbstractTextField name;
	private DescriptionTextArea description;
	private FormLayout rulesL;
	private List<RuleComponent> rules;
	
	public TranslationProfileEditor(UnityMessageSource msg,
			TranslationActionsRegistry registry, TranslationProfile toEdit)
	{
		super();
		editMode = toEdit != null;
		this.msg = msg;
		this.registry = registry;
		this.rules = new ArrayList<RuleComponent>();
		initUI(toEdit);

	}

	private void initUI(TranslationProfile toEdit)
	{
		
		setWidth(100, Unit.PERCENTAGE);
		setHeight(100, Unit.PERCENTAGE);
		setSpacing(true);
			
		
		name = new RequiredTextField(msg);
		name.setCaption(msg.getMessage("MessageTemplatesEditor.name") + ":");
		name.setSizeFull();
		description = new DescriptionTextArea(
				msg.getMessage("MessageTemplatesEditor.description") + ":");
		
		
		if (editMode)
		{	
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
			description.setValue(toEdit.getDescription());
			
		} else
			name.setValue(msg.getMessage("MessageTemplatesEditor.defaultName"));
		
		
		for (int i = 0; i < 2; i++)
		{

			final RuleComponent r = new RuleComponent(msg, registry, new Callback()
			{

				@Override
				public boolean moveUP(RuleComponent rule)
				{
					rules.set(0, rule);
					refreshRules();
					return true;
				}
			});
			rules.add(r);
		}
		rulesL = new FormLayout();
		rulesL.setImmediate(true);
		refreshRules();
		FormLayout main = new FormLayout();
		main.addComponents(name, description,rulesL);
		main.setSizeFull();
		addComponent(main);
	}

	protected void refreshRules()
	{
		rulesL.removeAllComponents();
		for(RuleComponent r:rules)
		{
			rulesL.addComponent(r);
		}
		
	}

	public TranslationProfile getProfile()
	{
		String n = name.getValue();
		String desc = description.getValue();
		
		
		//return new MessageTemplate(n, desc, m, cons);
		return null;
	}
	
	
}
