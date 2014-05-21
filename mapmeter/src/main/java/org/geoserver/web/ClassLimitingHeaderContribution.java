package org.geoserver.web;

import java.util.Set;

import org.apache.wicket.markup.html.WebPage;

public class ClassLimitingHeaderContribution extends HeaderContribution {

    private final Set<Class<?>> pagesToApplyTo;

    public ClassLimitingHeaderContribution(Set<Class<?>> pagesToApplyTo) {
        this.pagesToApplyTo = pagesToApplyTo;
    }

    @Override
    public boolean appliesTo(WebPage page) {
        Class<? extends WebPage> pageClass = page.getClass();
        boolean doesApply = pagesToApplyTo.contains(pageClass);
        return doesApply;
    }

}
