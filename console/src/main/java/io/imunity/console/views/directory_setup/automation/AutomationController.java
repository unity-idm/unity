/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.console.views.directory_setup.automation;

import io.imunity.console.tprofile.ActionEditor;
import io.imunity.console.tprofile.ActionParameterComponentProvider;
import io.imunity.vaadin.elements.NotificationPresenter;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRule;
import pl.edu.icm.unity.base.bulkops.ScheduledProcessingRuleParam;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.translation.ActionParameterDefinition;
import pl.edu.icm.unity.base.translation.TranslationAction;
import pl.edu.icm.unity.base.translation.TranslationRule;
import pl.edu.icm.unity.engine.api.BulkProcessingManagement;
import pl.edu.icm.unity.engine.api.bulkops.EntityActionsRegistry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;


@Component
class AutomationController
{
	private final MessageSource msg;
	private final BulkProcessingManagement bulkMan;
	private final EntityActionsRegistry registry;
	private final NotificationPresenter notificationPresenter;
	private final ActionParameterComponentProvider parameterFactory;

	AutomationController(MessageSource msg, BulkProcessingManagement bulkMan, EntityActionsRegistry registry,
						 NotificationPresenter notificationPresenter, ActionParameterComponentProvider parameterFactory)
	{
		this.msg = msg;
		this.bulkMan = bulkMan;
		this.registry = registry;
		this.parameterFactory = parameterFactory;
		this.notificationPresenter = notificationPresenter;
	}

	void applyRule(TranslationRule rule)
	{
		try
		{
			bulkMan.applyRule(rule);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AutomationController.runError"), e.getMessage());
		}
	}

	void scheduleRule(ScheduledProcessingRuleParam rule)

	{
		try
		{
			bulkMan.scheduleRule(rule);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AutomationController.addError"), e.getMessage());
		}
	}

	void removeScheduledRules(Set<ScheduledProcessingRule> toRemove)

	{
		for (ScheduledProcessingRule r : toRemove)
		{
			try
			{
				bulkMan.removeScheduledRule(r.getId());
			} catch (Exception e)
			{
				notificationPresenter.showError(msg.getMessage("AutomationController.removeError"), e.getMessage());
			}
		}
	}

	void updateScheduledRule(String id, ScheduledProcessingRuleParam toAdd)

	{
		try
		{
			bulkMan.removeScheduledRule(id);
			bulkMan.scheduleRule(toAdd);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AutomationController.updateError"), e.getMessage());
		}
	}

	ScheduledProcessingRule getScheduledRule(String id)

	{
		try
		{
			return bulkMan.getScheduledRule(id);
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AutomationController.getError"), e.getMessage());
		}
		return null;
	}

	public void runScheduleRules(Set<ScheduledProcessingRule> items)
	{
		for (ScheduledProcessingRule r : items)
		{
			try
			{
				bulkMan.applyRule(r);
			} catch (Exception e)
			{
				notificationPresenter.showError(msg.getMessage("AutomationController.runError"), e.getMessage());
			}
		}
	}

	Collection<ScheduledProcessingRule> getScheduleRules()
	{
		try
		{
			return bulkMan.getScheduledRules();
		} catch (Exception e)
		{
			notificationPresenter.showError(msg.getMessage("AutomationController.getAllError"), e.getMessage());
		}
		return List.of();
	}

	String getActionParamAsString(TranslationAction action)
	{
		List<String> actions = new ArrayList<>();

		ActionParameterDefinition[] parametersDef = registry.getByName(action.getName()).getActionType()
				.getParameters();

		String[] parameters = action.getParameters();

		for (int i = 0; i < parametersDef.length; i++)
		{
			actions.add(parametersDef[i].getName() + "=" + (parameters.length >= i ? parameters[i] : ""));
		}
		return String.join(" ,", actions);
	}

	public ActionEditor getActionEditor(TranslationRule translationRule)
	{
		try
		{
			parameterFactory.init();
		} catch (EngineException e)
		{
			notificationPresenter.showError(msg.getMessage("error"), e.getMessage());
		}
		return new ActionEditor(msg, registry, translationRule.getAction(), parameterFactory,(name, translationAction) -> translationAction.ifPresent(translationRule::setTranslationAction), notificationPresenter);
	}
}
