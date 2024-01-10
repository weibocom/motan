package com.weibo.api.motan.filter;

import com.weibo.api.motan.core.extension.SpiMeta;
import com.weibo.api.motan.exception.MotanAbstractException;
import com.weibo.api.motan.exception.MotanBizException;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.util.MotanFrameworkUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

/**
 * @author zhanglei28
 * @date 2023/10/31.
 */
@SpiMeta(name = "faultInjection")
public class FaultInjectionFilter implements Filter {

    @Override
    public Response filter(Caller<?> caller, Request request) {
        FaultInjectionConfig config = FaultInjectionUtil.getGlobalFaultInjectionConfig(request.getInterfaceName(), request.getMethodName());
        if (config == null) {
            return caller.call(request);
        }

        Response response;
        long delay;
        Exception exception = config.getException();
        if (exception != null) {
            response = MotanFrameworkUtil.buildErrorResponse(request, exception);
            delay = config.getExceptionTime();
        } else {
            response = caller.call(request);
            delay = config.getDelayTime(response.getProcessTime());
        }
        if (delay > 0) {
            // process injected delay ignore sync/async calls
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignore) {
            }
            response.setProcessTime(response.getProcessTime() + delay);
        }
        return response;
    }

    public static class FaultInjectionConfig {
        public String id; //rule id.
        public String servicePattern;
        public String methodPattern;
        public float delayRatio; // Delay ratio. For example, 0.25 means that the time-consuming delay is 0.25 times the actual time-consuming time.
        public long delayTime; // Specify the delay time(ms), for example, 1000 means a delay of 1 second based on the actual time taken. The delay includes remote exceptions.
        public int exceptionPercent; // Inject exception percentage. For example, setting it to 25 means that 25% of requests will return active exceptions. Percentages are used to facilitate calculations.
        public long exceptionTime; // Expected execution time when injecting an exception
        public String exceptionType; // Specifies the exception type when injecting exceptions.
        private Class<? extends Exception> exceptionClass;

        public boolean isMatch(String service, String method) {
            // check service
            boolean match = isMatch0(servicePattern, service);
            if (match && StringUtils.isNotBlank(methodPattern)) { // check method
                return isMatch0(methodPattern, method);
            }
            return match;
        }

        private boolean isMatch0(String pattern, String str) {
            if (StringUtils.isNotBlank(pattern)) {
                return Pattern.matches(pattern, str);
            }
            return false;
        }

        public Exception getException() {
            if (shouldException()) {
                if (exceptionClass != null) {
                    try {
                        Exception exception = exceptionClass.newInstance();
                        if (exception instanceof MotanAbstractException) { // return directly if it is motan exception
                            return exception;
                        }
                        return new MotanBizException(exception);
                    } catch (Exception ignore) {
                    }
                }
                return new MotanServiceException("exception from FaultInjectionFilter");
            }
            return null;
        }

        private boolean shouldException() {
            if (exceptionPercent > 0) {
                if (exceptionPercent >= 100) {
                    return true;
                }
                return exceptionPercent > ThreadLocalRandom.current().nextInt(100);
            }
            return false;
        }

        public long getDelayTime(long responseTime) {
            if (delayTime > 0) {
                return delayTime;
            } else if (delayRatio > 0) {
                return (long) (responseTime * (delayRatio));
            }
            return 0;
        }

        public long getExceptionTime() {
            return exceptionTime;
        }

        @SuppressWarnings("unchecked")
        public void init() {
            if (exceptionType != null) {
                try {
                    exceptionClass = (Class<? extends Exception>) Class.forName(exceptionType);
                } catch (Exception ignore) {
                }
            }
        }
    }

    public static class FaultInjectionUtil {
        private static final String SEPARATOR = "-";
        private static final FaultInjectionConfig NOT_MATCH = new FaultInjectionConfig();
        // Fault injection configuration matching cache
        private static final ConcurrentHashMap<String, FaultInjectionConfig> matchCache = new ConcurrentHashMap<>();
        private static List<FaultInjectionConfig> configList;

        // The configs to be updated must be complete configs，incremental updates are not supported.
        // Configs are regular expressions, multiple configs need to ensure matching order.
        // The fuzzy matching rules should after exact matching rules, the service rules should after the method matching rules.
        public static void updateConfigs(List<FaultInjectionConfig> configList) {
            configList.forEach(FaultInjectionConfig::init);
            FaultInjectionUtil.configList = configList;
            matchCache.clear();
        }

        public static void clearConfigs() {
            configList = null;
            matchCache.clear();
        }

        // get all configs
        public static List<FaultInjectionConfig> getConfigs() {
            return configList;
        }

        public static FaultInjectionConfig getGlobalFaultInjectionConfig(String service, String method) {
            if (configList == null) {
                return null;
            }
            FaultInjectionConfig config = matchCache.get(service + SEPARATOR + method);
            if (config == null) { // Not matched yet
                List<FaultInjectionConfig> configs = configList;
                for (FaultInjectionConfig temp : configs) {
                    if (temp.isMatch(service, method)) {
                        config = temp;
                        break;
                    }
                }
                if (config == null) { // Unmatched cache
                    config = NOT_MATCH;
                }
                matchCache.put(service + SEPARATOR + method, config);
            }
            if (config == NOT_MATCH) { // Mismatch
                return null;
            }
            return config;
        }
    }
}
