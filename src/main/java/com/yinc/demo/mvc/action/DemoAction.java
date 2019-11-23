package com.yinc.demo.mvc.action;

import com.yinc.demo.service.IDemoService;
import com.yinc.mvcframework.annotation.Autowired;
import com.yinc.mvcframework.annotation.Controller;
import com.yinc.mvcframework.annotation.RequestMapping;
import com.yinc.mvcframework.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
@RequestMapping("/demo")
public class DemoAction {

    @Autowired
    private IDemoService demoService;

    @RequestMapping("/get")
    public void get(HttpServletRequest req, HttpServletResponse resp,
                    @RequestParam("name") String name) {
        String result = demoService.add(name);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @RequestMapping("/query")
    public void query(HttpServletRequest req, HttpServletResponse resp,
                      @RequestParam("id") long id) {
        String result = demoService.query(id);
        try {
            resp.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @RequestMapping("/update")
    public void update(HttpServletRequest req, HttpServletResponse resp,
                       @RequestParam("name") String name, @RequestParam("id") long id) {
        int result = demoService.update(id, name);
        try {
            resp.getWriter().write("/update:" + result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @RequestMapping("/remote")
    public void remote(HttpServletRequest req, HttpServletResponse resp,
                       @RequestParam("id") long id) {
        String result = demoService.remote(id);
        try {
            resp.getWriter().write("/update:" + result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
