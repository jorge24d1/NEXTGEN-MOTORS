package com.concesionario.controller;

import com.concesionario.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class PasswordResetController {
    private static final Logger log = LoggerFactory.getLogger(PasswordResetController.class);

    @Autowired
    private PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm(Model model) {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                        RedirectAttributes redirectAttributes) {
        try {
            passwordResetService.initiatePasswordReset(email);
        } catch (RuntimeException e) {
            log.warn("No se pudo procesar una solicitud de recuperación ({})", e.getClass().getSimpleName());
        }
        redirectAttributes.addFlashAttribute("success",
                "Si hay una cuenta con ese correo, recibirás las instrucciones para restablecer tu contraseña.");
        return "redirect:/login";
    }

    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        try {
            passwordResetService.validateToken(token);
            model.addAttribute("token", token);
            return "auth/reset-password";
        } catch (Exception e) {
            model.addAttribute("error", "El enlace de recuperación es inválido o ha expirado");
            return "auth/reset-password";
        }
    }

    @PostMapping("/reset-password")
    public String processResetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            Model model) {
        try {
            passwordResetService.resetPassword(token, newPassword);

            // Usar close-window para reset-password
            model.addAttribute("closeWindow", true);
            model.addAttribute("redirectUrl", "/login");
            model.addAttribute("success", "¡Contraseña restablecida exitosamente! Esta ventana se cerrará automáticamente.");

            return "auth/close-window";

        } catch (Exception e) {
            model.addAttribute("error", "No se pudo restablecer la contraseña. Solicita un enlace nuevo.");
            model.addAttribute("token", token);
            return "auth/reset-password";
        }
    }

    // Vista especial para cerrar ventanas (solo para reset-password)
    @GetMapping("/close-window")
    public String closeWindow(@RequestParam(required = false) String redirectUrl,
                              @RequestParam(required = false) String message,
                              Model model) {
        if (redirectUrl != null) {
            model.addAttribute("redirectUrl", redirectUrl);
        }
        if (message != null) {
            model.addAttribute("message", message);
        }
        model.addAttribute("closeWindow", true);
        return "auth/close-window";
    }
}
