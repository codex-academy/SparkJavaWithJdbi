package net.webapp;

import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static spark.Spark.*;

public class App {
    public static void main(String[] args) {

        List<String> users = new ArrayList<>();

        staticFileLocation("/public");

        get("/", (req, res) -> {

            Map<String, Object> map = new HashMap<>();
            map.put("users", users);
            map.put("counter", users.size());

            return new ModelAndView(map, "hello.handlebars");
        }, new HandlebarsTemplateEngine());

        post("/greet", (req, res) -> {

            String username = req.queryParams("username");

            if (!username.trim().equals("") &&
                !users.contains(username)) {
                users.add(username);
            }


            res.redirect("/");
            return null;
        });

    }
}
