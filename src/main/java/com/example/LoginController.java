
package com.example;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.*;
import java.util.Enumeration;

import com.example.LoginBean;
import com.example.database;

@Controller
public class LoginController {
	
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String init(Model model) {
        return "index.html";
    }

    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String submit(Model model, @ModelAttribute("loginBean") LoginBean loginBean, HttpServletRequest request) {
        if (loginBean != null && loginBean.getUsername() != null & loginBean.getPassword() != null) {
			System.out.println("Checking Login ...");
            if (database.checkLogin(loginBean.getUsername(), loginBean.getPassword())) {
                model.addAttribute("error_msg", loginBean.getUsername());
				System.out.println("Login OK!");
				request.getSession().setAttribute("user", loginBean.getUsername());
				request.getSession().setAttribute("logged", "1");
				request.getSession().setAttribute("fullename", database.getUserFullName(loginBean.getUsername()));
                return "redirect:/";
            } else {
                model.addAttribute("error_msg", "Invalid Details");
				System.out.println("Login INVALID!");
                return "redirect:/";
            }
        } else {
            model.addAttribute("error_msg", "Please enter Details");
			System.out.println("Login NOT OK!");
            return "redirect:/";
        }
    }
	
	@RequestMapping(value = "/logout", method = RequestMethod.POST)
	public String logout(Model model, @ModelAttribute("loginBean") LoginBean loginBean, HttpServletRequest request) {
		Enumeration attributes = request.getSession().getAttributeNames();
		while(attributes.hasMoreElements())
		{
			String ele = attributes.nextElement().toString();
			request.getSession().removeAttribute(ele);
		}
		return "redirect:/";
	}
}