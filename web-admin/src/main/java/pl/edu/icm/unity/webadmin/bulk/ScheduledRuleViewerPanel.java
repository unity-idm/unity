/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.bulk;

import pl.edu.icm.unity.server.bulkops.EntityAction;
import pl.edu.icm.unity.server.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.server.registries.EntityActionsRegistry;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webadmin.tprofile.TranslationActionPresenter;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

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
		
		Label condition = new HtmlLabel(msg, msg.getMessage("preformatted"), rule.getCondition());
		condition.setCaption(msg.getMessage("ScheduledRuleViewerPanel.condition"));
		main.addComponent(condition);
		
		TranslationActionPresenter<EntityAction> action = new TranslationActionPresenter<>(
				msg, registry, rule.getAction());
		action.addToLayout(main);
	}
}
