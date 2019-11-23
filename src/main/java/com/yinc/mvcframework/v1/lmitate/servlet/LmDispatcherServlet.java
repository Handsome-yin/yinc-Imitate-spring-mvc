package com.yinc.mvcframework.v1.lmitate.servlet;

import com.yinc.mvcframework.annotation.Autowired;
import com.yinc.mvcframework.annotation.Controller;
import com.yinc.mvcframework.annotation.RequestMapping;
import com.yinc.mvcframework.annotation.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

public class LmDispatcherServlet extends HttpServlet {

    //className 的对应关系
    private HashMap<String, Object> classMapping = new HashMap<String, Object>();

    private HashMap<String, Method> urlHashMap = new HashMap<String, Method>();

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
        Method method = urlHashMap.get(uri);
        if (method == null) {
            resp.getWriter().write("404 Not Found!!");
            return;
        }
        resp.setHeader("Content-type", "text/html;charset=UTF-8");
        resp.setCharacterEncoding("UTF-8");

        Map<String, String[]> params = req.getParameterMap();
        //模拟 暂时没有实现参数匹配调用
        if (uri.contains("update")) {
            method.invoke(classMapping.get(method.getDeclaringClass().getName()), new Object[]{req, resp, params.get("name")[0], Integer.valueOf(params.get("id")[0])});
        } else if (uri.contains("get")) {
            method.invoke(classMapping.get(method.getDeclaringClass().getName()), new Object[]{req, resp, params.get("name")[0]});
        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        InputStream ipm = null;
        Properties confCont = new Properties();
        try {
            //获取配置文件对应名称
            ipm = this.getClass().getClassLoader().getResourceAsStream(config.getInitParameter("contextConfigLocation"));
            //简化流程直接 使用properties 文件 读取包路径
            confCont.load(ipm);
            String scanPackage = confCont.getProperty("scanPackage");
            //递归寻找每一个类
            doScanner(scanPackage);

            //初始化加注解的类
            initClassCtrl();

            //初始化成员变量注入
            intitCtrlParam();

            //下个版本 加参数匹配注入

        } catch (IOException e) {
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

    private void intitCtrlParam() {
        for (Object value : classMapping.values()) {
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
                        field.set(classMapping.get(clzz.getName()), classMapping.get(value1));
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
            for (String className : classMapping.keySet()) {
                Class<?> clzz = Class.forName(className);
                if (clzz.isAnnotationPresent(Controller.class)) {
                    //初始话带注解的类
                    classMapping.put(className, clzz.newInstance());
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
                            urlHashMap.put(url, method);

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
                    classMapping.put(beanName, newInstance);

                    //如此类有多个实现的接口
                    for (Class<?> i : clzz.getInterfaces()) {
                        classMapping.put(i.getName(), newInstance);
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
                classMapping.put(className, null);
            }

        }
    }
}
