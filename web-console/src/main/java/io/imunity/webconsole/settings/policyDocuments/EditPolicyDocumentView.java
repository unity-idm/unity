/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.settings.policyDocuments;

import java.util.Arrays;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.settings.policyDocuments.PolicyDocumentsView.PolicyDocumentsNavigationInfoProvider;
import io.imunity.webelements.helpers.NavigationHelper;
import io.imunity.webelements.helpers.NavigationHelper.CommonViewParam;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.policyDocument.PolicyDocumentWithRevision;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.AbstractDialog;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.StandardButtonsHelper;
import pl.edu.icm.unity.webui.exceptions.ControllerException;

@PrototypeComponent
class EditPolicyDocumentView extends CustomComponent implements UnityView
{
	public static final String VIEW_NAME = "EditPolicyDocument";

	private PolicyDocumentsController controller;
	private UnityMessageSource msg;
	private PolicyDocumentEditor editor;

	private String policyDocName;

	@Autowired
	EditPolicyDocumentView(PolicyDocumentsController controller, UnityMessageSource msg)
	{
		this.controller = controller;
		this.msg = msg;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		String id = NavigationHelper.getParam(event, CommonViewParam.id.toString());
		PolicyDocumentWithRevision doc;
		try
		{
			doc = controller.getPolicyDocument(Long.valueOf(id));
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(PolicyDocumentsView.VIEW_NAME);
			return;
		}

		policyDocName = doc.name;

		try
		{
			editor = controller.getEditor(doc);
		} catch (ControllerException e)
		{
			NotificationPopup.showError(msg, e);
			NavigationHelper.goToView(PolicyDocumentsView.VIEW_NAME);
			return;
		}
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);
		main.addComponent(editor);
		main.addComponent(StandardButtonsHelper.buildConfirmEditButtonsBar(msg, () -> onConfirm(),
				() -> onCancel()));
		setCompositionRoot(main);
	}

	private void onConfirm()
	{
		if (editor.hasErrors())
		{
			return;
		}

		new PolicyUpdateConfirmationDialog(msg).show();

	}

	private void onCancel()
	{
		NavigationHelper.goToView(PolicyDocumentsView.VIEW_NAME);

	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Override
	public String getDisplayedName()
	{
		return policyDocName;
	}

	private class PolicyUpdateConfirmationDialog extends AbstractDialog
	{
		private ComboBox<Boolean> withRevision;

		public PolicyUpdateConfirmationDialog(UnityMessageSource msg)
		{
			super(msg, msg.getMessage("EditPolicyDocumentView.confirm"), msg.getMessage("ok") , null);
			setSizeEm(60, 15);
		}

		@Override
		protected com.vaadin.ui.Component getContents() throws Exception
		{
			VerticalLayout main = new VerticalLayout();
			withRevision = new ComboBox<>();
			withRevision.setItems(Arrays.asList(true, false));
			withRevision.setItemCaptionGenerator(i -> msg.getMessage(
					"EditPolicyDocumentView.update" + (i ? "WithRevision" : "WithoutRevision")));
			withRevision.setValue(false);
			withRevision.setEmptySelectionAllowed(false);
			withRevision.setWidth(100, Unit.PERCENTAGE);
			main.addComponent(withRevision);
			return main;
		}

		@Override
		protected void onConfirm()
		{
			try
			{
				controller.updatePolicyDocument(editor.getUpdateRequest(), withRevision.getValue());

			} catch (ControllerException e)
			{

				NotificationPopup.showError(msg, e);
				return;
			}
			close();
			NavigationHelper.goToView(PolicyDocumentsView.VIEW_NAME);

		}
	}

	@Component
	public static class EditPolicyDocumentNavigationInfoProvider extends WebConsoleNavigationInfoProviderBase
	{
		@Autowired
		public EditPolicyDocumentNavigationInfoProvider(PolicyDocumentsNavigationInfoProvider parent,
				ObjectFactory<EditPolicyDocumentView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.ParameterizedView)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory).build());

		}
	}
}
