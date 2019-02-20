/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;

import io.imunity.webadmin.tprofile.TranslationActionPresenter;
import pl.edu.icm.unity.engine.api.bulkops.EntityActionsRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.bulkops.ScheduledProcessingRule;

/**
 * Shows details of a processing rule
 * @author K. Benedyczak
 */
public class ScheduledRuleViewerPanel extends CustomComponent
{
	private UnityMessageSource msg;
	private EntityActionsRegistry registry;
	
	private FormLayout main;
	
	
	public ScheduledRuleViewerPanel(UnityMessageSource msg, EntityActionsRegistry registry)
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
		condition.setWidth(100, Unit.PERCENTAGE);
		main.addComponent(condition);
		
		TranslationActionPresenter action = new TranslationActionPresenter(
				msg, registry, rule.getAction());
		action.addToLayout(main);
	}
}
