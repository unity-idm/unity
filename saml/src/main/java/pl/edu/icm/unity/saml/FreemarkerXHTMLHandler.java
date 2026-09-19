package pl.edu.icm.unity.saml;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import freemarker.core.XHTMLOutputFormat;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.utils.FreemarkerUtils;

@Component
public class FreemarkerXHTMLHandler
{
	private final Configuration cfg;

	@Autowired
	public FreemarkerXHTMLHandler(UnityServerConfiguration serverConfig)
	{
		cfg = new Configuration(Configuration.VERSION_2_3_34);
		String webRoot = serverConfig.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		cfg.setTemplateLoader(FreemarkerUtils.getTemplateLoader(webRoot,
				FreemarkerUtils.TEMPLATES_ROOT, getClass()));
		BeansWrapperBuilder builder = new BeansWrapperBuilder(Configuration.VERSION_2_3_34);
		cfg.setObjectWrapper(builder.build());
		cfg.setOutputFormat(XHTMLOutputFormat.INSTANCE);
		cfg.setAutoEscapingPolicy(Configuration.ENABLE_IF_SUPPORTED_AUTO_ESCAPING_POLICY);
	}

	public void printXHTMLDocument(Writer out, String template, Map<String, String> datamodel)
			throws IOException
	{
		FreemarkerUtils.processTemplate(cfg, template, datamodel, out);
	}
}
