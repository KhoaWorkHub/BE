package vn.fpt.diamond_shop.service.Impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.fpt.diamond_shop.repository.UserRepository;
import vn.fpt.diamond_shop.repository.UserRoleRepository;
import vn.fpt.diamond_shop.request.ManagerModifyAccountRequest;
import vn.fpt.diamond_shop.response.ManagerListAccountResponse;
import vn.fpt.diamond_shop.security.UserPrincipal;
import vn.fpt.diamond_shop.security.model.*;
import vn.fpt.diamond_shop.service.AdminService;

import java.util.ArrayList;
import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;
    private RoleRepository roleRepository;

    @Override
    public void changeInforAccount(ManagerModifyAccountRequest request) {
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (request.getAccountId().equals(userPrincipal.getId())) {
            throw new RuntimeException("Cannot change your own account");
        }

        User user = userRepository.findById(request.getAccountId()).orElseThrow(() -> new RuntimeException("User not found"));
        if (request.getEmail() != null)
            user.setEmail(request.getEmail());
        if (request.getFullName() != null) {
            user.setName(request.getFullName());
        }
        if (request.isDeactivate()) {
            deactivateAccount(request.getAccountId());
        } else {
            activateAccount(request.getAccountId());
        }
        if (request.getRoleId() != null) {
            setRole(request.getAccountId(), request.getRoleId());
        }
        userRepository.save(user);
    }

    @Override
    public List<ManagerListAccountResponse> listAccount() {
        List<ManagerListAccountResponse> responses = new ArrayList<>();
        List<User> users = userRepository.findAll();
        UserPrincipal userPrincipal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        users.forEach(e -> {
            UserRole role = userRoleRepository.findAllByAccountId(e.getId()).get(0);
            RoleEnum roleEnum = RoleEnum.getRoleEnumById(role.getRoleId());
            if (!e.getId().equals(userPrincipal.getId())) {
                responses.add(new ManagerListAccountResponse(
                        e.getId(),
                        e.getEmail(),
                        e.getName(),
                        e.getEmailVerified(),
                        roleEnum.name(),
                        e.getProvider().name(),
                        e.isActive()));
            }
        });
        return responses;
    }

    @Override
    public List<Role> listRole() {
        return roleRepository.findAll();
    }

    private void deactivateAccount(Long accountId) {
        User user = userRepository.findById(accountId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(false);
    }

    private void activateAccount(Long accountId) {
        User user = userRepository.findById(accountId).orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(true);
    }

    @Override
    public void setRole(Long accountId, Long roleId) {
        List<UserRole> userRoleList = userRoleRepository.findAllByAccountId(accountId);
        if (!userRoleList.isEmpty()) {
            userRoleList.forEach(e -> {
                userRoleRepository.save(new UserRole(e.getId(), accountId, roleId));
            });
            return;
        }

        UserRole role = new UserRole();
        role.setRoleId(roleId);
        role.setAccountId(accountId);
        userRoleRepository.save(role);
    }
}
