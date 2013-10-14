/**
 * Vosao CMS. Simple CMS for Google App Engine.
 * 
 * Copyright (C) 2009-2010 Vosao development team.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * email: vosao.dev@gmail.com
 */

package org.vosao.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.vosao.business.SetupBean;
import org.vosao.dao.DaoStat;
import org.vosao.dao.cache.EntityCache;
import org.vosao.dao.cache.QueryCache;
import org.vosao.entity.ConfigEntity;

/**
 * Application initial database creation filter.
 * @author Alexander Oleynik
 *
 */
public class InitFilter extends AbstractFilter implements Filter {
    
    private static final Log logger = LogFactory.getLog(SiteFilter.class);

    private static final String SETUP_URL = "/setup";
    
    private int localHits;
    private int cacheHits;
    private DaoStat daoStat;
    
    public InitFilter() {
    	super();
    }
  
    public void doFilter(ServletRequest request, ServletResponse response, 
    		FilterChain chain) throws IOException, ServletException {
    	HttpServletRequest httpRequest = (HttpServletRequest)request;
        HttpServletResponse httpResponse = (HttpServletResponse)response;
        String url = httpRequest.getServletPath();
        if (url.equals(SETUP_URL)) {
            ConfigEntity config = getDao().getConfigDao().getConfig();
            if (config == null || config.getVersion() == null) {
            	SetupBean setupBean = getBusiness().getSetupBean();
            	setupBean.clear();
            	setupBean.setup();
            	logger.info("Setup was successfully completed.");
            	setupBean.loadDefaultSite();
            }
        	httpResponse.sendRedirect("/");
        	return;
        }
        //startProfile();
        chain.doFilter(request, response);
        //endProfile(httpRequest.getRequestURL().toString());
        getBusiness().getSystemService().getCache().resetLocalCache();
    }
    
    private void startProfile() {
        localHits = getBusiness().getSystemService().getCache().getLocalHits();
        cacheHits = getBusiness().getSystemService().getCache().getCacheHits();
        daoStat = getDao().getDaoStat().clone();
    }
    
    private void endProfile(String url) {
        logger.info(url);
        logger.info("local cache hits: " + (
                getBusiness().getSystemService().getCache().getLocalHits() 
                - localHits));
        DaoStat daoStat2 = getDao().getDaoStat();
        logger.info("memcache hits: " + (
        		getBusiness().getSystemService().getCache().getCacheHits() 
        		- cacheHits));
        logger.info("dao get calls: " 
        		+ (daoStat2.getGetCalls() - daoStat.getGetCalls()) 
        		+ " ,dao query calls: " 
        		+ (daoStat2.getQueryCalls() - daoStat.getQueryCalls()) 
        		+ " ,entity cache hits: "
        		+ (daoStat2.getEntityCacheHits() - daoStat.getEntityCacheHits())
        		+ " ,query cache hits: "
        		+ (daoStat2.getQueryCacheHits() - daoStat.getQueryCacheHits())
        );
    }
    
    private EntityCache getEntityCache() {
    	return getDao().getEntityCache();
    }

    private QueryCache getQueryCache() {
    	return getDao().getQueryCache();
    }
    
}
