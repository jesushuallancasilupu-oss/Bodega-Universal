package com.pe.universal.controller;

import com.pe.universal.datos.Resumenes.UsuarioVista;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Component
public class AccesoWeb implements HandlerInterceptor, WebMvcConfigurer {
    public static final String USUARIO = "usuarioWeb";
    public static final String CSRF = "csrfBodegaToken";

    public static UsuarioVista actual(HttpSession sesion) {
        Object valor = sesion.getAttribute(USUARIO);
        return valor instanceof UsuarioVista usuario ? usuario : null;
    }

    public static int id(HttpSession sesion) {
        UsuarioVista usuario = actual(sesion);
        if (usuario == null) throw new IllegalStateException("Inicia sesión para continuar");
        return usuario.id();
    }

    public static String destino(UsuarioVista usuario) {
        if (usuario == null) return "/ingresar";
        return switch (usuario.rol()) {
            case "CLIENTE" -> "/cliente/pedidos";
            case "VENDEDOR" -> "/vendedor";
            case "SUPERVISOR" -> "/";
            default -> "/ingresar";
        };
    }

    @Override public boolean preHandle(HttpServletRequest solicitud,
            jakarta.servlet.http.HttpServletResponse respuesta, Object manejador) throws Exception {
        if ("POST".equalsIgnoreCase(solicitud.getMethod())) {
            HttpSession sesionActual = solicitud.getSession(false);
            String esperado = sesionActual == null ? null : (String) sesionActual.getAttribute(CSRF);
            String enviado = solicitud.getParameter("_bodega_csrf");
            if (esperado == null || enviado == null ||
                    !java.security.MessageDigest.isEqual(
                            esperado.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                            enviado.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                respuesta.sendError(403, "Formulario vencido o no autorizado. Recarga la página.");
                return false;
            }
        }
        String ruta = solicitud.getRequestURI().substring(solicitud.getContextPath().length());
        String rol = ruta.startsWith("/cliente/") ? "CLIENTE"
                : ruta.startsWith("/vendedor") ? "VENDEDOR"
                : ruta.startsWith("/admin") ? "SUPERVISOR" : null;
        if (rol == null) return true;
        UsuarioVista usuario = actual(solicitud.getSession(false) == null
                ? solicitud.getSession() : solicitud.getSession(false));
        if (usuario == null) { respuesta.sendRedirect(solicitud.getContextPath() + "/ingresar?requerido"); return false; }
        if (!rol.equals(usuario.rol())) { respuesta.sendRedirect(solicitud.getContextPath() + destino(usuario)); return false; }
        return true;
    }

    @Override public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(this).addPathPatterns("/**");
    }
}
