package org.geoserver.metrics;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Gauge;

public class MetricsInitializer implements GeoServerInitializer, ApplicationContextAware {

    ApplicationContext appContext;

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        Metrics.newGauge(Requests.class, "running-requests", new Gauge<Long>() {
            @Override
            public Long value() {
                return (Long) 
                    getControlFlowCallbackProperty(lookupControlFlowCallback(), "runningRequests");
            }
        });
        Metrics.newGauge(Requests.class, "queued-requests", new Gauge<Long>() {
            @Override
            public Long value() {
                return (Long) 
                    getControlFlowCallbackProperty(lookupControlFlowCallback(), "blockedRequests");
            }
        });
    }

    Object lookupControlFlowCallback() {
        try {
            return GeoServerExtensions.bean("controlFlowCallback", appContext);
        }
        catch(NoSuchBeanDefinitionException e) {
            throw new RuntimeException("control flow plug-in not installed.");
        }
    }

    Object getControlFlowCallbackProperty(Object callback, String property) {
        Object obj = callback;

        //handle case of monitoring enabled which wraps the control flow callback in a proxy
        if (Proxy.isProxyClass(obj.getClass())) {
            obj = Proxy.getInvocationHandler(obj);
        }

        return OwsUtils.get(obj, property);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.appContext = applicationContext;
    }

}
