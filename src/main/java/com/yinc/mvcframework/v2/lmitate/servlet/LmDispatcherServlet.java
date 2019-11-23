package com.yinc.mvcframework.v2.lmitate.servlet;

import com.yinc.mvcframework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class LmDispatcherServlet extends HttpServlet {

    private Properties confCont = new Properties();

    //className 的对应关系
    private HashMap<String, Object> ioc = new HashMap<String, Object>();

    private HashMap<String, Method> handlerMapping = new HashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatch(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception " + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {
        String uri = req.getRequestURI();
        String contextPath = req.getContextPath();
        uri = uri.replace(contextPath, "").replaceAll("/+", "/");
        Method method = handlerMapping.get(uri);
        if (method == null) {
            resp.getWriter().write("404 Not Found!!");
            return;
        }
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        //请求参数烈表
        Map<String, String[]> params = req.getParameterMap();
        //方法形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //实际调用方法时请求参数
        Object[] paramValue = new Object[parameterTypes.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (type == HttpServletRequest.class) {
                paramValue[i] = req;
            } else if (type == HttpServletResponse.class) {
                paramValue[i] = resp;
            } else if (type == String.class) {
                methodSetParam(method, i, params, paramValue);
            } else if (type == long.class) {
                methodSetParam(method, i, params, paramValue);
                paramValue[i] = Long.valueOf(String.valueOf(paramValue[i]));
            }
        }
        //模拟 暂时没有实现参数匹配调用
        method.invoke(ioc.get(method.getDeclaringClass().getName()), paramValue);
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        InputStream ipm = null;

        try {
            //获取配置文件对应名称
            doLoadConfig(config.getInitParameter("contextConfigLocation"));

            //递归寻找每一个类
            doScanner(confCont.getProperty("scanPackage"));

            //初始化加注解的类  IOC控制工厂 ,初始化handLerMapping
            initClassCtrl();

            //初始化成员变量注入 ,DI注入
            intitCtrlParam();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (ipm != null) {
                    ipm.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.print("MVC FRAMEWORK IS INIT");
    }

    private void methodSetParam(Method method, int i, Map<String, String[]> params, Object[] paramValue) {
        Annotation[][] pa = method.getParameterAnnotations();
        for (int i1 = 0; i1 < pa.length; i1++) {
            for (Annotation annotation : pa[i]) {
                if (annotation instanceof RequestParam) {
                    String value = ((RequestParam) annotation).value();
                    String s = Arrays.toString(params.get(value))
                            .replaceAll("\\[|\\]", "")
                            .replaceAll("\\s", ",");
                    paramValue[i] = s;
                }
            }

        }
    }

    private void doLoadConfig(String contextConfigLocation) throws IOException {
        //简化流程直接 使用properties 文件 读取包路径
        confCont.load(this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation));
    }

    private void intitCtrlParam() {
        for (Object value : ioc.values()) {
            if (value == null) {
                continue;
            }
            Class<?> clzz = value.getClass();
            if (clzz.isAnnotationPresent(Controller.class)) {
                Field[] fields = clzz.getDeclaredFields();
                for (Field field : fields) {
                    if (!field.isAnnotationPresent(Autowired.class)) {
                        continue;
                    }
                    Autowired annotation = field.getAnnotation(Autowired.class);
                    String value1 = annotation.value();
                    if ("".equals(value1)) {
                        value1 = field.getType().getName();
                    }
                    field.setAccessible(true);
                    try {
                        field.set(ioc.get(clzz.getName()), ioc.get(value1));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private void initClassCtrl() {
        try {
            //循环每个类判断是否有注解声明
            for (String className : ioc.keySet()) {
                Class<?> clzz = Class.forName(className);
                if (clzz.isAnnotationPresent(Controller.class)) {
                    //初始话带注解的类
                    ioc.put(className, clzz.newInstance());
                    //url和方法对应拼接类的URL
                    String baseUrl = "";
                    if (clzz.isAnnotationPresent(RequestMapping.class)) {
                        RequestMapping annotation = clzz.getAnnotation(RequestMapping.class);
                        baseUrl = annotation.value();
                    }

                    //拼接每个方法的URL
                    Method[] methods = clzz.getMethods();
                    for (Method method : methods) {
                        if (method.isAnnotationPresent(RequestMapping.class)) {
                            RequestMapping annotation = method.getAnnotation(RequestMapping.class);
                            String url = (baseUrl + "/" + annotation.value()).replaceAll("/+", "/");
                            handlerMapping.put(url, method);

                            System.out.println("初始化  每个接口(url:" + url + ";method:" + method + ")");
                        }
                    }


                } else if (clzz.isAnnotationPresent(Service.class)) {
                    Service annotation = clzz.getAnnotation(Service.class);
                    String beanName = annotation.value();
                    if ("".equals(beanName)) {
                        beanName = clzz.getName();
                    }
                    Object newInstance = clzz.newInstance();
                    ioc.put(beanName, newInstance);

                    //如此类有多个实现的接口
                    for (Class<?> i : clzz.getInterfaces()) {
                        ioc.put(i.getName(), newInstance);
                    }
                }

            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        File file = new File(url.getFile());
        for (File filee : file.listFiles()) {
            if (filee.isDirectory()) {
                doScanner(scanPackage + "." + filee.getName());
            } else {
                if (!filee.getName().endsWith(".class")) {
                    continue;
                }
                String className = scanPackage + "." + filee.getName().replace(".class", "");
                ioc.put(className, null);
            }

        }
    }
}
