package com.pe.universal.controller;

import com.pe.universal.datos.Resumenes.UsuarioVista;
import com.pe.universal.exception.NegocioException;
import com.pe.universal.exception.NoEncontradoException;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;

@ControllerAdvice
public class VistaComun {
    @ModelAttribute public void datosComunes(Model modelo, HttpSession sesion) {
        String csrf = (String) sesion.getAttribute(AccesoWeb.CSRF);
        if (csrf == null) {
            csrf = java.util.UUID.randomUUID().toString();
            sesion.setAttribute(AccesoWeb.CSRF, csrf);
        }
        modelo.addAttribute("csrfBodega", csrf);
        UsuarioVista usuario = AccesoWeb.actual(sesion);
        modelo.addAttribute("usuarioWeb", usuario);
        modelo.addAttribute("hoy", LocalDate.now());
        modelo.addAttribute("direccion", "Av. Federico Villarreal, Trujillo, Perú");
    }

    @ExceptionHandler({NegocioException.class, NoEncontradoException.class,
            IllegalArgumentException.class, IllegalStateException.class})
    public String error(Exception excepcion, Model modelo) {
        modelo.addAttribute("titulo", "No se pudo completar la operación");
        modelo.addAttribute("mensaje", excepcion.getMessage());
        return "error";
    }
}
