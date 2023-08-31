/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.directorySetup.automation;

import io.imunity.webconsole.tprofile.ActionEditor;
import io.imunity.webconsole.tprofile.ActionParameterComponentProviderV8;
import org.springframework.beans.factory.annotation.Autowired;
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
import pl.edu.icm.unity.webui.exceptions.ControllerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Bulk processing controller
 * 
 * @author P.Piernik
 *
 */
@Component("AutomationControllerV8")
class AutomationController
{
	private MessageSource msg;
	private BulkProcessingManagement bulkMan;
	private EntityActionsRegistry registry;
	private ActionParameterComponentProviderV8 parameterFactory;

	@Autowired
	AutomationController(MessageSource msg, BulkProcessingManagement bulkMan, EntityActionsRegistry registry,
			ActionParameterComponentProviderV8 parameterFactory)
	{
		this.msg = msg;
		this.bulkMan = bulkMan;
		this.registry = registry;
		this.parameterFactory = parameterFactory;
	}

	void applyRule(TranslationRule rule) throws ControllerException

	{
		try
		{
			bulkMan.applyRule(rule);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AutomationController.runError"), e);
		}
	}

	void scheduleRule(ScheduledProcessingRuleParam rule) throws ControllerException

	{
		try
		{
			bulkMan.scheduleRule(rule);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AutomationController.addError"), e);
		}
	}

	void removeScheduledRules(Set<ScheduledProcessingRule> toRemove) throws ControllerException

	{
		for (ScheduledProcessingRule r : toRemove)
		{
			try
			{
				bulkMan.removeScheduledRule(r.getId());
			} catch (Exception e)
			{
				throw new ControllerException(msg.getMessage("AutomationController.removeError"), e);
			}
		}
	}

	void updateScheduledRule(String id, ScheduledProcessingRuleParam toAdd) throws ControllerException

	{
		try
		{
			bulkMan.removeScheduledRule(id);
			bulkMan.scheduleRule(toAdd);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AutomationController.updateError"), e);
		}
	}

	ScheduledProcessingRule getScheduledRule(String id) throws ControllerException

	{
		try
		{
			return bulkMan.getScheduledRule(id);
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AutomationController.getError"), e);
		}
	}

	public void runScheduleRules(Set<ScheduledProcessingRule> items) throws ControllerException
	{
		for (ScheduledProcessingRule r : items)
		{
			try
			{
				bulkMan.applyRule(r);
			} catch (Exception e)
			{
				throw new ControllerException(msg.getMessage("AutomationController.runError"), e);
			}
		}
	}

	Collection<ScheduledProcessingRule> getScheduleRules() throws ControllerException
	{
		try
		{
			return bulkMan.getScheduledRules();
		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AutomationController.getAllError"), e);
		}
	}

	ScheduledRuleParamEditorImpl getScheduleRuleEditor(ScheduledProcessingRule toEdit) throws ControllerException
	{
		try
		{
			ScheduledRuleParamEditorImpl editor = new ScheduledRuleParamEditorImpl(msg, getActionEditor());
			if (toEdit != null)
			{
				editor.setInput(toEdit);
			}
			return editor;

		} catch (EngineException e)
		{
			throw new ControllerException(msg.getMessage("AutomationController.getEditorError"), e);
		}
	}

	RuleEditorImpl getRuleEditor(TranslationRule rule) throws ControllerException
	{
		try
		{
			RuleEditorImpl editor = new RuleEditorImpl(msg, getActionEditor());
			if (rule != null)
			{
				editor.setInput(rule);
			}
			return editor;
			

		} catch (Exception e)
		{
			throw new ControllerException(msg.getMessage("AutomationController.createEditorError"), e);
		}
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

	private ActionEditor getActionEditor() throws EngineException
	{
		parameterFactory.init();
		return new ActionEditor(msg, registry, null, parameterFactory);
	}
}
