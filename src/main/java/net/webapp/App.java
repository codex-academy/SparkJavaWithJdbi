package net.webapp;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static spark.Spark.*;

interface UserService {

    @SqlUpdate("INSERT INTO greet(name) VALUES (?)")
    void addUser(String username);

    @SqlUpdate("select name from greet")
    List<String> users();

    int userCount();

}

class UserServiceWithJdbi implements UserService {

    Handle handle;

    public UserServiceWithJdbi(Handle handle) {
        this.handle = handle;
    }

    private boolean userExists(String  userName) {
        int count = handle.select("select count(*) from greet where name = ?", userName)
                .mapTo(int.class)
                .findOnly();

        return count > 0;
    }


    @Override
    public void addUser(String username) {
        if (!userExists(username)) {
            handle.execute("insert into greet (name) values (?)", username);
        }
    }

    @Override
    public List<String> users() {
        List<String> names = handle.createQuery("select name from greet")
                        .mapTo(String.class)
                        .list();
        return names;
    }

    @Override
    public int userCount() {
        int count = handle.select("select count(*) from greet")
                        .mapTo(int.class)
                        .findOnly();
        return count;
    }
}

class UserServiceUsingList implements UserService {

    List<String> users = new ArrayList<>();

    public void addUser(String username) {
        if (!username.trim().equals("") &&
                !users.contains(username)) {
            users.add(username);
        }
    }

    public List<String> users() {
        return new ArrayList<>(users);
    }

    public int userCount() {
        return users.size();
    }

}


public class App {

    static int getHerokuAssignedPort() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        if (processBuilder.environment().get("PORT") != null) {
            return Integer.parseInt(processBuilder.environment().get("PORT"));
        }
        return 4567; //return default port if heroku-port isn't set (i.e. on localhost)
    }


    public static void main(String[] args) {

        Jdbi jdbi = Jdbi.create("jdbc:h2:mem:greetdb", "sa", "");

        Handle handle = jdbi.open();

        handle.execute("create table greet ( id integer identity, name varchar(50) )");

        UserService userService = new UserServiceWithJdbi(handle);

        staticFileLocation("/public");

        port(getHerokuAssignedPort());

        get("/", (req, res) -> {

            Map<String, Object> map = new HashMap<>();
            map.put("users", userService.users());
            map.put("counter", userService.userCount());

            return new ModelAndView(map, "hello.handlebars");
        }, new HandlebarsTemplateEngine());

        post("/greet", (req, res) -> {

            String username = req.queryParams("username");
            userService.addUser(username);

            res.redirect("/");
            return null;
        });

    }
}
