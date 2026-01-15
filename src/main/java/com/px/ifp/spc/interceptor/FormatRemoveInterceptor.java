package com.px.ifp.spc.interceptor;

import com.github.pagehelper.PageHelper;
import com.px.ifp.spc.util.BigDecimalUtils;
import com.px.ifp.spc.util.OperationDateUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FormatRemoveInterceptor implements HandlerInterceptor {

    //protected static final Logger logger = LoggerFactory.getLogger(FormatRemoveInterceptor.class);

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        PageHelper.clearPage();
        //logger.info(Thread.currentThread() + " PageHelper.clearPage");
        OperationDateUtils.remove();
        BigDecimalUtils.remove();
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
