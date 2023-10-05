/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.shared.endpoint.policies;

import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import io.imunity.vaadin.elements.UnityViewComponent;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentManagement;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;

import java.util.Optional;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppDisplayedName;
import static pl.edu.icm.unity.engine.api.endpoint.SharedEndpointManagement.POLICY_DOCUMENTS_PATH;

@Route(value = POLICY_DOCUMENTS_PATH + ":" + PublicPolicyDocumentView.POLICY_DOC_PARAM)
class PublicPolicyDocumentView extends UnityViewComponent implements BeforeEnterObserver
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PublicPolicyDocumentView.class);

	static final String POLICY_DOC_PARAM = "id";
	private final MessageSource msg;
	private final PolicyDocumentManagement policyDocMan;

	@Autowired
	PublicPolicyDocumentView(MessageSource msg, @Qualifier("insecure") PolicyDocumentManagement policyDocMan)
	{
		this.msg = msg;
		this.policyDocMan = policyDocMan;
	}

	public void init(PolicyDocumentWithRevision document)
	{
		getContent().removeAll();
		VerticalLayout layout = new VerticalLayout();
		layout.setWidthFull();
		layout.setMargin(false);
		layout.setSpacing(false);

		VerticalLayout contents = new VerticalLayout();
		contents.setWidth(50, Unit.PERCENTAGE);
		layout.add(contents);
		layout.setAlignItems(FlexComponent.Alignment.CENTER);

		H3 titleLabel = new H3(document.displayedName.getValue(msg));
		titleLabel.setWidthFull();
		contents.add(titleLabel);

		Html content = new Html("<div>" + document.content.getValue(msg) + "</div>");
		contents.add(content);

		getContent().setSizeFull();
		getContent().add(layout);
	}

	public void initError()
	{
		getContent().removeAll();
		Icon icon = VaadinIcon.EXCLAMATION_CIRCLE.create();
		Label label = new Label(msg.getMessage("Empty"));
		label.getStyle().set("color", "red");
		HorizontalLayout horizontalLayout = new HorizontalLayout(icon, label);
		horizontalLayout.setMargin(true);
		getContent().add(horizontalLayout);
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event)
	{
		event.getRouteParameters().get(POLICY_DOC_PARAM)
				.flatMap(this::getDocument)
				.ifPresentOrElse(this::init, this::initError);
	}

	private Optional<PolicyDocumentWithRevision> getDocument(String docId)
	{
		try
		{
			return Optional.of(policyDocMan.getPolicyDocument(Long.parseLong(docId)));
		} catch (Exception e)
		{
			log.error("Unknown policy document id", e);
			return Optional.empty();
		}
	}

	@Override
	public String getPageTitle()
	{
		return getCurrentWebAppDisplayedName();
	}
}
