package com.pe.universal.controller;

import com.pe.universal.datos.Resumenes.UsuarioVista;
import com.pe.universal.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IngresoController {
    private final UsuarioService usuarios;
    public IngresoController(UsuarioService usuarios) { this.usuarios = usuarios; }

    @GetMapping("/ingresar") 
    public String ingresar(Model modelo) {
        modelo.addAttribute("titulo", "Iniciar sesión"); return "acceso/ingresar";
    }

    @PostMapping("/ingresar") 
    public String procesarIngreso(@RequestParam String correo, @RequestParam String contrasena,
            HttpServletRequest solicitud) {
        UsuarioVista usuario = usuarios.iniciarSesion(correo, contrasena);
        solicitud.getSession(true);
        solicitud.changeSessionId();
        solicitud.getSession().setAttribute(AccesoWeb.USUARIO, usuario);
        return "redirect:" + AccesoWeb.destino(usuario);
    }

    @GetMapping("/registrarse") 
    public String registro(Model modelo) {
        modelo.addAttribute("titulo", "Registro de cliente"); return "acceso/registro";
    }

    @PostMapping("/registrarse") 
    public String procesarRegistro(@RequestParam String nombres,
            @RequestParam String apellidos, @RequestParam String dni, @RequestParam String telefono,
            @RequestParam String correo, @RequestParam String contrasena, @RequestParam String confirmar,
            RedirectAttributes mensajes) {
        if (!contrasena.equals(confirmar)) throw new IllegalArgumentException("Las contraseñas no coinciden");
        usuarios.registrarCliente(nombres, apellidos, dni, telefono, correo, contrasena);
        mensajes.addFlashAttribute("exito", "Cuenta creada. Ahora puedes iniciar sesión.");
        return "redirect:/ingresar";
    }

    @PostMapping("/salir") 
    public String salir(HttpSession sesion) {
        sesion.invalidate(); return "redirect:/";
    }
}
