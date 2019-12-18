package org.smartboot.servlet.conf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * web.xml对象
 *
 * @author 三刀
 * @version V1.0 , 2019/12/12
 */
public class WebAppInfo {

    private Map<String, ServletInfo> servlets = new HashMap();

    private Map<String, FilterInfo> filters = new HashMap<>();

    private List<FilterMappingInfo> filterMappings = new ArrayList<>();

    private List<String> listeners = new ArrayList<>();

    private Map<String, String> contextParams = new HashMap<>();

    private Map<Integer, ErrorPageInfo> errorPages = new HashMap<>();

    private int sessionTimeout = 0;

    public void addServlet(ServletInfo servletInfo) {
        servlets.put(servletInfo.getServletName(), servletInfo);
    }

    public void addFilter(FilterInfo filterInfo) {
        filters.put(filterInfo.getFilterName(), filterInfo);
    }

    public void addFilterMapping(FilterMappingInfo filterMappingInfo) {
        filterMappings.add(filterMappingInfo);
    }

    public void addListener(String listener) {
        listeners.add(listener);
    }

    public void addContextParam(String param, String value) {
        this.contextParams.put(param, value);
    }

    public ServletInfo getServlet(String servletName) {
        return servlets.get(servletName);
    }

    public void addErrorPage(ErrorPageInfo errorPageInfo) {
        errorPages.put(errorPageInfo.getErrorCode(), errorPageInfo);
    }

    public int getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public Map<String, ServletInfo> getServlets() {
        return servlets;
    }

    public Map<String, FilterInfo> getFilters() {
        return filters;
    }

    public List<FilterMappingInfo> getFilterMappings() {
        return filterMappings;
    }

    public List<String> getListeners() {
        return listeners;
    }

    public Map<String, String> getContextParams() {
        return contextParams;
    }

    public Map<Integer, ErrorPageInfo> getErrorPages() {
        return errorPages;
    }
}
