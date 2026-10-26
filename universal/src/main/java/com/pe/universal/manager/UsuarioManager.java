package com.pe.universal.manager;

import com.pe.universal.dao.RolRepository;
import com.pe.universal.dao.UsuarioRepository;
import com.pe.universal.datos.Resumenes;
import com.pe.universal.datos.Resumenes.UsuarioVista;
import com.pe.universal.exception.NegocioException;
import com.pe.universal.exception.NoEncontradoException;
import com.pe.universal.model.Rol;
import com.pe.universal.model.Usuario;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UsuarioManager {
    private final UsuarioRepository usuarios;
    private final RolRepository roles;
    private final BCryptPasswordEncoder codificador = new BCryptPasswordEncoder();

    public UsuarioManager(UsuarioRepository usuarios, RolRepository roles) {
        this.usuarios = usuarios;
        this.roles = roles;
    }

    public Usuario exigir(Integer usuarioId, String... rolesPermitidos) {
        Reglas.exigir(usuarioId != null, "Debe iniciar sesión");
        Usuario usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new NoEncontradoException("Usuario no encontrado"));
        Reglas.exigir(Byte.valueOf((byte) 1).equals(usuario.getEstado()), "Usuario inactivo");

        for (String rol : rolesPermitidos) {
            if (rol.equals(usuario.getRol().getNombreRol())) {
                return usuario;
            }
        }
        throw new NegocioException("No tiene permiso para esta operación");
    }

    public void propietario(Integer usuarioId, Usuario propietario) {
        Usuario usuario = exigir(usuarioId, "CLIENTE", "SUPERVISOR");
        Reglas.exigir(usuario.getIdUsuario().equals(propietario.getIdUsuario())
                || "SUPERVISOR".equals(usuario.getRol().getNombreRol()),
                "No puede consultar o modificar pedidos ajenos");
    }

    @Transactional(readOnly = true)
    public UsuarioVista iniciarSesion(String correo, String clave) {
        Reglas.exigir(correo != null && clave != null, "Ingrese correo y contraseña");
        Usuario usuario = usuarios.findByCorreoIgnoreCase(correo.trim())
                .orElseThrow(() -> new NegocioException("Credenciales incorrectas"));
        Reglas.exigir(Byte.valueOf((byte)1).equals(usuario.getEstado()) && codificador.matches(clave, usuario.getContrasena()),
                "Credenciales incorrectas");
        return Resumenes.usuario(usuario);
    }

    @Transactional
    public UsuarioVista registrarCliente(String nombres, String apellidos, String dni, String telefono,
            String correo, String clave) {
        return crear(nombres, apellidos, dni, telefono, correo, clave, "CLIENTE");
    }

    @Transactional
    public UsuarioVista registrarVendedor(Integer supervisorId, String nombres, String apellidos, String dni,
            String telefono, String correo, String clave) {
        exigir(supervisorId, "SUPERVISOR");
        return crear(nombres, apellidos, dni, telefono, correo, clave, "VENDEDOR");
    }

    private UsuarioVista crear(String nombres, String apellidos, String dni, String telefono, String correo,
            String clave, String rol) {
        Reglas.exigir(nombres != null && !nombres.isBlank() && nombres.trim().length() <= 80,
                "Nombre inválido");
        Reglas.exigir(apellidos != null && !apellidos.isBlank() && apellidos.trim().length() <= 80,
                "Apellidos inválidos");
        Reglas.exigir(dni != null && dni.matches("[0-9]{8}"), "DNI debe contener 8 dígitos");
        Reglas.exigir(telefono != null && !telefono.isBlank() && telefono.length() <= 15, "Teléfono inválido");
        Reglas.exigir(correo != null && correo.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$") && correo.length() <= 100,
                "Correo inválido");
        Reglas.exigir(clave != null && clave.length() >= 8 && clave.length() <= 72,
                "La contraseña debe tener entre 8 y 72 caracteres");
        Reglas.exigir(!usuarios.existsByDni(dni), "El DNI ya está registrado");
        Reglas.exigir(!usuarios.existsByCorreoIgnoreCase(correo.trim()), "El correo ya está registrado");
        Rol rolEncontrado = roles.findByNombreRol(rol)
                .orElseThrow(() -> new NegocioException("Falta el rol " + rol + " en la base de datos"));
        Usuario usuario = new Usuario();
        usuario.setRol(rolEncontrado); usuario.setNombres(nombres.trim()); usuario.setApellidos(apellidos.trim());
        usuario.setDni(dni); usuario.setTelefono(telefono.trim()); usuario.setCorreo(correo.trim().toLowerCase());
        usuario.setContrasena(codificador.encode(clave)); usuario.setEstado((byte) 1);
        return Resumenes.usuario(usuarios.save(usuario));
    }

    @Transactional(readOnly = true)
    public List<UsuarioVista> listarVendedores(Integer supervisorId) {
        exigir(supervisorId, "SUPERVISOR");
        return usuarios.findByRolNombreRolAndEstado("VENDEDOR", (byte) 1).stream()
                .map(Resumenes::usuario).toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioVista> listarClientes(Integer usuarioId) {
        exigir(usuarioId, "VENDEDOR", "SUPERVISOR");
        return usuarios.findByRolNombreRolAndEstado("CLIENTE", (byte) 1)
                .stream().map(Resumenes::usuario).toList();
    }

    @Transactional
    public void desactivarVendedor(Integer supervisorId, Integer vendedorId) {
        exigir(supervisorId, "SUPERVISOR");
        Usuario usuario = usuarios.findById(vendedorId)
                .orElseThrow(() -> new NoEncontradoException("Vendedor inexistente"));
        Reglas.exigir("VENDEDOR".equals(usuario.getRol().getNombreRol()), "El usuario no es vendedor");
        usuario.setEstado((byte) 0);
    }
}
