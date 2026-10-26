package com.pe.universal.config;

import com.pe.universal.dao.RolRepository;
import com.pe.universal.dao.UsuarioRepository;
import com.pe.universal.model.Usuario;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SupervisorInicial {
    @Bean
    ApplicationRunner primerSupervisor(Environment entorno, UsuarioRepository usuarios, RolRepository roles) {
        return argumentos -> {
            if (usuarios.existsByRolNombreRol("SUPERVISOR")) return;
            String correo = entorno.getProperty("SUPERVISOR_INICIAL_CORREO", "");
            String clave = entorno.getProperty("SUPERVISOR_INICIAL_CLAVE", "");
            String dni = entorno.getProperty("SUPERVISOR_INICIAL_DNI", "");
            if (correo.isBlank() && clave.isBlank() && dni.isBlank()) return;
            if (!dni.matches("[0-9]{8}") || !correo.contains("@") || clave.length() < 8)
                throw new IllegalStateException("Datos incompletos para supervisor inicial");
            if (usuarios.existsByCorreoIgnoreCase(correo) || usuarios.existsByDni(dni))
                throw new IllegalStateException("El correo o DNI del supervisor ya está registrado");
            Usuario supervisor = new Usuario();
            supervisor.setRol(roles.findByNombreRol("SUPERVISOR")
                    .orElseThrow(() -> new IllegalStateException("Rol de supervisor no encontrado en la base de datos")));
            supervisor.setNombres(entorno.getProperty("SUPERVISOR_INICIAL_NOMBRES", "Supervisor"));
            supervisor.setApellidos(entorno.getProperty("SUPERVISOR_INICIAL_APELLIDOS", "Inicial"));
            supervisor.setDni(dni);
            supervisor.setTelefono(entorno.getProperty("SUPERVISOR_INICIAL_TELEFONO", "000000000"));
            supervisor.setCorreo(correo.trim().toLowerCase());
            supervisor.setContrasena(new BCryptPasswordEncoder().encode(clave));
            supervisor.setEstado((byte) 1);
            usuarios.save(supervisor);
        };
    }
}
