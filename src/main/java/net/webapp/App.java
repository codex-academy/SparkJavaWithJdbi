package net.webapp;

import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static spark.Spark.*;

public class App {

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
    }


    public static void main(String[] args) {

        List<String> users = new ArrayList<>();

        staticFileLocation("/public");

        port(getHerokuAssignedPort());

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
