/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ldap.endpoint;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import pl.edu.icm.unity.ldaputils.LDAPAttributeTypesConverter;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Note: default settings contain one system user with
 * "cn=system administrator".
 */
public class LdapServerFacade
{
    private LdapServer impl;

    private DirectoryService ds;

    private String host;

    private int port;

    private String name;

    private String workdir;

    public String partitionResource = "partitions.zip";

    public LdapServerFacade(String host, int port, String nameOrNull, String workdir)
    {
        this.host = host;
        this.port = port;
        this.name = nameOrNull;
        this.workdir = workdir;

        this.impl = null;
        this.ds = null;
    }

    public DirectoryService getDs()
    {
        return ds;
    }

    public Attribute getAttribute(String uid, String upId) throws LdapException
    {
        AttributeType at = ds.getSchemaManager().lookupAttributeTypeRegistry(
                null == upId ? uid : upId);
        Attribute da = new DefaultAttribute(uid, at);
        return da;
    }

    public void init(boolean deleteWorkDir, BaseInterceptor interceptor) throws Exception
    {
        impl = new LdapServer();
        impl.setServiceName(name);
        impl.setTransports(new TcpTransport(host, port));

        ds = new DefaultDirectoryService();
        ds.getChangeLog().setEnabled(false);

        // prepare the working dir with all the required settings
        prepareWorkDir(deleteWorkDir);

        // load the required settings
        loadSettings();

        //
        setUnityInterceptor(interceptor);

        //
        impl.setDirectoryService(ds);
        ds.startup();
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
        File workdirF = new File(workdir);
        if (delete_work_dir && workdirF.exists())
        {
            FileUtils.deleteDirectory(workdirF);
        }
        boolean shouldExtract = !workdirF.exists();
        ds.setInstanceLayout(new InstanceLayout(workdirF));

        if (shouldExtract)
        {
            InputStream jarZipIs = LdapServerFacade.class.getClassLoader().getResourceAsStream(
                partitionResource);
            ZipInputStream zis = new ZipInputStream(jarZipIs);
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File entryDestination = new File(workdirF,  entry.getName());
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    OutputStream out = new FileOutputStream(entryDestination);
                    IOUtils.copy(zis, out);
                    out.close();
                }
            }
            zis.close();
        }

        return workdirF;
    }

    /**
     *
     */
    private void loadSettings() throws Exception
    {
        File schemaPartitionDirectory = new File(ds.getInstanceLayout()
                .getPartitionsDirectory(), "schema");
        SchemaLoader loader = new LdifSchemaLoader(schemaPartitionDirectory);
        SchemaManager schemaManager = new DefaultSchemaManager(loader);
        schemaManager.loadAllEnabled();
        ds.setSchemaManager(schemaManager);

        LdifPartition schemaLdifPartition = new LdifPartition(schemaManager,
                ds.getDnFactory());
        schemaLdifPartition.setPartitionPath(schemaPartitionDirectory.toURI());
        SchemaPartition schemaPartition = new SchemaPartition(schemaManager);
        schemaPartition.setWrappedPartition(schemaLdifPartition);
        ds.setSchemaPartition(schemaPartition);

        JdbmPartition systemPartition = new JdbmPartition(ds.getSchemaManager(),
                ds.getDnFactory());
        systemPartition.setId("system");
        systemPartition.setPartitionPath(new File(ds.getInstanceLayout()
                .getPartitionsDirectory(), systemPartition.getId()).toURI());
        systemPartition.setSuffixDn(new Dn(ServerDNConstants.SYSTEM_DN));
        systemPartition.setSchemaManager(ds.getSchemaManager());

        ds.setSystemPartition(systemPartition);
    }

    private void setUnityInterceptor(BaseInterceptor injectedInterceptor)
    {
        List<Interceptor> interceptors = ds.getInterceptors();
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
        ds.setInterceptors(interceptors);
    }

    public void changeAdminPasswordBeforeStart(String password) throws LdapException
    {
        Dn systemDN = new Dn(ServerDNConstants.ADMIN_SYSTEM_DN);
        Partition p = ds.getPartitionNexus().getPartition(systemDN);
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
        Entry serverEntry = new DefaultEntry(ds.getSchemaManager(), userDN);
        serverEntry.put(SchemaConstants.OBJECT_CLASS_AT, SchemaConstants.TOP_OC,
                SchemaConstants.PERSON_OC,
                SchemaConstants.ORGANIZATIONAL_PERSON_OC,
                SchemaConstants.INET_ORG_PERSON_OC);
        serverEntry.put(SchemaConstants.UID_AT, PartitionNexus.ADMIN_UID);
        serverEntry.put(SchemaConstants.USER_PASSWORD_AT, Strings.getBytesUtf8(password));
        serverEntry.put(SchemaConstants.DISPLAY_NAME_AT, displayName);
        serverEntry.put(SchemaConstants.CN_AT, cn);
        serverEntry.add(SchemaConstants.ENTRY_CSN_AT, ds.getCSN().toString());
        ds.getPartitionNexus().add(new AddOperationContext(null, userDN, serverEntry));
    }

    public void start() throws Exception
    {
        impl.start();
    }
}