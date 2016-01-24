/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.server.registries.TranslationActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.TranslationActionPresenter;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

/**
 * Shows details of a processing rule
 * @author K. Benedyczak
 */
public class ScheduledRuleViewerPanel extends CustomComponent
{
	private UnityMessageSource msg;
	private TranslationActionsRegistry registry;
	
	private FormLayout main;
	
	
	public ScheduledRuleViewerPanel(UnityMessageSource msg, TranslationActionsRegistry registry)
	{
		this.msg = msg;
		this.registry = registry;
		main = new FormLayout();
		setCompositionRoot(main);
	}

	public void setInput(ScheduledProcessingRule rule)
	{
		main.removeAllComponents();
		if (rule == null)
			return;
		Label id = new Label(rule.getId());
		id.setCaption(msg.getMessage("ScheduledRuleViewerPanel.id"));
		main.addComponent(id);
		
		Label schedule = new Label(rule.getCronExpression());
		schedule.setCaption(msg.getMessage("ScheduledRuleViewerPanel.schedule"));
		main.addComponent(schedule);
		
		Label condition = new Label(rule.getCondition());
		condition.setCaption(msg.getMessage("ScheduledRuleViewerPanel.condition"));
		main.addComponent(schedule);
		
		TranslationActionPresenter action = new TranslationActionPresenter(msg, registry, rule.getAction());
		action.iterator().forEachRemaining(c -> main.addComponent(c));
	}
}
