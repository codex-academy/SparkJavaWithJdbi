package net.webapp;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

import spark.ModelAndView;
import spark.template.handlebars.HandlebarsTemplateEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static spark.Spark.*;

interface UserService {

    void addUser(String username);

    List<String> users();

    int userCount();

}

class UserServiceWithPG implements UserService {

    Jdbi jdbi;

    UserServiceWithPG(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    private boolean userExists(String username) {

        int count = jdbi.withHandle(h -> h.select("select count(*) from person where first_name = ?", username)
                .mapTo(int.class)
                .findOnly());
        return count > 0;
    }

    @Override
    public void addUser(String username) {
        if (!"".equals(username) &&  !userExists(username)) {
            this.jdbi.withHandle(handle ->
                    handle.execute("insert into person (first_name, counter) values (?,?)", username, 0));
        }
    }

    @Override
    public List<String> users() {
        return this.jdbi.withHandle(h -> h
                .createQuery("select first_name from person")
                .mapTo(String.class)
                .list());
    }

    @Override
    public int userCount() {
        return this.jdbi.withHandle(h -> h
                .select("select count(*) from person")
                .mapTo(int.class)
                .findOnly());
    }
}

class UserServiceWithJdbi implements UserService {

    Handle handle;

    public UserServiceWithJdbi(Handle handle) {
        this.handle = handle;
    }

    private boolean userExists(String userName) {
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

        String dbDiskURL = "jdbc:h2:file:./greetdb";
//        String dbMemoryURL = "jdbc:h2:mem:greetdb";

        Jdbi jdbi = Jdbi.create(dbDiskURL, "sa", "");

        Jdbi jdbiPG = Jdbi.create("jdbc:postgresql://localhost/greeter");


        // get a handle to the database
        Handle handle = jdbi.open();

        // create the table if needed
        handle.execute("create table if not exists greet ( id integer identity, name varchar(50), counter int )");

        // pass the database connection to the service to use
//        UserService userService = new UserServiceWithJdbi(handle);
        UserService userService = new UserServiceWithPG(jdbiPG);

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
