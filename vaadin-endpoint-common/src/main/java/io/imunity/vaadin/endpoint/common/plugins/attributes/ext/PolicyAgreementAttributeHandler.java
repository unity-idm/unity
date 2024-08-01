/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.plugins.attributes.ext;

import static pl.edu.icm.unity.engine.api.utils.TimeUtil.formatStandardInstant;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;

import io.imunity.vaadin.elements.ReadOnlyField;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeSyntaxEditor;
import io.imunity.vaadin.endpoint.common.plugins.attributes.AttributeViewerContext;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandler;
import io.imunity.vaadin.endpoint.common.plugins.attributes.WebAttributeHandlerFactory;
import io.imunity.vaadin.endpoint.common.plugins.attributes.components.TextOnlyAttributeHandler;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.policyAgreement.PolicyAgreementState;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.stdext.attr.PolicyAgreementAttributeSyntax;

class PolicyAgreementAttributeHandler extends TextOnlyAttributeHandler
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PolicyAgreementAttributeHandler.class);

	private final PolicyDocumentManagement docMan;

	public PolicyAgreementAttributeHandler(MessageSource msg, PolicyDocumentManagement docMan,
	                                       AttributeValueSyntax<?> syntax)
	{
		super(msg, syntax);
		this.docMan = docMan;
	}

	@Override
	protected List<String> getHints()
	{
		return Collections.emptyList();
	}

	@Override
	public Component getRepresentation(String value, AttributeViewerContext context)
	{
		PolicyAgreementState state;
		PolicyDocumentWithRevision policyDocument;
		try
		{
			state = PolicyAgreementState.fromJson(value);
			policyDocument = docMan.getPolicyDocument(state.policyDocumentId);
		} catch (EngineException e)
		{
			log.error("Invalid value of policy agreement attribute:" + value);
			return super.getRepresentation(value, context);
		}

		String nvalue = msg.getMessage("PolicyAgreeementsAttributeHandler.representation", policyDocument.name,
				state.policyDocumentRevision,
				msg.getMessage("PolicyAgreementAcceptanceStatus." + state.acceptanceStatus),
				formatStandardInstant(state.decisionTs.toInstant()));

		if (!context.isShowAsLabel())
		{
			ReadOnlyField component = new ReadOnlyField(nvalue);
			if (context.isCustomWidth())
			{
				if (!context.isCustomWidthAsString())
				{
					if (context.getCustomWidth() > 0)
					{
						component.getElement().getStyle().set("width", context.getCustomWidth() + context.getCustomWidthUnit().getSymbol());
					} else
					{
						component.getElement().getStyle().set("width", "unset");
					}
				}else 
				{
					component.getElement().getStyle().set("width", context.getCustomWidthAsString());

				}
			}
			if (context.isCustomHeight())
			{
				if (context.getCustomHeight() > 0)
				{
					component.setHeight(context.getCustomHeight(), context.getCustomHeightUnit());
				}

				else
				{
					component.setHeight("unset");
				}
			}
			return component;
		}
		else
		{
			return new Span(nvalue);
		}
	}

	private static class PolicyAgreementSyntaxEditor implements AttributeSyntaxEditor<String>
	{
		@Override
		public Optional<Component>  getEditor()
		{
			return Optional.empty();
		}

		@Override
		public AttributeValueSyntax<String> getCurrentValue()
		{
			return new PolicyAgreementAttributeSyntax();
		}
	}

	@org.springframework.stereotype.Component
	static class PolicyAgreementAttributeHandlerFactory implements WebAttributeHandlerFactory
	{
		private final MessageSource msg;
		private final PolicyDocumentManagement docMan;

		@Autowired
		PolicyAgreementAttributeHandlerFactory(MessageSource msg, PolicyDocumentManagement docMan)
		{
			this.msg = msg;
			this.docMan = docMan;
		}

		@Override
		public String getSupportedSyntaxId()
		{
			return PolicyAgreementAttributeSyntax.ID;
		}

		@Override
		public WebAttributeHandler createInstance(AttributeValueSyntax<?> syntax)
		{
			return new PolicyAgreementAttributeHandler(msg, docMan, syntax);
		}

		@Override
		public AttributeSyntaxEditor<String> getSyntaxEditorComponent(AttributeValueSyntax<?> initialValue)
		{
			return new PolicyAgreementSyntaxEditor();
		}
	}
}
