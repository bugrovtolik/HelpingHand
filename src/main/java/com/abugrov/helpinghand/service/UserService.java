package com.abugrov.helpinghand.service;

import com.abugrov.helpinghand.domain.Role;
import com.abugrov.helpinghand.domain.User;
import com.abugrov.helpinghand.repos.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    @Value("${upload.path}")
    private String uploadPath;

    @Value("${hostname}")
    private String hostname;

    private final UserRepo userRepo;
    private final MailSender mailSender;
    public final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepo userRepo, MailSender mailSender, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepo.findByEmail(email);

        if (user == null) {
            throw new UsernameNotFoundException("Пользователь не найден!");
        }

        return user;
    }

    public boolean addUser(User user) {
        User userFromDb = userRepo.findByEmail(user.getEmail());

        if (userFromDb != null) {
            return false;
        }

        user.setActive(false);
        user.setRoles(Collections.singleton(Role.USER));
        user.setActivationCode(UUID.randomUUID().toString());
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        userRepo.save(user);

        String message = String.format(
                "Приветсвуем Вас, %s!\n" +
                "Благодарим за регистрацию на сайте Helping Hand.\n" +
                "Пожалуйста, проследуйте по следующей ссылке для завершения " +
                "регистрации: http://%s/activate/%s",
                user.getUsername(), hostname, user.getActivationCode()
        );

        sendMessage(user,"Код активации для Helping Hand", message);

        return true;
    }

    public User findByActivationCode(String code) {
        return userRepo.findByActivationCode(code);
    }

    public User findByEmail(String email) {
        return userRepo.findByEmail(email);
    }

    public User findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public boolean activateUser(String code) {
        User user = findByActivationCode(code);

        if (user == null) {
            return false;
        }

        user.setActivationCode(null);
        user.setActive(true);

        userRepo.save(user);

        return true;
    }

    public List<User> findAll() {
        return userRepo.findAll();
    }

    public void saveUser(User user,
                         String username,
                         Map<String, String> form,
                         MultipartFile file
    ) throws IOException {

        user.setUsername(username);

        updateAvatar(user, file);

        Set<String> roles = Arrays.stream(Role.values())
                .map(Role::name)
                .collect(Collectors.toSet());

        user.getRoles().clear();

        for (String key : form.keySet()) {
            if (roles.contains(key)) {
                user.getRoles().add(Role.valueOf(key));
            }
        }

        userRepo.save(user);
    }

    private void sendMessage(User user, String topic, String message) {
        if (StringUtils.hasText(user.getEmail())) {
            mailSender.send(user.getEmail(), topic, message);
        }
    }

    public void recover(User user) {
        user.setActivationCode(UUID.randomUUID().toString());

        userRepo.save(user);

        String message = String.format(
            "Приветсвуем Вас, %s!\n" +
            "Пройдите по ссылке для создания нового пароля: http://%s/user/recover/%s",
            user.getUsername(), hostname, user.getActivationCode()
        );

        sendMessage(user,"Восстановление пароля на Helping Hand", message);
    }

    public boolean updatePassword(User user, String oldpass, String newpass) {
        if (StringUtils.hasText(newpass) && passwordEncoder.matches(oldpass, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(newpass));
            userRepo.save(user);

            return true;
        }

        return false;
    }

    public boolean setNewPassword(User user, String newpass) {
        if (StringUtils.hasText(newpass)) {
            user.setActivationCode(null);
            user.setPassword(passwordEncoder.encode(newpass));
            userRepo.save(user);

            return true;
        }

        return false;
    }

    public boolean updateAvatar(User user, MultipartFile file) throws IOException {
        if (file != null && !file.getOriginalFilename().isEmpty()) {
            File uploadDir = new File(uploadPath);

            if (!uploadDir.exists()) {
                uploadDir.mkdir();
            }

            String uuidFile = UUID.randomUUID().toString();
            String resultFilename = uuidFile + "." + file.getOriginalFilename();

            file.transferTo(new File(uploadPath + "/" + resultFilename));

            new File(uploadPath + "/" + user.getAvatar()).delete();

            user.setAvatar(resultFilename);

            userRepo.save(user);
        } else {
            return false;
        }

        userRepo.save(user);
        return true;
    }

    public boolean updateUsername(User user, String username) {
        if (StringUtils.hasText(username)) {
            user.setUsername(username);
            userRepo.save(user);

            return true;
        }
        
        return false;
    }
}