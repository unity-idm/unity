/**
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.maintenance.fido;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import io.imunity.webadmin.importExport.ImportExportComponent;
import io.imunity.webconsole.WebConsoleNavigationInfoProviderBase;
import io.imunity.webconsole.maintenance.MaintenanceNavigationInfoProvider;
import io.imunity.webelements.navigation.NavigationInfo;
import io.imunity.webelements.navigation.NavigationInfo.Type;
import io.imunity.webelements.navigation.UnityView;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.FidoService;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.fido.FidoComponent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@PrototypeComponent
class FidoTestingView extends CustomComponent implements UnityView
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, FidoTestingView.class);
	public static final String VIEW_NAME = "FidoTestView";

	private UnityMessageSource msg;
	private FidoService fidoService;

	private final Button clearButton = new Button();
	private final TextArea logs = new TextArea();

	@Autowired
	FidoTestingView(UnityMessageSource msg, ImportExportComponent importExportComponent, FidoService fidoService)
	{
		this.msg = msg;
		this.fidoService = fidoService;
	}

	@Override
	public void enter(ViewChangeEvent event)
	{
		VerticalLayout main = new VerticalLayout();
		main.setMargin(false);

		FidoComponent fidoComponent = new FidoComponent(fidoService);

		clearButton.setCaption("Remove data");
		clearButton.addClickListener((Button.ClickListener) clickEvent -> {
			fidoComponent.removeFidoCredentials();
		});

		logs.setRows(20);
		logs.setWidth(500, Unit.PIXELS);

		main.addComponents(fidoComponent, clearButton, logs);
		setCompositionRoot(main);
	}

	private void executeJSFromFile(String filepath) {
		try {
			File file = new File(
					filepath
			);
			StringBuffer buffer = new StringBuffer();
			String firstLine = null;
			try (FileReader reader = new FileReader(file);
				BufferedReader br = new BufferedReader(reader)) {

				String line;
				while ((line = br.readLine()) != null) {
					if (buffer.length() == 0) {
						firstLine = line;
					}
					buffer.append(line);
					buffer.append("\n");
				}
			}
			logs.setValue(logs.getValue()
					+ "Read file: " + firstLine + "\n");

			JavaScript.getCurrent().execute(buffer.toString());

		} catch (IOException e) {
			log.error("Got exception", e);
		}
	}

	@Override
	public String getDisplayedName()
	{
		return "Fido testing view";
	}

	@Override
	public String getViewName()
	{
		return VIEW_NAME;
	}

	@Component
	public static class FidoTestViewInfoProvider extends WebConsoleNavigationInfoProviderBase
	{

		@Autowired
		public FidoTestViewInfoProvider(UnityMessageSource msg,
				MaintenanceNavigationInfoProvider parent,
				ObjectFactory<FidoTestingView> factory)
		{
			super(new NavigationInfo.NavigationInfoBuilder(VIEW_NAME, Type.View)
					.withParent(parent.getNavigationInfo()).withObjectFactory(factory)
					.withCaption("Fido testing view")
					.withIcon(Images.cloud_download.getResource())
					.withPosition(10).build());

		}
	}
}
