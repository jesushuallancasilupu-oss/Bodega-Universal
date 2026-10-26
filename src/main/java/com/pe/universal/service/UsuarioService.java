package com.pe.universal.service;

import com.pe.universal.datos.Resumenes.UsuarioVista;
import com.pe.universal.manager.UsuarioManager;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {
    private final UsuarioManager gestor;
    public UsuarioService(UsuarioManager gestor) { this.gestor = gestor; }

    public UsuarioVista iniciarSesion(String correo, String contrasena) {
        return gestor.iniciarSesion(correo, contrasena);
    }
    public UsuarioVista registrarCliente(String nombres, String apellidos, String dni,
            String telefono, String correo, String contrasena) {
        return gestor.registrarCliente(nombres, apellidos, dni, telefono, correo, contrasena);
    }
    public UsuarioVista registrarVendedor(Integer supervisorId, String nombres, String apellidos,
            String dni, String telefono, String correo, String contrasena) {
        return gestor.registrarVendedor(supervisorId, nombres, apellidos, dni, telefono, correo, contrasena);
    }
    public List<UsuarioVista> listarVendedores(Integer supervisorId) {
        return gestor.listarVendedores(supervisorId);
    }
    public List<UsuarioVista> listarClientes(Integer usuarioId) {
        return gestor.listarClientes(usuarioId);
    }
    public void desactivarVendedor(Integer supervisorId, Integer vendedorId) {
        gestor.desactivarVendedor(supervisorId, vendedorId);
    }
}
