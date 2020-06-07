/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.common.policyAgreement;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * View for single policy document presentation.
 * 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class PublicPolicyDocumentView extends CustomComponent implements View
{
	public static final String POLICY_DOC_VIEW = "policyDoc";
	public static final String POLICY_DOC_PARAM = "id";
	private MessageSource msg;
	private PolicyDocumentWithRevision doc;

	public PublicPolicyDocumentView(MessageSource msg)
	{
		this.msg = msg;
	}

	public PublicPolicyDocumentView init(PolicyDocumentWithRevision doc)
	{
		this.doc = doc;
		return this;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout vmain = new VerticalLayout();
		vmain.setWidth(100, Unit.PERCENTAGE);
		vmain.setMargin(false);
		vmain.setSpacing(false);

		VerticalLayout contents = new VerticalLayout();
		contents.setWidth(50, Unit.PERCENTAGE);
		vmain.addComponent(contents);
		vmain.setComponentAlignment(contents, Alignment.MIDDLE_CENTER);

		Label titleLabel = new Label(doc.displayedName.getValue(msg));
		titleLabel.setStyleName(Styles.textTitle.toString());
		titleLabel.setWidth(100, Unit.PERCENTAGE);

		Label content = new Label(doc.content.getValue(msg));
		content.setContentMode(ContentMode.HTML);
		content.setStyleName(Styles.textLarge.toString());
		content.setWidth(100, Unit.PERCENTAGE);

		contents.addComponent(titleLabel);
		contents.setComponentAlignment(titleLabel, Alignment.MIDDLE_CENTER);

		contents.addComponent(content);
		contents.setComponentAlignment(content, Alignment.MIDDLE_CENTER);

		setCompositionRoot(vmain);
		setWidth(100, Unit.PERCENTAGE);
	}
}
