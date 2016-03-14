/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.*;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.schema.registries.SchemaLoader;
import org.apache.directory.api.ldap.schema.extractor.SchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.api.ldap.schema.loader.LdifSchemaLoader;
import org.apache.directory.api.ldap.schema.manager.impl.DefaultSchemaManager;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.InstanceLayout;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.partition.Partition;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.server.core.api.schema.SchemaPartition;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.protocol.shared.transport.TcpTransport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Note: default settings contain one system user with
 * "cn=system administrator".
 */
public class LdapServerFacade
{
	private LdapServer impl_;

	private DirectoryService ds_;

	private String host_;

	private int port_;

	private String name_;

	private String workdir_;

	private BaseInterceptor interceptor_;

	public LdapServerFacade(String host, int port, BaseInterceptor interceptor,
			String nameOrNull, String workdir)
	{
		this.host_ = host;
		this.port_ = port;
		this.name_ = nameOrNull;
		this.workdir_ = workdir;
		this.interceptor_ = interceptor;

		this.impl_ = null;
		this.ds_ = null;
	}

	public DirectoryService getDs()
	{
		return ds_;
	}

	public Attribute getAttribute(String uid, String upId) throws LdapException
	{
		AttributeType at = ds_.getSchemaManager().lookupAttributeTypeRegistry(
				null == upId ? uid : upId);
		Attribute da = new DefaultAttribute(uid, at);
		return da;
	}

	public void init(boolean deleteWorkDir) throws Exception
	{
		impl_ = new LdapServer();
		impl_.setServiceName(name_);
		impl_.setTransports(new TcpTransport(host_, port_));

		ds_ = new DefaultDirectoryService();
		ds_.getChangeLog().setEnabled(false);

		// prepare the working dir with all the required settings
		prepareWorkDir(deleteWorkDir);

		// load the required settings
		loadSettings();

		//
		setUnityInterceptor(interceptor_);

		//
		impl_.setDirectoryService(ds_);
		ds_.startup();
	}

	public String getAdminDN()
	{
		return ServerDNConstants.ADMIN_SYSTEM_DN;
	}

	/**
	 * Apache DS has to be initialised with schemas etc. - this directory
	 * will be used to hold all the required configuration which is then
	 * loaded
	 */
	private File prepareWorkDir(boolean delete_work_dir) throws IOException
	{
		File workdirF = new File(workdir_);
		if (delete_work_dir && workdirF.exists())
		{
			FileUtils.deleteDirectory(workdirF);
		}
		boolean shouldExtract = !workdirF.exists();
		ds_.setInstanceLayout(new InstanceLayout(workdirF));

		if (shouldExtract)
		{
			SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor(ds_
					.getInstanceLayout().getPartitionsDirectory());
			extractor.extractOrCopy();
		}

		return workdirF;
	}

	/**
     *
     */
	private void loadSettings() throws Exception
	{
		File schemaPartitionDirectory = new File(ds_.getInstanceLayout()
				.getPartitionsDirectory(), "schema");
		SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
		SchemaManager schemaManager = new DefaultSchemaManager(loader);
		schemaManager.loadAllEnabled();
		ds_.setSchemaManager(schemaManager);

		LdifPartition schemaLdifPartition = new LdifPartition(schemaManager,
				ds_.getDnFactory());
		schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());
		SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
		schemaPartition.setWrappedPartition(schemaLdifPartition);
		ds_.setSchemaPartition(schemaPartition);

		JdbmPartition systemPartition = new JdbmPartition(ds_.getSchemaManager(),
				ds_.getDnFactory());
		systemPartition.setId("system");
		systemPartition.setPartitionPath(new File(ds_.getInstanceLayout()
				.getPartitionsDirectory(), systemPartition.getId()).toURI());
		systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
		systemPartition.setSchemaManager(ds_.getSchemaManager());

		ds_.setSystemPartition(systemPartition);
	}

	private void setUnityInterceptor(BaseInterceptor injectedInterceptor)
	{
		List<Interceptor> interceptors = ds_.getInterceptors();
		// Find Normalization interceptor in chain
		int insertionPosition = -1;
		for (int pos = 0; pos < interceptors.size(); ++pos)
		{
			Interceptor interceptor = interceptors.get(pos);
			if (interceptor instanceof NormalizationInterceptor)
			{
				insertionPosition = pos;
				break;
			}
		}
		// insert our new interceptor just behind
		interceptors.add(insertionPosition + 1, injectedInterceptor);
		ds_.setInterceptors(interceptors);
	}

	public void changeAdminPasswordBeforeStart(String password) throws LdapException
	{
		Dn systemDN = new Dn(ServerDNConstants.ADMIN_SYSTEM_DN);
		Partition p = ds_.getPartitionNexus().getPartition(systemDN);
		List<Modification> lmods = new ArrayList<>();
		// magic constants
		// https://tools.ietf.org/html/rfc4519 - Page 27
		Attribute da = getAttribute("userPassword", "2.5.4.35");
		da.add(password);
		DefaultModification dm = new DefaultModification(
				ModificationOperation.REPLACE_ATTRIBUTE, da);
		lmods.add(dm);
		p.modify(new ModifyOperationContext(null, systemDN, lmods));
	}

	public void addUser(String cn, String password, String displayName) throws LdapException
	{
		Dn userDN = new Dn("cn=" + cn + "," + ServerDNConstants.SYSTEM_DN);
		Entry serverEntry = new DefaultEntry(ds_.getSchemaManager(), userDN);
		serverEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC,
				SchemaConstants.PERSON_OC,
				SchemaConstants.ORGANIZATIONAL_PERSON_OC,
				SchemaConstants.INET_ORG_PERSON_OC);
		serverEntry.put(SchemaConstants.UID_AT, PartitionNexus.ADMIN_UID);
		serverEntry.put(SchemaConstants.USER_PASSWORD_AT, Strings.getBytesUtf8(password));
		serverEntry.put(SchemaConstants.DISPLAY_NAME_AT, displayName);
		serverEntry.put(SchemaConstants.CN_AT, cn);
		serverEntry.add(SchemaConstants.ENTRY_CSN_AT, ds_.getCSN().toString());
		ds_.getPartitionNexus().add(new AddOperationContext(null, userDN, serverEntry));
	}

	public void start() throws Exception
	{
		impl_.start();
	}
}