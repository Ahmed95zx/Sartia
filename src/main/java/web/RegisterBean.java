package web;

import model.User;
import service.UserService;
import service.exception.BusinessException;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/** Backs the customer registration form. */
@Named("registerBean")
@RequestScoped
public class RegisterBean {

    @Inject
    private UserService userService;

    @Inject
    private SessionBean session;

    private String username;
    private String password;
    private String passwordConfirm;
    private String email;
    private String fullName;
    private String phone;

    /**
     * Creates the account and signs the new customer straight in - asking
     * someone to type the credentials they just chose adds nothing.
     */
    public String register() {
        if (password == null || !password.equals(passwordConfirm)) {
            Messages.error("The passwords do not match");
            return null;
        }

        try {
            User created = userService.register(username, password, email, fullName, phone);
            session.login(created);
            Messages.info("Registration complete. Welcome, " + created.getFullName());
            return "/catalog.xhtml?faces-redirect=true";
        } catch (BusinessException failure) {
            Messages.error(failure.getMessage());
            return null;
        } finally {
            password = null;
            passwordConfirm = null;
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
