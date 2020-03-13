/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.fido;

import com.vaadin.annotations.JavaScript;
import com.vaadin.shared.ui.JavaScriptComponentState;
import com.vaadin.ui.AbstractJavaScriptComponent;
import com.vaadin.ui.JavaScriptFunction;
import elemental.json.JsonArray;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.FidoService;
import pl.edu.icm.unity.exceptions.FidoException;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Java counterpart of FidoComponent for BE processing support.
 */
@JavaScript({"fido.js"})
public class FidoComponent extends AbstractJavaScriptComponent {
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, FidoComponent.class);

	private FidoService fidoService;

	public interface ValueChangeListener extends Serializable {
		void valueChange();
	}
	ArrayList<ValueChangeListener> listeners =
			new ArrayList<ValueChangeListener>();
	public void addValueChangeListener(
			ValueChangeListener listener) {
		listeners.add(listener);
	}

	public FidoComponent(final FidoService fidoService) {
		this(fidoService, true, "Register", true, "Login");
	}

	public FidoComponent(final FidoService fidoService, final boolean registrationEnabled, final String registrationCaption, final boolean loginEnabled, final String loginCaption) {
		this.fidoService = fidoService;
		FidoComponentState state = getState();
		state.regEnabled = registrationEnabled;
		state.regCaption = Optional.ofNullable(registrationCaption).orElse("");
		state.loginEnabled = loginEnabled;
		state.loginCaption = Optional.ofNullable(loginCaption).orElse("");

		// Create Javascript registration functions
		addFunction("invokeRegistration", new JavaScriptFunction() {
			@Override
			public void call(JsonArray arguments) {
				log.debug("Invoke registration for user={}", arguments.getString(0));
				try {
					AbstractMap.SimpleEntry<String, String> options = fidoService.getRegistrationOptions(arguments.getString(0));
					log.debug("reqId={}", options.getKey());
					com.vaadin.ui.JavaScript.getCurrent().execute("createCredentials('" + options.getKey() + "', '" + options.getValue() + "');");
				} catch (FidoException e) {
					log.error("Got exception: ", e);
					NotificationPopup.showError("Fido registration", e.getLocalizedMessage());
				}
			}
		});
		addFunction("finalizeRegistration", new JavaScriptFunction() {
			@Override
			public void call(JsonArray arguments) {
				log.info("Invoke finalize registration for reqId={}", arguments.getString(0));
				try {
					fidoService.registerFidoCredentials(arguments.getString(0), arguments.getString(1));
					NotificationPopup.showSuccess("Fido registration", "New FIDO credentials stored.");
				} catch (FidoException e) {
					log.error("Registration finalization failed: ", e);
					NotificationPopup.showError("Fido registration", e.getLocalizedMessage());
				}
			}
		});

		// Create Javascript authentication functions
		addFunction("invokeAuthentication", new JavaScriptFunction() {
			@Override
			public void call(JsonArray arguments) {
				log.debug("Invoke authentication for user={}", arguments.getString(0));
				try {
					AbstractMap.SimpleEntry<String, String> options = fidoService.getAuthenticationOptions(arguments.getString(0));
					log.debug("reqId={}", options.getKey());
					com.vaadin.ui.JavaScript.getCurrent().execute("getCredentials('" + options.getKey() + "', '" + options.getValue() + "');");
				} catch (FidoException e) {
					log.error("Got exception: ", e);
					NotificationPopup.showError("Fido authentication", e.getLocalizedMessage());
				}
			}
		});
		addFunction("finalizeAuthentication", new JavaScriptFunction() {
			@Override
			public void call(JsonArray arguments) {
				log.info("Invoke finalize authentication for reqId={}", arguments.getString(0));
				try {
					fidoService.verifyAuthentication(arguments.getString(0), arguments.getString(1));
					NotificationPopup.showSuccess("Fido authentication", "User successfully authenticated.");
				} catch (FidoException e) {
					log.error("Authentication failed: ", e);
					NotificationPopup.showError("Fido authentication", e.getLocalizedMessage());
				}
			}
		});

		// Create error notification function
		addFunction("showError", new JavaScriptFunction() {
			@Override
			public void call(JsonArray arguments) {
				NotificationPopup.showError(arguments.getString(0), arguments.getString(1));
			}
		});
	}

	@Override
	protected FidoComponentState getState() {
		return (FidoComponentState) super.getState();
	}

	public void removeFidoCredentials() {
		fidoService.removeFidoCredentials();
	}

	public static class FidoComponentState extends JavaScriptComponentState {
		// Has to be public to allow access for super() class.
		public boolean regEnabled;
		public boolean loginEnabled;
		public String regCaption;
		public String loginCaption;
	}
}
