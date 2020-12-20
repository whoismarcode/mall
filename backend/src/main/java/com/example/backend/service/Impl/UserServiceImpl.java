package com.example.backend.service.Impl;

import com.example.backend.common.CommonResult;
import com.example.backend.exception.Asserts;
import com.example.backend.model.dto.UserRegisterRequest;
import com.example.backend.model.dto.UserUpdateRequest;
import com.example.backend.model.entity.Role;
import com.example.backend.model.entity.User;
import com.example.backend.model.entity.UserRole;
import com.example.backend.model.mapper.UserEditMapper;
import com.example.backend.model.mapper.UserViewMapper;
import com.example.backend.model.view.UserView;
import com.example.backend.repository.RoleRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.repository.UserRoleRepository;
import com.example.backend.service.AdminCacheService;
import com.example.backend.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.Random;

@Service
public class UserServiceImpl implements UserService {
    public static final String USERNAME = "username:";
    public static final String EMAIL = "email:";
    public static final String ROLE = "role:";
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserEditMapper userEditMapper;
    private final UserViewMapper userViewMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AdminCacheService adminCacheService;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository, BCryptPasswordEncoder passwordEncoder, UserViewMapper userViewMapper, UserEditMapper userEditMapper, AdminCacheService adminCacheService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.userEditMapper = userEditMapper;
        this.userViewMapper = userViewMapper;
        this.adminCacheService = adminCacheService;
    }

    public User save(UserRegisterRequest userRegisterRequest) {


        Optional usernamePresence = userRepository.findByUsername(userRegisterRequest.getUsername());
        Optional emailPresence = userRepository.findByEmail(userRegisterRequest.getEmail());
        Optional rolePresence = roleRepository.findByName(userRegisterRequest.getRole());

        if (usernamePresence.isPresent() || emailPresence.isPresent()) {
            Asserts.fail((usernamePresence.isPresent() ? "Username" : "Email") + " already exist");
            return null;
        }

        if (rolePresence.isPresent() == false) {
            Asserts.fail("Role not found");
        }

        User user = userEditMapper.requestToUser(userRegisterRequest);
        //TODO: emailVerified
        user.setIsEmailVerified(false);
        //TODO: enabled
        user.setIsEnabled(true);

        user.setPassword(passwordEncoder.encode(userRegisterRequest.getPassword()));
        userRepository.save(user);

        Role role = roleRepository.findByName(userRegisterRequest.getRole()).get();
        userRoleRepository.save(new UserRole(user, role));

        System.out.println(user);

        return user;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(
                () -> new UsernameNotFoundException("Username or password error")
        );
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(
                () -> new RuntimeException("Email not found")
        );
    }

    public CommonResult update(UserUpdateRequest userUpdateRequest) {
        Optional usernamePresence = userRepository.findByUsername(userUpdateRequest.getUsername());
        Optional emailPresence = userRepository.findByEmail(userUpdateRequest.getEmail());
        Optional rolePresence = roleRepository.findByName(userUpdateRequest.getRole());

        User user = findByEmail(userUpdateRequest.getEmail());
        String username = userUpdateRequest.getUsername();
        String password = userUpdateRequest.getPassword();
        String role = userUpdateRequest.getRole();

        if (Objects.nonNull(username)) {
            if (usernamePresence.isPresent()) {
                return CommonResult.failed("Username already exist");
            }

            user.setUsername(username);
        }
        if (Objects.nonNull(password)) {
            user.setPassword(passwordEncoder.encode(password));
        }
        if (Objects.nonNull(role)) {
            if (!rolePresence.isPresent()) {
                return CommonResult.failed("Role not found");
            }

        }
        userRepository.save(user);

        return CommonResult.success(userViewMapper.userToUserView(user));
    }

    public void delete(String email) {
        if (!userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email not found");
        }
        userRepository.deleteByEmail(email);
    }

    public Page<UserView> getAll(int pageNum, int pageSize) {
        return userRepository.findAll(PageRequest.of(pageNum, pageSize)).map(userViewMapper::userToUserView);
    }

    public boolean checkPassword(String currentPassword, String password) {
        return this.passwordEncoder.matches(currentPassword, password);
    }

    @Override
    public String generateOtp(String email) {
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        for (int i=0; i<6;i++) {
            stringBuilder.append(random.nextInt(10));
        }
        String otp = stringBuilder.toString();
        adminCacheService.setOtp(email, otp);
        return otp;
    }

    @Override
    public boolean verifyOtp(String otp, String email) {
        if (!StringUtils.hasLength(otp)) {
            return false;
        }
        String cacheOtp = adminCacheService.getOtp(email);
        return otp.equals(cacheOtp);
    }
}
