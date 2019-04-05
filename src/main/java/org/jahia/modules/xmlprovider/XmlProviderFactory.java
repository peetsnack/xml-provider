/**
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.xmlprovider;

import org.jahia.exceptions.JahiaInitializationException;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.xmlprovider.factory.XmlMountPointFactoryHandler;
import org.jahia.services.SpringContextSingleton;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.ProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.jcr.RepositoryException;

import static org.jahia.modules.xmlprovider.factory.XmlMountPointFactory.XMLMOUNTPOINT_NT;
import static org.jahia.modules.xmlprovider.factory.XmlMountPointFactory.XMLPATH_PROPERTY;

public class XmlProviderFactory implements ProviderFactory, ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(XmlMountPointFactoryHandler.class);
    private static final String CACHE_NAME  = "xml-cache";
    private ApplicationContext applicationContext;

    @Override
    public String getNodeTypeName() { return XMLMOUNTPOINT_NT; }

    @Override
    public JCRStoreProvider mountProvider(JCRNodeWrapper mountPoint) throws RepositoryException {
        ExternalContentStoreProvider provider = (ExternalContentStoreProvider) SpringContextSingleton.getBean("ExternalStoreProviderPrototype");
        provider.setKey(mountPoint.getIdentifier());
        provider.setMountPoint(mountPoint.getPath());
        XmlDataSourceWritable dataSource = applicationContext.getBean(XmlDataSourceWritable.class);

        // Initialization of the EhCache if necessary
        if (dataSource.getCache() == null) {
            try {
                if (!dataSource.getEhCacheProvider().getCacheManager().cacheExists(CACHE_NAME+mountPoint.getIdentifier())) {
                    dataSource.getEhCacheProvider().getCacheManager().addCache(CACHE_NAME+mountPoint.getIdentifier());
                }
                dataSource.setCache(dataSource.getEhCacheProvider().getCacheManager().getCache(CACHE_NAME+mountPoint.getIdentifier()));
            } catch (Exception e) {
                logger.error("Error with the cache : " + e.getMessage());
            }
        }

        // Setting the XML path from the mount point
        dataSource.setXmlFilePath(mountPoint.getPropertyAsString(XMLPATH_PROPERTY));
        // Setting the data source then start
        provider.setDataSource(dataSource);
        provider.setDynamicallyMounted(true);
        provider.setSessionFactory(JCRSessionFactory.getInstance());
        try {
            provider.start();
        } catch (JahiaInitializationException e) {
            throw new RepositoryException(e);
        }
        return provider;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
